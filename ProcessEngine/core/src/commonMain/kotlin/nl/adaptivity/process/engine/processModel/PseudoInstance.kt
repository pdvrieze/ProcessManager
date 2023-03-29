/*
 * Copyright (c) 2021.
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
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.multiplatform.PrincipalCompat

class PseudoInstance(
    private val processContext: PseudoContext,
    override val handle: PNIHandle,
    override val node: ExecutableProcessNode,
    override val entryNo: Int,
    predecessors: Set<PNIHandle>
) : IProcessNodeInstance {

    override val predecessors = predecessors.toMutableSet()

    private val owner: PrincipalCompat
        get() = processContext.processInstance.processModel.rootModel.owner

    override var state: NodeInstanceState = NodeInstanceState.Pending

    override val hProcessInstance: PIHandle
        get() = processContext.processInstance.handle

    override val results: List<ProcessData>
        get() = emptyList()

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance.Builder<*, ProcessNodeInstance<*>> {
        throw UnsupportedOperationException("Pseudo instances should not be made into builders")
    }

    override fun toString(): String {
        return "pseudo instance ($handle, ${node.id}[$entryNo] - $state)"
    }

    class PseudoContext(
        val processInstance: IProcessInstance,
    ) : ProcessInstanceContext {
        constructor(readAccess: ProcessEngineDataAccess, hProcessInstance: PIHandle) :
            this(readAccess.instance(hProcessInstance).withPermission())

        private val handleOffset: Int =
            (processInstance.allChildNodeInstances().maxOf { it.handle.handleValue } + 1).toInt()
        private val overlay = arrayOfNulls<IProcessNodeInstance>(handleOffset)

        private val pseudoNodes: MutableList<PseudoInstance> = mutableListOf()

        override val processInstanceHandle: PIHandle
            get() = processInstance.handle

        override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
            return processInstance.allChildNodeInstances().filter { it.node.id == name.id }.toList()
        }

        init {
            for (child in processInstance.allChildNodeInstances()) {
                val p = child
                overlay[p.handle.handleValue.toInt()] = p
            }
        }

        fun getNodeInstance(handle: PNIHandle): IProcessNodeInstance? = when {
            handle.handleValue < handleOffset
            -> overlay[handle.handleValue.toInt()]

            else -> pseudoNodes[(handle.handleValue - handleOffset).toInt()]
        }

        private fun getNodeInstance(
            node: ExecutableProcessNode,
            hPred: PNIHandle
        ): IProcessNodeInstance? {
            return (overlay.asSequence() + pseudoNodes.asSequence())
                .firstOrNull { it?.node == node && hPred in it.predecessors }
        }

        private fun getNodeInstance(node: ExecutableProcessNode, entryNo: Int): IProcessNodeInstance? {
            return (overlay.asSequence() + pseudoNodes.asSequence())
                .firstOrNull { it?.node == node && (!it.node.isMultiInstance || it.entryNo == entryNo) }
        }

        fun create(
            pred: PNIHandle,
            node: ExecutableProcessNode,
            entryNo: Int
        ): PseudoInstance {

            val inst = PseudoInstance(
                this,
                Handle((handleOffset + pseudoNodes.size).toLong()),
                node,
                entryNo,
                setOf(pred)
            )
            pseudoNodes.add(inst)
            return inst
        }

        fun create(
            pred: PNIHandle,
            base: IProcessNodeInstance
        ): PseudoInstance {

            val inst = PseudoInstance(
                this,
                base.handle.takeIf { it.isValid } ?: Handle((handleOffset + pseudoNodes.size).toLong()),
                base.node,
                base.entryNo,
                base.predecessors + pred
            ).apply { state = base.state }
            if (inst.handle.handleValue < handleOffset) {
                overlay[inst.handle.handleValue.toInt()] = inst
            } else {
                pseudoNodes.add(inst)
            }
            return inst
        }

        fun getOrCreate(
            hPred: PNIHandle,
            node: ExecutableProcessNode,
            entryNo: Int
        ): IProcessNodeInstance {
            getNodeInstance(node, hPred)?.let { return it } // the predecessor is already known

            val instance = getNodeInstance(node, entryNo) // predecessor not linked yet
            if (instance != null) {
                if (!instance.handle.isValid) {
                    return create(hPred, instance)
                } else {
                    when (instance) {
                        is PseudoInstance -> instance.predecessors.add(hPred)
                        is ProcessNodeInstance.Builder<*, *> -> instance.predecessors.add(hPred)
                        else -> return create(hPred, instance)
                    }
                    return instance
                }
            }
            return create(hPred, node, entryNo)
        }

        fun populatePredecessorsFor(handle: PNIHandle) {
            val interestedNodes: IdentifyableSet<ExecutableProcessNode>
            val targetEntryNo: Int
            run {
                val targetInstance = getNodeInstance(handle) ?: throw IllegalArgumentException("No such node exists")

                interestedNodes = targetInstance.node.transitivePredecessors()
                targetEntryNo = targetInstance.entryNo
            }
            val toProcess = ArrayDeque<IProcessNodeInstance>()
            processInstance.allChildNodeInstances().filterTo(toProcess) {
                it.node is StartNode && it.node in interestedNodes
            }
            while (toProcess.isNotEmpty()) {
                val child = toProcess.removeFirst()
                if (!child.node.isMultiInstance || child.entryNo <= targetEntryNo) {
                    for (successorNodeId in child.node.successors) {
                        val updatedTarget = getNodeInstance(handle)!!
                        if (successorNodeId == updatedTarget.node.identifier) {
                            if (child.handle !in updatedTarget.predecessors) {
                                // Create a new overlay node to record the new predecessor
                                when (updatedTarget) {
                                    is PseudoInstance -> updatedTarget.predecessors.add(child.handle)
                                    else -> create(child.handle, updatedTarget)
                                }
                            }
                        } else {
                            val successorNode = interestedNodes.get(successorNodeId)
                            if (successorNode != null) {
                                toProcess.add(getOrCreate(child.handle, successorNode, child.entryNo))
                            }
                        }
                    }
                }
            }
        }

    }
}
