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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.SplitInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableModelCommon
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.writeHandleAttr
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
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMResult


class ProcessInstance : MutableHandleAware<SecureObject<ProcessInstance>>, SecureObject<ProcessInstance> {

  class Updater(private var instance: ProcessInstance) {

    private fun <N:ProcessNodeInstance> N.managedUpdate(engineData: MutableProcessEngineDataAccess, body: ProcessNodeInstance.Builder<out ExecutableProcessNode>.() -> Unit): N {
      @Suppress("UNCHECKED_CAST")
      return update(engineData, body).let {
        instance = it.instance
        it.node as N
      }
    }

    private fun <N:ProcessNodeInstance> pnipair(node:ProcessNodeInstance) : PNIPair<N> {
      @Suppress("UNCHECKED_CAST")
      return PNIPair(instance, node as N)
    }

    @Suppress("UNCHECKED_CAST")
    fun <N:ProcessNodeInstance> takeTask(engineData: MutableProcessEngineDataAccess, origNodeInstance: N ): PNIPair<N> {
      val startNext = origNodeInstance.node.takeTask(origNodeInstance)

      val updatedNode = origNodeInstance.managedUpdate(engineData) { state = NodeInstanceState.Taken }

      if (startNext) {
        @Suppress("DEPRECATION")
        return updatedNode.startTask(engineData, instance).apply {
          this@Updater.instance = instance
        } as PNIPair<N>
      } else {
        return pnipair(updatedNode)
      }
    }

  }

  data class PNIPair<out T: ProcessNodeInstance>(val instance:ProcessInstance, val node: T) {
    @Suppress("UNCHECKED_CAST")
    fun startTask(engineData: MutableProcessEngineDataAccess): PNIPair<T> = node.startTask(engineData, instance) as PNIPair<T>
    fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: Node?=null): PNIPair<T> = node.finishTask(engineData, instance, resultPayload) as PNIPair<T>
  }

  @Deprecated("Is really impossible", level = DeprecationLevel.WARNING)
  private class InstanceFuture<T:ProcessNodeInstance, N: ExecutableProcessNode>(internal val origBuilder: ProcessNodeInstance.Builder<out N>, val store:Boolean) : Future<T> {
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
    var handle: ComparableHandle<out SecureObject<ProcessInstance>>
    var parentActivity: ComparableHandle<out SecureObject<ProcessNodeInstance>>
    var owner: Principal
    var processModel: ExecutableModelCommon
    var instancename: String?
    var uuid: UUID
    var state: State
    val children: List<ComparableHandle<out SecureObject<ProcessNodeInstance>>>
    val   inputs: MutableList<ProcessData>
    val  outputs: MutableList<ProcessData>
    fun build(data: MutableProcessEngineDataAccess): ProcessInstance
    fun <T:ProcessNodeInstance> addChild(child: T): Future<T>
    fun <T:ProcessNodeInstance> storeChild(child:T): Future<T>
    fun <N:ExecutableProcessNode> getChild(node: N, entryNo: Int): ProcessNodeInstance.Builder<N>?
    fun <T : ProcessNodeInstance> addChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode>): Future<T>
    fun <T : ProcessNodeInstance> storeChild(child: ProcessNodeInstance.ExtBuilder<out ExecutableProcessNode, out ProcessNodeInstance>): Future<T>
    fun <N:ExecutableProcessNode> updateChild(node: N, entryNo: Int, body: ProcessNodeInstance.Builder<N>.()->Unit)
  }

  data class BaseBuilder(override var handle: ComparableHandle<out SecureObject<ProcessInstance>> = Handles.getInvalid(),
                         override var owner: Principal = SecurityProvider.SYSTEMPRINCIPAL,
                         override var processModel: ExecutableModelCommon,
                         override var instancename: String? = null,
                         override var uuid: UUID = UUID.randomUUID(),
                         override var state: State=State.NEW,
                         override var parentActivity: ComparableHandle<out SecureObject<ProcessNodeInstance>> /*= Handles.getInvalid()*/) : Builder {
    override var generation: Int = 0
    private val _pendingChildren = mutableListOf<InstanceFuture<out ProcessNodeInstance, *>>()
    override val pendingChildren: List<Future<out ProcessNodeInstance>> get() = _pendingChildren
    override val children = mutableListOf<ComparableHandle<out SecureObject<ProcessNodeInstance>>>()
    override val   inputs = mutableListOf<ProcessData>()
    override val  outputs = mutableListOf<ProcessData>()
    override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
      return ProcessInstance(data, this)
    }

    override fun <T:ProcessNodeInstance> addChild(child: T): Future<T> {
      return addChild(child.builder())
    }

    override fun <T : ProcessNodeInstance> addChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode>): Future<T> {
      return InstanceFuture<T, ExecutableProcessNode>(child, store = false).apply {
        if (handle.valid) throw IllegalArgumentException("Adding an existing child node"); _pendingChildren.add(this)
      }
    }

    override fun <T : ProcessNodeInstance> storeChild(child: T): Future<T> {
      return storeChild(child.builder())
    }

    override fun <T : ProcessNodeInstance> storeChild(child: ProcessNodeInstance.ExtBuilder<out ExecutableProcessNode, out ProcessNodeInstance>): Future<T> {
      return InstanceFuture<T, ExecutableProcessNode>(child, store = true).apply {
        if (!handle.valid) throw IllegalArgumentException("Storing a non-existing child"); _pendingChildren.add(this)
      }
    }

    override fun <N : ExecutableProcessNode> getChild(node: N, entryNo: Int): ProcessNodeInstance.Builder<N>? {
      return _pendingChildren.firstOrNull { it.origBuilder.node == node && it.origBuilder.entryNo==entryNo}
        ?.let{ it.origBuilder as ProcessNodeInstance.Builder<N> }
    }

    override fun <N : ExecutableProcessNode> updateChild(node: N,
                                                         entryNo: Int,
                                                         body: ProcessNodeInstance.Builder<N>.() -> Unit){
      val existingBuilder = _pendingChildren.firstOrNull { it.origBuilder.node == node && it.origBuilder.entryNo == entryNo }
          ?: throw ProcessException ("Attempting to update a nonexisting child")
      (existingBuilder as ProcessNodeInstance.Builder<N>).apply(body)
    }
  }

  class ExtBuilder(private val base: ProcessInstance) : Builder {
    override var generation = base.generation+1
      private set
    private val _pendingChildren = mutableListOf<InstanceFuture<out ProcessNodeInstance, out ExecutableProcessNode>>()
    override val pendingChildren: List<Future<out ProcessNodeInstance>> get() = _pendingChildren
    override var handle by overlay { base.handle }
    override var parentActivity by overlay { base.parentActivity }

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

    override fun <T:ProcessNodeInstance> addChild(child: T): Future<T> = addChild<T>(child.builder())

    override fun <T : ProcessNodeInstance> addChild(childBuilder: ProcessNodeInstance.Builder<out ExecutableProcessNode>): Future<T> {
      return InstanceFuture<T, ExecutableProcessNode>(childBuilder, store = false).apply { _pendingChildren.add(this) }
    }

    override fun <T : ProcessNodeInstance> storeChild(child: T): Future<T>
      = storeChild<T>(child.builder())

    override fun <T : ProcessNodeInstance> storeChild(child: ProcessNodeInstance.ExtBuilder<out ExecutableProcessNode, *>): Future<T> {
      return InstanceFuture<T, ExecutableProcessNode>(child, store = true).apply { _pendingChildren.add(this) }
    }

    override fun <N : ExecutableProcessNode> getChild(node: N, entryNo: Int): ProcessNodeInstance.Builder<N>? {
      return _pendingChildren.firstOrNull { it.origBuilder.node == node && it.origBuilder.entryNo == entryNo }?.let { it as ProcessNodeInstance.Builder<N> } ?:
              base.childNodes.asSequence()
                .map { it.withPermission() }
                .firstOrNull { it.node == node && it.entryNo == entryNo }?.builder()
                ?.let {
                  (it  as ProcessNodeInstance.Builder<N>).also {
                    _pendingChildren.add(InstanceFuture<ProcessNodeInstance, N>(it, store = true))
                  }
                }

    }

    override fun <N : ExecutableProcessNode> updateChild(node: N,
                                                         entryNo: Int,
                                                         body: ProcessNodeInstance.Builder<N>.() -> Unit){
      @Suppress("UNCHECKED_CAST")
      val existingBuilder = _pendingChildren.asSequence()
                              .map{it.origBuilder as ProcessNodeInstance.Builder<N>}
                              .firstOrNull { it.node == node && it.entryNo == entryNo }
      if(existingBuilder!=null) { existingBuilder.apply(body); return }

      base.childNodes.asSequence()
        .map { it.withPermission() }
        .firstOrNull { it.node == node && it.entryNo == entryNo }
        ?.also {
          val b = it.builder() as ProcessNodeInstance.ExtBuilder<N, ProcessNodeInstance>
          b.apply(body)
          if (b.changed) { _pendingChildren.add(InstanceFuture<ProcessNodeInstance, N>(b, true)) }
        } ?: throw ProcessException ("Attempting to update a nonexisting child")
    }

    fun initialize() {
      if (state != State.NEW || base.active.isNotEmpty() || _pendingChildren.any { ! it.origBuilder.state.isFinal }) {
        throw IllegalStateException("The instance already appears to be initialised")
      }

      processModel.startNodes.forEach { node ->
        addChild(node.createOrReuseInstance(this, 1).build()) // Start with sequence 1
      }
      state = State.INITIALIZED
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

  class ProcessInstanceRef(processInstance: ProcessInstance) : ComparableHandle<SecureObject<ProcessInstance>>, XmlSerializable {

    override val handleValue = processInstance.handle.handleValue

    val processModel = processInstance.processModel.rootModel.getHandle()

    val name: String = processInstance.name.let { if (it.isNullOrBlank()) {
      buildString {
        append(processInstance.processModel.rootModel.name)
        if (processInstance.processModel !is ExecutableProcessModel) append(" child") else append(' ')
        append("instance ").append(handleValue)
      }
    } else it!! }

    val parentActivity = processInstance.parentActivity

    var uuid: UUID = processInstance.uuid

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
      out.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
        writeHandleAttr("handle", this@ProcessInstanceRef)
        writeHandleAttr("processModel", processModel)
        writeHandleAttr("parentActivity", parentActivity)
        writeAttribute("name", name)
        writeAttribute("uuid", uuid)
      }
    }

    override val valid: Boolean
      get() = handleValue >= 0L
  }

  val generation: Int

  val processModel: ExecutableModelCommon

  val childNodes: Collection<SecureObject<ProcessNodeInstance>>

  val parentActivity: ComparableHandle<out SecureObject<ProcessNodeInstance>>

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

  val completedNodeInstances: Sequence<SecureObject<ProcessNodeInstance>> get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filter { it.state.isFinal && it.node is EndNode<*,*> }

  val completedEndnodes: Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>> get() = completedNodeInstances
      .map { it.withPermission().getHandle() }
      .toList()

  private val pendingJoinNodes get() = childNodes.asSequence()
      .map { it.withPermission() }
      .filterIsInstance(JoinInstance::class.java)
      .filter { ! it.state.isFinal }

  private val pendingJoins: Map<ExecutableJoin, JoinInstance> get() =
      pendingJoinNodes.associateBy { it.node }

  private var handle: ComparableHandle<out SecureObject<ProcessInstance>>

  override fun getHandle() = handle

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  val inputs: List<ProcessData>

  val outputs: List<ProcessData>

  val name: String?

  override val owner: Principal

  val state: State

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
    parentActivity = builder.parentActivity

    val pending = builder.pendingChildren.asSequence().map { it as InstanceFuture<*,*> }.toList()

    val createdNodes = pending
        .filter { ! it.origBuilder.handle.valid }
        .map { data.putNodeInstance(it) }

    val updatedNodes = pending.asSequence()
        .filter { it.origBuilder.handle.valid }
        .map {
          assert(it.origBuilder.hProcessInstance == handle)
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

  constructor(data: MutableProcessEngineDataAccess,
              processModel: ExecutableModelCommon,
              parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance>>,
              body: Builder.() -> Unit) : this(data, BaseBuilder(processModel=processModel, parentActivity = parentActivity).apply(body))

  override fun withPermission() = this

  private fun checkOwnership(node: ProcessNodeInstance) {
    if (node.hProcessInstance!=handle) throw ProcessException("The node is not owned by this instance")
  }

  @Suppress("NOTHING_TO_INLINE")
  @Deprecated("Use data access", ReplaceWith("initialize(transaction.writableEngineData)"))
  inline fun initialize(transaction: ProcessTransaction) = initialize(transaction.writableEngineData)

  @Throws(SQLException::class)
  @Synchronized
  fun initialize(engineData: MutableProcessEngineDataAccess): ProcessInstance {

    return update(engineData) {
      initialize()
    }
  }

  @PublishedApi
  internal fun __storeNewValueIfNeeded(writableEngineData: MutableProcessEngineDataAccess, newInstance: ProcessInstance):ProcessInstance {

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

  fun update(writableEngineData: MutableProcessEngineDataAccess, body: ExtBuilder.() -> Unit): ProcessInstance {
    val newValue = builder().apply(body).build(writableEngineData)
    return __storeNewValueIfNeeded(writableEngineData, newValue).apply {
      assert(writableEngineData.instances[newValue.getHandle()]?.withPermission() == this) {
        "ProcessNodes should match after storage"
      }
    }
  }

  fun <T:ProcessNodeInstance> updateNode(writableEngineData: MutableProcessEngineDataAccess, processNodeInstance: T): ProcessInstance.PNIPair<T> {
    checkOwnership(processNodeInstance)
    return PNIPair(update(writableEngineData) {
      addChild(processNodeInstance)
    }, processNodeInstance)/* XXX .apply {
      instance.childNodes
          .filter { it!=node && (it is SplitInstance) }
          .fold(this) { (instance, node), split -> PNIPair((split as SplitInstance).updateState(writableEngineData, instance, messageservice), node) }
    }*/
  }

  @Deprecated("Use the method on a builder")
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
          if (parentActivity.valid) {
            val parentNode = engineData.nodeInstance(parentActivity).withPermission()
            val parentInstance = engineData.instance(parentNode.hProcessInstance).withPermission()
            parentInstance.finishTask(engineData, parentNode, getOutputPayload())
          }

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
  fun getNodeInstances(identified: Identified): Sequence<ProcessNodeInstance> {
    return childNodes.asSequence().map { it.withPermission() }.filter { it.node.id==identified.id }
  }

  @Synchronized @Throws(SQLException::class)
  fun getNodeInstance(identified: Identified, entryNo: Int): ProcessNodeInstance? {
    return childNodes.asSequence().map { it.withPermission() }.firstOrNull { it.node.id==identified.id && it.entryNo == entryNo }
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

  /**
   * Get the output of this instance as an xml node or `null` if there is no output
   */
  fun getOutputPayload(): Node? {
    if (outputs.isEmpty()) return null
    val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware=true }.newDocumentBuilder().newDocument()
    return document.createDocumentFragment().apply {
      XmlStreaming.newWriter(DOMResult(this)).use { writer ->
        outputs.forEach { output ->
          output.serialize(writer)
        }
      }
    }
  }

  @Synchronized @Throws(SQLException::class)
  @Deprecated("Use data access", ReplaceWith("start(transaction.writableEngineData, messageService, payload)"))
  inline fun start(transaction: ProcessTransaction, messageService: IMessageService<*>, payload: Node?=null) = start(transaction.writableEngineData, payload)

  fun start(engineData: MutableProcessEngineDataAccess, payload: Node? = null): ProcessInstance {
    return (if (state == State.NEW) initialize(engineData) else this)
      .update(engineData) { state = State.STARTED; inputs.addAll(processModel.toInputs(payload)) }
      .run { // need run as the this needs to be captured at fold
        active.asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .filter { !it.state.isFinal }
          .fold(this) { self, task -> task.provideTask(engineData, self).instance }
      }
  }

  @Synchronized @Throws(SQLException::class)
  fun <N: ProcessNodeInstance>finishTask(engineData: MutableProcessEngineDataAccess,
                 node: N,
                 resultPayload: Node?): PNIPair<N> {
    checkOwnership(node)
    if (node.state === NodeInstanceState.Complete) {
      throw IllegalStateException("Task was already complete")
    }
    // Make sure the finish is recorded.
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    val newInstances = node.finishTask(engineData, this, resultPayload).apply { engineData.commit() } as PNIPair<N>

    return newInstances.instance.handleFinishedState(engineData, newInstances.node)
  }

  fun <N:ProcessNodeInstance> skipTask(engineData: MutableProcessEngineDataAccess, node: N): PNIPair<N> {
    return skipTask(engineData, node, NodeInstanceState.Skipped)
  }

  fun <N:ProcessNodeInstance> skipCancelTask(engineData: MutableProcessEngineDataAccess, node: N): PNIPair<N> {
    return skipTask(engineData, node, NodeInstanceState.SkippedCancel)
  }

  fun <N:ProcessNodeInstance> skipFailTask(engineData: MutableProcessEngineDataAccess, node: N): PNIPair<N> {
    return skipTask(engineData, node, NodeInstanceState.SkippedFail)
  }

  fun <N:ProcessNodeInstance> skipTask(engineData: MutableProcessEngineDataAccess, node: N, newState: NodeInstanceState): PNIPair<N> {
    if (node.state.isFinal) {
      throw ProcessException("Instance $this is already in a final state and cannot be skipped anymore")
    }
    return node.update(engineData) {
      this.state = newState
    }.let { pnipair ->
      engineData.commit()
      @Suppress("UNCHECKED_CAST")
      pnipair.instance.handleFinishedState(engineData, pnipair.node) as PNIPair<N>
    }
  }


  @Synchronized @Throws(SQLException::class)
  private fun <N: ProcessNodeInstance> handleFinishedState(engineData: MutableProcessEngineDataAccess,
                                  node: N):PNIPair<N> {
    // XXX todo, handle failed or cancelled tasks
    try {
      if (node.node is EndNode<*, *>) {
        return PNIPair(finish(engineData).apply {
          assert(node.getHandle() !in active)
          assert(node.getHandle() !in finished)
          assert(node.getHandle() in completedEndnodes)
        }, node)
      } else {
        return PNIPair(updateSplits(engineData).startSuccessors(engineData, node), node)
      }
    } catch (e: RuntimeException) {
      engineData.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    } catch (e: SQLException) {
      engineData.rollback()
      Logger.getAnonymousLogger().log(Level.WARNING, "Failure to start follow on task", e)
    }
    return PNIPair(this, node)
  }

  private fun updateSplits(engineData: MutableProcessEngineDataAccess):ProcessInstance {
    return childNodes.asSequence()
        .filterIsInstance(SplitInstance::class.java)
        .fold(this) {
          origProcessInstance, split ->
          split.updateState(engineData, origProcessInstance).instance
        }
  }

/*
  private fun ExtBuilder.updateSplits(engineData: MutableProcessEngineDataAccess):ExtBuilder {
    this.children
      .asSequence()
      .filter { it.valid }
      .map { engineData.nodeInstance(it).withPermission() }
      .filterIsInstance(SplitInstance::class.java)
      .forEach { split ->
        val splitBuilder = split.builder().updateState(engineData, this)
        if (splitBuilder.changed) this.storeChild(splitBuilder.build())
      }
    return this
  }
*/

  @Synchronized @Throws(SQLException::class)
  private fun startSuccessors(engineData: MutableProcessEngineDataAccess,
                              predecessor: ProcessNodeInstance):ProcessInstance {
    val startedTasks = ArrayList<ProcessNodeInstance>(predecessor.node.successors.size)
    val joinsToEvaluate = ArrayList<JoinInstance>()

    var self = this
    for (successorId in predecessor.node.successors) {
      val nodeInstance:ProcessNodeInstance = run {
        val nonRegisteredNodeInstance = processModel.getNode(successorId).mustExist(successorId).createOrReuseInstance(
          engineData, this@ProcessInstance, predecessor, predecessor.entryNo )
        val pair = nonRegisteredNodeInstance.update(engineData) {
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
    self = startedTasks.fold(self) { self, task ->
      when (predecessor.state) {
        NodeInstanceState.Complete  ->
            task.provideTask(engineData, self).instance
        NodeInstanceState.SkippedCancel,
        NodeInstanceState.SkippedFail,
        NodeInstanceState.Skipped   ->
            self.skipTask(engineData, task, predecessor.state).instance
        NodeInstanceState.Cancelled ->
            self.skipCancelTask(engineData, task).instance
        NodeInstanceState.Failed    ->
            self.skipFailTask(engineData, task).instance
        else                                                                  -> throw ProcessException("Node ${predecessor} is not in a supported state to initiate successors")
      }
    }
    self = joinsToEvaluate.fold(self) {self, join -> join.startTask(engineData, self).instance }
    return self
  }

  @Synchronized @Throws(SQLException::class)
  fun getActivePredecessorsFor(engineData: ProcessEngineDataAccess, join: JoinInstance): Collection<ProcessNodeInstance> {
    return active.asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .filter { it.node.isPredecessorOf(join.node) }
          .toList()
  }

  @Synchronized @Throws(SQLException::class)
  fun getDirectSuccessors(engineData: ProcessEngineDataAccess, predecessor: ProcessNodeInstance): Collection<ComparableHandle<out SecureObject<ProcessNodeInstance>>> {
    checkOwnership(predecessor)
    // TODO rewrite, this can be better with the children in the instance
    val result = ArrayList<ComparableHandle<out SecureObject<ProcessNodeInstance>>>(predecessor.node.successors.size)

    fun addDirectSuccessor(candidate: ProcessNodeInstance,
                           predecessor: ComparableHandle<out SecureObject<ProcessNodeInstance>>) {

      // First look for this node, before diving into it's children
      if (candidate.directPredecessors.any { it.handleValue == predecessor.handleValue }) {
        result.add(candidate.getHandle())
        return
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
      writeHandleAttr("handle", handle)
      writeAttribute("name", name)
      when (processModel) {
        is ExecutableProcessModel -> writeHandleAttr("processModel", processModel.getHandle())
        else -> writeHandleAttr("parentActivity", parentActivity)
      }


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
  private fun XmlWriter.writeActiveNodeRef(transaction: ProcessTransaction, handleNodeInstance: ComparableHandle<out SecureObject<ProcessNodeInstance>>) {

    val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
    startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
      writeNodeRefCommon(nodeInstance)
    }
  }

  @Throws(XmlException::class, SQLException::class)
  private fun XmlWriter.writeResultNodeRef(transaction: ProcessTransaction, handleNodeInstance: ComparableHandle<out SecureObject<ProcessNodeInstance>>) {
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
  fun tickle(transaction: ProcessTransaction, messageService: IMessageService<*>) {
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
            self2 = self2.handleFinishedState(engineData, nodeInstance).instance
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
    private fun <T:ProcessNodeInstance> MutableProcessEngineDataAccess.putNodeInstance(value: InstanceFuture<T,*>):T {
      val handle = (nodeInstances as MutableHandleMap).put(value.origBuilder.build())
      @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
      val newValue = nodeInstance(handle).withPermission() as T
      value.set(newValue)
      return newValue
    }

    @JvmStatic
    private fun <T:ProcessNodeInstance> MutableProcessEngineDataAccess.storeNodeInstance(value: InstanceFuture<T,*>):T {
      val handle = value.origBuilder.handle
      (nodeInstances as MutableHandleMap)[handle] = value.origBuilder.build()
      @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
      return (nodeInstance(handle).withPermission() as T).also { value.set(it) }
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
  return node.update(writableEngineData, body) as ProcessInstance.PNIPair<T>
}