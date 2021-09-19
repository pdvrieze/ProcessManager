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
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmStatic
import kotlin.jvm.Synchronized

@RequiresOptIn("Not safe access to store stuff")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ProcessInstanceStorage

class ProcessInstance : MutableHandleAware<SecureObject<ProcessInstance>>, SecureObject<ProcessInstance>, NodeInstanceSource {

    private class InstanceFuture<T : ProcessNodeInstance<*>, N : ExecutableProcessNode>(val origBuilder: ProcessNodeInstance.Builder<out ExecutableProcessNode, out T>) :
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

    interface Builder : ProcessInstanceContext, NodeInstanceSource {
        val generation: Int
        val pendingChildren: List<Future<out ProcessNodeInstance<*>>>
        override var handle: Handle<SecureObject<ProcessInstance>>
        var parentActivity: Handle<SecureObject<ProcessNodeInstance<*>>>
        var owner: Principal
        var processModel: ExecutableModelCommon
        var instancename: String?
        var uuid: UUID
        var state: State
        val children: List<Handle<SecureObject<ProcessNodeInstance<*>>>>
        val inputs: MutableList<ProcessData>
        val outputs: MutableList<ProcessData>
        fun build(data: MutableProcessEngineDataAccess): ProcessInstance
        fun <T : ProcessNodeInstance<*>> storeChild(child: T): Future<T>

        override fun getChildNodeInstance(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): IProcessNodeInstance =
            allChildNodeInstances { it.handle == handle }.first()

        fun getChildBuilder(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): ProcessNodeInstance.ExtBuilder<*, *>

        fun <N : ExecutableProcessNode> getChildNodeInstance(node: N, entryNo: Int): ProcessNodeInstance.Builder<N, *>? =
            getChildren(node).firstOrNull { it.entryNo == entryNo }

        fun <N : ExecutableProcessNode> getChildren(node: N): Sequence<ProcessNodeInstance.Builder<N, *>>
        fun <T : ProcessNodeInstance<*>> storeChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode, T>): Future<T>
        fun <N : ExecutableProcessNode> updateChild(
            node: N,
            entryNo: Int,
            body: ProcessNodeInstance.Builder<out ExecutableProcessNode, *>.() -> Unit
        )

        override fun allChildNodeInstances(): Sequence<IProcessNodeInstance>

        fun allChildNodeInstances(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance>

        fun active(): Sequence<IProcessNodeInstance> {
            return allChildNodeInstances { !it.state.isFinal }
        }

        /**
         * Store the current instance to the database. This
         */
        fun store(data: MutableProcessEngineDataAccess)

        fun getDirectSuccessorsFor(predecessor: Handle<SecureObject<ProcessNodeInstance<*>>>): Sequence<IProcessNodeInstance> {
            val comparablePred = predecessor.toComparableHandle()
            return allChildNodeInstances { comparablePred in it.predecessors }
        }

        fun updateSplits(engineData: MutableProcessEngineDataAccess) {
            for (split in allChildNodeInstances { !it.state.isFinal && it.node is Split }) {
                if (split.state != NodeInstanceState.Pending) {
                    updateChild(split) {
                        if ((this as SplitInstance.Builder).updateState(engineData) && !state.isFinal) {
                            state = NodeInstanceState.Complete
                            startSuccessors(engineData, this) // XXX evaluate whether this is needed
                            engineData.queueTickle(this@Builder.handle)
                        }
                    }
                }
            }
        }

        fun updateState(engineData: MutableProcessEngineDataAccess) {
            if (state == State.STARTED) {
                if (active().none()) { // do finish
                    finish(engineData)
                }
            }
        }

        fun startSuccessors(engineData: MutableProcessEngineDataAccess, predecessor: IProcessNodeInstance) {
            assert((predecessor.state.isFinal) || (predecessor !is SplitInstance)) {
                "The predecessor $predecessor is not final when starting successors"
            }

            val startedTasks = ArrayList<ProcessNodeInstance.Builder<*, *>>(predecessor.node.successors.size)
            val joinsToEvaluate = ArrayList<JoinInstance.Builder>()
            val nodesToSkipSuccessorsOf = mutableListOf<ProcessNodeInstance.Builder<*, *>>()
            val nodesToSkipPredecessorsOf = ArrayDeque<ProcessNodeInstance.Builder<*, *>>()

            for (successorId in predecessor.node.successors) {
                val nonRegisteredNodeInstance = processModel
                    .getNode(successorId.id)
                    .mustExist(successorId)
                    .createOrReuseInstance(engineData, this, predecessor, predecessor.entryNo, false)

                // TODO remove the line below
//                nonRegisteredNodeInstance.predecessors.add(predecessor.handle.toComparableHandle())
                val conditionResult = nonRegisteredNodeInstance.condition( this, predecessor)
                if (conditionResult == ConditionResult.NEVER) {
                    nonRegisteredNodeInstance.state = NodeInstanceState.Skipped
                    storeChild(nonRegisteredNodeInstance)//.get()
                    if (nonRegisteredNodeInstance is JoinInstance.Builder) {
                        nodesToSkipPredecessorsOf.add(nonRegisteredNodeInstance)
                    }
                    nodesToSkipSuccessorsOf.add(nonRegisteredNodeInstance)
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

            for (node in nodesToSkipSuccessorsOf) {
                skipSuccessors(engineData, node, NodeInstanceState.Skipped)
            }

            for (startedTask in startedTasks) {
                when (predecessor.state) {
                    NodeInstanceState.Complete -> engineData.queueTickle(handle)//startedTask.provideTask(engineData)
                    NodeInstanceState.SkippedCancel,
                    NodeInstanceState.SkippedFail,
                    NodeInstanceState.Skipped -> startedTask.skipTask(engineData, predecessor.state)
                    NodeInstanceState.Cancelled -> startedTask.skipTask(engineData, NodeInstanceState.SkippedCancel)
                    NodeInstanceState.Failed -> startedTask.skipTask(engineData, NodeInstanceState.SkippedFail)
                    else -> throw ProcessException("Node $predecessor is not in a supported state to initiate successors")
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
            assert(predecessor.handle.isValid) {
                "Successors can only be skipped for a node with a handle"
            }
            // this uses handles as updates can happen intermediately
            val joinsToEvaluate = mutableListOf<Handle<SecureObject<ProcessNodeInstance<*>>>>()
            for (successorNode in predecessor.node.successors.map { processModel.getNode(it.id)!! }.toList()) {
                // Attempt to get the successor instance as it may already be final. In that case the system would attempt to
                // create a new instance. We don't want to do that just to skip it.
                val successorInstance = getChildNodeInstance(successorNode, predecessor.entryNo)
                    ?: successorNode.createOrReuseInstance(
                        engineData,
                        this,
                        predecessor,
                        predecessor.entryNo,
                        true
                    ).also { storeChild(it) }
                if (!successorInstance.state.isFinal) { // If the successor is already final no skipping is possible.
                    if (successorInstance is JoinInstance.Builder) {
                        if (! successorInstance.handle.isValid) store(engineData)
                        assert(successorInstance.handle.isValid) // the above should make the handle valid
                        joinsToEvaluate.add(successorInstance.handle)
                    } else {
                        successorInstance.skipTask(engineData, state)
                    }
                }
            }
            for (joinHandle in joinsToEvaluate) {
                updateChild(joinHandle) {
                    startTask(engineData)
                }
            }
        }


        /**
         * Trigger the instance to reactivate pending tasks.
         * @param engineData The database data to use
         *
         * @param messageService The message service to use for messenging.
         */
        fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
            val self = this
            val children = allChildNodeInstances().toList()
            // make a copy as the list may be changed due to tickling.
            val nonFinalChildren = mutableListOf<Handle<SecureObject<ProcessNodeInstance<*>>>>()
            val successorCounts = IntArray(children.size)

            for (child in children) { // determine the children of interest
                for (predHandle in child.predecessors) {
                    val predIdx = children.indexOfFirst { it.handle == predHandle }
                    successorCounts[predIdx]++
                }
                if (! child.state.isFinal) {
                    nonFinalChildren.add(child.handle)
                }
            }

            for (hNodeInstance in nonFinalChildren) {
                updateChild(hNodeInstance) {
                    tickle(engineData, messageService)
                }
                val newNodeInstance = engineData.nodeInstance(hNodeInstance).withPermission()
                if (newNodeInstance.state.isFinal) {
                    handleFinishedState(engineData, newNodeInstance)
                }
            }

            // If children don't have the needed amount of successors, we handle the finished state
            // this should start those successors.
            for ((idx, child) in children.withIndex()) {
                if (child.state.isFinal && successorCounts[idx] < child.node.successors.size) {
                    handleFinishedState(engineData, child)
                }
            }

            updateSplits(engineData)
            updateState(engineData)
            if (!state.isFinal && active().none()) {
                self.finish(engineData)
            }
        }

        //        @Synchronized
        private fun <N : IProcessNodeInstance> handleFinishedState(
            engineData: MutableProcessEngineDataAccess,
            nodeInstance: N
        ) {

            // XXX todo, handle failed or cancelled tasks
            try {
                if (nodeInstance.node is EndNode) {
                    if (!state.isFinal) {
                        engineData.logger.log(
                            LogLevel.WARNING,
                            "Calling finish on a process instance that should already be finished."
                        )
                        finish(engineData).apply {
                            val h = nodeInstance.handle
                            assert(getChildNodeInstance(h).let { it.state.isFinal && it.node is ExecutableEndNode })
                        }
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

            // TODO("Make the state dependent on the kind of child state")
            val endNodes = allChildNodeInstances { it.state.isFinal && it.node is EndNode }.toList()
            if (endNodes.count() >= processModel.endNodeCount) {
                var multiInstanceEndNode = false
                var success = 0
                var cancelled = 0
                var fail = 0
                var skipped = 0
                for (endNode in endNodes) {
                    if (endNode.node.isMultiInstance) {
                        multiInstanceEndNode = true
                    }
                    when (endNode.state) {
                        NodeInstanceState.Complete -> success++
                        NodeInstanceState.Cancelled -> cancelled++
                        NodeInstanceState.SkippedCancel -> {
                            skipped++; cancelled++
                        }
                        NodeInstanceState.SkippedFail -> {
                            skipped++; fail++
                        }
                        NodeInstanceState.SkippedInvalidated -> {
                            skipped++
                        }
                        NodeInstanceState.Failed -> fail++
                        NodeInstanceState.Skipped -> skipped++
                        else -> throw AssertionError("Unexpected state for end node: $endNode")
                    }
                }
                val active = active()
                if (!multiInstanceEndNode || active.none()) {
                    active.forEach {
                        updateChild(it) {
                            cancelAndSkip(engineData)
                        }
                    }
                    when {
                        fail > 0 -> state = State.FAILED
                        cancelled > 0 -> state = State.CANCELLED
                        success > 0 -> state = State.FINISHED
                        skipped > 0 -> state = State.SKIPPED
                        else -> state = State.CANCELLED
                    }
                }
                store(engineData)
                if (state == State.FINISHED) {
                    for (output in processModel.exports) {
                        val x = output.applyFromProcessInstance(engineData, this)
                        outputs.add(x)
                    }
                    // Storing here is essential as the updating of the node goes of the database, not the local
                    // state.
                    store(engineData)
                }
                if (parentActivity.isValid) {
                    val parentNodeInstance =
                        engineData.nodeInstance(parentActivity).withPermission() as CompositeInstance
                    engineData.updateInstance(parentNodeInstance.hProcessInstance) {
                        updateChild(parentNodeInstance) {
                            finishTask(engineData)
                        }
                    }
                    store(engineData)
                }
                engineData.commit()
                // TODO don't remove old transactions
                engineData.handleFinishedInstance(handle)
            }
        }

        /**
         * Get the output of this instance as an xml node or `null` if there is no output
         */
        fun getOutputPayload(): ICompactFragment? {
            if (outputs.isEmpty()) return null

            return CompactFragment { xmlWriter ->
                val xmlEncoder = XML
                outputs.forEach { output ->
                    xmlEncoder.encodeToWriter(xmlWriter, output)
                }
            }
        }


    }

    data class BaseBuilder(
        override var handle: Handle<SecureObject<ProcessInstance>> = getInvalidHandle(),
        override var owner: Principal = SYSTEMPRINCIPAL,
        override var processModel: ExecutableModelCommon,
        override var instancename: String? = null,
        override var uuid: UUID = randomUUID(),
        override var state: State = State.NEW,
        override var parentActivity: Handle<SecureObject<ProcessNodeInstance<*>>> /*= Handles.getInvalid()*/
    ) : Builder {
        override var generation: Int = 0
            private set
        private val _pendingChildren = mutableListOf<InstanceFuture<out ProcessNodeInstance<*>, *>>()
        override val pendingChildren: List<Future<out ProcessNodeInstance<*>>> get() = _pendingChildren
        internal var rememberedChildren: MutableList<ProcessNodeInstance<*>> = mutableListOf()
        override val children: List<Handle<SecureObject<ProcessNodeInstance<*>>>>
            get() = rememberedChildren.map(ProcessNodeInstance<*>::handle)
        override val inputs = mutableListOf<ProcessData>()
        override val outputs = mutableListOf<ProcessData>()

        override fun allChildNodeInstances(): Sequence<IProcessNodeInstance> {
            return _pendingChildren.asSequence().map { it.origBuilder }
        }

        override fun allChildNodeInstances(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance> {
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
                    ?.let { _ ->
                        throw ProcessException("Attempting to store a new child with an already existing handle")
                    }
                if (_pendingChildren.none { it.origBuilder == child }) _pendingChildren.add(this)
            }
        }

        override fun getChildBuilder(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): ProcessNodeInstance.ExtBuilder<*, *> {
            val childNode = rememberedChildren.asSequence()
                .filter { it.handle == handle }
                .first()
            val builder = childNode.builder(this)
            _pendingChildren.add(InstanceFuture<ProcessNodeInstance<*>, ExecutableProcessNode>(builder))
            return builder
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
            val existingBuilder = _pendingChildren
                .firstOrNull { it.origBuilder.node == node && it.origBuilder.entryNo == entryNo }
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

    class ExtBuilder(base: ProcessInstance) : Builder {
        @set:ProcessInstanceStorage
        internal var base = base
            private set(value) {
                field = value
                generation = value.generation+1
                _pendingChildren.clear()
            }
        override var generation = base.generation + 1
            private set(value) {
                assert(value == base.generation+1)
                field = value
            }

        private val _pendingChildren =
            mutableListOf<InstanceFuture<out ProcessNodeInstance<*>, out ExecutableProcessNode>>()
        override val pendingChildren: List<Future<out ProcessNodeInstance<*>>> get() = _pendingChildren
        override var handle: Handle<SecureObject<ProcessInstance>> by overlay { base.handle }
        override var parentActivity: Handle<SecureObject<ProcessNodeInstance<*>>> by overlay { base.parentActivity }

        override var owner: Principal by overlay { base.owner }
        override var processModel: ExecutableModelCommon by overlay { base.processModel }
        override var instancename: String? by overlay { base.name }
        override var uuid: UUID by overlay({ newVal ->
                                               generation = 0; handle = getInvalidHandle(); newVal
                                           }) { base.uuid }
        override var state: State by overlay(base = { base.state }, update = { newValue ->
            if (base.state.isFinal) {
                throw IllegalStateException("Cannot change from final instance state ${base.state} to ${newValue}")
            }
            newValue
        })
        override val children: List<Handle<SecureObject<ProcessNodeInstance<*>>>>
            get() = base.childNodes.map {
                it.withPermission()
                    .handle
            }
        override val inputs: MutableList<ProcessData> by lazy { base.inputs.toMutableList() }
        override val outputs: MutableList<ProcessData> by lazy { base.outputs.toMutableList() }

        override fun allChildNodeInstances(): Sequence<IProcessNodeInstance> {
            val pendingChildren = _pendingChildren.asSequence().map { it.origBuilder }
            val pendingHandles = pendingChildren.map { it.handle }.toSet()
            return pendingChildren + base.childNodes.asSequence()
                .map { it.withPermission() }
                .filter { it.handle !in pendingHandles }
        }

        override fun allChildNodeInstances(childFilter: (IProcessNodeInstance) -> Boolean): Sequence<IProcessNodeInstance> {
            val pendingHandles = mutableSetOf<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>()
            val pendingChildren =
                _pendingChildren.map { pendingHandles.add(it.origBuilder.handle.toComparableHandle()); it.origBuilder }
                    .filter { childFilter(it) }
            return pendingChildren.asSequence() + base.childNodes.asSequence()
                .map { it.withPermission() }
                .filter { it.handle !in pendingHandles && childFilter(it) }
        }

        override fun build(data: MutableProcessEngineDataAccess): ProcessInstance {
            return ProcessInstance(data, this)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ProcessNodeInstance<*>> storeChild(child: T) = storeChild(child.builder(this)) as Future<T>

        override fun <T : ProcessNodeInstance<*>> storeChild(child: ProcessNodeInstance.Builder<out ExecutableProcessNode, T>): Future<T> {
            return InstanceFuture<T, ExecutableProcessNode>(child).apply {
                val existingIdx = _pendingChildren.indexOfFirst {
                    it.origBuilder == child ||
                        (child.handle.isValid && it.origBuilder.handle == child.handle) ||
                        (it.origBuilder.node == child.node && it.origBuilder.entryNo == child.entryNo)
                }

                if (existingIdx >= 0) {
                    _pendingChildren[existingIdx] = this
                } else if (_pendingChildren.any { child.handle.isValid && it.origBuilder.handle == child.handle && it.origBuilder != child }) {
                    throw ProcessException("Attempting to store a new child with an already existing handle")
                } else {
                    _pendingChildren.add(this)
                }
            }
        }


        override fun getChildBuilder(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): ProcessNodeInstance.ExtBuilder<*,*> {
            if (! handle.isValid) throw IllegalArgumentException("Cannot look up with invalid handles")
            _pendingChildren.asSequence()
                .map { it.origBuilder as? ProcessNodeInstance.ExtBuilder }
                .firstOrNull { it?.handle == handle }
                ?.let { return it }
            return base.childNodes.asSequence()
                .map { it.withPermission() }
                .first { it.handle == handle }
                .let {
                    it.builder(this).also { _pendingChildren.add(InstanceFuture(it)) }
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

        @ProcessInstanceStorage
        override fun store(data: MutableProcessEngineDataAccess) {
            // TODO: monitor for changes
            val newInstance = build(data)
            data.instances[handle] = newInstance
            base = newInstance
            _pendingChildren.clear()

        }

        @ProcessInstanceStorage
        @PublishedApi
        internal fun __storeNewValueIfNeeded(
            writableEngineData: MutableProcessEngineDataAccess,
            newInstanceBuilderXX: ExtBuilder
        ): ProcessInstance {

            val newInstance = build(writableEngineData)
            val base = this.base

            fun dataValid(): Boolean {
                val stored = writableEngineData.instance(handle).withPermission()
                assert(uuid == base.uuid) { "Uuid mismatch this: $uuid, base: ${base.uuid}" }
                assert(stored.uuid == base.uuid) { "Uuid mismatch this: $uuid, stored: ${stored.uuid}" }
                assert(newInstance.uuid == base.uuid) { "Uuid mismatch this: $uuid, new: ${newInstance.uuid}" }
                assert(base.generation == stored.generation) {
                    "Generation mismatch this: ${base.generation} stored: ${stored.generation} - $base"
                }
                assert(base.generation + 1 == newInstance.generation) { "Generation mismatch this+1: ${base.generation + 1} new: ${newInstance.generation}" }
                return newInstance.handle.isValid && handle.isValid
            }

            if (handle.isValid && handle.isValid) {
                assert(dataValid()) { "Instance generations lost in the waves" }
                writableEngineData.instances[handle] = newInstance
                this.base = newInstance
                return newInstance
            }
            this.base = newInstance
            return newInstance
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

        @ProcessInstanceStorage
        fun start(engineData: MutableProcessEngineDataAccess, payload: CompactFragment? = null) {
            if (state == State.NEW) initialize()

            state = State.STARTED
            inputs.addAll(processModel.toInputs(payload))

            store(engineData) // make sure we have a valid handle

            for (task in active()) {
                updateChild(task) {
                    provideTask(engineData)
                }
            }

        }


    }

    enum class State {
        NEW,
        INITIALIZED,
        STARTED,
        FINISHED {
            override val isFinal: Boolean get() = true
        },
        SKIPPED {
            override val isFinal: Boolean get() = true
        },
        FAILED {
            override val isFinal: Boolean get() = true
        },
        CANCELLED {
            override val isFinal: Boolean get() = true
        };

        open val isFinal: Boolean get() = false
    }

    class ProcessInstanceRef(processInstance: ProcessInstance) : ComparableHandle<SecureObject<ProcessInstance>>,
                                                                 XmlSerializable {

        override val handleValue = processInstance.handle.handleValue

        val processModel = processInstance.processModel.rootModel.handle

        val name: String = processInstance.name.let {
            if (it.isNullOrBlank()) {
                buildString {
                    append(processInstance.processModel.rootModel.name)
                    if (processInstance.processModel !is ExecutableProcessModel) append(" child") else append(' ')
                    append("instance ").append(handleValue)
                }
            } else it
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

    val parentActivity: Handle<SecureObject<ProcessNodeInstance<*>>>

    val children: Sequence<Handle<SecureObject<ProcessNodeInstance<*>>>>
        get() = childNodes.asSequence().map { it.withPermission().handle }

    val activeNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { !it.state.isFinal }

    val active: Collection<Handle<SecureObject<ProcessNodeInstance<*>>>>
        get() = activeNodes
            .map { it.handle }
            .toList()

    val finishedNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { it.state.isFinal && it.node !is EndNode }

    val finished: Collection<Handle<SecureObject<ProcessNodeInstance<*>>>>
        get() = finishedNodes
            .map { it.handle }
            .toList()

    val completedNodeInstances: Sequence<SecureObject<ProcessNodeInstance<*>>>
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filter { it.state.isFinal && it.node is EndNode }

    val completedEndnodes: Collection<Handle<SecureObject<ProcessNodeInstance<*>>>>
        get() = completedNodeInstances
            .map { it.withPermission().handle }
            .toList()

    private val pendingJoinNodes
        get() = childNodes.asSequence()
            .map { it.withPermission() }
            .filterIsInstance<JoinInstance>()
            .filter { !it.state.isFinal }

    private val pendingJoins: Map<ExecutableJoin, JoinInstance>
        get() =
            pendingJoinNodes.associateBy { it.node }

    //    private var _handle: Handle<SecureObject<ProcessInstance>>
    override var handle: Handle<SecureObject<ProcessInstance>>
        private set

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
        handle = builder.handle.toComparableHandle()
        parentActivity = builder.parentActivity

        val pending = builder.pendingChildren.asSequence().map { it as InstanceFuture<*, *> }

        val createdNodes = mutableListOf<ProcessNodeInstance<*>>()
        val updatedNodes = mutableMapOf<Handle<SecureObject<ProcessNodeInstance<*>>>, ProcessNodeInstance<*>>()
        for (future in pending) {
            if (!future.origBuilder.handle.isValid) {
                // Set the handle on the builder so that lookups in the future will be more correct.
                createdNodes += data.putNodeInstance(future).also {
                    future.origBuilder.handle = it.handle.toComparableHandle()
                    future.origBuilder.invalidateBuilder(data) // Actually invalidate the original builder/keep it valid
                }
            } else if((future.origBuilder as? ProcessNodeInstance.ExtBuilder)?.changed != false){
                // We don't store unchanged extBuilders

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
        parentActivity: Handle<SecureObject<ProcessNodeInstance<*>>>,
        body: Builder.() -> Unit
    ) : this(data, BaseBuilder(processModel = processModel, parentActivity = parentActivity).apply(body))

    override fun withPermission() = this

    private fun checkOwnership(node: ProcessNodeInstance<*>) {
        if (node.hProcessInstance != handle) throw ProcessException("The node is not owned by this instance")
    }

    @ProcessInstanceStorage
    inline fun update(
        writableEngineData: MutableProcessEngineDataAccess,
        body: ExtBuilder.() -> Unit
    ): ProcessInstance {
        val newValue = builder().apply(body)
        return newValue.__storeNewValueIfNeeded(writableEngineData, newValue).apply {
            assert(writableEngineData.instances[newValue.handle]?.withPermission() == this) {
                "Process instances should match after storage"
            }
        }
    }

    fun builder() = ExtBuilder(this)

    @OptIn(ProcessInstanceStorage::class)
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

    override fun getChildNodeInstance(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): ProcessNodeInstance<*> {
        return childNodes
            .asSequence()
            .map { it.withPermission() }
            .first { it.handle == handle }
    }

    override fun allChildNodeInstances(): Sequence<IProcessNodeInstance> {
        return childNodes.asSequence().map { it.withPermission() }
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
    fun getOutputPayload(): CompactFragment? {
        if (outputs.isEmpty()) return null
        val str = buildString {
            for (output in outputs) {
                append(generateXmlString(true) { writer ->
                    writer.smartStartTag(output.name!!.toQname()) {
                        output.content.serialize(this)
                    }
                })
            }
        }
        return CompactFragment(str)
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
    ): Collection<Handle<SecureObject<ProcessNodeInstance<*>>>> {
        checkOwnership(predecessor)
        // TODO rewrite, this can be better with the children in the instance
        val result = ArrayList<Handle<SecureObject<ProcessNodeInstance<*>>>>(predecessor.node.successors.size)

        fun addDirectSuccessor(
            candidate: ProcessNodeInstance<*>,
            predecessor: Handle<SecureObject<ProcessNodeInstance<*>>>
        ) {

            // First look for this node, before diving into it's children
            if (candidate.predecessors.any { it.handleValue == predecessor.handleValue }) {
                result.add(candidate.handle)
                return
            }
            candidate.predecessors
                .map { engineData.nodeInstance(it).withPermission() }
                .forEach { successorInstance -> addDirectSuccessor(successorInstance, predecessor) }
        }


        val data = engineData
        active.asSequence()
            .map { data.nodeInstance(it).withPermission() }
            .forEach { addDirectSuccessor(it, predecessor.handle) }

        return result
    }

    @Synchronized
    fun serialize(transaction: ProcessTransaction, writer: XmlWriter) {
        //
        writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "processInstance", Constants.PROCESS_ENGINE_NS_PREFIX) {
            writeHandleAttr("handle", handle)
            writeAttribute("name", name)
            when (processModel) {
                is ExecutableProcessModel -> writeHandleAttr("processModel", processModel.handle)
                else -> writeHandleAttr("parentActivity", parentActivity)
            }


            writeAttribute("owner", owner.getName())
            writeAttribute("state", state.name)

            smartStartTag(Constants.PROCESS_ENGINE_NS, "inputs") {
                val xml = XML
                inputs.forEach { xml.encodeToWriter(this, it) }
            }

            writer.smartStartTag(Constants.PROCESS_ENGINE_NS, "outputs") {
                val xml = XML
                outputs.forEach { xml.encodeToWriter(this, it) }
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
        handleNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>
    ) {

        val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
        startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
            writeNodeRefCommon(nodeInstance)
        }
    }

    private fun XmlWriter.writeResultNodeRef(
        transaction: ProcessTransaction,
        handleNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>
    ) {
        val nodeInstance = transaction.readableEngineData.nodeInstance(handleNodeInstance).withPermission()
        startTag(Constants.PROCESS_ENGINE_NS, "nodeinstance") {
            writeNodeRefCommon(nodeInstance)

            startTag(Constants.PROCESS_ENGINE_NS, "results") {
                val xml = XML
                nodeInstance.results.forEach { xml.encodeToWriter(this, it) }
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
                    (value as InstanceFuture<T, *>).run {
                        set(it)
                    }
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
    childHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
    body: ProcessNodeInstance.ExtBuilder<out ExecutableProcessNode, *>.() -> Unit
): ProcessNodeInstance.ExtBuilder<*, *> {
    return getChildBuilder(childHandle).apply(body)
}

inline fun ProcessInstance.ExtBuilder.updateChild(
    childHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
    body: ProcessNodeInstance.ExtBuilder<out ExecutableProcessNode, *>.() -> Unit
): ProcessNodeInstance.ExtBuilder<*, *> {
    return getChildBuilder(childHandle).apply(body)
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

