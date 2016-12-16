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
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.FileNotFoundException
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


class ProcessInstance : MutableHandleAware<ProcessInstance>, SecureObject<ProcessInstance> {

  data class PNIPair<out T: IProcessNodeInstance<*>>(val instance:ProcessInstance, val node: T)

  class InstanceFuture<T:ProcessNodeInstance>(internal val orig: T, val store:Boolean) : Future<T> {
    private var cancelled = false

    private var updated: T? = null

    var origSetInvocation: Exception? = null

    override fun isCancelled() = cancelled

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      return if (updated!=null || cancelled) return true else {
        cancelled = true
        true
      }
    }

    override fun get(): T {
      if (cancelled) throw CancellationException()
      return updated ?: throw IllegalStateException("No value known yet")
    }

    override fun get(timeout: Long, unit: TimeUnit) = get()

    fun set(value: T) {
      if (cancelled) throw CancellationException()
      if (updated!=null) throw IllegalStateException("Value already set").apply { if (origSetInvocation!=null) initCause(origSetInvocation) }
      assert ( run {origSetInvocation = Exception("Original set stacktrace"); true })
      updated = value
    }

    override fun isDone() = updated!=null
  }

  interface Builder {
    val generation: Int
    val pendingChildren: List<Future<out ProcessNodeInstance>>
    var handle: ComparableHandle<out ProcessInstance>
    var owner: Principal
    var processModel: ExecutableProcessModel
    var instancename: String?
    var uuid: UUID
    var state: State?
    val children: List<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    val   inputs: MutableList<ProcessData>
    val  outputs: MutableList<ProcessData>
    fun build(data: MutableProcessEngineDataAccess): ProcessInstance
    fun <T:ProcessNodeInstance> addChild(child: T): Future<T>
    fun <T:ProcessNodeInstance> storeChild(child:T): Future<T>
  }

  data class BaseBuilder(override var handle: ComparableHandle<out ProcessInstance> = Handles.getInvalid(),
                         override var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
                         override var processModel: ExecutableProcessModel,
                         override var instancename: String? = null,
                         override var uuid: UUID = UUID.randomUUID(),
                         override var state: State?=null) : Builder {
    override var generation: Int = 0
    private val _pendingChildren = mutableListOf<Future<out ProcessNodeInstance>>()
    override val pendingChildren: List<Future<out ProcessNodeInstance>> get() = _pendingChildren
    override val children = mutableListOf<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()
    override val   inputs = mutableListOf<ProcessData>()
    override val  outputs = mutableListOf<ProcessData>()
    override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
      return ProcessInstance(data, this)
    }

    override fun <T:ProcessNodeInstance> addChild(child: T): Future<T>
        = InstanceFuture(child, store=false).apply { if (handle.valid) throw IllegalArgumentException("Adding an existing child node") ; _pendingChildren.add(this) }

    override fun <T : ProcessNodeInstance> storeChild(child: T): Future<T>
        = InstanceFuture(child, store=true).apply { if (!handle.valid) throw IllegalArgumentException("Storing a non-existing child"); _pendingChildren.add(this) }
  }

  class ExtBuilder(private val base: ProcessInstance) : Builder {
    override var generation = base.generation+1
      private set
    private val _pendingChildren = mutableListOf<Future<out ProcessNodeInstance>>()
    override val pendingChildren: List<Future<out ProcessNodeInstance>> get() = _pendingChildren
    override var handle by overlay { base.handle }

    override var owner by overlay { base.owner }
    override var processModel by overlay { base.processModel }
    override var instancename by overlay { base.name }
    override var uuid by overlay({ generation = 0; handle = Handles.getInvalid() }) { base.uuid }
    override var state by overlay { base.state }
    override val children get()=base.childNodes.map { it.withPermission().getHandle() }
    override val inputs by lazy { base.inputs.toMutableList() }
    override val outputs by lazy { base.outputs.toMutableList() }

    override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
      return ProcessInstance(data, this)
    }

    override fun <T:ProcessNodeInstance> addChild(child: T): Future<T>
        = InstanceFuture(child, store=false).apply { _pendingChildren.add(this) }

    override fun <T : ProcessNodeInstance> storeChild(child: T): Future<T>
        = InstanceFuture(child, store=true).apply { _pendingChildren.add(this) }

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

    val processModel: Handle<out ProcessModel<*,*>> = processInstance.processModel.getHandle()

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

  val generation: Int

  val processModel: ExecutableProcessModel

  val childNodes: Collection<SecureObject<ProcessNodeInstance>>

  val children: Sequence<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    get() = childNodes.asSequence().map { it.withPermission().getHandle() }

  val activeNodes get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filter { ! it.state.isFinal }

  val active: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>> get() = activeNodes
      .map { it.getHandle() }
      .toList()

  val finishedNodes get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filter { it.state.isFinal && it.node !is EndNode<*,*> }

  val finished: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>> get() = finishedNodes
      .map { it.getHandle() }
      .toList()

  val completedNodeInstances get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filter { it.state.isFinal && it.node is EndNode<*,*> }

  val completedEndnodes: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>> get() = completedNodeInstances
      .map { it.getHandle() }
      .toList()

  private val pendingJoinNodes get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filterIsInstance(JoinInstance::class.java)
      .filter { ! it.state.isFinal }

  private val pendingJoins: Map<ExecutableJoin, JoinInstance> get() =
      pendingJoinNodes.associateBy { it.node }

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

  private constructor(data: MutableProcessEngineDataAccess, builder: Builder) {
    generation = builder.generation
    name = builder.instancename
    owner = builder.owner
    uuid = builder.uuid
    processModel = builder.processModel
    state = builder.state
    handle = Handles.handle(builder.handle)

    val pending = builder.pendingChildren.asSequence().map { it as InstanceFuture<*> }.toList()

    val createdNodes = pending
        .filter { ! it.orig.getHandle().valid }
        .map { data.putNodeInstance(it) }

    val updatedNodes = pending.asSequence()
        .filter { it.orig.getHandle().valid }
        .map {
          assert(it.orig.hProcessInstance == handle)
          data.storeNodeInstance(it)
        }
        .associateBy { it.getHandle() }.toMutableMap()

    val nodes = createdNodes + builder.children.asSequence().map { childHandle ->
      updatedNodes.remove(childHandle) ?: data.nodeInstance(childHandle).withPermission()
    }.toList()

    assert(updatedNodes.isEmpty()) { "All updated nodes must be used" }

    childNodes = nodes
    inputs = builder.inputs.toList()
    outputs = builder.outputs.toList()
  }

  constructor(data: MutableProcessEngineDataAccess, processModel: ExecutableProcessModel, body: Builder.() -> Unit) : this(data, BaseBuilder(processModel=processModel).apply(body))

  override fun withPermission() = this

  @Synchronized @Throws(SQLException::class)
  fun initialize(transaction: ProcessTransaction):ProcessInstance {
    if (state != State.NEW || active.isNotEmpty()) {
      throw IllegalStateException("The instance already appears to be initialised")
    }

    return update(transaction.writableEngineData ) {
      processModel.startNodes.forEach { node ->
        addChild(node.createOrReuseInstance(transaction, this@ProcessInstance))
      }
      state = State.INITIALIZED
    }
  }

  fun __storeNewValueIfNeeded(writableEngineData: MutableProcessEngineDataAccess, newInstance: ProcessInstance):ProcessInstance {

    fun dataValid():Boolean {
      val stored = writableEngineData.instance(handle).withPermission()
      assert(stored.uuid == uuid) { "Uuid mismatch this: $uuid, stored: ${stored.uuid}" }
      assert(newInstance.uuid == uuid) { "Uuid mismatch this: $uuid, new: ${newInstance.uuid}" }
      assert(generation == stored.generation) { "Generation mismatch this: $generation stored: ${stored.generation}" }
      assert(generation +1 == newInstance.generation) { "Generation mismatch this+1: ${generation + 1} new: ${newInstance.generation}" }
      return newInstance.handle.valid && handle.valid
    }

    if (getHandle().valid && handle.valid) {
      assert(dataValid()) { "Instance generations lost in the waves" }
      writableEngineData.instances[handle] = newInstance
      return newInstance
    }
    return newInstance
  }

  inline fun update(writableEngineData: MutableProcessEngineDataAccess, body: Builder.() -> Unit): ProcessInstance {
    val newValue = builder().apply(body).build(writableEngineData)
    return __storeNewValueIfNeeded(writableEngineData, newValue).apply {
      assert(writableEngineData.instances[newValue.getHandle()]?.withPermission() == this) {
        "ProcessNodes should match after storage"
      }
    }
  }

  fun <T:ProcessNodeInstance> updateNode(writableEngineData: MutableProcessEngineDataAccess, processNodeInstance: T): ProcessInstance.PNIPair<T> {
    return PNIPair(update(writableEngineData) {
      addChild(processNodeInstance)
    }, processNodeInstance)/* XXX .apply {
      instance.childNodes
          .filter { it!=node && (it is SplitInstance) }
          .fold(this) { (instance, node), split -> PNIPair((split as SplitInstance).updateState(writableEngineData, instance, messageservice), node) }
    }*/
  }

  fun <T:ProcessNodeInstance> addChild(data: MutableProcessEngineDataAccess, child: T): PNIPair<T> {
    val b = builder()
    val future = b.addChild(child)
    return PNIPair(__storeNewValueIfNeeded(data,b.build(data)), future.get())
  }

  fun builder() = ExtBuilder(this)

  @Synchronized @Throws(SQLException::class)
  fun finish(engineData: MutableProcessEngineDataAccess):ProcessInstance {
    // This needs to update first as at this point the node state may not be valid.
    // TODO reduce the need to do a double update.
    update(engineData ) {}.let { newInstance ->
      if (newInstance.completedNodeInstances.count() >= processModel.endNodeCount) {
        // TODO mark and store results
        return newInstance.update(engineData) {
          state = State.FINISHED
        }.apply {
          engineData.commit()
          // TODO don't remove old transactions
          engineData.handleFinishedInstance(handle)
        }

      } else {
        return newInstance
      }
    }
  }

  @Synchronized @Throws(SQLException::class)
  fun getNodeInstance(identified: Identified): ProcessNodeInstance? {
    return childNodes.asSequence().map { it.withPermission() }.firstOrNull { it.node.id==identified.id }
  }

  @Synchronized @Throws(SQLException::class)
  internal fun getJoinInstance(join: ExecutableJoin, predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>): JoinInstance {
    return pendingJoinNodes.firstOrNull { it.node == join }
        ?: JoinInstance(join, listOf(predecessor), this.handle, owner)
  }

  @Synchronized override fun setHandleValue(handleValue: Long) {
    if (handle.handleValue!=handleValue) {
      if (handleValue==-1L) { throw IllegalArgumentException("Setting the handle to invalid is not allowed") }
      if (handle.valid) throw IllegalStateException("Handles are not allowed to change")
      handle = Handles.handle(handleValue)
    }
  }

  fun getChild(nodeId: String): SecureObject<ProcessNodeInstance>? {
    return childNodes.firstOrNull { it.withPermission().node.id == nodeId }
  }

  @Synchronized @Throws(SQLException::class)
  fun start(transaction: ProcessTransaction, messageService: IMessageService<*, MutableProcessEngineDataAccess, ProcessNodeInstance>, payload: Node?):ProcessInstance {
    return (if (state == null) { initialize(transaction) } else this)
        .update(transaction.writableEngineData) { state = State.STARTED; inputs.addAll(processModel.toInputs(payload)) }
        .let { self ->
          self.active.asSequence()
              .map { transaction.readableEngineData.nodeInstance(it).withPermission() }
              .filter { !it.state.isFinal }
              .fold(self) { self, task -> task.provideTask(transaction.writableEngineData, self, messageService).instance }
        }
  }

  @Synchronized @Throws(SQLException::class)
  fun finishTask(engineData: MutableProcessEngineDataAccess,
                 messageService: IMessageService<*, MutableProcessEngineDataAccess, in ProcessNodeInstance>,
                 node: ProcessNodeInstance,
                 resultPayload: Node?): PNIPair<ProcessNodeInstance> {
    if (node.state === NodeInstanceState.Complete) {
      throw IllegalStateException("Task was already complete")
    }
    // Make sure the finish is recorded.
    @Suppress("DEPRECATION")
    val newInstances = node.finishTask(engineData, this, resultPayload).apply { engineData.commit() }

    return PNIPair(newInstances.instance.handleFinishedState(engineData, messageService, newInstances.node), newInstances.node)
  }

  @Synchronized @Throws(SQLException::class)
  private fun handleFinishedState(engineData: MutableProcessEngineDataAccess,
                                  messageService: IMessageService<*, MutableProcessEngineDataAccess, in ProcessNodeInstance>,
                                  node: ProcessNodeInstance):ProcessInstance {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.node is EndNode<*, *>) {
        return finish(engineData).apply {
          assert(node.getHandle() !in active)
          assert(node.getHandle() !in finished)
          assert(node.getHandle() in completedEndnodes)
        }
      } else {
        return startSuccessors(engineData, messageService, node)
      }
    } catch (e: RuntimeException) {
      engineData.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    } catch (e: SQLException) {
      engineData.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    }
    return this
  }

  @Synchronized @Throws(SQLException::class)
  private fun startSuccessors(engineData: MutableProcessEngineDataAccess,
                              messageService: IMessageService<*, MutableProcessEngineDataAccess, in ProcessNodeInstance>,
                              predecessor: ProcessNodeInstance):ProcessInstance {

    val startedTasks = ArrayList<ProcessNodeInstance>(predecessor.node.successors.size)
    val joinsToEvaluate = ArrayList<JoinInstance>()

    var self = this
    for (successorId in predecessor.node.successors) {
      val nodeInstance:ProcessNodeInstance = run {
        val nonRegisteredNodeInstance = processModel.getNode(successorId).mustExist(successorId).createOrReuseInstance(engineData, this@ProcessInstance, predecessor.getHandle())
        val pair = nonRegisteredNodeInstance.update(engineData, self) {
          predecessors.add(predecessor.getHandle())
        }
        self = pair.instance
        if (pair.node.getHandle().valid) pair.node else {
          self.addChild(engineData, pair.node).apply { self = instance }.node
        }

      }

      if (nodeInstance is JoinInstance) {
        joinsToEvaluate.add(nodeInstance)
      } else {
        startedTasks.add(nodeInstance)
      }
    }

    // Commit the registration of the follow up nodes before starting them.
    engineData.commit()
    self = startedTasks.fold(self) { self, task -> task.provideTask(engineData, self, messageService).instance }
    self = joinsToEvaluate.fold(self) {self, join -> join.startTask(engineData, self, messageService).instance }
    return self
  }

  @Synchronized @Throws(SQLException::class)
  fun getActivePredecessorsFor(engineData: MutableProcessEngineDataAccess, join: ExecutableJoin): Collection<ProcessNodeInstance> {
    return active.asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .filter { it.node.isPredecessorOf(join) }
          .toList()
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(engineData: ProcessEngineDataAccess, predecessor: ProcessNodeInstance): Collection<Handle<out SecureObject<ProcessNodeInstance>>> {
    // TODO rewrite, this can be better with the children in the instance
    val result = ArrayList<Handle<out SecureObject<ProcessNodeInstance>>>(predecessor.node.successors.size)

    fun addDirectSuccessor(candidate: ProcessNodeInstance,
                           predecessor: Handle<out SecureObject<ProcessNodeInstance>>) {

      // First look for this node, before diving into it's children
      candidate.directPredecessors.asSequence()
            .filter { it.handleValue == predecessor.handleValue }
            .forEach { node ->
              result.add(candidate.getHandle())
              return  // Assume that there is no further "successor" down the chain
            }
      for (hnode in candidate.directPredecessors) {
        // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
        val node = engineData.nodeInstance(hnode).withPermission()
        addDirectSuccessor(node, predecessor)
      }
    }


    val data = engineData
    active.asSequence()
          .map { data.nodeInstance(it).withPermission() }
          .forEach { addDirectSuccessor(it, predecessor.getHandle()) }

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
  fun tickle(transaction: ProcessTransaction, messageService: IMessageService<*, MutableProcessEngineDataAccess, ProcessNodeInstance>) {
    val engineData = transaction.writableEngineData
    fun ticklePredecessors(self: ProcessInstance, successor: ProcessNodeInstance): ProcessInstance {
      return successor.directPredecessors.asSequence()
            .map { transaction.writableEngineData.nodeInstances[it]?.withPermission() }
            .filterNotNull()
            .fold(self) { self: ProcessInstance, pred ->
              ticklePredecessors(self, pred).let { self ->
                pred.tickle(engineData, self, messageService).instance
              }
            }
    }

    // make a copy as the list may be changed due to tickling.
    var self = active.toList().fold(this) { self , handle ->
      try {
        transaction.writableEngineData.run {
          invalidateCachePNI(handle)
          val nodeInstance = nodeInstance(handle).withPermission()
          var self2 = ticklePredecessors(self, nodeInstance)
          val instanceState = nodeInstance.state
          if (instanceState.isFinal) {
            self2 = self2.handleFinishedState(engineData, messageService, nodeInstance)
          }
          self2
        }

      } catch (e: SQLException) {
        Logger.getLogger(javaClass.name).log(Level.WARNING, "Error when tickling process instance", e)
        this
      }

    }
    if (active.isEmpty()) {
      try {
        self.finish(engineData)
      } catch (e: SQLException) {
        Logger.getLogger(javaClass.name).log(Level.WARNING,
                                             "Error when trying to finish a process instance as result of tickling",
                                             e)
      }

    }
  }

  override fun toString(): String {
    return "ProcessInstance(handle=${handle.handleValue}, name=$name, state=$state, generation=$generation, childNodes=$childNodes)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as ProcessInstance

    if (generation != other.generation) return false
    if (processModel != other.processModel) return false
    if (childNodes != other.childNodes) return false
    if (handle != other.handle) return false
    if (inputs != other.inputs) return false
    if (outputs != other.outputs) return false
    if (name != other.name) return false
    if (owner != other.owner) return false
    if (state != other.state) return false
    if (uuid != other.uuid) return false

    return true
  }

  override fun hashCode(): Int {
    var result = generation
    result = 31 * result + processModel.hashCode()
    result = 31 * result + childNodes.hashCode()
    result = 31 * result + handle.hashCode()
    result = 31 * result + inputs.hashCode()
    result = 31 * result + outputs.hashCode()
    result = 31 * result + (name?.hashCode() ?: 0)
    result = 31 * result + owner.hashCode()
    result = 31 * result + (state?.hashCode() ?: 0)
    result = 31 * result + uuid.hashCode()
    return result
  }

  companion object {

    private val serialVersionUID = 1145452195455018306L

    @JvmStatic
    private fun <T:ProcessNodeInstance> MutableProcessEngineDataAccess.putNodeInstance(value: InstanceFuture<T>):T {
      val handle = (nodeInstances as MutableHandleMap).put(value.orig)
      val newValue = nodeInstance(handle).withPermission() as T
      @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
      value.set(newValue)
      return newValue
    }

    @JvmStatic
    private fun <T:ProcessNodeInstance> MutableProcessEngineDataAccess.storeNodeInstance(value: InstanceFuture<T>):T {
      val handle = value.orig.getHandle()
      (nodeInstances as MutableHandleMap)[handle] = value.orig
      @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
      val newValue = nodeInstance(handle).withPermission() as T
      value.set(newValue)
      return newValue
    }

    @Throws(XmlException::class)
    private fun XmlWriter.writeNodeRefCommon(nodeInstance: ProcessNodeInstance) {
      writeAttribute("nodeid", nodeInstance.node.id)
      writeAttribute("handle", nodeInstance.getHandleValue())
      attribute(null, "state", null, nodeInstance.state.toString())
      if (nodeInstance.state === NodeInstanceState.Failed) {
        val failureCause = nodeInstance.failureCause
        val value = if (failureCause == null) "<unknown>" else failureCause.javaClass.name + ": " + failureCause.message
        attribute(null, "failureCause", null, value)
      }

    }
  }

}

internal fun <T:ProcessNodeInstance> ProcessInstance.PNIPair<T>.update(writableEngineData: MutableProcessEngineDataAccess, body: ProcessNodeInstance.Builder<out ExecutableProcessNode>.() -> Unit): ProcessInstance.PNIPair<T> {
  @Suppress("UNCHECKED_CAST")
  return node.update(writableEngineData, instance, body) as ProcessInstance.PNIPair<T>
}