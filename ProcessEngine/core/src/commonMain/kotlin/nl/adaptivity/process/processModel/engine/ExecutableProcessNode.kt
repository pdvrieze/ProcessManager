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
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.multiplatform.PrincipalCompat


/**
 * Base type for any process node that can be executed
 */
interface ExecutableProcessNode : ProcessNode, Identified {

    interface Builder : ProcessNode.Builder {

        override fun result(builder: XmlResultType.Builder.() -> Unit) {
            results.add(XmlResultType.Builder().apply(builder).build())
        }

        fun build(
            buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ProcessModel<ExecutableProcessNode>, *, *>,
            otherNodes: Iterable<ProcessNode.Builder>
        ): ExecutableProcessNode

    }

    override val ownerModel: ExecutableModelCommon

    override val identifier: Identifier
        get() = Identifier(id)

//  override val defines: List<XmlDefineType>

    /**
     * Create an instance of the node or return it if it already exist.
     *
     * TODO handle failRetry nodes
     */
    fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean,
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, ProcessNodeInstance<*>> {
        processInstanceBuilder.getChildNodeInstance(this, entryNo)?.let {
            return it
        }
        if (!isMultiInstance && entryNo > 1) {
            processInstanceBuilder.allChildNodeInstances { it.node == this && it.entryNo != entryNo }.forEach {
                processInstanceBuilder.updateChild(it) {
                    invalidateTask(data)
                }
            }
        }
        val predecessors = listOf(predecessor.handle)
        return data.processContextFactory.createNodeInstance(
            node = this,
            predecessors = predecessors,
            processInstanceBuilder = processInstanceBuilder,
            owner = processInstanceBuilder.owner,
            entryNo = entryNo
        )
    }

    fun isOtherwiseCondition(predecessor: ExecutableProcessNode): Boolean = false

    /**
     * Should this node be able to be provided?
     * @param engineData
     *
     * @param The predecessor that is evaluating the condition
     *
     * @param nodeInstance The instance against which the condition should be evaluated.
     *
     * @return `true` if the node can be started, `false` if
     *          not.
     */
    fun evalCondition(
        nodeInstanceSource: IProcessInstance,
        predecessor: IProcessNodeInstance,
        nodeInstance: IProcessNodeInstance
    ): ConditionResult = when {
        nodeInstance.state==NodeInstanceState.Complete || !nodeInstance.state.isFinal -> ConditionResult.TRUE
        else -> ConditionResult.NEVER
    }


    /**
     * Take action to make task available
     *
     * @param engineData
     *
     * @param instanceBuilder The processnode instance involved.
     *
     * @return `true` if the task can/must be automatically taken
     */
    fun canProvideTaskAutoProgress(
        engineData: ProcessEngineDataAccess,
        instanceBuilder: ProcessNodeInstance.Builder<*, *>
    ): Boolean = true

    fun <C: ActivityInstanceContext> canTakeTaskAutoProgress(
        activityContext: C,
        instance: ProcessNodeInstance.Builder<*, *>,
        assignedUser: PrincipalCompat?
    ): Boolean = true

    fun canTakeTaskAutoProgress(
        instance: ProcessNodeInstance.Builder<*, *>,
        assignedUser: PrincipalCompat?
    ): Boolean = true

    fun canStartTaskAutoProgress(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

    private fun preceeds(node: ExecutableProcessNode, reference: ExecutableProcessNode, seenIds: MutableSet<String>):Boolean {
        if (node in reference.predecessors) return true
        seenIds+=id

        return reference.predecessors.asSequence()
            .filter { it.id !in seenIds }
            .map { ownerModel.getNode(it)!! }
            .firstOrNull()
            ?.let { preceeds(node, it, seenIds) }
            ?: false
    }

    /**
     * Determine whether this node is a "possible" predecessor of the reference node.
     */
    public infix fun  preceeds(reference: ExecutableProcessNode): Boolean {
        if (this===reference) return false
        return preceeds(this, reference, HashSet<String>())
    }

    /**
     * Determine whether this node is a "possible" successor of the reference node.
     */
    public infix fun succceeds(reference: ExecutableProcessNode): Boolean {
        if (this===reference) return false
        return preceeds(reference, this, HashSet<String>())
    }

    fun transitivePredecessors(): IdentifyableSet<ExecutableProcessNode> {
        val preds = IdentifyableSet.processNodeSet<ExecutableProcessNode>()
        fun addPreds(node: ExecutableProcessNode) {
            for (predId in node.predecessors) {
                val pred = ownerModel.requireNode(predId)
                if(preds.add(pred)) {
                    addPreds(pred)
                }
            }
        }
        addPreds(this)
        return preds
    }

    fun checkPredSuccCounts(predRange: IntRange = 1..1, succRange: IntRange = 1 .. 1) {
        require(predecessors.size in predRange) {
            "The amount of predecessors is expected to be in the range ${predRange}"
        }
        require(successors.size in succRange) {
            "The amount of successors is expected to be in the range ${succRange}"
        }
    }

}

