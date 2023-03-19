/*
 * Copyright (c) 2017.
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

import net.devrieze.util.Handle
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ConditionResult
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.evalNodeStartCondition
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.util.ICompactFragment

class JoinInstance<C: ActivityInstanceContext> : ProcessNodeInstance<JoinInstance<C>, C> {

    interface Builder<C : ActivityInstanceContext> : ProcessNodeInstance.Builder<ExecutableJoin, JoinInstance<C>, C> {

        val isFinished: Boolean
            get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

        override fun <MSG_T> doProvideTask(
            engineData: MutableProcessEngineDataAccess<C>,
            messageService: IMessageService<MSG_T, C>
        ): Boolean {
            if (!isFinished) {
                val shouldProgress = node.canProvideTaskAutoProgress(engineData, this)
                if (shouldProgress) {
                    val directSuccessors = processInstanceBuilder.getDirectSuccessorsFor(this.handle)

                    val canAdd = directSuccessors
                        .none { it.state.isCommitted || it.state.isFinal }
                    if (canAdd) {
                        state = NodeInstanceState.Acknowledged
                        return true
                    }
                }
                return shouldProgress

            }
            return false
        }

        override fun canTakeTaskAutomatically(): Boolean = true

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat?
        ): Boolean {

            return node.canTakeTaskAutoProgress(createActivityContext(engineData), this, assignedUser)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            if (node.canStartTaskAutoProgress(this)) {
                return updateTaskState(engineData, cancelState = NodeInstanceState.Cancelled)
            } else {
                return false
            }
        }

        override fun doFinishTask(
            engineData: MutableProcessEngineDataAccess<C>,
            resultPayload: ICompactFragment?
        ) {
            if (state == NodeInstanceState.Complete) {
                return
            }
            var committedPredecessorCount = 0
            var completedPredecessorCount = 0
            predecessors
                .map { processInstanceBuilder.getChildNodeInstance(it) }
                .filter { it.state.isCommitted }
                .forEach {
                    if (!it.state.isFinal) {
                        throw ProcessException("Predecessor $it is committed but not final, cannot finish join without cancelling the predecessor")
                    } else {
                        committedPredecessorCount++

                        if (it.state == NodeInstanceState.Complete) {
                            if (node.evalCondition(processInstanceBuilder, it, this) == ConditionResult.TRUE) {
                                completedPredecessorCount++
                            }
                        }
                    }
                }

            super.doFinishTask(engineData, resultPayload)

            // Store before cancelling predecessors, the cancelling will likely hit this child
            processInstanceBuilder.storeChild(this)
            processInstanceBuilder.store(engineData)

            skipPredecessors(engineData)

            if (committedPredecessorCount < node.min) {
                throw ProcessException("Finishing the join is not possible as the minimum amount of predecessors ${node.min} was not reached (predecessor count: $committedPredecessorCount)")
            }

            engineData.commit()

        }

        fun failSkipOrCancel(
            engineData: MutableProcessEngineDataAccess<C>,
            cancelState: NodeInstanceState,
            cause: Throwable
        ) {
            when {
                state.isCommitted -> failTask(engineData, cause)

                cancelState.isSkipped -> state = NodeInstanceState.Skipped

                else -> cancel(engineData)
            }
        }

    }

    class ExtBuilder<C : ActivityInstanceContext>(
        instance: JoinInstance<C>,
        processInstanceBuilder: ProcessInstance.Builder<C>
    ) : ProcessNodeInstance.ExtBuilder<ExecutableJoin, JoinInstance<C>, C>(instance, processInstanceBuilder),
        Builder<C> {

        override var node: ExecutableJoin by overlay { instance.node }

        override fun build() = if (changed) JoinInstance<C>(this).also { invalidateBuilder(it) } else base

        override fun skipTask(
            engineData: MutableProcessEngineDataAccess<C>,
            newState: NodeInstanceState
        ) = skipTaskImpl(engineData, newState)

    }

    class BaseBuilder<C: ActivityInstanceContext>(
        node: ExecutableJoin,
        predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<ExecutableJoin, JoinInstance<C>, C>(
        node,
        predecessors,
        processInstanceBuilder,
        owner,
        entryNo,
        handle,
        state
    ), Builder<C> {

        override fun build() = JoinInstance<C>(this)

        override fun skipTask(
            engineData: MutableProcessEngineDataAccess<C>,
            newState: NodeInstanceState
        ) = skipTaskImpl(engineData, newState)

    }

    override val node: ExecutableJoin
        get() = super.node as ExecutableJoin

    @Suppress("UNCHECKED_CAST")
    override val handle: Handle<SecureObject<JoinInstance<C>>>
        get() = super.handle as Handle<SecureObject<JoinInstance<C>>>

    fun canFinish() = predecessors.size >= node.min

    constructor(
        node: ExecutableJoin,
        predecessors: Collection<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        hProcessInstance: Handle<SecureObject<ProcessInstance<C>>>,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending,
        results: Iterable<ProcessData> = emptyList()
    ) : super(node, predecessors, processInstanceBuilder, hProcessInstance, owner, entryNo, handle, state, results) {
        if (predecessors.any { !it.isValid }) {
            throw ProcessException("When creating joins all handles should be valid $predecessors")
        }
    }

    constructor(builder: Builder<C>) : this(
        builder.node,
        builder.predecessors,
        builder.processInstanceBuilder,
        builder.hProcessInstance,
        builder.owner,
        builder.entryNo,
        builder.handle,
        builder.state,
        builder.results
    )

    override fun builder(processInstanceBuilder: ProcessInstance.Builder<C>) =
        ExtBuilder(this, processInstanceBuilder)

    companion object {
        fun <C : ActivityInstanceContext> build(
            joinImpl: ExecutableJoin,
            predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
            processInstanceBuilder: ProcessInstance.Builder<C>,
            entryNo: Int,
            handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            body: Builder<C>.() -> Unit
        ): JoinInstance<C> {
            return JoinInstance(
                BaseBuilder<C>(
                    joinImpl,
                    predecessors,
                    processInstanceBuilder,
                    processInstanceBuilder.owner,
                    entryNo,
                    handle,
                    state
                ).apply(body)
            )
        }

        fun <C : ActivityInstanceContext> build(
            joinImpl: ExecutableJoin,
            predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
            processInstance: ProcessInstance<C>,
            entryNo: Int,
            handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            body: Builder<C>.() -> Unit
        ): JoinInstance<C> {
            return build(joinImpl, predecessors, processInstance.builder(), entryNo, handle, state, body)
        }

        /**
         * Update the state of the task. Returns true if the task should now be finished by the caller.
         * @param cancelState The state to use when the instance cannot work
         * @return `true` if the caller should finish the task, `false` if not
         */
        @JvmStatic
        private fun <C: ActivityInstanceContext> Builder<C>.updateTaskState(
            engineData: MutableProcessEngineDataAccess<C>,
            cancelState: NodeInstanceState
        ): Boolean {
            if (state.isFinal) return false // Don't update if we're already complete

            val join = node
            val totalPossiblePredecessors = join.predecessors.size
            var realizedPredecessors = 0

            val instantiatedPredecessors = predecessors.map {
                processInstanceBuilder.getChildNodeInstance(it).also {
                    if (it.state.isFinal) realizedPredecessors++
                }
            }

            var mustDecide = false

            if (realizedPredecessors == totalPossiblePredecessors) {
                /* Did we receive all possible predecessors. In this case we need to decide */
                state = NodeInstanceState.Started
                mustDecide = true
            }

            var complete = 0
            var skippedOrNever = 0
            var pending = 0
            for (predecessor in instantiatedPredecessors) {
                val condition = node.conditions[predecessor.node.identifier] as? ExecutableCondition

                val conditionResult = condition.evalNodeStartCondition(processInstanceBuilder, predecessor, this)
                if (conditionResult == ConditionResult.NEVER) {
                    skippedOrNever += 1
                } else {
                    when (predecessor.state) {
                        NodeInstanceState.Complete -> complete += 1

                        NodeInstanceState.Skipped,
                        NodeInstanceState.SkippedCancel,
                        NodeInstanceState.Cancelled,
                        NodeInstanceState.SkippedFail,
                        NodeInstanceState.Failed -> skippedOrNever += 1

                        else -> pending += 1
                    }
                }
            }
            if (totalPossiblePredecessors - skippedOrNever < join.min) {
                // XXX this needs to be done in the caller
                if (complete > 0) {
                    failTask(engineData, ProcessException("Too many predecessors have failed"))
                } else {
                    // cancelNoncompletedPredecessors(engineData)
                    failSkipOrCancel(engineData, cancelState, ProcessException("Too many predecessors have failed"))
                }
            }

            if (complete >= join.min) {
                if (totalPossiblePredecessors - complete - skippedOrNever == 0) return true
                if (complete >= join.max || (realizedPredecessors == totalPossiblePredecessors)) {
                    return true
                }
            }
            if (mustDecide) {
                if (state == NodeInstanceState.Started) {
                    cancel(engineData)
                } else {
                    failSkipOrCancel(engineData, cancelState, ProcessException("Unexpected failure to complete join"))
                }
            }
            return false
        }

        private fun <C: ActivityInstanceContext> Builder<C>.skipTaskImpl(engineData: MutableProcessEngineDataAccess<C>, newState: NodeInstanceState) {
            // Skipping a join merely triggers a recalculation
            assert(newState == NodeInstanceState.Skipped || newState == NodeInstanceState.SkippedCancel || newState == NodeInstanceState.SkippedFail)
            val updateResult = updateTaskState(engineData, newState)
            processInstanceBuilder.storeChild(this)
            processInstanceBuilder.store(engineData)

            if (state.isSkipped) {

                skipPredecessors(engineData)
            }
        }

        private fun <C: ActivityInstanceContext> Builder<C>.skipPredecessors(engineData: MutableProcessEngineDataAccess<C>) {
            if (node.isMultiMerge) return // multimerge joins should not have their predecessors skipped

            val pseudoContext = PseudoInstance.PseudoContext(processInstanceBuilder)

            pseudoContext.populatePredecessorsFor(handle)

            val toSkipCancel = mutableListOf<ProcessNodeInstance<*, C>>()
            val predQueue = ArrayDeque<IProcessNodeInstance<C>>()//.apply { add(pseudoContext.getNodeInstance(handle)!!) }

            for (hpred in pseudoContext.getNodeInstance(handle)!!.predecessors) {
                val pred = pseudoContext.getNodeInstance(hpred)
                if (pred?.state?.isFinal == false) predQueue.add(pred)
            }

            while (predQueue.isNotEmpty()) {
                val pred = predQueue.removeFirst()
                when {
                    pred.state.isFinal && pred.node !is StartNode -> Unit

                    pred is PseudoInstance<C> -> if (pred.node !is Split) {
                        pred.state = NodeInstanceState.AutoCancelled
                        for (hppred in pred.predecessors) {
                            pseudoContext.getNodeInstance(hppred)?.let { predQueue.add(it) }
                        }
                    }

                    pred is ProcessNodeInstance<*, C> -> {
                        if (pred.node !is Split) {
                            toSkipCancel.add(pred)
                            for (hppred in pred.predecessors) {
                                val ppred = pseudoContext.getNodeInstance(hppred) ?: continue
                                predQueue.add(ppred)
                            }
                        }
                    }
                }

            }

            for (predecessor in toSkipCancel) {
                processInstanceBuilder.updateChild(predecessor.handle) {
                    cancelAndSkip(engineData)
                }
            }
        }


    }

}
