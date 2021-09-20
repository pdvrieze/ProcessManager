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

import net.devrieze.util.toComparableHandle
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.DefaultProcessNodeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet


/**
 * Base type for any process node that can be executed
 */
interface ExecutableProcessNode : ProcessNode, Identified {

    interface Builder : ProcessNode.Builder {

        override fun result(builder: XmlResultType.Builder.() -> Unit) {
            results.add(XmlResultType.Builder().apply(builder).build())
        }

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
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        processInstanceBuilder.getChildNodeInstance(this, entryNo)?.let { return it }
        if (!isMultiInstance && entryNo > 1) {
            processInstanceBuilder.allChildNodeInstances { it.node == this && it.entryNo != entryNo }.forEach {
                processInstanceBuilder.updateChild(it) {
                    invalidateTask(data)
                }
            }
        }
        return DefaultProcessNodeInstance.BaseBuilder(
            this, listOf(predecessor.handle),
            processInstanceBuilder,
            processInstanceBuilder.owner, entryNo
        )
    }


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
        nodeInstanceSource: NodeInstanceSource,
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
    fun provideTask(engineData: ProcessEngineDataAccess, instanceBuilder: ProcessNodeInstance.Builder<*, *>): Boolean
        = true

    fun takeTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

    fun startTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = true

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

}

/**
 * Should this node be able to be provided?
 * @param engineData
 *
 * @param The predecessor that is evaluating the condition
 *
 * @param nodeInstanceBuilder The instance against which the condition should be evaluated.
 *
 * @return `true` if the node can be started, `false` if not.
 */
internal fun ExecutableCondition?.evalCondition(
    nodeInstanceSource: NodeInstanceSource,
    predecessor: IProcessNodeInstance,
    nodeInstance: IProcessNodeInstance
): ConditionResult {
    // If the instance is final, the condition maps to the state
    if (nodeInstance.state.isFinal) {
        return if(nodeInstance.state == NodeInstanceState.Complete) ConditionResult.TRUE else ConditionResult.NEVER
    }
    // A lack of condition is a true result
    if (this==null) return ConditionResult.TRUE

    if (isAlternate) { // An alternate is only true if all others are never/finalised
        val successorCount = predecessor.node.successors.size
        val hPred = predecessor.handle.toComparableHandle()
        var nonTakenSuccessorCount:Int = 0
        for (sibling in nodeInstanceSource.allChildNodeInstances()) {
            if (sibling.handle != nodeInstance.handle && hPred in sibling.predecessors) {
                when (sibling.condition(nodeInstanceSource, predecessor)) {
                    ConditionResult.TRUE -> return ConditionResult.NEVER
                    ConditionResult.MAYBE -> return ConditionResult.MAYBE
                    ConditionResult.NEVER -> nonTakenSuccessorCount++
                }
            }
        }
        if (nonTakenSuccessorCount+1>=successorCount) return ConditionResult.TRUE
        return ConditionResult.MAYBE
    }

    return eval(nodeInstanceSource, nodeInstance)
}
