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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identified


class ExecutableJoin(
    builder: Join.Builder,
    buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, *, *, *>,
    otherNodes: Iterable<ProcessNode.Builder>
) : JoinBase<ExecutableProcessNode, ExecutableModelCommon>(builder, buildHelper, otherNodes), ExecutableProcessNode {

    init {
        checkPredSuccCounts(predRange = 1..Int.MAX_VALUE)
    }

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override fun canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *, *>): Boolean {
        return super.canStartTaskAutoProgress(instance)
    }

    fun getExistingInstance(
        data: ProcessEngineDataAccess<*>,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        neededEntryNo: Int,
        allowFinalInstance: Boolean
    ): Pair<JoinInstance.Builder<*>?, Int> {
        var candidateNo = if (isMultiInstance || isMultiMerge) neededEntryNo else 1
        for (candidate in processInstanceBuilder.getChildren(this).sortedBy { it.entryNo }) {
            if (predecessor.handle in candidate.predecessors) {
                return (candidate as JoinInstance.Builder) to candidateNo
            }
            if ((allowFinalInstance || candidate.state != NodeInstanceState.Complete) &&
                (candidate.entryNo == neededEntryNo || candidate.predecessors.any {
                    when (predecessor.handle.isValid && it.isValid) {
                        true -> predecessor.handle == it
                        else -> data.nodeInstance(it).withPermission()
                            .run { this.entryNo == neededEntryNo && node.id == predecessor.node.id }
                    }
                })
            ) {
                return (candidate as JoinInstance.Builder) to candidateNo
            }
            // TODO Throw exceptions for cases where this is not allowed
            if (candidate.entryNo == candidateNo) {
                candidateNo++
            } // Increase the candidate entry number
        }
        return null to candidateNo
    }

    override fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean {
        return conditions.get(predecessor.identifier)?.toExecutableCondition()?.isOtherwise == true
    }

    override fun evalCondition(
        nodeInstanceSource: IProcessInstance,
        predecessor: IProcessNodeInstance,
        nodeInstance: IProcessNodeInstance
    ): ConditionResult {
        val predCondResult = (conditions[predecessor.node.identifier] as ExecutableCondition?)
            .evalNodeStartCondition(nodeInstanceSource, predecessor, nodeInstance)

        var neverCount = 0
        var alwaysCount = 0

        for (hPred in nodeInstance.predecessors) {
            val siblingResult = when (hPred) {
                predecessor.handle -> predCondResult
                else -> {
                    val sibling = nodeInstanceSource.getChildNodeInstance(hPred)
                    (conditions[sibling.node.identifier] as ExecutableCondition?)
                        .evalNodeStartCondition(nodeInstanceSource, sibling, nodeInstance)
                }
            }

            when (siblingResult) {
                ConditionResult.TRUE -> alwaysCount++
                ConditionResult.NEVER -> neverCount++
                else -> { /* Ignore, can't be determined yet */ }
            }
        }

        if (alwaysCount>=min) return ConditionResult.TRUE
        if (neverCount>(predecessors.size-min)) return ConditionResult.NEVER
        if (neverCount+1==predecessors.size && isOtherwiseCondition(predecessor.node)) return ConditionResult.TRUE
        return ConditionResult.MAYBE
    }

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess<*>,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*, *>, *> {
        val (existingInstance, candidateNo) = getExistingInstance(
            data,
            processInstanceBuilder,
            predecessor,
            entryNo,
            allowFinalInstance
        )
        existingInstance?.let {
            if (predecessor.handle.isValid) {
                if (it.predecessors.add(predecessor.handle)) {
                    // Store the new predecessor, so when resetting the predecessor isn't lost
                    processInstanceBuilder.storeChild(it)
                    processInstanceBuilder.store(data)
                }
            }
            return it
        }

        if (!(isMultiInstance || isMultiMerge) && candidateNo != 1) {
            throw ProcessException("Attempting to start a second instance of a single instantiation join $id:$entryNo")
        }
        return JoinInstance.BaseBuilder<ActivityInstanceContext>(
            this,
            listOf(predecessor.handle),
            processInstanceBuilder,
            processInstanceBuilder.owner,
            candidateNo
        )
    }

    class Builder : JoinBase.Builder, ExecutableProcessNode.Builder {

        constructor(
            id: String? = null,
            predecessors: Collection<Identified> = emptyList(),
            successor: Identified? = null, label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            min: Int = -1,
            max: Int = -1,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiMerge: Boolean = false,
            isMultiInstance: Boolean = false
        ) : super(
            id, predecessors, successor, label, defines, results, x, y, min, max, isMultiMerge, isMultiInstance
        )

        constructor(node: Join) : super(node)

        override fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): ExecutableJoin {
            return ExecutableJoin(this, buildHelper, otherNodes)
        }
    }

}
