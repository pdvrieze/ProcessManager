/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.*
import net.devrieze.util.HandleMap.HandleAware
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.JoinImpl
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.FileNotFoundException
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class ProcessInstance<T : Transaction> : HandleAware<ProcessInstance<T>>, SecureObject, XmlSerializable {

  enum class State {
    NEW,
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED
  }

  class ProcessInstanceRef : Handle<ProcessInstance<*>>, XmlSerializable {

    override var handleValue = -1L
      private set(value: Long) {
        field = value
      }

    var processModel: Long = 0
      private set

    var name: String? = null

    var uuid: String? = null

    constructor() {
      // empty constructor;
    }

    constructor(processInstance: ProcessInstance<*>) {
      setHandle(processInstance.handleValue)
      setProcessModel(processInstance.processModel.handle)
      name = processInstance.name.let { if (it.isNullOrBlank()) "${processInstance.processModel.name} instance $handleValue" else it }
      uuid = processInstance.uuid?.toString()
    }

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
      out.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX)
      if (handleValue >= 0) {
        out.attribute("", "handle", "", java.lang.Long.toString(handleValue))
      }
      out.writeAttribute("processModel", processModel)
      out.writeAttribute("name", name)
      out.writeAttribute("uuid", uuid)
      out.endTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX)
    }

    fun setHandle(handle: Long) {
      handleValue = handle
    }

    override val valid: Boolean
      get() = handleValue >= 0L

    fun setProcessModel(processModel: Handle<out ProcessModelImpl>) {
      this.processModel = processModel.handleValue
    }

  }

  val processModel: ProcessModelImpl

  private val mThreads: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mFinishedNodes: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mEndResults: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mJoins: HashMap<JoinImpl, ComparableHandle<out JoinInstance<T>>>

  private var mHandle = -1L

  val engine: ProcessEngine<T>// XXX actually introduce a generic parameter for transactions

  private var mInputs: MutableList<ProcessData> = ArrayList()

  private val mOutputs = ArrayList<ProcessData>()

  val name: String?

  val owner: Principal

  var state: State? = null
    private set

  val uuid: UUID?

  @Deprecated("")
  internal constructor(handle: Long, owner: Principal, processModel: ProcessModelImpl, name: String?, uUid: UUID, state: State, engine: ProcessEngine<T>) : this(
        Handles.handle<ProcessInstance<T>>(handle),
        owner,
        processModel,
        name,
        uUid,
        state,
        engine) {
  }

  internal constructor(handle: Handle<ProcessInstance<T>>, owner: Principal, processModel: ProcessModelImpl, name: String?, uUid: UUID, state: State?, engine: ProcessEngine<T>) {
    mHandle = handle.handleValue
    this.processModel = processModel
    this.owner = owner
    uuid = uUid
    this.engine = engine
    this.name = name
    this.state = state ?: State.NEW
    mThreads = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
    mJoins = HashMap<JoinImpl, ComparableHandle<out JoinInstance<T>>>()
    mEndResults = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
    mFinishedNodes = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
  }

  constructor(owner: Principal, processModel: ProcessModelImpl, name: String, uUid: UUID, state: State?, engine: ProcessEngine<T>) {
    this.processModel = processModel
    this.name = name
    uuid = uUid
    this.engine = engine
    mThreads = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
    this.owner = owner
    mJoins = HashMap<JoinImpl, ComparableHandle<out JoinInstance<T>>>()
    mEndResults = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
    mFinishedNodes = ArraySet<ComparableHandle<out ProcessNodeInstance<T>>>()
    this.state = state ?: State.NEW
  }

  @Synchronized @Throws(SQLException::class)
  internal fun setChildren(transaction: T, children: Collection<Handle<out ProcessNodeInstance<T>>>) {
    mJoins.clear() // TODO proper synchronization
    mThreads.clear()
    mFinishedNodes.clear()
    mEndResults.clear()

    val nodes = ArrayList<ProcessNodeInstance<T>>()
    val threads = TreeSet<ComparableHandle<out ProcessNodeInstance<T>>>()

    for (handle in children) {
      val inst = engine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL).mustExist(handle)
      nodes.add(inst)
      threads.add(Handles.handle(handle))
      if (inst is JoinInstance<*>) {
        val joinInst = inst as JoinInstance<T>?
        mJoins.put(joinInst!!.node, joinInst.handle)
      }
    }

    for (instance in nodes) {

      if (instance.node is EndNode<*, *>) {
        val handle = instance.handle
        mEndResults.add(handle)
        threads.remove(handle)
      }

      val preds = instance.directPredecessors
      for (pred in preds) {
        if (threads.contains(pred)) {
          mFinishedNodes.add(pred)
          threads.remove(pred)
        }
      }

    }
    mThreads.addAll(threads)
  }

  @Synchronized @Throws(SQLException::class)
  internal fun setThreads(transaction: T, threads: Collection<ComparableHandle<out ProcessNodeInstance<T>>>) {
    if (!CollectionUtil.hasNull(threads)) {
      throw NullPointerException()
    }
    mThreads.addAll(threads)
  }

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: T) {
    if (state != State.NEW || mThreads.size > 0) {
      throw IllegalStateException("The instance already appears to be initialised")
    }
    for (node in processModel.startNodes) {
      val instance = ProcessNodeInstance(node, Handles.getInvalid<ProcessNodeInstance<T>>(), this)
      val handle = engine.registerNodeInstance(transaction, instance) ?: throw NullPointerException()
      mThreads.add(handle)
    }
    state = State.INITIALIZED
    engine.updateStorage(transaction, this)
  }

  @Synchronized @Throws(SQLException::class)
  fun finish(transaction: T) {
    val mFinished = finishedCount
    if (mFinished >= processModel.endNodeCount) {
      // TODO mark and store results
      state = State.FINISHED
      engine.updateStorage(transaction, this)
      transaction.commit()
      engine.finishInstance(transaction, handle)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun getNodeInstance(transaction: T, identifiable: Identifiable): ProcessNodeInstance<T>? {
    for (handle in CollectionUtil.combine(mEndResults, mFinishedNodes, mThreads)) {
      val instance = engine.getNodeInstance(transaction,
                                            handle,
                                            SecurityProvider.SYSTEMPRINCIPAL) ?: throw IllegalStateException("Member node $handle could not be found")
      if (identifiable.id == instance.node.id) {
        return instance
      }
      val result = instance.getPredecessor(transaction, identifiable.id)
      if (result != null) {
        return engine.getNodeInstance(transaction, result, SecurityProvider.SYSTEMPRINCIPAL)
      }
    }
    return null
  }

  private val finishedCount: Int
    get() = mEndResults.size

  @Synchronized @Throws(SQLException::class)
  fun getJoinInstance(transaction: T,
                      join: JoinImpl,
                      predecessor: ComparableHandle<out ProcessNodeInstance<T>>): JoinInstance<T> {
    synchronized(mJoins) {
      val joinHandle = mJoins[join]
      var result: JoinInstance<T>? = if (joinHandle == null) null else engine.getNodeInstance(transaction,
                                                                                              joinHandle,
                                                                                              SecurityProvider.SYSTEMPRINCIPAL) as JoinInstance<T>?
      if (result == null) {
        val predecessors = ArrayList<ComparableHandle<out ProcessNodeInstance<T>>>(join.predecessors.size)
        predecessors.add(predecessor)
        result = JoinInstance(transaction, join, predecessors, this)
        val resultHandle = engine.registerNodeInstance<JoinInstance<T>>(transaction, result)
        mJoins.put(join, resultHandle)
      } else {
        result.addPredecessor(transaction, predecessor)
      }
      return result
    }
  }

  @Synchronized fun removeJoin(join: JoinInstance<T>) {
    mJoins.remove(join.node)
  }

  @get:Synchronized val handleValue: Long get() {
    return mHandle
  }

  override val handle: Handle<out ProcessInstance<T>>
    @Synchronized get() = Handles.handle(mHandle)

  @Synchronized override fun setHandleValue(handleValue: Long) {
    mHandle = handleValue
  }

  val ref: ProcessInstanceRef
    get() = ProcessInstanceRef(this)

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  var inputs: List<ProcessData>
    @Synchronized get() = mInputs
    internal set(inputs) {
      mInputs.clear()
      mInputs.addAll(inputs)
    }

  @Synchronized @Throws(SQLException::class)
  fun start(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>, payload: Node) {
    if (state == null) {
      initialize(transaction)
    }
    val threads = ArrayList(mThreads)
    if (threads.size == 0) {
      throw IllegalStateException("No starting nodes in process")
    }
    mInputs = processModel.toInputs(payload)
    for (hnode in threads) {
      val node = engine.getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hnode)
      provideTask(transaction, messageService, node)
    }
    state = State.STARTED
    engine.updateStorage(transaction, this)
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance.  */
  fun reinitialize(transaction: T) {
    // TODO Auto-generated method stub

  }

  @Synchronized @Throws(SQLException::class)
  fun provideTask(transaction: T,
                  messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                  node: ProcessNodeInstance<T>) {
    assert(node.handle.valid)
    if (node.provideTask(transaction, messageService)) {
      takeTask(transaction, messageService, node)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun takeTask(transaction: T,
               messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
               node: ProcessNodeInstance<T>) {
    if (node.takeTask(transaction, messageService)) {
      startTask(transaction, messageService, node)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun startTask(transaction: T,
                messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                node: ProcessNodeInstance<T>) {
    if (node.startTask(transaction, messageService)) {
      finishTask(transaction, messageService, node, null)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun finishTask(transaction: T,
                 messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                 node: ProcessNodeInstance<T>,
                 resultPayload: Node?) {
    if (node.state === NodeInstanceState.Complete) {
      throw IllegalStateException("Task was already complete")
    }
    node.finishTask(transaction, resultPayload)
    // Make sure the finish is recorded.
    transaction.commit()

    handleFinishedState(transaction, messageService, node)

  }

  @Synchronized @Throws(SQLException::class)
  private fun handleFinishedState(transaction: T,
                                  messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                                  node: ProcessNodeInstance<T>) {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.node is EndNode<*, *>) {
        mEndResults.add(node.handle)
        mThreads.remove(node.handle)
        finish(transaction)
      } else {
        startSuccessors(transaction, messageService, node)
      }
    } catch (e: RuntimeException) {
      transaction.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    } catch (e: SQLException) {
      transaction.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    }

  }

  @Synchronized @Throws(SQLException::class)
  private fun startSuccessors(transaction: T,
                              messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                              predecessor: ProcessNodeInstance<T>) {
    if (!mFinishedNodes.contains(predecessor.handle)) {
      mFinishedNodes.add(predecessor.handle)
    }
    mThreads.remove(predecessor.handle)

    val startedTasks = ArrayList<ProcessNodeInstance<T>>(predecessor.node.successors.size)
    val joinsToEvaluate = ArrayList<JoinInstance<T>>()
    for (successorNode in predecessor.node.successors) {
      val instance = createProcessNodeInstance(transaction, predecessor, processModel.getNode(successorNode))
      if (!instance.handle.valid) {
        engine.registerNodeInstance(transaction, instance)
      }
      val instanceHandle = instance.handle
      if (instance is JoinInstance<*>) {
        if (!mThreads.contains(instanceHandle)) {
          mThreads.add(instanceHandle)
        }
        joinsToEvaluate.add(instance as JoinInstance<T>)
      } else {
        mThreads.add(instanceHandle)
        startedTasks.add(instance)
      }
    }
    // Commit the registration of the follow up nodes before starting them.
    transaction.commit()
    for (task in startedTasks) {
      provideTask(transaction, messageService, task)
    }
    for (join in joinsToEvaluate) {
      startTask(transaction, messageService, join)
      if (join.state.isFinal) {
        val joinHandle = join.handle
        mThreads.remove(joinHandle)
        mFinishedNodes.add(joinHandle)
      }
    }
  }

  @Synchronized @Throws(SQLException::class)
  private fun createProcessNodeInstance(transaction: T,
                                        predecessor: ProcessNodeInstance<T>,
                                        node: ExecutableProcessNode): ProcessNodeInstance<T> {
    if (node is JoinImpl) {
      return getJoinInstance(transaction, node, predecessor.handle)
    } else {
      return ProcessNodeInstance(node, predecessor.handle, this)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun failTask(transaction: T,
               messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
               node: ProcessNodeInstance<T>,
               cause: Throwable) {
    node.failTask(transaction, cause)
  }

  @Synchronized @Throws(SQLException::class)
  fun cancelTask(transaction: T,
                 messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                 node: ProcessNodeInstance<T>) {
    node.cancelTask(transaction)
  }

  @Synchronized @Throws(SQLException::class)
  fun getActivePredecessorsFor(transaction: T, join: JoinImpl): Collection<ProcessNodeInstance<T>> {
    val activePredecesors = ArrayList<ProcessNodeInstance<T>>(Math.min(join.predecessors.size, mThreads.size))
    for (hnode in mThreads) {
      val node = engine.getNodeInstance(transaction,
                                        hnode,
                                        SecurityProvider.SYSTEMPRINCIPAL) ?: throw IllegalStateException("Missing node " + hnode)
      if (node.node.isPredecessorOf(join)) {
        activePredecesors.add(node)
      }
    }
    return activePredecesors
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(transaction: T,
                          predecessor: ProcessNodeInstance<T>): Collection<Handle<out ProcessNodeInstance<T>>> {
    val result = ArrayList<Handle<out ProcessNodeInstance<T>>>(predecessor.node.successors.size)
    for (hcandidate in mThreads) {
      val candidate = engine.getNodeInstance(transaction, hcandidate, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hcandidate)
      addDirectSuccessor(transaction, result, candidate, predecessor.handle)
    }
    return result
  }

  @Synchronized @Throws(SQLException::class)
  private fun addDirectSuccessor(transaction: T,
                                 result: ArrayList<Handle<out ProcessNodeInstance<T>>>,
                                 candidate: ProcessNodeInstance<T>,
                                 predecessor: Handle<out ProcessNodeInstance<T>>) {
    // First look for this node, before diving into it's children
    for (node in candidate.directPredecessors) {
      if (node.handleValue == predecessor.handleValue) {
        result.add(candidate.handle)
        return  // Assume that there is no further "successor" down the chain
      }
    }
    for (hnode in candidate.directPredecessors) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      val node = candidate.processInstance.engine.getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hnode)
      addDirectSuccessor(transaction, result, node, predecessor)
    }
  }

  val active: Collection<Handle<out ProcessNodeInstance<T>>>
    @Synchronized get() = ArrayList(mThreads)

  val finished: Collection<Handle<out ProcessNodeInstance<T>>>
    @Synchronized get() = ArrayList(mFinishedNodes)

  val results: Collection<Handle<out ProcessNodeInstance<T>>>
    @Synchronized get() = ArrayList(mEndResults)

  @Synchronized @Throws(XmlException::class)
  override fun serialize(writer: XmlWriter) {
    //
    writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX)
    try {
      writer.writeAttribute("handle", if (mHandle < 0) null else java.lang.Long.toString(mHandle))
      writer.writeAttribute("name", name)
      writer.writeAttribute("processModel", java.lang.Long.toString(processModel.handleValue))
      writer.writeAttribute("owner", owner.name)
      writer.writeAttribute("state", state!!.name)

      writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "inputs", null)
      try {
        for (input in mInputs) {
          input.serialize(writer)
        }
      } finally {
        writer.endTag(Constants.PROCESS_ENGINE_NS, "inputs", null)
      }

      writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "outputs", null)
      try {
        for (output in mOutputs) {
          output.serialize(writer)
        }
      } finally {
        writer.endTag(Constants.PROCESS_ENGINE_NS, "outputs", null)
      }

      try {
        engine.startTransaction().use { transaction ->

          if (mThreads.size > 0) {
            try {
              writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "active", null)
              for (active in mThreads) {
                writeActiveNodeRef(transaction, writer, active)
              }
            } finally {
              writer.endTag(Constants.PROCESS_ENGINE_NS, "active", null)
            }
          }
          if (mFinishedNodes.size > 0) {
            try {
              writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "finished", null)
              for (finished in mFinishedNodes) {
                writeActiveNodeRef(transaction, writer, finished)
              }
            } finally {
              writer.endTag(Constants.PROCESS_ENGINE_NS, "finished", null)
            }
          }
          if (mEndResults.size > 0) {
            try {
              writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "endresults", null)
              for (result in mEndResults) {
                writeResultNodeRef(transaction, writer, result)
              }
            } finally {
              writer.endTag(Constants.PROCESS_ENGINE_NS, "endresults", null)
            }
          }
          transaction.commit()
        }
      } catch (e: SQLException) {
        throw XmlException(e)
      }

    } finally {
      writer.endTag(Constants.PROCESS_ENGINE_NS, "processInstance", null)
    }

  }

  @Throws(XmlException::class, SQLException::class)
  private fun writeActiveNodeRef(transaction: T, out: XmlWriter, handleNodeInstance: Handle<out ProcessNodeInstance<T>>) {
    val nodeInstance = engine.getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL).mustExist(handleNodeInstance)
    out.startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null)
    try {
      writeNodeRefCommon(out, nodeInstance)
    } finally {
      out.endTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null)
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun writeResultNodeRef(transaction: T,
                                 out: XmlWriter,
                                 handleNodeInstance: Handle<out ProcessNodeInstance<T>>) {
    val nodeInstance = engine.getNodeInstance(transaction,
                                              handleNodeInstance,
                                              SecurityProvider.SYSTEMPRINCIPAL) ?: throw IllegalStateException("Missing node " + handleNodeInstance)
    out.startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null)
    try {
      writeNodeRefCommon(out, nodeInstance)
      out.startTag(Constants.PROCESS_ENGINE_NS, "results", null)
      try {
        val results = nodeInstance.results
        for (result in results) {
          result.serialize(out)
        }
      } finally {
        out.endTag(Constants.PROCESS_ENGINE_NS, "results", null)
      }
    } finally {
      out.endTag(Constants.PROCESS_ENGINE_NS, "nodeinstance", null)
    }
  }

  fun setOutputs(outputs: List<ProcessData>) {
    mOutputs.clear()
    mOutputs.addAll(outputs)
  }

  /**
   * Trigger the instance to reactivate pending tasks.
   * @param transaction The database transaction to use
   * *
   * @param messageService The message service to use for messenging.
   */
  @Throws(FileNotFoundException::class)
  fun tickle(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>) {
    val threads = ArrayList(mThreads) // make a copy as the list may be changed due to tickling.
    for (handle in threads) {
      try {
        engine.tickleNode(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL)
        val instance = engine.getNodeInstance(transaction,
                                              handle,
                                              SecurityProvider.SYSTEMPRINCIPAL) ?: throw IllegalStateException("Missing Node Instance " + handle)
        val instanceState = instance.state
        if (instanceState.isFinal) {
          handleFinishedState(transaction, messageService, instance)
        }
      } catch (e: SQLException) {
        Logger.getLogger(javaClass.name).log(Level.WARNING, "Error when tickling process instance", e)
      }

    }
    if (mThreads.isEmpty()) {
      try {
        finish(transaction)
      } catch (e: SQLException) {
        Logger.getLogger(javaClass.name).log(Level.WARNING,
                                             "Error when trying to finish a process instance as result of tickling",
                                             e)
      }

    }
  }

  companion object {

    private val serialVersionUID = 1145452195455018306L

    @Throws(XmlException::class)
    private fun writeNodeRefCommon(out: XmlWriter, nodeInstance: ProcessNodeInstance<*>) {
      out.attribute(null, "nodeid", null, nodeInstance.node.id)
      out.attribute(null, "handle", null, java.lang.Long.toString(nodeInstance.getHandleValue()))
      out.attribute(null, "state", null, nodeInstance.state.toString())
      if (nodeInstance.state === NodeInstanceState.Failed) {
        val failureCause = nodeInstance.failureCause
        val value = if (failureCause == null) "<unknown>" else failureCause.javaClass.name + ": " + failureCause.message
        out.attribute(null, "failureCause", null, value)
      }

    }
  }

}
