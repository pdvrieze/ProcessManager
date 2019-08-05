/*
 * Copyright (c) 2019.
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
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.impl.*
import nl.adaptivity.process.engine.impl.dom.*
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.writeHandleAttr
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.multiplatform.initCauseCompat
import nl.adaptivity.util.multiplatform.randomUUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.*
import kotlin.jvm.JvmStatic
import kotlin.jvm.Synchronized


class ProcessInstance : MutableHandleAware<SecureObject<ProcessInstance>>, SecureObject<ProcessInstance> {

    fun serializable(transaction: ProcessTransaction): XmlSerializable {
        return object : XmlSerializable {
            override fun serialize(out: XmlWriter) {
                serialize(transaction, out)
            }
        }
    }

    private class InstanceFuture<T : ProcessNodeInstance<*>, N : ExecutableProcessNode>(internal val origBuilder: ProcessNodeInstance.Builder<out ExecutableProcessNode, out T>) :
        Future<T> {
        private var cancelled = false

        private var updated: T? = null

        var origSetInvocation: Exception? = null

        override fun isCancelled() = cancelled

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            return if (updated != null || cancelled) return true else {
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
            if (updated != null) throw IllegalStateException("Value already set").apply {
                origSetInvocation?.let { initCauseCompat(it) }
            }
            assert(run { origSetInvocation = Exception("Original set stacktrace"); true })
            updated = value
        }

        override fun isDone() = updated != null

        override fun toString(): String {
            return "-> ${if (updated != null) "!$updated" else origBuilder.toString()}"
        }
    }

    interface Builder {
        val generation: Int
        val pendingChildren: List<Future<out ProcessNodeInstance<*>>>
        var handle: ComparableHandle<SecureObject<ProcessInstance>>
        var parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
        var owner: Principal
        var processModel: ExecutableModelCommon
        var instancename: String?
        var uuid: UUID
        var state: State
        val children: List<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
        val inputs: MutableList<ProcessData>
        val outputs: MutableList<ProcessData>
        fun build(data: MutableProcessEngineDataAccess): ProcessInstance
        fun <T : ProcessNodeInstance<*>> storeChild(child: T): Future<T>
        fun getChild(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): IProcessNodeInstance =
            allChildren { it.handle() == handle }.first()

        fun <N : ExecutableProcessNode> getChild(node: N, entryNo: Int): ProcessNodeInstance.Builder<N, *>? =
            getChildren(node).firstOrNull { it.entryNo == entryNo }

        fun <N : ExecutableProcessNode> getChildren(node: N): Sequence<ProcessNodeInstance.Builder<N, *>>
        fun <T : ProcessNodeInstance<*>> storeChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode, T>): Future<T>
        fun <N : ExecutableProcessNode> updateChild(
            node: N,
            entryNo: Int,
            body: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
                                                   )

        fun allChildren(): Sequence<IProcessNodeInstance>

        fun allChildren(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance>

        fun active(): Sequence<IProcessNodeInstance> {
            return allChildren { !it.state.isFinal }
        }

        /**
         * Store the current instance to the database. This
         */
        fun store(data: MutableProcessEngineDataAccess)

        fun getDirectSuccessorsFor(predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>): Sequence<IProcessNodeInstance> {
            return allChildren { predecessor in it.predecessors }
        }

        fun updateSplits(engineData: MutableProcessEngineDataAccess) {
            for (split in allChildren { !it.state.isFinal && it.node is Split }) {
                updateChild(split) {
                    if ((this as SplitInstance.Builder).updateState(engineData) && state != NodeInstanceState.Complete) {
                        state = NodeInstanceState.Complete
                    }
                }
            }
        }

        fun updateState(engineData: MutableProcessEngineDataAccess) {
            if (state == State.STARTED) {
                if (active().none()) {
                    var success = 0
                    var fail = 0
                    var skipped = 0
                    allChildren { it.state.isFinal && it.node is EndNode }.forEach {
                        when {
                            it.state == NodeInstanceState.Complete -> success++
                            it.state.isSkipped                     -> skipped++
                            it.state == NodeInstanceState.Failed   -> fail++
                            else                                   -> throw AssertionError("Unexpected state for end node: $it")
                        }
                    }
                    when {
                        fail > 0    -> state = State.FAILED
                        success > 0 -> state = State.FINISHED
                        skipped > 0 -> state = State.SKIPPED
                        else        -> state = State.CANCELLED
                    }
                    store(engineData)
                    if (parentActivity.isValid) {
                        val parentNodeInstance =
                            engineData.nodeInstance(parentActivity).withPermission() as CompositeInstance
                        engineData.instance(parentNodeInstance.hProcessInstance).withPermission().update(engineData) {
                            updateChild(parentNodeInstance) {
                                finishTask(engineData)
                            }
                        }
                    }
                }
            }
        }

        fun startSuccessors(engineData: MutableProcessEngineDataAccess, predecessor: IProcessNodeInstance) {
            assert((predecessor.state.isFinal) || (predecessor !is SplitInstance)) {
                "The predecessor $predecessor is not final when starting successors"
            }

            val startedTasks = ArrayList<ProcessNodeInstance.Builder<*, *>>(predecessor.node.successors.size)
            val joinsToEvaluate = ArrayList<JoinInstance.Builder>()

            for (successorId in predecessor.node.successors) {
                val nonRegisteredNodeInstance = processModel
                    .getNode(successorId.id)
                    .mustExist(successorId)
                    .createOrReuseInstance(engineData, this, predecessor, predecessor.entryNo)

                nonRegisteredNodeInstance.predecessors.add(predecessor.handle())
                val conditionResult = nonRegisteredNodeInstance.condition(engineData, predecessor)
                if (conditionResult == ConditionResult.NEVER) {
                    nonRegisteredNodeInstance.state = NodeInstanceState.Skipped
                    storeChild(nonRegisteredNodeInstance)
                    skipSuccessors(engineData, nonRegisteredNodeInstance, NodeInstanceState.Skipped)
                } else if (conditionResult == ConditionResult.TRUE) {
                    storeChild(nonRegisteredNodeInstance)

                    if (nonRegisteredNodeInstance is JoinInstance.Builder) {
                        joinsToEvaluate.add(nonRegisteredNodeInstance)
                    } else {
                        startedTasks.add(nonRegisteredNodeInstance)
                    }

                }

            }
            store(engineData)

            // Commit the registration of the follow up nodes before starting them.
            engineData.commit()

            for (startedTask in startedTasks) {
                when (predecessor.state) {
                    NodeInstanceState.Complete  -> startedTask.provideTask(engineData)
                    NodeInstanceState.SkippedCancel,
                    NodeInstanceState.SkippedFail,
                    NodeInstanceState.Skipped   -> startedTask.skipTask(engineData, predecessor.state)
                    NodeInstanceState.Cancelled -> startedTask.skipTask(engineData, NodeInstanceState.SkippedCancel)
                    NodeInstanceState.Failed    -> startedTask.skipTask(engineData, NodeInstanceState.SkippedFail)
                    else                        -> throw ProcessException("Node $predecessor is not in a supported state to initiate successors")
                }
            }

            for (join in joinsToEvaluate) {
                join.startTask(engineData)
            }

        }

        fun skipSuccessors(
            engineData: MutableProcessEngineDataAccess,
            predecessor: IProcessNodeInstance,
            state: NodeInstanceState
                          ) {
            for (successorNode in predecessor.node.successors.map { processModel.getNode(it.id)!! }.toList()) {
                // Attempt to get the successor instance as it may already be final. In that case the system would attempt to
                // create a new instance. We don't want to do that just to skip it.
                val successorInstance = getChild(successorNode, predecessor.entryNo)
                    ?: successorNode.createOrReuseInstance(
                        engineData,
                        this,
                        predecessor,
                        predecessor.entryNo
                                                          ).also { storeChild(it) }
                successorInstance.skipTask(engineData, state)
            }
        }


        /**
         * Trigger the instance to reactivate pending tasks.
         * @param engineData The database data to use
         *
         * @param messageService The message service to use for messenging.
         */
        fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
            fun ticklePredecessors(successor: IProcessNodeInstance): IProcessNodeInstance {
                successor.predecessors.asSequence()
                    .map { getChild(it) }
                    .filterNotNull()
                    .forEach { pred ->
                        ticklePredecessors(pred)
                        updateChild(pred) {
                            this.tickle(engineData, messageService)
                        }
                    }
                return getChild(successor.handle())
            }

            val self = this
            // make a copy as the list may be changed due to tickling.
            active().toList().forEach { nodeInstance ->
                val newNodeInstance = updateChild(ticklePredecessors(nodeInstance)) {
                    tickle(engineData, messageService)
                }
                if (newNodeInstance.state.isFinal) {
                    handleFinishedState(engineData, newNodeInstance)
                }

            }
            if (active().none()) {
                self.finish(engineData)
            }
        }

        @Synchronized
        private fun <N : IProcessNodeInstance> handleFinishedState(
            engineData: MutableProcessEngineDataAccess,
            nodeInstance: N
                                                                  ) {
            // XXX todo, handle failed or cancelled tasks
            try {
                if (nodeInstance.node is EndNode) {
                    finish(engineData).apply {
                        val h = nodeInstance.handle()
                        assert(getChild(h).let { it.state.isFinal && it.node is ExecutableEndNode })
                    }
                } else {
                    startSuccessors(engineData, nodeInstance)
                }
            } catch (e: RuntimeException) {
                engineData.rollback()
                engineData.logger.log(LogLevel.WARNING, "Failure to start follow on task", e)
            } catch (e: Exception) {
                engineData.rollback()
                engineData.logger.log(LogLevel.WARNING, "Failure to start follow on task", e)
            }
        }

        fun finish(engineData: MutableProcessEngineDataAccess) {
            // This needs to update first as at this point the node state may not be valid.
            // TODO reduce the need to do a double update.
            this.let { newInstance ->
                // TODO("Make the state dependent on the kind of child state")
                val endNodes = allChildren { it.state.isFinal && it.node is EndNode }.toList()
                if (endNodes.count() >= processModel.endNodeCount) {
                    state = when {
                        endNodes.any { it.state == NodeInstanceState.Failed || it.state == NodeInstanceState.SkippedFail }      -> State.FAILED
                        endNodes.any { it.state == NodeInstanceState.Cancelled || it.state == NodeInstanceState.SkippedCancel } -> State.CANCELLED
                        else                                                                                                    -> State.FINISHED
                    }
                    if (parentActivity.isValid) {
                        val parentNode = engineData.nodeInstance(parentActivity).withPermission()
                        val parentInstance = engineData.instance(parentNode.hProcessInstance).withPermission()
                        parentInstance.update(engineData) {
                            updateChild(parentNode) {
                                finishTask(engineData, getOutputPayload())
                            }
                        }
                    }

                    store(engineData)
                    engineData.commit()
                    // TODO don't remove old transactions
                    engineData.handleFinishedInstance(handle)
                }
            }
        }

        /**
         * Get the output of this instance as an xml node or `null` if there is no output
         */
        fun getOutputPayload(): Node? {
            if (outputs.isEmpty()) return null
            val document = newDocumentBuilderFactory().apply { isNamespaceAware = true }.newDocumentBuilder()
                .newDocument()
            return document.createDocumentFragment().apply {
                val writer = DOMResult(this).newWriter()
                try {
                    outputs.forEach { output ->
                        output.serialize(writer)
                    }
                } finally {
                    writer.close()
                }
            }
        }


    }

    data class BaseBuilder(
        override var handle: ComparableHandle<SecureObject<ProcessInstance>> = getInvalidHandle(),
        override var owner: Principal = SYSTEMPRINCIPAL,
        override var processModel: ExecutableModelCommon,
        override var instancename: String? = null,
        override var uuid: UUID = randomUUID(),
        override var state: State = State.NEW,
        override var parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> /*= Handles.getInvalid()*/
                          ) : Builder {
        override var generation: Int = 0
            private set
        private val _pendingChildren = mutableListOf<InstanceFuture<out ProcessNodeInstance<*>, *>>()
        override val pendingChildren: List<Future<out ProcessNodeInstance<*>>> get() = _pendingChildren
        internal var rememberedChildren: MutableList<ProcessNodeInstance<*>> = mutableListOf()
        override val children: List<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
            get() = rememberedChildren.map(ProcessNodeInstance<*>::getHandle)
        override val inputs = mutableListOf<ProcessData>()
        override val outputs = mutableListOf<ProcessData>()

        override fun allChildren(): Sequence<IProcessNodeInstance> {
            return _pendingChildren.asSequence().map { it.origBuilder }
        }

        override fun allChildren(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance> {
            return _pendingChildren.asSequence().map { it.origBuilder }.filter { childFilter(it) }
        }

        override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
            return ProcessInstance(data, this)
        }

        override fun <T : ProcessNodeInstance<*>> storeChild(child: T): Future<T> {
            @Suppress("UNCHECKED_CAST")
            return storeChild(child.builder(this)) as Future<T>
        }

        override fun <T : ProcessNodeInstance<*>> storeChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode, T>): Future<T> {
            return InstanceFuture<T, ExecutableProcessNode>(child).apply {
                if (!handle.isValid) throw IllegalArgumentException("Storing a non-existing child")
                _pendingChildren.firstOrNull { child.handle.isValid && it.origBuilder.handle == child.handle && it.origBuilder != child }
                    ?.let { oldChild ->
                        throw ProcessException("Attempting to store a new child with an already existing handle")
                    }
                if (!_pendingChildren.any { it.origBuilder == child }) _pendingChildren.add(this)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <N : ExecutableProcessNode> getChildren(node: N): Sequence<ProcessNodeInstance.Builder<N, *>> {
            return _pendingChildren.asSequence().filter { it.origBuilder.node == node }
                .map { it.origBuilder as ProcessNodeInstance.Builder<N, *> }
        }

        override fun <N : ExecutableProcessNode> updateChild(
            node: N,
            entryNo: Int,
            body: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
                                                            ) {
            val existingBuilder =
                _pendingChildren.firstOrNull { it.origBuilder.node == node && it.origBuilder.entryNo == entryNo }
                    ?: throw ProcessException("Attempting to update a nonexisting child")
            @Suppress("UNCHECKED_CAST")
            (existingBuilder as ProcessNodeInstance.Builder<N, *>).apply(body)
        }

        override fun store(data: MutableProcessEngineDataAccess) {
            val newInstance = build(data)
            if (handle.isValid) data.instances[handle] = newInstance else handle = data.instances.put(newInstance)
            generation = newInstance.generation + 1
            rememberedChildren.replaceBy(newInstance.childNodes.map { it.withPermission() })
            _pendingChildren.clear()
        }
    }

    class ExtBuilder(internal var base: ProcessInstance) : Builder {
        override var generation = base.generation + 1
            private set

        private val _pendingChildren =
            mutableListOf<InstanceFuture<out ProcessNodeInstance<*>, out ExecutableProcessNode>>()
        override val pendingChildren: List<Future<out ProcessNodeInstance<*>>> get() = _pendingChildren
        override var handle by overlay { base.handle }
        override var parentActivity by overlay { base.parentActivity }

        override var owner by overlay { base.owner }
        override var processModel by overlay { base.processModel }
        override var instancename by overlay { base.name }
        override var uuid by overlay({ generation = 0; handle = getInvalidHandle() }) { base.uuid }
        override var state by overlay { base.state }
        override val children get() = base.childNodes.map { it.withPermission().getHandle() }
        override val inputs by lazy { base.inputs.toMutableList() }
        override val outputs by lazy { base.outputs.toMutableList() }

        override fun allChildren(): Sequence<IProcessNodeInstance> {
            val pendingChildren = _pendingChildren.asSequence().map { it.origBuilder }
            val pendingHandles = pendingChildren.map { it.handle }.toSet()
            return pendingChildren + base.childNodes.asSequence()
                .map { it.withPermission() }
                .filter { it.handle() !in pendingHandles }
        }

        override fun allChildren(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance> {
            val pendingHandles = mutableSetOf<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>()
            val pendingChildren = _pendingChildren.map { pendingHandles.add(it.origBuilder.handle); it.origBuilder }
                .filter { childFilter(it) }
            return pendingChildren.asSequence() + base.childNodes.asSequence()
                .map { it.withPermission() }
                .filter { it.handle() !in pendingHandles && childFilter(it) }
        }

        override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
            return ProcessInstance(data, this)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ProcessNodeInstance<*>> storeChild(child: T) = storeChild(child.builder(this)) as Future<T>

        override fun <T : ProcessNodeInstance<*>> storeChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode, T>): Future<T> {
            return InstanceFuture<T, ExecutableProcessNode>(child).apply {
                val existingIdx =
                    _pendingChildren.indexOfFirst { it.origBuilder == child || (child.handle.isValid && it.origBuilder.handle == child.handle) || (it.origBuilder.node == child.node && it.origBuilder.entryNo == child.entryNo) }
                if (existingIdx >= 0) {
                    _pendingChildren[existingIdx] = this
                } else {
                    _pendingChildren.firstOrNull { child.handle.isValid && it.origBuilder.handle == child.handle && it.origBuilder != child }
                        ?.let { oldChild ->
                            throw ProcessException("Attempting to store a new child with an already existing handle")
                        }
                    _pendingChildren.add(this)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <N : ExecutableProcessNode> getChildren(node: N): Sequence<ProcessNodeInstance.Builder<N, *>> {
            return _pendingChildren.asSequence()
                .filter { it.origBuilder.node == node }
                .map { it.origBuilder as ProcessNodeInstance.Builder<N, *> } +
                base.childNodes.asSequence()
                    .map { it.withPermission() }
                    .filter { child ->
                        child.node == node &&
                            _pendingChildren.none { pending ->
                                child.node == pending.origBuilder.node && child.entryNo == pending.origBuilder.entryNo
                            }
                    }
                    .map {
                        (it.builder(this) as ProcessNodeInstance.Builder<N, *>).also {
                            // The type stuff here is a big hack to avoid having to "know" what the instance type actually is
                            _pendingChildren.add(InstanceFuture<ProcessNodeInstance<*>, ExecutableProcessNode>(it))
                        }
                    }

        }

        override fun <N : ExecutableProcessNode> updateChild(
            node: N,
            entryNo: Int,
            body: ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>>.() -> Unit
                                                            ) {
            @Suppress("UNCHECKED_CAST")
            val existingBuilder = _pendingChildren.asSequence()
                .map { it.origBuilder as ProcessNodeInstance.Builder<N, *> }
                .firstOrNull { it.node == node && it.entryNo == entryNo }
            if (existingBuilder != null) {
                existingBuilder.apply(body); return
            }

            base.childNodes.asSequence()
                .map { it.withPermission() }
                .firstOrNull { it.node == node && it.entryNo == entryNo }
                ?.also {
                    it.builder(this).apply(body)
                    if (it.builder(this).changed) {
                        _pendingChildren.add(InstanceFuture<ProcessNodeInstance<*>, N>(it.builder(this)))
                    }
                } ?: throw ProcessException("Attempting to update a nonexisting child")
        }

        override fun store(data: MutableProcessEngineDataAccess) {
            val newInstance = build(data)
            data.instances[handle] = newInstance
            generation = newInstance.generation + 1
            base = newInstance
            _pendingChildren.clear()

        }

        fun initialize() {
            if (state != State.NEW || base.active.isNotEmpty() || _pendingChildren.any { !it.origBuilder.state.isFinal }) {
                throw IllegalStateException("The instance already appears to be initialised")
            }

            processModel.startNodes.forEach { node ->
                storeChild(
                    node.createOrReuseInstance(
                        this,
                        1
                                              ).build() as DefaultProcessNodeInstance
                          ) // Start with sequence 1
            }
            state = State.INITIALIZED
        }


    }

    enum class State {
        NEW,
        INITIALIZED,
        STARTED,
        FINISHED,
        SKIPPED,
        FAILED,
        CANCELLED
    }

    class ProcessInstanceRef(processInstance: ProcessInstance) : ComparableHandle<SecureObject<ProcessInstance>>,
                                                                 XmlSerializable {

        override val handleValue = processInstance.handle.handleValue

        val processModel = processInstance.processModel.rootModel.getHandle()

        val name: String = processInstance.name.let {
            if (it.isNullOrBlank()) {
                buildString {
                    append(processInstance.processModel.rootModel.name)
                    if (processInstance.processModel !is ExecutableProcessModel) append(" child") else append(' ')
                    append("instance ").append(handleValue)
                }
            } else it!!
        }

        val parentActivity = processInstance.parentActivity

        val uuid: UUID = processInstance.uuid

        val state = processInstance.state

        override fun serialize(out: XmlWriter) {
            out.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
                writeHandleAttr("handle", this@ProcessInstanceRef)
                writeHandleAttr("processModel", processModel)
                writeHandleAttr("parentActivity", parentActivity)
                writeAttribute("name", name)
                writeAttribute("uuid", uuid)
                writeAttribute("state", state)
            }
        }
    }

    val generation: Int

    val processModel: ExecutableModelCommon

    val childNodes: Collection<SecureObject<ProcessNodeInstance<*>>>

    val parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>

    val children: Sequence<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
        get() = childNodes.asSequence().map { it.withPermission().getHandle() }

    val activeNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { !it.state.isFinal }

    val active: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
        get() = activeNodes
            .map { it.getHandle() }
            .toList()

    val finishedNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { it.state.isFinal && it.node !is EndNode }

    val finished: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
        get() = finishedNodes
            .map { it.getHandle() }
            .toList()

    val completedNodeInstances: Sequence<SecureObject<ProcessNodeInstance<*>>>
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { it.state.isFinal && it.node is EndNode }

    val completedEndnodes: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
        get() = completedNodeInstances
            .map { it.withPermission().getHandle() }
            .toList()

    private val pendingJoinNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filterIsInstance<JoinInstance>()
            .filter { !it.state.isFinal }

    private val pendingJoins: Map<ExecutableJoin, JoinInstance>
        get() =
            pendingJoinNodes.associateBy { it.node }

    private var handle: ComparableHandle<SecureObject<ProcessInstance>>

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
        handle = handle(builder.handle)
        parentActivity = builder.parentActivity

        val pending = builder.pendingChildren.asSequence().map { it as InstanceFuture<*, *> }

        val createdNodes = mutableListOf<ProcessNodeInstance<*>>()
        val updatedNodes = mutableMapOf<Handle<SecureObject<ProcessNodeInstance<*>>>, ProcessNodeInstance<*>>()
        for (future in pending) {
            if (!future.origBuilder.handle.isValid) {
                // Set the handle on the builder so that lookups in the future will be more correct.
                createdNodes += data.putNodeInstance(future).also { future.origBuilder.handle = it.handle() }
            } else {
                assert(future.origBuilder.hProcessInstance == handle)
                updatedNodes[future.origBuilder.handle] = data.storeNodeInstance(future)
            }
        }


        val nodes = createdNodes + builder.children.asSequence().map { childHandle ->
            updatedNodes.remove(childHandle) ?: data.nodeInstance(childHandle).withPermission()
        }.toList()

        assert(updatedNodes.isEmpty()) { "All updated nodes must be used, still missing: [${updatedNodes.values.joinToString()}]" }

        childNodes = nodes
        inputs = builder.inputs.toList()
        outputs = builder.outputs.toList()
    }

    constructor(
        data: MutableProcessEngineDataAccess,
        processModel: ExecutableModelCommon,
        parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
        body: Builder.() -> Unit
               ) : this(data, BaseBuilder(processModel = processModel, parentActivity = parentActivity).apply(body))

    override fun withPermission() = this

    private fun checkOwnership(node: ProcessNodeInstance<*>) {
        if (node.hProcessInstance != handle) throw ProcessException("The node is not owned by this instance")
    }

    @Synchronized
    fun initialize(engineData: MutableProcessEngineDataAccess): ProcessInstance {

        return update(engineData) {
            initialize()
        }
    }

    @PublishedApi
    internal fun __storeNewValueIfNeeded(
        writableEngineData: MutableProcessEngineDataAccess,
        newInstanceBuilder: ExtBuilder
                                        ): ProcessInstance {
        val newInstance = newInstanceBuilder.build(writableEngineData)
        val base = newInstanceBuilder.base

        fun dataValid(): Boolean {
            val stored = writableEngineData.instance(handle).withPermission()
            assert(uuid == base.uuid) { "Uuid mismatch this: $uuid, base: ${base.uuid}" }
            assert(stored.uuid == base.uuid) { "Uuid mismatch this: $uuid, stored: ${stored.uuid}" }
            assert(newInstance.uuid == base.uuid) { "Uuid mismatch this: $uuid, new: ${newInstance.uuid}" }
            assert(base.generation == stored.generation) { "Generation mismatch this: ${base.generation} stored: ${stored.generation}" }
            assert(base.generation + 1 == newInstance.generation) { "Generation mismatch this+1: ${base.generation + 1} new: ${newInstance.generation}" }
            return newInstance.handle.isValid && handle.isValid
        }

        if (getHandle().isValid && handle.isValid) {
            assert(dataValid()) { "Instance generations lost in the waves" }
            writableEngineData.instances[handle] = newInstance
            return newInstance
        }
        return newInstance
    }

    inline fun update(
        writableEngineData: MutableProcessEngineDataAccess,
        body: ExtBuilder.() -> Unit
                     ): ProcessInstance {
        val newValue = builder().apply(body)
        return __storeNewValueIfNeeded(writableEngineData, newValue).apply {
            assert(writableEngineData.instances[newValue.handle]?.withPermission() == this) {
                "Process instances should match after storage"
            }
        }
    }

    fun builder() = ExtBuilder(this)

    @Synchronized
    fun finish(engineData: MutableProcessEngineDataAccess): ProcessInstance {
        // This needs to update first as at this point the node state may not be valid.
        // TODO reduce the need to do a double update.
        update(engineData) {}.let { newInstance ->
            if (newInstance.completedNodeInstances.count() >= processModel.endNodeCount) {
                // TODO mark and store results
                return newInstance.update(engineData) {
                    state = State.FINISHED
                }.apply {
                    if (parentActivity.isValid) {
                        val parentNode = engineData.nodeInstance(parentActivity).withPermission()
                        val parentInstance = engineData.instance(parentNode.hProcessInstance).withPermission()
                        parentInstance.update(engineData) {
                            updateChild(parentNode) {
                                finishTask(engineData, getOutputPayload())
                            }
                        }
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

    @Synchronized
fun getNodeInstances(identified: Identified): Sequence<ProcessNodeInstance<*>> {
        return childNodes.asSequence().map { it.withPermission() }.filter { it.node.id == identified.id }
    }

    @Synchronized
fun getNodeInstance(identified: Identified, entryNo: Int): ProcessNodeInstance<*>? {
        return childNodes.asSequence().map { it.withPermission() }
            .firstOrNull { it.node.id == identified.id && it.entryNo == entryNo }
    }

    @Synchronized
    override fun setHandleValue(handleValue: Long) {
        if (handle.handleValue != handleValue) {
            if (handleValue == -1L) {
                throw IllegalArgumentException("Setting the handle to invalid is not allowed")
            }
            if (handle.isValid) throw IllegalStateException("Handles are not allowed to change")
            handle = handle(handle = handleValue)
        }
    }

    fun getChild(nodeId: String, entryNo: Int): SecureObject<ProcessNodeInstance<*>>? {
        return childNodes.firstOrNull { it.withPermission().run { node.id == nodeId && this.entryNo == entryNo } }
    }

    /**
     * Get the output of this instance as an xml node or `null` if there is no output
     */
    fun getOutputPayload(): Node? {
        if (outputs.isEmpty()) return null
        val document =
            newDocumentBuilderFactory().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()
        return document.createDocumentFragment().apply {
            val writer = DOMResult(this).newWriter()
            try {
                outputs.forEach { output ->
                    output.serialize(writer)
                }
            } finally {
                writer.close()
            }
        }
    }

    fun start(engineData: MutableProcessEngineDataAccess, payload: Node? = null): ProcessInstance {
        return (if (state == State.NEW) initialize(engineData) else this)
            .update(engineData) {
                state = State.STARTED
                inputs.addAll(processModel.toInputs(payload))

                store(engineData) // make sure we have a valid handle

                for (task in active()) {
                    updateChild(task) {
                        provideTask(engineData)
                    }
                }

            }
/*
      .run { // need run as the this needs to be captured at fold
        active.asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .filter { !it.state.isFinal }
          .fold(this) { self, task -> task.provideTask(engineData, self).instance }
      }
*/

    }

    @Synchronized
fun getActivePredecessorsFor(
        engineData: ProcessEngineDataAccess,
        join: JoinInstance
                                ): Collection<ProcessNodeInstance<*>> {
        return active.asSequence()
            .map { engineData.nodeInstance(it).withPermission() }
            .filter { it.node.isPredecessorOf(join.node) }
            .toList()
    }

    @Synchronized
fun getDirectSuccessors(
        engineData: ProcessEngineDataAccess,
        predecessor: ProcessNodeInstance<*>
                           ): Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> {
        checkOwnership(predecessor)
        // TODO rewrite, this can be better with the children in the instance
        val result = ArrayList<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>(predecessor.node.successors.size)

        fun addDirectSuccessor(
            candidate: ProcessNodeInstance<*>,
            predecessor: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
                              ) {

            // First look for this node, before diving into it's children
            if (candidate.predecessors.any { it.handleValue == predecessor.handleValue }) {
                result.add(candidate.getHandle())
                return
            }
            candidate.predecessors
                .map { engineData.nodeInstance(it).withPermission() }
                .forEach { successorInstance -> addDirectSuccessor(successorInstance, predecessor) }
        }


        val data = engineData
        active.asSequence()
            .map { data.nodeInstance(it).withPermission() }
            .forEach { addDirectSuccessor(it, predecessor.getHandle()) }

        return result
    }

    @Synchronized
    fun serialize(transaction: ProcessTransaction, writer: XmlWriter) {
        //
        writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
            writeHandleAttr("handle", handle)
            writeAttribute("name", name)
            when (processModel) {
                is ExecutableProcessModel -> writeHandleAttr("processModel", processModel.getHandle())
                else                      -> writeHandleAttr("parentActivity", parentActivity)
            }


            writeAttribute("owner", owner.getName())
            writeAttribute("state", state.name)

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

    private fun XmlWriter.writeActiveNodeRef(
        transaction: ProcessTransaction,
        handleNodeInstance: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
                                            ) {

        val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
        startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
            writeNodeRefCommon(nodeInstance)
        }
    }

    private fun XmlWriter.writeResultNodeRef(
        transaction: ProcessTransaction,
        handleNodeInstance: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>
                                            ) {
        val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
        startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
            writeNodeRefCommon(nodeInstance)

            startTag(Constants.PROCESS_ENGINE_NS, "results") {
                nodeInstance.results.forEach { it.serialize(this) }
            }
        }
    }

    override fun toString(): String {
        return "ProcessInstance(handle=${handle.handleValue}, name=$name, state=$state, generation=$generation, childNodes=$childNodes)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.getClass() != getClass()) return false

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
        result = 31 * result + state.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    companion object {

        private val serialVersionUID = 1145452195455018306L

        @Suppress("NOTHING_TO_INLINE")
        private fun MutableProcessEngineDataAccess.putNodeInstance(value: InstanceFuture<*, *>): ProcessNodeInstance<*> {

            fun <T : ProcessNodeInstance<T>> impl(value: InstanceFuture<*, *>): ProcessNodeInstance<*> {
                val handle = (nodeInstances as MutableHandleMap).put(value.origBuilder.build())
                value.origBuilder.handle =
                    handle // Update the builder handle as when merely storing the original builder will remain used and needs to be updated.
                @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
                val newValue = nodeInstance(handle).withPermission() as T
                @Suppress("UNCHECKED_CAST")
                (value as InstanceFuture<T, *>).set(newValue)
                return newValue
            }

            return impl<DefaultProcessNodeInstance>(value)
        }

        @JvmStatic
        private fun MutableProcessEngineDataAccess.storeNodeInstance(value: InstanceFuture<*, *>): ProcessNodeInstance<*> {
            fun <T : ProcessNodeInstance<T>> impl(value: InstanceFuture<*, *>): ProcessNodeInstance<*> {
                val handle = value.origBuilder.handle
                (nodeInstances as MutableHandleMap)[handle] = value.origBuilder.build()
                @Suppress("UNCHECKED_CAST") // Semantically this should always be valid
                return (nodeInstance(handle).withPermission() as T).also {
                    (value as InstanceFuture<T, *>).set(it)
                }
            }

            return impl<DefaultProcessNodeInstance>(value) // hack to work around generics issues
        }

        private fun XmlWriter.writeNodeRefCommon(nodeInstance: ProcessNodeInstance<*>) {
            writeAttribute("nodeid", nodeInstance.node.id)
            writeAttribute("handle", nodeInstance.getHandleValue())
            attribute(null, "state", null, nodeInstance.state.toString())
            if (nodeInstance.state === NodeInstanceState.Failed) {
                val failureCause = nodeInstance.failureCause
                val value =
                    if (failureCause == null) "<unknown>" else "${failureCause.getClass()}: " + failureCause.message
                attribute(null, "failureCause", null, value)
            }

        }
    }

}


inline fun ProcessInstance.Builder.updateChild(
    childHandler: Handle<SecureObject<ProcessNodeInstance<*>>>,
    body: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
                                              ): ProcessNodeInstance.Builder<*, *> {
    return updateChild(getChild(childHandler), body)
}

inline fun ProcessInstance.Builder.updateChild(
    node: IProcessNodeInstance,
    body: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
                                              ): ProcessNodeInstance.Builder<*, *> {
    if (node is ProcessNodeInstance.Builder<*, *>) {
        return node.apply(body)
    } else {
        return node.builder(this).apply {
            body()
            storeChild(this)
        }
    }
}

