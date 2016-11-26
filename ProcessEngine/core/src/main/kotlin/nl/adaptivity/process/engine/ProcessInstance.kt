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
import net.devrieze.util.HandleMap.MutableHandleAware
import net.devrieze.util.security.SecureObject
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

  interface Builder<T : ProcessTransaction<T>> {
    var handle: ComparableHandle<out ProcessInstance<T>>
    var owner: Principal
    var processModel: ProcessModelImpl
    var instancename: String?
    var uuid: UUID
    var state: State?
    val children: MutableList<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>
    val   inputs: MutableList<ProcessData>
    val  outputs: MutableList<ProcessData>
    fun build(data: ProcessEngineDataAccess<T>): ProcessInstance<T>
  }

  data class BaseBuilder<T: ProcessTransaction<T>>(override var handle: ComparableHandle<out ProcessInstance<T>>, override var owner: Principal, override var processModel: ProcessModelImpl, override var instancename: String?, override var uuid: UUID, override var state: State?) : Builder<T> {
    override val children = mutableListOf<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>()
    override val   inputs = mutableListOf<ProcessData>()
    override val  outputs = mutableListOf<ProcessData>()
    override fun build(data: ProcessEngineDataAccess<T>): ProcessInstance<T> {
      return ProcessInstance<T>(data, this)
    }
  }

  class ExtBuilder<T: ProcessTransaction<T>>(private val base: ProcessInstance<T>) : Builder<T> {
    override var handle by overlay { base.handle }

    override var owner by overlay { base.owner }
    override var processModel by overlay { base.processModel }
    override var instancename by overlay { base.name }
    override var uuid by overlay { base.uuid }
    override var state by overlay { base.state }
    override val children by lazy { base.children.toMutableList() }
    override val inputs by lazy { base.inputs.toMutableList() }
    override val outputs by lazy { base.outputs.toMutableList() }

    override fun build(data: ProcessEngineDataAccess<T>): ProcessInstance<T> {
      return ProcessInstance(data, this)
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

    override val handleValue = processInstance.handle.handleValue

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

  val children: Sequence<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>
    get() {
      return (active.asSequence() + finished.asSequence() + results.asSequence())
    }

  val active: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  val finished: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  private val finishedCount: Int
    get() = results.size

  val results: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>

  private val mJoins: HashMap<JoinImpl, ComparableHandle<out SecureObject<JoinInstance<T>>>>

  override var handle: ComparableHandle<out ProcessInstance<T>>
    private set

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  val inputs: List<ProcessData>

  val outputs: List<ProcessData>

  val name: String?

  override val owner: Principal

  val state: State?

  val uuid: UUID

  val ref: ProcessInstanceRef
    get() = ProcessInstanceRef(this)

  private constructor(data: ProcessEngineDataAccess<T>, builder: Builder<T>) {
    name = builder.instancename
    owner = builder.owner
    uuid = builder.uuid
    processModel = builder.processModel
    state = builder.state
    handle = Handles.handle(builder.handle)

    val joins = hashMapOf<JoinImpl, ComparableHandle<out SecureObject<JoinInstance<T>>>>()

    val threads = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>()

    val endResults = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>()

    val finishedNodes = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>>()

    val nodes = builder.children
          .map { data.nodeInstances[it].mustExist(it).withPermission() }

    nodes.forEach { instance ->
      if (instance is JoinInstance) {
        joins.put(instance.node, instance.handle)
      }

      if (instance.state.isFinal && instance.node is EndNode<*, *>) {
        endResults.add(instance.handle)
      } else {
        threads.add(instance.handle)
      }

    }
    nodes.forEach { instance ->
      instance.directPredecessors.forEach { pred ->
        if (threads.remove(pred)) {
          finishedNodes.add(pred)
        }
      }
    }

    active = threads
    results = endResults
    finished = finishedNodes

    mJoins = joins
    inputs = builder.inputs.toList()
    outputs = builder.outputs.toList()
  }

  internal constructor(handle: Handle<ProcessInstance<T>>, owner: Principal, processModel: ProcessModelImpl, name: String?, uUid: UUID, state: State?) {
    this.handle = Handles.handle(handle)
    this.processModel = processModel
    this.owner = owner
    uuid = uUid
    this.name = name
    this.state = state ?: State.NEW
    active = ArraySet()
    results = ArraySet()
    finished = ArraySet()
    mJoins = HashMap()
    inputs = emptyList()
    outputs = emptyList()
  }

  constructor(owner: Principal, processModel: ProcessModelImpl, name: String, uUid: UUID, state: State?) {
    this.processModel = processModel
    this.name = name
    handle = Handles.getInvalid()
    uuid = uUid
    this.owner = owner
    mJoins = HashMap()
    active = ArraySet()
    results = ArraySet()
    finished = ArraySet()
    this.state = state ?: State.NEW
    inputs = emptyList()
    outputs = emptyList()
  }

  override fun withPermission() = this

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: T):ProcessInstance<T> {
    if (state != State.NEW || active.isNotEmpty()) {
      throw IllegalStateException("The instance already appears to be initialised")
    }

    return update(transaction) {
      processModel.startNodes.forEach { node ->
        val nodeInstance = ProcessNodeInstance(node, Handles.getInvalid<ProcessNodeInstance<T>>(), this@ProcessInstance)
        val handle = transaction.writableEngineData.nodeInstances.put(nodeInstance)
        children.add(Handles.handle(handle)) // function needed to make the handle comparable
      }
      state = State.INITIALIZED
    }
  }

  fun update(transaction: T, body: Builder<T>.() -> Unit):ProcessInstance<T> {
    val origHandle = handle
    return builder().apply { body() }.build(transaction.readableEngineData).apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.instances[handle] = this
    }
  }

  fun builder() = ExtBuilder<T>(this)

  @Synchronized @Throws(SQLException::class)
  fun finish(transaction: T):ProcessInstance<T> {
    // This needs to update first as at this point the node state may not be valid.
    // TODO reduce the need to do a double update.
    update(transaction) {}.let { newInstance ->
      if (newInstance.finishedCount >= processModel.endNodeCount) {
        // TODO mark and store results
        return newInstance.update(transaction) {
          state = State.FINISHED
        }.apply {
          transaction.commit()
          // TODO don't remove old transactions
          transaction.writableEngineData.handleFinishedInstance(handle)
        }

      } else {
        return newInstance
      }
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun getNodeInstance(transaction: T, identifiable: Identifiable): ProcessNodeInstance<T>? {
    return children.map { handle ->
      val nodeInstances = transaction.readableEngineData.nodeInstances
      val instance = nodeInstances[handle].mustExist(handle).withPermission()
      if (identifiable.id == instance.node.id) {
        instance
      } else {
        instance.getPredecessor(transaction, identifiable.id)?.let { nodeInstances[it].mustExist(it).withPermission() }
      }
    }.filterNotNull().firstOrNull()
  }

  @Synchronized @Throws(SQLException::class)
  private fun getJoinInstance(transaction: T, join: JoinImpl, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance<T>>>): JoinInstance<T> {
    synchronized(mJoins) {
      val nodeInstances = transaction.writableEngineData.nodeInstances

      val joinInstance = mJoins[join]?.let {nodeInstances[it]?.withPermission() as JoinInstance<T> }
      if (joinInstance==null) {
        val joinHandle= nodeInstances.put(JoinInstance(join, listOf(predecessor), this.handle, owner))

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

  @Synchronized override fun setHandleValue(handleValue: Long) {
    if (handle.handleValue!=handleValue) {
      if (handleValue==-1L) { throw IllegalArgumentException("Setting the handle to invalid is not allowed") }
      if (handle.valid) throw IllegalStateException("Handles are not allowed to change")
      handle = Handles.handle(handleValue)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun start(transaction: T, messageService: IMessageService<*, T, ProcessNodeInstance<T>>, payload: Node?):ProcessInstance<T> {
    return transaction.writableEngineData.let { engineData ->
      (if (state == null) initialize(transaction) else this)
            .update(transaction) {
              if (active.isEmpty()) {
                throw IllegalStateException("No starting nodes in process")
              }
              state = State.STARTED

              inputs.addAll(processModel.toInputs(payload))
            }.apply {
        active.asSequence()
              .map { engineData.nodeInstances[it].mustExist(it).withPermission() }
              .filter { !it.state.isFinal }
              .forEach { it.provideTask(transaction, messageService) }

      }
    }
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
    val newNode = transaction.commit(node.finishTask(transaction, resultPayload))

    handleFinishedState(transaction, messageService, newNode)
    return newNode

  }

  @Synchronized @Throws(SQLException::class)
  private fun handleFinishedState(transaction: T,
                                  messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                                  node: ProcessNodeInstance<T>):ProcessInstance<T> {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.node is EndNode<*, *>) {
        return finish(transaction).apply {
          assert(node.handle !in active)
          assert(node.handle !in finished)
          assert(node.handle in results)
        }
      } else {
        return startSuccessors(transaction, messageService, node)
      }
    } catch (e: RuntimeException) {
      transaction.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    } catch (e: SQLException) {
      transaction.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    }
    return this
  }

  @Synchronized @Throws(SQLException::class)
  private fun startSuccessors(transaction: T,
                              messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                              predecessor: ProcessNodeInstance<T>):ProcessInstance<T> {

    val startedTasks = ArrayList<ProcessNodeInstance<T>>(predecessor.node.successors.size)
    val joinsToEvaluate = ArrayList<JoinInstance<T>>()

    val nodeInstances = transaction.writableEngineData.nodeInstances
    val self = update(transaction) {
      predecessor.node.successors.asSequence()
            .map { successorId ->
              val pni = createProcessNodeInstance(transaction, predecessor, processModel.getNode(successorId))
              Handles.handle(nodeInstances.put(pni))
            }.forEach { instanceHandle ->
        run {
          nodeInstances[instanceHandle].mustExist(instanceHandle).withPermission()
        }.let { instance ->
          children.add(instanceHandle)
          if (instance is JoinInstance) {
            joinsToEvaluate.add(instance)
          } else {
            startedTasks.add(instance)
          }
        }
      }
    }

    // Commit the registration of the follow up nodes before starting them.
    transaction.commit()
    startedTasks.forEach { task -> task.provideTask(transaction, messageService) }

    joinsToEvaluate.forEach { join ->
      join.startTask(transaction, messageService)
    }
    return self
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
  @Deprecated("Not needed", ReplaceWith("node.failTask<*>(transaction, cause, messageService)"))
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
    return active.asSequence()
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
    active.asSequence()
          .map { nodeInstances[it].mustExist(it).withPermission() }
          .forEach { addDirectSuccessor(it, predecessor.handle) }

    return result
  }

  @Synchronized @Throws(XmlException::class)
  fun serialize(transaction: T, writer: XmlWriter) {
    //
    writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
      writeAttribute("handle", if (!handle.valid) null else java.lang.Long.toString(handle.handleValue))
      writeAttribute("name", name)
      writeAttribute("processModel", java.lang.Long.toString(processModel.handleValue))
      writeAttribute("owner", owner.name)
      writeAttribute("state", state!!.name)

      smartStartTag(Constants.PROCESS_ENGINE_NS, "inputs") {
        inputs.forEach { it.serialize(this) }
      }

      writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "outputs") {
        outputs.forEach { it.serialize(this) }
      }

      writeListIfNotEmpty(active, Constants.PROCESS_ENGINE_NS, "active") {
        writeActiveNodeRef(transaction, it)
      }

      writeListIfNotEmpty(finished, Constants.PROCESS_ENGINE_NS, "finished") {
        writeActiveNodeRef(transaction, it)
      }

      writeListIfNotEmpty(results, Constants.PROCESS_ENGINE_NS, "endresults") {
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

    // make a copy as the list may be changed due to tickling.
    for (handle in active.toList()) {
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
    if (active.isEmpty()) {
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
