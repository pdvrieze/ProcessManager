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

import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceByNotNull
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.LogLevel
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.engine.ConditionResult
import nl.adaptivity.process.processModel.engine.ExecutableSplit
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 * Specialisation of process node instance for splits
 */
class SplitInstance : ProcessNodeInstance<SplitInstance> {

    interface Builder : ProcessNodeInstance.Builder<ExecutableSplit, SplitInstance> {
        override fun build(): SplitInstance
        var predecessor: Handle<SecureObject<ProcessNodeInstance<*>>>?
            get() = predecessors.firstOrNull()
            set(value) = predecessors.replaceByNotNull(value)

        override fun doProvideTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return node.provideTask(engineData, this)
        }

        override fun doTakeTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return node.takeTask(this)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            state = NodeInstanceState.Started
            return updateState(engineData)
        }

        override fun doFinishTask(engineData: MutableProcessEngineDataAccess, resultPayload: ICompactFragment?) {
            val committedSuccessors = processInstanceBuilder.allChildNodeInstances { it.state.isCommitted }
            if (committedSuccessors.count() < node.min) {
                throw ProcessException("A split can only be finished once the minimum amount of children is committed")
            }
            super.doFinishTask(engineData, resultPayload)
        }
    }

    class ExtBuilder(private val instance: SplitInstance, processInstanceBuilder: ProcessInstance.Builder) :
        ProcessNodeInstance.ExtBuilder<ExecutableSplit, SplitInstance>(instance, processInstanceBuilder), Builder {
        override var node: ExecutableSplit by overlay { instance.node }
        override fun build() = if (changed) SplitInstance(this).also { invalidateBuilder(it) } else base

        override fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
            super<Builder>.tickle(engineData, messageService) // XXX for debugging
        }
    }

    class BaseBuilder(
        node: ExecutableSplit,
        predecessor: Handle<SecureObject<ProcessNodeInstance<*>>>,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: Principal,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<ExecutableSplit, SplitInstance>(
        node, listOf(predecessor), processInstanceBuilder, owner, entryNo,
        handle, state
    ), Builder {
        override fun build() = SplitInstance(this)

        override fun tickle(engineData: MutableProcessEngineDataAccess, messageService: IMessageService<*>) {
            super<Builder>.tickle(engineData, messageService)
        }
    }

    override val node: ExecutableSplit
        get() = super.node as ExecutableSplit

    @Suppress("UNCHECKED_CAST")
    override val handle: Handle<SecureObject<SplitInstance>>
        get() = super.handle as Handle<SecureObject<SplitInstance>>

    constructor(
        node: ExecutableSplit,
        predecessor: Handle<SecureObject<ProcessNodeInstance<*>>>,
        processInstanceBuilder: ProcessInstance.Builder,
        hProcessInstance: Handle<SecureObject<ProcessInstance>>,
        owner: Principal,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending,
        results: Iterable<ProcessData> = emptyList(),
        entryNo: Int
    ) : super(
        node,
        listOf(predecessor),
        processInstanceBuilder,
        hProcessInstance,
        owner,
        entryNo,
        handle,
        state,
        results
    )

    constructor(builder: Builder) : this(
        builder.node,
        builder.predecessor ?: throw NullPointerException("Missing predecessor node instance"),
        builder.processInstanceBuilder,
        builder.hProcessInstance,
        builder.owner,
        builder.handle,
        builder.state,
        builder.results,
        builder.entryNo
    )

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder {
        return ExtBuilder(this, processInstanceBuilder)
    }

    private fun successorInstances(engineData: ProcessEngineDataAccess): Sequence<ProcessNodeInstance<*>> {
        val instance = engineData.instance(hProcessInstance).withPermission()
        return node.successors
            .asSequence()
            .mapNotNull { instance.getChild(it.id, entryNo)?.withPermission() }
            .filter { it.entryNo == entryNo }
    }

    companion object {

        internal fun isActiveOrCompleted(it: IProcessNodeInstance): Boolean {
            return when (it.state) {
                NodeInstanceState.Started,
                NodeInstanceState.Complete,
                NodeInstanceState.Skipped,
                NodeInstanceState.SkippedCancel,
                NodeInstanceState.SkippedFail,
                NodeInstanceState.Failed,
                NodeInstanceState.Taken -> true
                else -> false
            }
        }

    }
}


/**
 * Update the state of the split.
 *
 * @return Whether or not the split is complete.
 *
 * TODO Review this algorithm
 */
internal fun SplitInstance.Builder.updateState(engineData: MutableProcessEngineDataAccess): Boolean {

    if (state.isFinal) return true

    val successorNodes = node.successors.map { node.ownerModel.getNode(it).mustExist(it) }.toList()

    var skippedCount = 0
    var failedCount = 0
    var activeCount = 0
    var committedCount = 0

    for (successorNode in successorNodes) {
        if (committedCount >= node.max) break // stop the loop when we are at the maximum successor count

        if (successorNode is Join && successorNode.conditions[node.identifier] == null) {
            throw IllegalStateException("Splits cannot be immediately followed by unconditional joins")
        }
        // Don't attempt to create an additional instance for a non-multi-instance successor
        if (!successorNode.isMultiInstance && processInstanceBuilder.allChildNodeInstances { it.node == successorNode && !it.state.isSkipped && it.entryNo != entryNo }
                .count() > 0) continue

        val successorBuilder = successorNode
            .createOrReuseInstance(engineData, processInstanceBuilder, this, entryNo, allowFinalInstance = true)

        processInstanceBuilder.storeChild(successorBuilder)
        if (successorBuilder.state == NodeInstanceState.Pending ||
            (successorBuilder.node is Join && successorBuilder.state.isActive)) {

            when (successorBuilder.condition(processInstanceBuilder, this)) {
                // only if it can be executed, otherwise just drop it.
                ConditionResult.TRUE -> if (successorBuilder.state.canRestart ||
                    successorBuilder is Join.Builder
                ) successorBuilder.provideTask(engineData)

                ConditionResult.NEVER -> successorBuilder.skipTask(engineData, NodeInstanceState.Skipped)

                ConditionResult.MAYBE -> Unit /*option can not be advanced for now*/
            }
        }

        successorBuilder.state.also { state ->
            when {
                state.isSkipped -> skippedCount++
                state == NodeInstanceState.Failed -> failedCount++
                state.isCommitted -> committedCount++
                state.isActive -> activeCount++
            }
        }

    }
    if ((successorNodes.size - (skippedCount + failedCount)) < node.min) { // No way to succeed, try to cancel anything that is not in a final state
        for (successor in processInstanceBuilder.allChildNodeInstances { handle in it.predecessors && !it.state.isFinal }) {
            try {
                successor.builder(processInstanceBuilder).cancel(engineData)
            } catch (e: IllegalArgumentException) {
                engineData.logger.log(LogLevel.WARNING, "Task could not be cancelled", e)
            } // mainly ignore
        }
        state = NodeInstanceState.Failed
        return true // complete, but invalid
    }
    // If we determined the maximum
    if (committedCount >= node.max) {
        processInstanceBuilder
            .allChildNodeInstances { !SplitInstance.isActiveOrCompleted(it) && handle in it.predecessors }
            .forEach {
                processInstanceBuilder.updateChild(it) {
                    cancelAndSkip(engineData)
                }
            }
        return true // We reached the max
    }

    // If all successors are committed
    if (activeCount == 0 && successorNodes.size <= skippedCount + failedCount + committedCount) {
        // no need to cancel, just complete
        return true
    }
    return false
}
