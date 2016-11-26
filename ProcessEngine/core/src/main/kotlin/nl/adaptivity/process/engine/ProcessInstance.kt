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

import net.devrieze.util.ArraySet
import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.HandleMap.MutableHandleAware
import net.devrieze.util.Handles
import net.devrieze.util.security.SecureObject
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


class ProcessInstance<T : ProcessTransaction<T>> : MutableHandleAware<ProcessInstance<T>>, SecureObject<ProcessInstance<T>> {

  data class Builder<T: ProcessTransaction<T>>(var handle: ComparableHandle<ProcessInstance<T>>, var owner: SimplePrincipal, var processModel: ProcessModelImpl, var instancename: String?, var uuid: UUID, var state: State) {
    val children = mutableListOf<Handle<ProcessNodeInstance<T>>>()
    val   inputs = mutableListOf<ProcessData>()
    val  outputs = mutableListOf<ProcessData>()
    fun build(transaction: T, engine: ProcessEngine<T>): ProcessInstance<T> {
      return ProcessInstance<T>(transaction, this)
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

  private val mThreads: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  private val mFinishedNodes: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  private val mEndResults: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  private val mJoins: HashMap<JoinImpl, ComparableHandle<out SecureObject<JoinInstance<T>>>>

  private var mHandleValue = -1L

  private val mInputs: MutableList<ProcessData> = ArrayList()

  private val mOutputs = ArrayList<ProcessData>()

  val name: String?

  override val owner: Principal

  var state: State? = null
    private set

  val uuid: UUID

  private constructor(transaction: T, builder:Builder<T>):
        this(builder.handle, builder.owner, builder.processModel, builder.instancename, builder.uuid, builder.state) {

    val threads = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>()

    val data = transaction.readableEngineData
    val nodes = builder.children
          .map { data.nodeInstances[it].mustExist(it).withPermission() }

    nodes.forEach { instance ->
      if (instance is JoinInstance) {
        mJoins.put(instance.node, instance.handle)
      }

      if (instance.node is EndNode<*, *>) {
        mEndResults.add(instance.handle)
      } else {
        threads.add(instance.handle)
      }

      instance.directPredecessors.forEach { pred ->
        if (threads.remove(pred)) {
          mFinishedNodes.add(pred)
        }
      }
    }
    mThreads.addAll(threads)

    mInputs.addAll(builder.inputs)
    mOutputs.addAll(builder.outputs)
  }

  internal constructor(handle: Handle<ProcessInstance<T>>, owner: Principal, processModel: ProcessModelImpl, name: String?, uUid: UUID, state: State?) {
    mHandleValue = handle.handleValue
    this.processModel = processModel
    this.owner = owner
    uuid = uUid
    this.name = name
    this.state = state ?: State.NEW
    mThreads = ArraySet()
    mJoins = HashMap()
    mEndResults = ArraySet()
    mFinishedNodes = ArraySet()
  }

  constructor(owner: Principal, processModel: ProcessModelImpl, name: String, uUid: UUID, state: State?) {
    this.processModel = processModel
    this.name = name
    uuid = uUid
    mThreads = ArraySet()
    this.owner = owner
    mJoins = HashMap()
    mEndResults = ArraySet()
    mFinishedNodes = ArraySet()
    this.state = state ?: State.NEW
  }

  override fun withPermission() = this

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: T) {
    if (state != State.NEW || mThreads.size > 0) {
      throw IllegalStateException("The instance already appears to be initialised")
    }

    processModel.startNodes.forEach { node ->
      val nodeInstance = ProcessNodeInstance(node, Handles.getInvalid<ProcessNodeInstance<T>>(), this)
      val handle = transaction.writableEngineData.nodeInstances.put(nodeInstance)
      mThreads.add(Handles.handle(handle)) // function needed to make the handle comparable
    }

    state = State.INITIALIZED
    transaction.writableEngineData.instances[handle] = this
  }

  @Synchronized @Throws(SQLException::class)
  fun finish(transaction: T) {
    val mFinished = finishedCount
    if (mFinished >= processModel.endNodeCount) {
      // TODO mark and store results
      state = State.FINISHED
      transaction.writableEngineData.instances[handle] = this
      transaction.commit()
      transaction.writableEngineData.instances.remove(handle)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun getNodeInstance(transaction: T, identifiable: Identifiable): ProcessNodeInstance<T>? {
    return (mEndResults.asSequence() + mFinishedNodes.asSequence() + mThreads.asSequence()).map { handle ->
      val nodeInstances = transaction.readableEngineData.nodeInstances
      val instance = nodeInstances[handle].mustExist(handle).withPermission()
      if (identifiable.id == instance.node.id) {
        instance
      } else {
        instance.getPredecessor(transaction, identifiable.id)?.let { nodeInstances[it].mustExist(it).withPermission() }
      }
    }.filterNotNull().firstOrNull()
  }

  private val finishedCount: Int
    get() = mEndResults.size

  @Synchronized @Throws(SQLException::class)
  private fun getJoinInstance(transaction: T, join: JoinImpl, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>): JoinInstance<T> {
    synchronized(mJoins) {
      val nodeInstances = transaction.writableEngineData.nodeInstances

      val joinInstance = mJoins[join]?.let {nodeInstances[it]?.withPermission() as JoinInstance<T> }
      if (joinInstance==null) {
        val joinHandle= nodeInstances.put(JoinInstance(join, listOf(predecessor), this))

        mJoins[join] = Handles.handle(joinHandle.handleValue)
        return nodeInstances[joinHandle] as JoinInstance<T>
      } else {
        return joinInstance.updateJoin(transaction) {
          predecessors.add(predecessor)
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
    transaction.writableEngineData.let { engineData ->
      mInputs.apply { clear() }.addAll(processModel.toInputs(payload))

      mThreads.let { threads ->

        if (threads.isEmpty()) {
          throw IllegalStateException("No starting nodes in process")
        }

        threads.asSequence()
              .map { engineData.nodeInstances[it].mustExist(it).withPermission() }
              .forEach { it.provideTask(transaction, messageService) }
      }

      state = State.STARTED
      engineData.instances[handle] = this
    }
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance.  */
  fun reinitialize(transaction: T) {
    // TODO Auto-generated method stub

  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.provideTask(transaction, messageService)"))
  fun provideTask(transaction: T,
                  messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                  node: ProcessNodeInstance<T>):ProcessNodeInstance<T> {
    assert(node.handle.valid) { "The handle is not valid: ${node.handle}" }
    return node.provideTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.takeTask(transaction, messageService)"))
  fun takeTask(transaction: T,
               messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
               node: ProcessNodeInstance<T>): ProcessNodeInstance<T> {
    return node.takeTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.startTask<*>(transaction, messageService)"))
  fun startTask(transaction: T,
                messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                node: ProcessNodeInstance<T>): ProcessNodeInstance<T> {
    return node.startTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  fun finishTask(transaction: T,
                 messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                 node: ProcessNodeInstance<T>,
                 resultPayload: Node?): ProcessNodeInstance<T> {
    if (node.state === NodeInstanceState.Complete) {
      throw IllegalStateException("Task was already complete")
    }
    // Make sure the finish is recorded.
    val newNodeState = transaction.commit(node.finishTask(transaction, resultPayload))

    handleFinishedState(transaction, messageService, newNodeState)

    return newNodeState

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

    val nodeInstances = transaction.writableEngineData.nodeInstances
    predecessor.node.successors.asSequence()
          .map { successorId ->
            val pni = createProcessNodeInstance(transaction, predecessor, processModel.getNode(successorId))
            Handles.handle(nodeInstances.put(pni))
          }.forEach { instanceHandle ->
            run {
              nodeInstances[instanceHandle].mustExist(instanceHandle).withPermission()
            }.let { instance ->

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
    }

    // Commit the registration of the follow up nodes before starting them.
    transaction.commit()
    startedTasks.forEach { task -> task.provideTask(transaction, messageService) }

    joinsToEvaluate.forEach { join ->
      join.startTask(transaction, messageService).let { join ->
        if (join.state.isFinal) {
          val joinHandle = join.handle
          mThreads.remove(joinHandle)
          mFinishedNodes.add(joinHandle)
        }
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
  @Deprecated("Not needed", ReplaceWith("node.failTask<*>(transaction, messageService)"))
  fun failTask(transaction: T,
               messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
               node: ProcessNodeInstance<T>,
               cause: Throwable) {
    node.failTask(transaction, cause)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.cancelTask<*>(transaction, messageService)"))
  fun cancelTask(transaction: T,
                 messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                 node: ProcessNodeInstance<T>) {
    node.cancelTask(transaction)
  }

  @Synchronized @Throws(SQLException::class)
  fun getActivePredecessorsFor(transaction: T, join: JoinImpl): Collection<ProcessNodeInstance<T>> {
    return mThreads.asSequence()
          .map { transaction.readableEngineData.nodeInstances[it].mustExist(it).withPermission() }
          .filter { it.node.isPredecessorOf(join) }
          .toList()
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(transaction: T, predecessor: ProcessNodeInstance<T>): Collection<Handle<out SecureObject<ProcessNodeInstance<T>>>> {

    val result = ArrayList<Handle<out SecureObject<ProcessNodeInstance<T>>>>(predecessor.node.successors.size)

    fun addDirectSuccessor(candidate: ProcessNodeInstance<T>,
                            predecessor: Handle<out SecureObject<ProcessNodeInstance<T>>>) {

      // First look for this node, before diving into it's children
      candidate.directPredecessors.asSequence()
            .filter { it.handleValue == predecessor.handleValue }
            .forEach { node ->
              result.add(candidate.handle)
              return  // Assume that there is no further "successor" down the chain
            }
      for (hnode in candidate.directPredecessors) {
        // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
        val node = transaction.readableEngineData.nodeInstances[hnode].mustExist(hnode).withPermission()
        addDirectSuccessor(node, predecessor)
      }
    }


    val nodeInstances = transaction.readableEngineData.nodeInstances
    mThreads.asSequence()
          .map { nodeInstances[it].mustExist(it).withPermission() }
          .forEach { addDirectSuccessor(it, predecessor.handle) }

    return result
  }

  val active: Collection<Handle<out SecureObject<ProcessNodeInstance<T>>>>
    @Synchronized get() = ArrayList(mThreads)

  val finished: Collection<Handle<out SecureObject<ProcessNodeInstance<T>>>>
    @Synchronized get() = ArrayList(mFinishedNodes)

  val results: Collection<Handle<out SecureObject<ProcessNodeInstance<T>>>>
    @Synchronized get() = ArrayList(mEndResults)

  @Synchronized @Throws(XmlException::class)
  fun serialize(transaction: T, writer: XmlWriter) {
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

      writeListIfNotEmpty(mThreads, Constants.PROCESS_ENGINE_NS, "active") {
        writeActiveNodeRef(transaction, it)
      }

      writeListIfNotEmpty(mFinishedNodes, Constants.PROCESS_ENGINE_NS, "finished") {
        writeActiveNodeRef(transaction, it)
      }

      writeListIfNotEmpty(mEndResults, Constants.PROCESS_ENGINE_NS, "endresults") {
        writeResultNodeRef(transaction, it)
      }
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeActiveNodeRef(transaction: T, handleNodeInstance: Handle<out SecureObject<ProcessNodeInstance<T>>>) {

    val nodeInstance = transaction.readableEngineData.nodeInstances[handleNodeInstance].mustExist(handleNodeInstance).withPermission()
    startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
      writeNodeRefCommon(nodeInstance)
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeResultNodeRef(transaction: T, handleNodeInstance: Handle<out SecureObject<ProcessNodeInstance<T>>>) {
    val nodeInstance = transaction.readableEngineData.nodeInstances[handleNodeInstance].mustExist(handleNodeInstance).withPermission()
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

    fun tickePredecessors(successor: ProcessNodeInstance<T>) {
      successor.directPredecessors.asSequence()
            .map { transaction.writableEngineData.nodeInstances[it]?.withPermission() }
            .filterNotNull()
            .forEach {
              tickePredecessors(it);
              it.tickle(transaction, messageService)
            }
    }

    val threads = ArrayList(mThreads) // make a copy as the list may be changed due to tickling.
    for (handle in threads) {
      try {
        transaction.writableEngineData.run {
          invalidateCachePNI(handle)
          val instance = nodeInstances[handle].mustExist(handle).withPermission()
          tickePredecessors(instance)
          val instanceState = instance.state
          if (instanceState.isFinal) {
            handleFinishedState(transaction, messageService, instance)
          }
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
