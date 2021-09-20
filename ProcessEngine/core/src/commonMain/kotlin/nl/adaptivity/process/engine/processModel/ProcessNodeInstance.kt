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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.*
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.processModel.IPlatformXmlResultType
import nl.adaptivity.process.processModel.MessageActivity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.addSuppressedCompat
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Base interface for process instance.
 * @property node The node that this is an instance of.
 * @param predecessors The node instances that are direct predecessors of this one
 * @property hProcessInstance The handle to the owning process instance.
 * @property owner The owner of the node (generally the owner of the instance)
 * @param handle The handle for this instance (or invalid if not registered yet)
 * @property state The current state of the instance
 * @param results A list of the results associated with this node. This would imply a state of [NodeInstanceState.Complete]
 * @property entryNo The sequence number of this instance. Normally this will be 1, but for nodes that allow reentry,
 *                   this may be a higher number. Values below 1 are invalid.
 * @property failureCause For a failure, the cause of the failure
 */
abstract class ProcessNodeInstance<T : ProcessNodeInstance<T>>(
    override val node: ExecutableProcessNode,
    predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*>>>>,
    processInstanceBuilder: ProcessInstance.Builder,
    override val hProcessInstance: Handle<SecureObject<ProcessInstance>>,
    final override val owner: Principal,
    final override val entryNo: Int,
    handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
    final override val state: NodeInstanceState = Pending,
    results: Iterable<ProcessData> = emptyList(),
    val failureCause: Throwable? = null
) : SecureObject<ProcessNodeInstance<T>>,
    ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>>,
    IProcessNodeInstance {

    private val _handle: Handle<SecureObject<ProcessNodeInstance<*>>> = handle

    override val handle: Handle<SecureObject<ProcessNodeInstance<*>>> get() = _handle

    override val results: List<ProcessData> = results.toList()

    override val predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*>>>> =
        predecessors.asSequence().filter { it.isValid }.toArraySet()

    init {
        @Suppress("LeakingThis")
        if (state != SkippedInvalidated &&
            !(node.isMultiInstance || ((node as? ExecutableJoin)?.isMultiMerge == true))
        ) {
            if (processInstanceBuilder.allChildNodeInstances { it.node == node && it.entryNo != entryNo && it.state != SkippedInvalidated }
                    .any()) {
                throw ProcessException("Attempting to create a new instance $entryNo for node $node that does not support reentry")
            }
        }
    }

    constructor(builder: Builder<*, T>) : this(
        builder.node, builder.predecessors,
        builder.processInstanceBuilder,
        builder.hProcessInstance, builder.owner,
        builder.entryNo, builder.handle, builder.state,
        builder.results, builder.failureCause
    )

    override val processContext: ProcessInstanceContext
        get() = object : ProcessInstanceContext {
            override val handle: Handle<SecureObject<ProcessInstance>> get() = hProcessInstance
        }

    override fun build(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance<T> = this

    override abstract fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<out ExecutableProcessNode, T>

    private fun precedingClosure(processData: ProcessEngineDataAccess): Sequence<SecureObject<ProcessNodeInstance<*>>> {
        return predecessors.asSequence().flatMap { predHandle ->
            val pred = processData.nodeInstance(predHandle).withPermission()
            pred.precedingClosure(processData) + sequenceOf(pred)
        }
    }

    fun update(
        processInstanceBuilder: ProcessInstance.Builder,
        body: Builder<out ExecutableProcessNode, T>.() -> Unit
    ): Future<out T>? {
        val builder = builder(processInstanceBuilder).apply(body)

        return processInstanceBuilder.storeChild(builder)
            .let { if (builder.changed) it else null }
    }

    @Suppress("UNCHECKED_CAST")
    private inline val asT
        get() = this as T

    override fun withPermission(): ProcessNodeInstance<T> = this

    private fun hasDirectPredecessor(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): Boolean {
        return predecessors.any { it.handleValue == handle.handleValue }
    }

    fun resolvePredecessors(engineData: ProcessEngineDataAccess): Collection<ProcessNodeInstance<*>> {
        return predecessors.asSequence().map {
            engineData.nodeInstance(it).withPermission()
        }.toList()
    }

    fun getHandleValue(): Long {
        return _handle.handleValue
    }

    override fun toString(): String {
        return "nodeInstance  ($handle, ${node.id}[$entryNo] - $state)"
    }

    fun serialize(nodeInstanceSource: IProcessInstance, out: XmlWriter, localEndpoint: EndpointDescriptor) {
        out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
            writeAttribute("state", state.name)
            writeAttribute("processinstance", hProcessInstance.handleValue)

            if (_handle.isValid) writeAttribute("handle", _handle.handleValue)

            writeAttribute("nodeid", node.id)

            predecessors.forEach {
                writeSimpleElement(
                    XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME,
                    it.handleValue.toString()
                )
            }

            for (result in results) {
                XML.encodeToWriter(this, result)
            }

            (node as? MessageActivity)?.message?.messageBody?.let { body ->
                instantiateXmlPlaceholders(nodeInstanceSource, body.getXmlReader(), out, true, localEndpoint)
            }
        }
    }

    fun toSerializable(engineData: ProcessEngineDataAccess, localEndpoint: EndpointDescriptor): XmlProcessNodeInstance {
        val builder = builder(engineData.instance(hProcessInstance).withPermission().builder())

        val body: ICompactFragment? = (node as? MessageActivity)?.message?.let { message ->
            try {
                val xmlReader = message.messageBody.getXmlReader()
                instantiateXmlPlaceholders(builder.processInstanceBuilder, xmlReader, true, localEndpoint)
            } catch (e: XmlException) {
                engineData.logger.log(LogLevel.WARNING, "Error processing body", e)
                throw e
            }
        }

        return builder.toXmlInstance(body)
    }

    interface Builder<N : ExecutableProcessNode, T : ProcessNodeInstance<*>> : IProcessNodeInstance {
        override var node: N
        override val predecessors: MutableSet<Handle<SecureObject<ProcessNodeInstance<*>>>>
        val processInstanceBuilder: ProcessInstance.Builder
        override val hProcessInstance: Handle<SecureObject<ProcessInstance>> get() = processInstanceBuilder.handle
        override var owner: Principal
        override var handle: Handle<SecureObject<ProcessNodeInstance<*>>>
        override var state: NodeInstanceState
        override val results: MutableList<ProcessData>
        fun toXmlInstance(body: ICompactFragment?): XmlProcessNodeInstance
        override val entryNo: Int
        var failureCause: Throwable?

        fun invalidateBuilder(engineData: ProcessEngineDataAccess)

        fun build(): T

        override fun builder(processInstanceBuilder: ProcessInstance.Builder) = this

        fun failTaskCreation(cause: Throwable) {
            failureCause = cause
            state = FailRetry
        }

        fun failTaskExecution(cause: Throwable) {
            failureCause = cause
            state = Failed
        }

        /**
         * Store the current state of the builder to the database.
         */
        fun store(engineData: MutableProcessEngineDataAccess) {
            val mutableNodeInstances =
                engineData.nodeInstances as MutableHandleMap<SecureObject<ProcessNodeInstance<*>>>
            if (handle.isValid) {
                mutableNodeInstances[handle] = build()
            } else {
                processInstanceBuilder.storeChild(this)
            }
            // Must be updated as well as the process node instance may mean the process instance is changed.
            processInstanceBuilder.store(engineData)
            engineData.commit()
        }

        fun failTask(engineData: MutableProcessEngineDataAccess, cause: Throwable)

        /** Function that will eventually do progression */
        fun provideTask(engineData: MutableProcessEngineDataAccess)

        /** Function that will do provision, but not progress. This is where custom implementations live */
        fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean

        fun takeTask(engineData: MutableProcessEngineDataAccess)
        fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean

        fun startTask(engineData: MutableProcessEngineDataAccess)
        fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean

        fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState)
        fun doSkipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
            if (state.isFinal && state != newState) {
                throw ProcessException("Attempting to skip a finalised node ${node.id}($handle-$entryNo)")
            }
            state = newState
        }

        fun invalidateTask(engineData: MutableProcessEngineDataAccess)

        fun cancel(engineData: MutableProcessEngineDataAccess)
        fun doCancel(engineData: MutableProcessEngineDataAccess) {
            state = Cancelled
        }

        fun cancelAndSkip(engineData: MutableProcessEngineDataAccess)
        fun doCancelAndSkip(engineData: MutableProcessEngineDataAccess) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (state) {
                Pending,
                FailRetry -> state = Skipped
                Sent,
                Taken,
                Started,
                Acknowledged -> {
                    // The full cancel will trigger successors. We only want to do the actual cancellation
                    // action without triggering successors. This is still marked as cancelled, but successors
                    // may be marked as skipped.
                    doCancel(engineData)
                    if (!state.isSkipped) { // allow skipping if that is more appropriate
                        state = AutoCancelled
                    }
                }
            }
        }

        fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: ICompactFragment? = null)

        fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: ICompactFragment? = null) {
            if (state.isFinal) {
                throw ProcessException("instance ${node.id}:${handle.handleValue}($state) cannot be finished as it is already in a final state.")
            }
            state = Complete
            node.results.mapTo(results.apply { clear() }) { (it as IPlatformXmlResultType).applyData(resultPayload) }
        }


        fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (state) {
                FailRetry,
                Pending -> provideTask(engineData)
            }// ignore
        }

    }

    abstract class AbstractBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<*>> : Builder<N, T> {

        override val processContext: ProcessInstanceContext
            get() = processInstanceBuilder

        override fun toXmlInstance(body: ICompactFragment?): XmlProcessNodeInstance {
            return XmlProcessNodeInstance(
                nodeId = node.id,
                predecessors = predecessors.map { if (it.handleValue < 0) Handle.invalid() else Handle(handleValue = it.handleValue) },
                processInstance = hProcessInstance.handleValue,
                handle = if (handle.handleValue < 0) Handle.invalid() else Handle(handleValue = handle.handleValue),
                state = state,
                results = results,
                body = body
            )
        }

        override var failureCause: Throwable? = null

        /**
         * Update the state if the current state would indicate that to be the expected action
         */
        private fun softUpdateState(engineData: MutableProcessEngineDataAccess, targetState: NodeInstanceState) {
            invalidateBuilder(engineData)
            if (state == targetState) return
            val doSet = when (targetState) {
                Pending -> throw IllegalArgumentException("Updating a state to pending is not allowed")
                Sent -> state == Pending
                Acknowledged -> state == Pending || state == Sent
                Taken -> state == Sent || state == Acknowledged
                Started -> state == Taken || state == Sent || state == Acknowledged
                Complete -> state == Started
                SkippedCancel -> state == Pending
                SkippedFail -> state == Pending
                Skipped -> state == Pending
                SkippedInvalidated -> state == Pending
                Cancelled,
                AutoCancelled -> !state.isFinal
                FailRetry -> throw ProcessException("Recovering a retryable failed state is not a soft state update")
                Failed -> throw ProcessException("Failed states can not be changed, this should not be attempted")
            }
            if (doSet) {
                state = targetState
                store(engineData)
            }
        }

        final override fun provideTask(engineData: MutableProcessEngineDataAccess) {
            if (this !is JoinInstance.Builder) {
                val predecessors = predecessors.map { engineData.nodeInstance(it).withPermission() }
                for (predecessor in predecessors) {
                    if (predecessor !is SplitInstance && !predecessor.state.isFinal) {
                        throw ProcessException("Attempting to start successor ${node.id}[$handle] for non-final predecessor ${predecessor.node.id}[${predecessor._handle} - ${predecessor.state}]")
                    }
                }
            }
            if (doProvideTask(engineData).also { softUpdateState(engineData, Sent) }) {
                takeTask(engineData)
            }
        }

        final override fun takeTask(engineData: MutableProcessEngineDataAccess) {
            if (doTakeTask(engineData).also { softUpdateState(engineData, Taken) })
                startTask(engineData)
        }

        final override fun startTask(engineData: MutableProcessEngineDataAccess) {
            if (doStartTask(engineData).also { softUpdateState(engineData, Started) }) {
                finishTask(engineData)
            }
        }

        final override fun finishTask(engineData: MutableProcessEngineDataAccess, resultPayload: ICompactFragment?) {
            if (state.isFinal) {
                throw ProcessException("instance ${node.id}:${handle.handleValue}($state) cannot be finished as it is already in a final state.")
            }
            doFinishTask(engineData, resultPayload)
            state = Complete
            processInstanceBuilder.store(engineData)
            store(engineData)
            engineData.commit()
            engineData.processContextFactory.onActivityTermination(engineData, this)
            // TODO use tickle instead
            // The splits need to be updated before successors are started. This prevents unneeded/unexpected cancellations.
            // Joins should trigger updates before cancellations anyway though as a safeguard.
            processInstanceBuilder.updateSplits(engineData)
            processInstanceBuilder.startSuccessors(engineData, this)
            //TODO may not do anything
            processInstanceBuilder.updateSplits(engineData)
            processInstanceBuilder.updateState(engineData)
        }

        override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
            assert(newState == Skipped || newState == SkippedCancel || newState == SkippedFail) {
                "Skipping task with unsupported new state $newState"
            }
            doSkipTask(engineData, newState)
            store(engineData)
            processInstanceBuilder.storeChild(this)
            assert(state == Skipped || state == SkippedCancel || state == SkippedFail) {
                "When skipping a task ($node:$handle) the current state was not a skipped type as expected: $state"
            }
            processInstanceBuilder.skipSuccessors(engineData, this, newState)
        }

        override fun invalidateTask(engineData: MutableProcessEngineDataAccess) {
            if (!(state.isSkipped || state == Pending || state == Sent)) {
                throw ProcessException("Attempting to invalidate a non-skipped node $this with state: $state")
            }
            state = SkippedInvalidated
            store(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            processInstanceBuilder.storeChild(this)
        }

        final override fun failTask(engineData: MutableProcessEngineDataAccess, cause: Throwable) {
            failureCause = cause
            state = if (state == Pending) FailRetry else Failed
            engineData.processContextFactory.onActivityTermination(engineData, this)
            store(engineData)
            processInstanceBuilder.skipSuccessors(engineData, this, SkippedFail)
        }

        final override fun cancel(engineData: MutableProcessEngineDataAccess) {
            doCancel(engineData)
            softUpdateState(engineData, Cancelled)
//            processInstanceBuilder.storeChild(this)
//            processInstanceBuilder.store(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            engineData.queueTickle(processInstanceBuilder.handle)
//            processInstanceBuilder.skipSuccessors(engineData, this, SkippedCancel)
        }

        final override fun cancelAndSkip(
            engineData: MutableProcessEngineDataAccess
        ) {
            doCancelAndSkip(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            processInstanceBuilder.skipSuccessors(engineData, this, Skipped)
        }

        override fun toString(): String {
            return "${node::class}  ($handle, ${node.id}[$entryNo] - $state)"
        }

    }

    abstract class BaseBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<T>>(
        final override var node: N,
        predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*>>>>,
        final override val processInstanceBuilder: ProcessInstance.Builder,
        final override var owner: Principal,
        final override val entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
        state: NodeInstanceState = Pending
    ) : AbstractBuilder<N, T>() {

        final override var handle: Handle<SecureObject<ProcessNodeInstance<*>>> = handle

        final override var state = state
            set(value) {
                field = value
                processInstanceBuilder.storeChild(this)
            }


        override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
            engineData.nodeInstances[handle]?.withPermission()?.let { newBase ->
                @Suppress("UNCHECKED_CAST")
                node = newBase.node as N
                predecessors.replaceBy(newBase.predecessors)
                owner = newBase.owner
                state = newBase.state
            }
        }

        final override var predecessors: MutableSet<Handle<SecureObject<ProcessNodeInstance<*>>>> =
            predecessors.asSequence().toMutableArraySet()
        final override val results = mutableListOf<ProcessData>()
    }

    abstract class ExtBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<*>>(
        protected var base: T,
        override val processInstanceBuilder: ProcessInstance.Builder
    ) : AbstractBuilder<N, T>() {
        private val observer: Observer<Any?> = { newValue -> changed = true; newValue }

        @Suppress("UNCHECKED_CAST")
        protected fun <T> observer(): Observer<T> = observer as Observer<T>

        final override val predecessors = ObservableSet(base.predecessors.toMutableArraySet(), { changed = true })

        final override var owner by overlay(observer()) { base.owner }
        final override var handle: Handle<SecureObject<ProcessNodeInstance<*>>> by overlay(observer()) { base.handle }

        private var _state: NodeInstanceState = base.state
        final override var state
            get() = _state
            set(value) {
                if (_state != value) {
                    _state = value
                    changed = true
                    processInstanceBuilder.storeChild(this)
                }
            }
        final override var results = ObservableList(base.results.toMutableList(), { changed = true })
        var changed: Boolean = false
        final override val entryNo: Int = base.entryNo

        fun invalidateBuilder(newBase: T) {
            changed = false

            base = newBase
            predecessors.replaceBy(newBase.predecessors)
            owner = newBase.owner
            handle = newBase.handle
            _state = newBase.state
            results.replaceBy(newBase.results)

        }

        override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
            @Suppress("UNCHECKED_CAST")
            invalidateBuilder(engineData.nodeInstance(handle).withPermission() as T)
        }

        override abstract fun build(): T

    }

    companion object {

    }

}

private typealias Observer<T> = (T) -> T

@OptIn(ExperimentalContracts::class)
internal inline fun <R> ProcessNodeInstance.Builder<*, *>.tryCreateTask(body: () -> R): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return _tryHelper(body) { e ->
        failTaskCreation(e)
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <R> ProcessNodeInstance.Builder<*, *>.tryRunTask(body: () -> R): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return _tryHelper(body) { e ->
        failTaskExecution(e)
    }
}


@OptIn(ExperimentalContracts::class)
@Suppress("unused", "FunctionName")
@PublishedApi
internal inline fun <R> _tryHelper(
    engineData: MutableProcessEngineDataAccess,
    processInstance: ProcessInstance,
    body: () -> R, failHandler: (MutableProcessEngineDataAccess, ProcessInstance, Exception) -> Unit
): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        body()
    } catch (e: Exception) {
        try {
            failHandler(engineData, processInstance, e)
        } catch (f: Exception) {
            e.addSuppressedCompat(f)
        }
        throw e
    }
}


@OptIn(ExperimentalContracts::class)
@PublishedApi
internal inline fun <R> _tryHelper(body: () -> R, failHandler: (Exception) -> Unit): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        body()
    } catch (e: Exception) {
        try {
            failHandler(e)
        } catch (f: Exception) {
            e.addSuppressedCompat(f)
        }
        throw e
    }
}
