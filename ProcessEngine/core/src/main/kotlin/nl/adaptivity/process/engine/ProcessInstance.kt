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
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.ProcessModel
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


class ProcessInstance<T : Transaction> : HandleAware<ProcessInstance<T>>, SecureObject<ProcessInstance<T>>, XmlSerializable {

  data class Builder<T: Transaction>(var handle: ComparableHandle<ProcessInstance<T>>, var owner: SimplePrincipal, var processModel: ProcessModelImpl, var instancename: String?, var uuid: UUID, var state: State) {
    val children = mutableListOf<Handle<ProcessNodeInstance<T>>>()
    val   inputs = mutableListOf<ProcessData>()
    val  outputs = mutableListOf<ProcessData>()
    fun build(transaction: T, engine: ProcessEngine<T>): ProcessInstance<T> {
      return ProcessInstance<T>(transaction, engine, this)
    }
  }

  enum class State {
    NEW,
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED
  }

  class ProcessInstanceRef(processInstance: ProcessInstance<*>) : Handle<ProcessInstance<*>>, XmlSerializable {

    override val handleValue = processInstance.handleValue

    val processModel: Handle<out ProcessModel<*,*>> = processInstance.processModel.handle

    var name: String = processInstance.name.let { if (it.isNullOrBlank()) "${processInstance.processModel.name} instance $handleValue" else it!! }

    var uuid: UUID = processInstance.uuid

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
      out.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
        if (handleValue >= 0) writeAttribute("handle", java.lang.Long.toString(handleValue))
        writeAttribute("processModel", processModel)
        writeAttribute("name", name)
        writeAttribute("uuid", uuid)
      }
    }

    override val valid: Boolean
      get() = handleValue >= 0L

  }

  val processModel: ProcessModelImpl

  private val mThreads: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mFinishedNodes: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mEndResults: MutableSet<ComparableHandle<out ProcessNodeInstance<T>>>

  private val mJoins: HashMap<JoinImpl, ComparableHandle<out JoinInstance<T>>>

  private var mHandleValue = -1L

  @Deprecated("This should be a parameter only")
  val engine: ProcessEngine<T>// XXX actually introduce a generic parameter for transactions

  private val mInputs: MutableList<ProcessData> = ArrayList()

  private val mOutputs = ArrayList<ProcessData>()

  val name: String?

  override val owner: Principal

  var state: State? = null
    private set

  val uuid: UUID

  private constructor(transaction: T, engine: ProcessEngine<T>, builder:Builder<T>):
        this(builder.handle, builder.owner, builder.processModel, builder.instancename, builder.uuid, builder.state, engine) {
    setChildren(transaction, builder.children)
    mInputs.addAll(builder.inputs)
    mOutputs.addAll(builder.outputs)
  }

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
    mHandleValue = handle.handleValue
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

  override fun withPermission() = this

  @Synchronized @Throws(SQLException::class)
  internal fun setChildren(transaction: T, children: Collection<Handle<out ProcessNodeInstance<T>>>) {
    mJoins.clear() // TODO proper synchronization
    mThreads.clear()
    mFinishedNodes.clear()
    mEndResults.clear()

    val threads = TreeSet<ComparableHandle<out ProcessNodeInstance<T>>>()

    val nodes = children
          .map { handle ->
            engine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL).mustExist(handle).apply {
              val h = if (this is JoinInstance) { val h2 = this.handle
                mJoins.put(this.node, h2)
                h2} else { this.handle }
              threads.add(h)
            }
          }

    nodes.forEach { instance ->
      if (instance.node is EndNode<*, *>) {
        instance.handle.let { handle ->
          mEndResults.add(handle)
          threads.remove(handle)
        }
      }

      instance.directPredecessors.forEach { pred ->
        if (threads.remove(pred)) {
          mFinishedNodes.add(pred)
        }
      }
    }
    mThreads.addAll(threads)
  }

  @Synchronized @Throws(SQLException::class)
  internal fun setThreads(transaction: T, threads: Collection<ComparableHandle<out ProcessNodeInstance<T>>>) {
    mThreads.addAll(threads)
  }

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: T) {
    if (state != State.NEW || mThreads.size > 0) {
      throw IllegalStateException("The instance already appears to be initialised")
    }

    processModel.startNodes.forEach { node ->
      mThreads.add(engine.registerNodeInstance(transaction, ProcessNodeInstance(node, Handles.getInvalid<ProcessNodeInstance<T>>(), this)))
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
    return (mEndResults.asSequence() + mFinishedNodes.asSequence() + mThreads.asSequence()).map { handle ->
      val instance = engine.getNodeInstance(transaction, handle, SecurityProvider.SYSTEMPRINCIPAL).mustExist(handle)
      if (identifiable.id == instance.node.id) {
        instance
      } else {
        instance.getPredecessor(transaction, identifiable.id)?.let {engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL)}
      }
    }.firstOrNull()
  }

  private val finishedCount: Int
    get() = mEndResults.size

  @Synchronized @Throws(SQLException::class)
  private fun getJoinInstance(transaction: T, join: JoinImpl, predecessor: ComparableHandle<out ProcessNodeInstance<T>>): JoinInstance<T> {
    synchronized(mJoins) {
      return mJoins[join]?.let {
        engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL) as JoinInstance<T>?
      }?.apply { addPredecessor(transaction, predecessor) } ?: run {
        return JoinInstance(join, listOf(predecessor), this).apply {
          mJoins.put(join, engine.registerNodeInstance<JoinInstance<T>>(transaction, this))
        }
      }
    }
  }

  @Synchronized fun removeJoin(join: JoinInstance<T>) {
    mJoins.remove(join.node)
  }

  @get:Synchronized val handleValue: Long get() {
    return mHandleValue
  }

  override val handle: Handle<out ProcessInstance<T>>
    @Synchronized get() = Handles.handle(mHandleValue)

  @Synchronized override fun setHandleValue(handleValue: Long) {
    mHandleValue = handleValue
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
  fun start(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>, payload: Node?) {
    if (state == null) {
      initialize(transaction)
    }

    mThreads.let { threads ->

      if (threads.isEmpty()) {
        throw IllegalStateException("No starting nodes in process")
      }

      threads.asSequence()
            .map { engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL).mustExist(it) }
            .forEach { provideTask(transaction, messageService, it) }
    }

    mInputs.apply { clear() }.addAll(processModel.toInputs(payload))
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

    predecessor.node.successors.asSequence()
          .map { successorId -> createProcessNodeInstance(transaction, predecessor, processModel.getNode(successorId)) }
          .forEach { instance ->

      if (!instance.handle.valid) {
        engine.registerNodeInstance(transaction, instance)
      }

      val instanceHandle = instance.handle
      if (instance is JoinInstance) {
        if (!mThreads.contains(instanceHandle)) {
          mThreads.add(instanceHandle)
        }
        joinsToEvaluate.add(instance)
      } else {
        mThreads.add(instanceHandle)
        startedTasks.add(instance)
      }
    }

    // Commit the registration of the follow up nodes before starting them.
    transaction.commit()
    startedTasks.forEach { task -> provideTask(transaction, messageService, task) }

    joinsToEvaluate.forEach { join ->
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
    return mThreads.asSequence()
          .map { engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL).mustExist(it) }
          .filter { it.node.isPredecessorOf(join) }
          .toList()
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(transaction: T, predecessor: ProcessNodeInstance<T>): Collection<Handle<out ProcessNodeInstance<T>>> {

    val result = ArrayList<Handle<out ProcessNodeInstance<T>>>(predecessor.node.successors.size)

    fun addDirectSuccessor(candidate: ProcessNodeInstance<T>,
                            predecessor: Handle<out ProcessNodeInstance<T>>) {

      // First look for this node, before diving into it's children
      candidate.directPredecessors.asSequence()
            .filter { it.handleValue == predecessor.handleValue }
            .forEach { node ->
              result.add(candidate.handle)
              return  // Assume that there is no further "successor" down the chain
            }
      for (hnode in candidate.directPredecessors) {
        // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
        val node = candidate.processInstance.engine.getNodeInstance(transaction, hnode, SecurityProvider.SYSTEMPRINCIPAL).mustExist(hnode)
        addDirectSuccessor(node, predecessor)
      }
    }


    mThreads.asSequence()
          .map { engine.getNodeInstance(transaction, it, SecurityProvider.SYSTEMPRINCIPAL).mustExist(it) }
          .forEach { addDirectSuccessor(it, predecessor.handle) }

    return result
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
    writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
      writeAttribute("handle", if (mHandleValue < 0) null else java.lang.Long.toString(mHandleValue))
      writeAttribute("name", name)
      writeAttribute("processModel", java.lang.Long.toString(processModel.handleValue))
      writeAttribute("owner", owner.name)
      writeAttribute("state", state!!.name)

      smartStartTag(Constants.PROCESS_ENGINE_NS, "inputs") {
        mInputs.forEach { it.serialize(this) }
      }

      writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "outputs") {
        mOutputs.forEach { it.serialize(this) }
      }

      try {
        engine.startTransaction().use { transaction ->

          writeListIfNotEmpty(mThreads, Constants.PROCESS_ENGINE_NS, "active") {
            writeActiveNodeRef(transaction, it)
          }

          writeListIfNotEmpty(mFinishedNodes, Constants.PROCESS_ENGINE_NS, "finished") {
            writeActiveNodeRef(transaction, it)
          }

          writeListIfNotEmpty(mEndResults, Constants.PROCESS_ENGINE_NS, "endresults") {
            writeResultNodeRef(transaction, it)
          }

          transaction.commit()
        }
      } catch (e: SQLException) {
        throw XmlException(e)
      }
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeActiveNodeRef(transaction: T, handleNodeInstance: Handle<out ProcessNodeInstance<T>>) {
    val nodeInstance = engine.getNodeInstance(transaction, handleNodeInstance, SecurityProvider.SYSTEMPRINCIPAL).mustExist(handleNodeInstance)
    startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
      writeNodeRefCommon(nodeInstance)
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeResultNodeRef(transaction: T, handleNodeInstance: Handle<out ProcessNodeInstance<T>>) {
    val nodeInstance = engine.getNodeInstance(transaction, handleNodeInstance,
                                              SecurityProvider.SYSTEMPRINCIPAL) ?: throw IllegalStateException("Missing node " + handleNodeInstance)
    startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
      writeNodeRefCommon(nodeInstance)

      startTag(Constants.PROCESS_ENGINE_NS, "results") {
        nodeInstance.results.forEach { it.serialize(this) }
      }
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
    private fun XmlWriter.writeNodeRefCommon(nodeInstance: ProcessNodeInstance<*>) {
      attribute(null, "nodeid", null, nodeInstance.node.id)
      attribute(null, "handle", null, java.lang.Long.toString(nodeInstance.getHandleValue()))
      attribute(null, "state", null, nodeInstance.state.toString())
      if (nodeInstance.state === NodeInstanceState.Failed) {
        val failureCause = nodeInstance.failureCause
        val value = if (failureCause == null) "<unknown>" else failureCause.javaClass.name + ": " + failureCause.message
        attribute(null, "failureCause", null, value)
      }

    }
  }

}
