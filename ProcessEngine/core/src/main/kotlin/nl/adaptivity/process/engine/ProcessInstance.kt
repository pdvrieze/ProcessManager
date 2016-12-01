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
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
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


class ProcessInstance : MutableHandleAware<ProcessInstance>, SecureObject<ProcessInstance> {

  interface Builder {
    var handle: ComparableHandle<out ProcessInstance>
    var owner: Principal
    var processModel: ExecutableProcessModel
    var instancename: String?
    var uuid: UUID
    var state: State?
    val children: MutableList<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    val   inputs: MutableList<ProcessData>
    val  outputs: MutableList<ProcessData>
    fun build(data: ProcessEngineDataAccess): ProcessInstance
  }

  data class BaseBuilder(override var handle: ComparableHandle<out ProcessInstance>, override var owner: Principal, override var processModel: ExecutableProcessModel, override var instancename: String?, override var uuid: UUID, override var state: State?) : Builder {
    override val children = mutableListOf<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()
    override val   inputs = mutableListOf<ProcessData>()
    override val  outputs = mutableListOf<ProcessData>()
    override fun build(data: ProcessEngineDataAccess): ProcessInstance {
      return ProcessInstance(data, this)
    }
  }

  class ExtBuilder(private val base: ProcessInstance) : Builder {
    override var handle by overlay { base.handle }

    override var owner by overlay { base.owner }
    override var processModel by overlay { base.processModel }
    override var instancename by overlay { base.name }
    override var uuid by overlay { base.uuid }
    override var state by overlay { base.state }
    override val children by lazy { base.children.toMutableList() }
    override val inputs by lazy { base.inputs.toMutableList() }
    override val outputs by lazy { base.outputs.toMutableList() }

    override fun build(data: ProcessEngineDataAccess): ProcessInstance {
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

  class ProcessInstanceRef(processInstance: ProcessInstance) : Handle<ProcessInstance>, XmlSerializable {

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

  val processModel: ExecutableProcessModel

  val children: Sequence<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    get() {
      return (active.asSequence() + finished.asSequence() + completedEndnodes.asSequence())
    }

  val active: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>>

  val finished: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>>

  private val completedEndNodeCount: Int
    get() = completedEndnodes.size

  val completedEndnodes: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>>

  private val pendingJoins: HashMap<ExecutableJoin, ComparableHandle<out SecureObject<JoinInstance>>>

  private var handle: ComparableHandle<out ProcessInstance>

  override fun getHandle() = handle

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

  private constructor(data: ProcessEngineDataAccess, builder: Builder) {
    name = builder.instancename
    owner = builder.owner
    uuid = builder.uuid
    processModel = builder.processModel
    state = builder.state
    handle = Handles.handle(builder.handle)

    val joins = hashMapOf<ExecutableJoin, ComparableHandle<out SecureObject<JoinInstance>>>()

    val threads = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()

    val endResults = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()

    val finishedNodes = TreeSet<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()

    val nodes = builder.children
          .map { data.nodeInstance(it).withPermission() }

    nodes.forEach { instance ->
      if (instance is JoinInstance) {
        joins.put(instance.node, instance.getHandle())
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
    completedEndnodes = endResults
    finished = finishedNodes

    pendingJoins = joins
    inputs = builder.inputs.toList()
    outputs = builder.outputs.toList()
  }

  internal constructor(handle: Handle<ProcessInstance>, owner: Principal, processModel: ExecutableProcessModel, name: String?, uUid: UUID, state: State?) {
    this.handle = Handles.handle(handle)
    this.processModel = processModel
    this.owner = owner
    uuid = uUid
    this.name = name
    this.state = state ?: State.NEW
    active = ArraySet()
    completedEndnodes = ArraySet()
    finished = ArraySet()
    pendingJoins = HashMap()
    inputs = emptyList()
    outputs = emptyList()
  }

  constructor(owner: Principal, processModel: ExecutableProcessModel, name: String, uUid: UUID, state: State?) {
    this.processModel = processModel
    this.name = name
    handle = Handles.getInvalid()
    uuid = uUid
    this.owner = owner
    pendingJoins = HashMap()
    active = ArraySet()
    completedEndnodes = ArraySet()
    finished = ArraySet()
    this.state = state ?: State.NEW
    inputs = emptyList()
    outputs = emptyList()
  }

  override fun withPermission() = this

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: ProcessTransaction):ProcessInstance {
    if (state != State.NEW || active.isNotEmpty()) {
      throw IllegalStateException("The instance already appears to be initialised")
    }

    return update(transaction) {
      processModel.startNodes.forEach { node ->
        val nodeInstance = ProcessNodeInstance(node, Handles.getInvalid<ProcessNodeInstance>(), this@ProcessInstance)
        val handle = transaction.writableEngineData.nodeInstances.put(nodeInstance)
        children.add(Handles.handle(handle)) // function needed to make the handle comparable
      }
      state = State.INITIALIZED
    }
  }

  fun update(transaction: ProcessTransaction, body: Builder.() -> Unit):ProcessInstance {
    val origHandle = handle
    return builder().apply { body() }.build(transaction.readableEngineData).apply {
      if (origHandle.valid)
        if (handle.valid)
          transaction.writableEngineData.instances[handle] = this
    }
  }

  fun builder() = ExtBuilder(this)

  @Synchronized @Throws(SQLException::class)
  fun finish(transaction: ProcessTransaction):ProcessInstance {
    // This needs to update first as at this point the node state may not be valid.
    // TODO reduce the need to do a double update.
    update(transaction) {}.let { newInstance ->
      if (newInstance.completedEndNodeCount >= processModel.endNodeCount) {
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
  fun getNodeInstance(transaction: ProcessTransaction, identifiable: Identifiable): ProcessNodeInstance? {
    return children.map { handle ->
      val data = transaction.readableEngineData
      val instance = data.nodeInstance(handle).withPermission()
      if (identifiable.id == instance.node.id) {
        instance
      } else {
        instance.getPredecessor(transaction, identifiable.id)?.let { data.nodeInstance(it).withPermission() }
      }
    }.filterNotNull().firstOrNull()
  }

  @Synchronized @Throws(SQLException::class)
  private fun getJoinInstance(transaction: ProcessTransaction, join: ExecutableJoin, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>): JoinInstance {
    synchronized(pendingJoins) {
      val nodeInstances = transaction.writableEngineData.nodeInstances

      val joinInstance = pendingJoins[join]?.let {nodeInstances[it]?.withPermission() as JoinInstance }
      if (joinInstance==null) {
        val joinHandle= nodeInstances.put(JoinInstance(join, listOf(predecessor), this.handle, owner))

        pendingJoins[join] = Handles.handle(joinHandle.handleValue)
        return nodeInstances[joinHandle] as JoinInstance
      } else {
        return joinInstance.updateJoin(transaction) {
          predecessors.add(predecessor)
        }
      }
    }
  }

  @Synchronized fun removeJoin(join: JoinInstance) {
    pendingJoins.remove(join.node)
  }

  @Synchronized override fun setHandleValue(handleValue: Long) {
    if (handle.handleValue!=handleValue) {
      if (handleValue==-1L) { throw IllegalArgumentException("Setting the handle to invalid is not allowed") }
      if (handle.valid) throw IllegalStateException("Handles are not allowed to change")
      handle = Handles.handle(handleValue)
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun start(transaction: ProcessTransaction, messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>, payload: Node?):ProcessInstance {
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
              .map { engineData.nodeInstance(it).withPermission() }
              .filter { !it.state.isFinal }
              .forEach { it.provideTask(transaction, messageService) }

      }
    }
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.provideTask(transaction, messageService)"))
  fun provideTask(transaction: ProcessTransaction,
                  messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>,
                  node: ProcessNodeInstance):ProcessNodeInstance {
    assert(node.handle.valid) { "The handle is not valid: ${node.handle}" }
    return node.provideTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.takeTask(transaction, messageService)"))
  fun takeTask(transaction: ProcessTransaction,
               messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>,
               node: ProcessNodeInstance): ProcessNodeInstance {
    return node.takeTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.startTask<*>(transaction, messageService)"))
  fun startTask(transaction: ProcessTransaction,
                messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>,
                node: ProcessNodeInstance): ProcessNodeInstance {
    return node.startTask(transaction, messageService)
  }

  @Synchronized @Throws(SQLException::class)
  fun finishTask(transaction: ProcessTransaction,
                 messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>,
                 node: ProcessNodeInstance,
                 resultPayload: Node?): ProcessNodeInstance {
    if (node.state === NodeInstanceState.Complete) {
      throw IllegalStateException("Task was already complete")
    }
    // Make sure the finish is recorded.
    val newNode = transaction.commit(node.finishTask(transaction, resultPayload))

    handleFinishedState(transaction, messageService, newNode)
    return newNode

  }

  @Synchronized @Throws(SQLException::class)
  private fun handleFinishedState(transaction: ProcessTransaction,
                                  messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>,
                                  node: ProcessNodeInstance):ProcessInstance {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.node is EndNode<*, *>) {
        return finish(transaction).apply {
          assert(node.handle !in active)
          assert(node.handle !in finished)
          assert(node.handle in completedEndnodes)
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
  private fun startSuccessors(transaction: ProcessTransaction,
                              messageService: IMessageService<*, ProcessTransaction, in ProcessNodeInstance>,
                              predecessor: ProcessNodeInstance):ProcessInstance {

    val startedTasks = ArrayList<ProcessNodeInstance>(predecessor.node.successors.size)
    val joinsToEvaluate = ArrayList<JoinInstance>()

    val data = transaction.writableEngineData
    val self = update(transaction) {
      predecessor.node.successors.asSequence()
            .map { successorId ->
              val pni = createProcessNodeInstance(transaction, predecessor, processModel.getNode(successorId)?: throw ProcessException("Missing node ${successorId} in process model"))
              Handles.handle(data.nodeInstances.put(pni))
            }.forEach { instanceHandle ->
        run {
          data.nodeInstance(instanceHandle).withPermission()
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
  private fun createProcessNodeInstance(transaction: ProcessTransaction,
                                        predecessor: ProcessNodeInstance,
                                        node: ExecutableProcessNode): ProcessNodeInstance {
    if (node is ExecutableJoin) {
      return getJoinInstance(transaction, node, predecessor.handle)
    } else {
      return ProcessNodeInstance(node, predecessor.handle, this)
    }
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.failTask<*>(transaction, cause)"))
  fun failTask(transaction: ProcessTransaction,
               messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>,
               node: ProcessNodeInstance,
               cause: Throwable) {
    node.failTask(transaction, cause)
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Not needed", ReplaceWith("node.cancelTask<*>(transaction)"))
  fun cancelTask(transaction: ProcessTransaction,
                 messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>,
                 node: ProcessNodeInstance) {
    node.cancelTask(transaction)
  }

  @Synchronized @Throws(SQLException::class)
  fun getActivePredecessorsFor(transaction: ProcessTransaction, join: ExecutableJoin): Collection<ProcessNodeInstance> {
    return active.asSequence()
          .map { transaction.readableEngineData.nodeInstance(it).withPermission() }
          .filter { it.node.isPredecessorOf(join) }
          .toList()
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(transaction: ProcessTransaction, predecessor: ProcessNodeInstance): Collection<Handle<out SecureObject<ProcessNodeInstance>>> {

    val result = ArrayList<Handle<out SecureObject<ProcessNodeInstance>>>(predecessor.node.successors.size)

    fun addDirectSuccessor(candidate: ProcessNodeInstance,
                           predecessor: Handle<out SecureObject<ProcessNodeInstance>>) {

      // First look for this node, before diving into it's children
      candidate.directPredecessors.asSequence()
            .filter { it.handleValue == predecessor.handleValue }
            .forEach { node ->
              result.add(candidate.handle)
              return  // Assume that there is no further "successor" down the chain
            }
      for (hnode in candidate.directPredecessors) {
        // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
        val node = transaction.readableEngineData.nodeInstance(hnode).withPermission()
        addDirectSuccessor(node, predecessor)
      }
    }


    val data = transaction.readableEngineData
    active.asSequence()
          .map { data.nodeInstance(it).withPermission() }
          .forEach { addDirectSuccessor(it, predecessor.handle) }

    return result
  }

  @Synchronized @Throws(XmlException::class)
  fun serialize(transaction: ProcessTransaction, writer: XmlWriter) {
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

      writeListIfNotEmpty(completedEndnodes, Constants.PROCESS_ENGINE_NS, "endresults") {
        writeResultNodeRef(transaction, it)
      }
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeActiveNodeRef(transaction: ProcessTransaction, handleNodeInstance: Handle<out SecureObject<ProcessNodeInstance>>) {

    val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
    startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
      writeNodeRefCommon(nodeInstance)
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeResultNodeRef(transaction: ProcessTransaction, handleNodeInstance: Handle<out SecureObject<ProcessNodeInstance>>) {
    val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
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
  fun tickle(transaction: ProcessTransaction, messageService: IMessageService<*, ProcessTransaction, ProcessNodeInstance>) {

    fun tickePredecessors(successor: ProcessNodeInstance) {
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
          val instance = nodeInstance(handle).withPermission()
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
    private fun XmlWriter.writeNodeRefCommon(nodeInstance: ProcessNodeInstance) {
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
