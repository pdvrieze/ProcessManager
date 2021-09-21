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
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ConditionResult
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.evalCondition
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmStatic

class JoinInstance : ProcessNodeInstance<JoinInstance> {

    interface Builder : ProcessNodeInstance.Builder<ExecutableJoin, JoinInstance> {

        val isFinished: Boolean
            get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

        override fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean {
            if (!isFinished) {
                val shouldProgress = node.provideTask(engineData, this)
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

        override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return node.takeTask(this)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            if (node.startTask(this)) {
                return updateTaskState(engineData, NodeInstanceState.Cancelled)
            } else {
                return false
            }
        }

        override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: ICompactFragment?) {
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
            engineData: MutableProcessEngineDataAccess,
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

    class ExtBuilder(instance: JoinInstance, processInstanceBuilder: ProcessInstance.Builder) :
        ProcessNodeInstance.ExtBuilder<ExecutableJoin, JoinInstance>(instance, processInstanceBuilder), Builder {
        override var node: ExecutableJoin by overlay { instance.node }
        override fun build() = if (changed) JoinInstance(this).also { invalidateBuilder(it) } else base
        override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) =
            skipTaskImpl(engineData, newState)

    }

    class BaseBuilder(
        node: ExecutableJoin,
        predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*>>>>,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: Principal,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<ExecutableJoin, JoinInstance>(
        node,
        predecessors,
        processInstanceBuilder,
        owner,
        entryNo,
        handle,
        state
    ), Builder {

        override fun build() = JoinInstance(this)
        override fun skipTask(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) =
            skipTaskImpl(engineData, newState)

    }

    override val node: ExecutableJoin
        get() = super.node as ExecutableJoin

    @Suppress("UNCHECKED_CAST")
    override val handle: Handle<SecureObject<JoinInstance>>
        get() = super.handle as Handle<SecureObject<JoinInstance>>

    fun canFinish() = predecessors.size >= node.min

    constructor(
        node: ExecutableJoin,
        predecessors: Collection<Handle<SecureObject<ProcessNodeInstance<*>>>>,
        processInstanceBuilder: ProcessInstance.Builder,
        hProcessInstance: Handle<SecureObject<ProcessInstance>>,
        owner: Principal,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending,
        results: Iterable<ProcessData> = emptyList()
    ) :
        super(node, predecessors, processInstanceBuilder, hProcessInstance, owner, entryNo, handle, state, results) {
        if (predecessors.any { !it.isValid }) {
            throw ProcessException("When creating joins all handles should be valid $predecessors")
        }
    }

    constructor(builder: Builder) : this(
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

    override fun builder(processInstanceBuilder: ProcessInstance.Builder) =
        ExtBuilder(this, processInstanceBuilder)

    companion object {
        fun build(
            joinImpl: ExecutableJoin,
            predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*>>>>,
            processInstanceBuilder: ProcessInstance.Builder,
            entryNo: Int,
            handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            body: Builder.() -> Unit
        ): JoinInstance {
            return JoinInstance(
                BaseBuilder(
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

        fun build(
            joinImpl: ExecutableJoin,
            predecessors: Set<Handle<SecureObject<ProcessNodeInstance<*>>>>,
            processInstance: ProcessInstance,
            entryNo: Int,
            handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            body: Builder.() -> Unit
        ): JoinInstance {
            return build(joinImpl, predecessors, processInstance.builder(), entryNo, handle, state, body)
        }

        /**
         * Update the state of the task. Returns true if the task should now be finished by the caller.
         * @param cancelState The state to use when the instance cannot work
         * @return `true` if the caller should finish the task, `false` if not
         */
        @JvmStatic
        private fun Builder.updateTaskState(
            engineData: MutableProcessEngineDataAccess,
            cancelState: NodeInstanceState
        ): Boolean {
            if (state.isFinal) return false // Don't update if we're already complete

            val join = node
            val totalPossiblePredecessors = join.predecessors.size
            var realizedPredecessors = 0

            val instantiatedPredecessors = predecessors.map {
                processInstanceBuilder.getChildNodeInstance(it).also {
                    if(it.state.isFinal) realizedPredecessors++
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

                val conditionResult = condition.evalCondition(processInstanceBuilder, predecessor, this)
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
                if (state==NodeInstanceState.Started) {
                    cancel(engineData)
                } else {
                    failSkipOrCancel(engineData, cancelState, ProcessException("Unexpected failure to complete join"))
                }
            }
            return false
        }

        private fun Builder.skipTaskImpl(engineData: MutableProcessEngineDataAccess, newState: NodeInstanceState) {
            // Skipping a join merely triggers a recalculation
            assert(newState == NodeInstanceState.Skipped || newState == NodeInstanceState.SkippedCancel || newState == NodeInstanceState.SkippedFail)
            val updateResult = updateTaskState(engineData, newState)
            processInstanceBuilder.storeChild(this)
            processInstanceBuilder.store(engineData)

            if (state.isSkipped) {

                skipPredecessors(engineData)
            }
        }

        private fun Builder.skipPredecessors(engineData: MutableProcessEngineDataAccess) {
            val pseudoContext = PseudoInstance.PseudoContext(processInstanceBuilder)

            pseudoContext.populatePredecessorsFor(handle)

            val toSkipCancel = mutableListOf<ProcessNodeInstance<*>>()
            val predQueue = ArrayDeque<IProcessNodeInstance>()//.apply { add(pseudoContext.getNodeInstance(handle)!!) }

            for (hpred in pseudoContext.getNodeInstance(handle)!!.predecessors) {
                val pred = pseudoContext.getNodeInstance(hpred)
                if (pred?.state?.isFinal == false) predQueue.add(pred)
            }

            while (predQueue.isNotEmpty()) {
                val pred = predQueue.removeFirst()
                when {
                    pred.state.isFinal && pred.node !is StartNode -> Unit

                    pred is PseudoInstance -> if (pred.node !is Split) {
                        pred.state = NodeInstanceState.AutoCancelled
                        for (hppred in pred.predecessors) {
                            pseudoContext.getNodeInstance(hppred)?.let { predQueue.add(it) }
                        }
                    }

                    pred is ProcessNodeInstance<*> -> {
                        toSkipCancel.add(pred)

                        for (hppred in pred.predecessors) {
                            val ppred = pseudoContext.getNodeInstance(hppred) ?: continue
                            predQueue.add(ppred)
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
