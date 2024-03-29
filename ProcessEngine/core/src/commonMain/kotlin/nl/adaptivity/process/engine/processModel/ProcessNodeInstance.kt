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
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.addSuppressedCompat
import nl.adaptivity.util.multiplatform.assert
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
abstract class ProcessNodeInstance<out T : ProcessNodeInstance<T, C>, C: ActivityInstanceContext>(
    override val node: ExecutableProcessNode,
    predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
    processInstanceBuilder: ProcessInstance.Builder<*>,
    override val hProcessInstance: Handle<SecureObject<ProcessInstance<C>>>,
    final override val owner: PrincipalCompat,
    final override val entryNo: Int,
    override val handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
    final override val state: NodeInstanceState = Pending,
    results: Iterable<ProcessData> = emptyList(),
    val failureCause: Throwable? = null
) : SecureObject<ProcessNodeInstance<T, C>>,
    ReadableHandleAware<SecureObject<ProcessNodeInstance<*, *>>>,
    IProcessNodeInstance<C> {

    override val results: List<ProcessData> = results.toList()

    override val predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*, C>>>> =
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

    constructor(builder: Builder<*, T, C>) : this(
        node = builder.node,
        predecessors = builder.predecessors,
        processInstanceBuilder = builder.processInstanceBuilder,
        hProcessInstance = builder.hProcessInstance,
        owner = builder.owner,
        entryNo = builder.entryNo,
        handle = builder.handle,
        state = builder.state,
        results = builder.results,
        failureCause = builder.failureCause
    )

    override fun build(processInstanceBuilder: ProcessInstance.Builder<C>): ProcessNodeInstance<*, C> = this

    override abstract fun builder(processInstanceBuilder: ProcessInstance.Builder<C>): ExtBuilder<out ExecutableProcessNode, @UnsafeVariance T, C>

    private fun precedingClosure(processData: ProcessEngineDataAccess<*>): Sequence<SecureObject<ProcessNodeInstance<*, *>>> {
        return predecessors.asSequence().flatMap { predHandle ->
            val pred = processData.nodeInstance(predHandle).withPermission()
            pred.precedingClosure(processData) + sequenceOf(pred)
        }
    }

    fun update(
        processInstanceBuilder: ProcessInstance.Builder<C>,
        body: Builder<out ExecutableProcessNode, T, C>.() -> Unit
    ): Future<out T>? {
        val builder = builder(processInstanceBuilder).apply(body)

        return processInstanceBuilder.storeChild(builder)
            .let { if (builder.changed) it else null }
    }

    @Suppress("UNCHECKED_CAST")
    private inline val asT
        get() = this as T

    override fun withPermission(): ProcessNodeInstance<T, C> = this

    private fun hasDirectPredecessor(handle: Handle<SecureObject<ProcessNodeInstance<*, *>>>): Boolean {
        return predecessors.any { it.handleValue == handle.handleValue }
    }

    fun resolvePredecessors(engineData: ProcessEngineDataAccess<C>): Collection<ProcessNodeInstance<*, C>> {
        return predecessors.asSequence().map {
            engineData.nodeInstance(it).withPermission()
        }.toList()
    }

    fun getHandleValue(): Long {
        return handle.handleValue
    }

    override fun toString(): String {
        return "nodeInstance  ($handle, ${node.id}[$entryNo] - $state)"
    }

    fun C.serialize(
        nodeInstanceSource: IProcessInstance<C>,
        out: XmlWriter,
        localEndpoint: EndpointDescriptor
    ) {
        out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
            writeAttribute("state", state.name)
            writeAttribute("processinstance", hProcessInstance.handleValue)

            if (handle.isValid) writeAttribute("handle", handle.handleValue)

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

    fun C.toSerializable(
        engineData: ProcessEngineDataAccess<C>,
        localEndpoint: EndpointDescriptor
    ): XmlProcessNodeInstance {
        val builder = builder(engineData.instance(hProcessInstance).withPermission().builder())

        val body: ICompactFragment? = (node as? MessageActivity)?.message?.let { message ->
            try {
                val xmlReader = message.messageBody.getXmlReader()
                instantiateXmlPlaceholders(builder.processInstanceBuilder, xmlReader, true, localEndpoint)
                    as ICompactFragment
            } catch (e: XmlException) {
                engineData.logger.log(LogLevel.WARNING, "Error processing body", e)
                throw e
            }
        }

        return builder.toXmlInstance(body)
    }

    interface Builder<N : ExecutableProcessNode, out T : ProcessNodeInstance<T, C>, C: ActivityInstanceContext> : IProcessNodeInstance<C> {
        override var node: N
        override val predecessors: MutableSet<Handle<SecureObject<ProcessNodeInstance<*, C>>>>
        val processInstanceBuilder: ProcessInstance.Builder<C>
        override val hProcessInstance: Handle<SecureObject<ProcessInstance<C>>> get() = processInstanceBuilder.handle
        var owner: PrincipalCompat
        override var handle: Handle<SecureObject<ProcessNodeInstance<*, C>>>
        override var state: NodeInstanceState
        override val results: MutableList<ProcessData>
        fun toXmlInstance(body: ICompactFragment?): XmlProcessNodeInstance
        override val entryNo: Int
        var failureCause: Throwable?

        override var assignedUser: PrincipalCompat?
            get() = super.assignedUser
            set(value) {
                require(value == null) { "Only message activities can have assigned users" }
            }

        fun invalidateBuilder(engineData: ProcessEngineDataAccess<C>)

        fun build(): T

        override fun builder(processInstanceBuilder: ProcessInstance.Builder<C>): Builder<*, *, C> =
            this

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
        fun store(engineData: MutableProcessEngineDataAccess<C>) {
            val mutableNodeInstances =
                engineData.nodeInstances as MutableHandleMap<SecureObject<ProcessNodeInstance<*, *>>>
            if (handle.isValid) {
                mutableNodeInstances[handle] = build()
            } else {
                processInstanceBuilder.storeChild(this)
            }
            // Must be updated as well as the process node instance may mean the process instance is changed.
            processInstanceBuilder.store(engineData)
            engineData.commit()
        }

        fun failTask(engineData: MutableProcessEngineDataAccess<C>, cause: Throwable)

        /** Function that will eventually do progression */
        fun provideTask(
            engineData: MutableProcessEngineDataAccess<C>,
            autoContinue: Boolean = true
        )

        /** Function that will do provision, but not progress. This is where custom implementations live */
        fun doProvideTask(engineData: MutableProcessEngineDataAccess<C>): Boolean

        fun canTakeTaskAutomatically(): Boolean

        fun takeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat? = null,
            autoContinue: Boolean = true
        )

        fun doTakeTask(engineData: MutableProcessEngineDataAccess<C>, assignedUser: PrincipalCompat? = null): Boolean

        fun startTask(engineData: MutableProcessEngineDataAccess<C>, autoContinue: Boolean = true)
        fun doStartTask(engineData: MutableProcessEngineDataAccess<C>): Boolean

        fun skipTask(engineData: MutableProcessEngineDataAccess<C>, newState: NodeInstanceState)
        fun doSkipTask(engineData: MutableProcessEngineDataAccess<C>, newState: NodeInstanceState) {
            if (state.isFinal && state != newState) {
                throw ProcessException("Attempting to skip a finalised node ${node.id}($handle-$entryNo)")
            }
            state = newState
        }

        fun invalidateTask(engineData: MutableProcessEngineDataAccess<C>)

        fun cancel(engineData: MutableProcessEngineDataAccess<C>)
        fun doCancel(engineData: MutableProcessEngineDataAccess<C>) {
            state = Cancelled
        }

        fun cancelAndSkip(engineData: MutableProcessEngineDataAccess<C>)
        fun doCancelAndSkip(engineData: MutableProcessEngineDataAccess<C>) {
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

                Complete -> if (node is StartNode) { // just special case start nodes
                    state = AutoCancelled
                }

                else -> {
                    throw IllegalStateException("The current state ($state) doesn't support cancellation")
                }
            }
        }

        fun finishTask(engineData: MutableProcessEngineDataAccess<C>, resultPayload: ICompactFragment? = null)

        fun doFinishTask(engineData: MutableProcessEngineDataAccess<C>, resultPayload: ICompactFragment? = null) {
            if (state.isFinal) {
                throw ProcessException("instance ${node.id}:${handle.handleValue}($state) cannot be finished as it is already in a final state.")
            }
            state = Complete
            node.results.mapTo(results.apply { clear() }) { (it as IPlatformXmlResultType).applyData(resultPayload) }
        }


        fun tickle(engineData: MutableProcessEngineDataAccess<C>, messageService: IMessageService<*, C>) {
            when (state) {
                FailRetry,
                Pending -> provideTask(engineData)

                else -> { /* ignore */
                }
            }// ignore
        }

    }

    abstract class AbstractBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<T, C>, C: ActivityInstanceContext> : Builder<N, T, C> {

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
        private fun softUpdateState(engineData: MutableProcessEngineDataAccess<C>, targetState: NodeInstanceState) {
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

        final override fun provideTask(engineData: MutableProcessEngineDataAccess<C>, autoContinue: Boolean) {
            if (this !is JoinInstance.Builder<*>) {
                val predecessors = predecessors.map { engineData.nodeInstance(it).withPermission() }
                for (predecessor in predecessors) {
                    if (predecessor !is SplitInstance && !predecessor.state.isFinal) {
                        throw ProcessException("Attempting to start successor ${node.id}[$handle] for non-final predecessor ${predecessor.node.id}[${predecessor.handle} - ${predecessor.state}]")
                    }
                }
            }
            if (doProvideTask(engineData).also { softUpdateState(engineData, Sent) } &&
                autoContinue &&
                canTakeTaskAutomatically()) {
                takeTask(engineData)
            }
        }

        final override fun takeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat?,
            autoContinue: Boolean
        ) {
            if (doTakeTask(engineData, assignedUser).also { softUpdateState(engineData, Taken) } && autoContinue)
                startTask(engineData)
        }

        final override fun startTask(engineData: MutableProcessEngineDataAccess<C>, autoContinue: Boolean) {
            if (doStartTask(engineData).also { softUpdateState(engineData, Started) } && autoContinue) {
                finishTask(engineData)
            }
        }

        final override fun finishTask(engineData: MutableProcessEngineDataAccess<C>, resultPayload: ICompactFragment?) {
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

        override fun skipTask(engineData: MutableProcessEngineDataAccess<C>, newState: NodeInstanceState) {
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

        override fun invalidateTask(engineData: MutableProcessEngineDataAccess<C>) {
            if (!(state.isSkipped || state == Pending || state == Sent)) {
                throw ProcessException("Attempting to invalidate a non-skipped node $this with state: $state")
            }
            state = SkippedInvalidated
            store(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            processInstanceBuilder.storeChild(this)
        }

        final override fun failTask(
            engineData: MutableProcessEngineDataAccess<C>,
            cause: Throwable
        ) {
            failureCause = cause
            state = if (state == Pending) FailRetry else Failed
            engineData.processContextFactory.onActivityTermination(engineData, this)
            store(engineData)
            processInstanceBuilder.skipSuccessors(engineData, this, SkippedFail)
        }

        final override fun cancel(engineData: MutableProcessEngineDataAccess<C>) {
            doCancel(engineData)
            softUpdateState(engineData, Cancelled)
//            processInstanceBuilder.storeChild(this)
//            processInstanceBuilder.store(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            engineData.queueTickle(processInstanceBuilder.handle)
//            processInstanceBuilder.skipSuccessors(engineData, this, SkippedCancel)
        }

        final override fun cancelAndSkip(engineData: MutableProcessEngineDataAccess<C>) {
            doCancelAndSkip(engineData)
            engineData.processContextFactory.onActivityTermination(engineData, this)
            processInstanceBuilder.skipSuccessors(engineData, this, Skipped)
        }

        override fun toString(): String {
            return "${node::class}  ($handle, ${node.id}[$entryNo] - $state)"
        }

    }

    abstract class BaseBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<T, C>, C: ActivityInstanceContext>(
        final override var node: N,
        predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
        final override val processInstanceBuilder: ProcessInstance.Builder<C>,
        final override var owner: PrincipalCompat,
        final override val entryNo: Int,
        final override var handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = Pending
    ) : AbstractBuilder<N, T, C>() {

        final override var state = state
            set(value) {
                field = value
                processInstanceBuilder.storeChild(this)
            }


        override fun invalidateBuilder(engineData: ProcessEngineDataAccess<C>) {
            engineData.nodeInstances[handle]?.withPermission()?.let { newBase ->
                @Suppress("UNCHECKED_CAST")
                node = newBase.node as N
                predecessors.replaceBy(newBase.predecessors)
                owner = newBase.owner
                state = newBase.state
            }
        }

        final override var predecessors: MutableSet<Handle<SecureObject<ProcessNodeInstance<*, C>>>> =
            predecessors.asSequence().toMutableArraySet()
        final override val results = mutableListOf<ProcessData>()
    }

    abstract class ExtBuilder<N : ExecutableProcessNode, T : ProcessNodeInstance<T, C>, C: ActivityInstanceContext>(
        protected var base: T,
        override val processInstanceBuilder: ProcessInstance.Builder<C>
    ) : AbstractBuilder<N, T, C>() {
        private val observer: Observer<Any?> = { newValue -> changed = true; newValue }

        @Suppress("UNCHECKED_CAST")
        protected fun <T> observer(): Observer<T> = observer as Observer<T>

        final override val predecessors = ObservableSet(base.predecessors.toMutableArraySet(), { changed = true })

        final override var owner by overlay(observer()) { base.owner }
        final override var handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> by overlay(observer()) { base.handle }

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

        override fun invalidateBuilder(engineData: ProcessEngineDataAccess<C>) {
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
inline fun <R> ProcessNodeInstance.Builder<*, *, *>.tryCreateTask(body: () -> R): R {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return _tryHelper(body) { e ->
        failTaskCreation(e)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> ProcessNodeInstance.Builder<*, *, *>.tryRunTask(body: () -> R): R {
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
internal inline fun <R, C: ActivityInstanceContext> _tryHelper(
    engineData: MutableProcessEngineDataAccess<C>,
    processInstance: ProcessInstance<C>,
    body: () -> R, failHandler: (MutableProcessEngineDataAccess<C>, ProcessInstance<C>, Exception) -> Unit
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
