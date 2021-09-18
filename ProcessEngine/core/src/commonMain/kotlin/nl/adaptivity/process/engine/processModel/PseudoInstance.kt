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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.handle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.toComparableHandle
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.security.Principal

class PseudoInstance(
    override val processContext: PseudoContext,
    override val handle: Handle<SecureObject<ProcessNodeInstance<*>>>,
    override val node: ExecutableProcessNode,
    override val entryNo: Int,
    predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>
) : IProcessNodeInstance {

    override val predecessors = predecessors.toMutableSet()

    override val owner: Principal
        get() = processContext.processInstance.processModel.rootModel.owner

    override var state: NodeInstanceState = NodeInstanceState.Pending

    override val hProcessInstance: Handle<SecureObject<ProcessInstance>>
        get() = processContext.processInstance.handle

    override val results: List<ProcessData>
        get() = emptyList()

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance.Builder<*, *> {
        throw UnsupportedOperationException("Pseudo instances should not be made into builders")
    }

    class PseudoContext(
        val readAccess: ProcessEngineDataAccess,
        val processInstance: ProcessInstance,
    ) : ProcessInstanceContext {
        constructor(readAccess: ProcessEngineDataAccess, hProcessInstance: Handle<SecureObject<ProcessInstance>>):
            this(readAccess, readAccess.instance(hProcessInstance).withPermission())

        private val handleOffset: Int = (processInstance.childNodes.maxOf { it.withPermission().getHandleValue() } + 1).toInt()
        private val overlay = arrayOfNulls<IProcessNodeInstance>(handleOffset)

        private val pseudoNodes: MutableList<PseudoInstance> = mutableListOf()

        override val handle: ComparableHandle<SecureObject<ProcessInstance>>
            get() = processInstance.handle.toComparableHandle()

        init {
            val pib = processInstance.builder()
            for(child in processInstance.childNodes) {
                val p = child.withPermission()
                overlay[p.handle.handleValue.toInt()] = p.builder(pib)
            }
        }

        fun getInstance(handle: Handle<SecureObject<ProcessNodeInstance<*>>>): IProcessNodeInstance? = when {
            handle.handleValue < handleOffset
                 -> overlay[handle.handleValue.toInt()]

            else -> pseudoNodes[(handle.handleValue - handleOffset).toInt()]
        }

        fun getInstance(node: ExecutableProcessNode, entryNo: Int): IProcessNodeInstance? {
            overlay.asSequence()
                .firstOrNull { it?.node == node && it.entryNo == entryNo }
                ?.let { return it }

            return pseudoNodes.firstOrNull { it.node == node && it.entryNo == entryNo }
        }

        fun create(
            pred: Handle<SecureObject<ProcessNodeInstance<*>>>,
            node: ExecutableProcessNode,
            entryNo: Int
        ): PseudoInstance {

            val inst = PseudoInstance(this, handle((handleOffset + pseudoNodes.size).toLong()), node, entryNo, setOf(pred.toComparableHandle()))
            pseudoNodes.add(inst)
            return inst
        }

        fun getOrCreate(
            pred: Handle<SecureObject<ProcessNodeInstance<*>>>,
            node: ExecutableProcessNode,
            entryNo: Int
        ): IProcessNodeInstance {
            val instance = getInstance(node, entryNo)
            if (instance!=null) {
                val predInstance = getInstance(pred)
                if (predInstance!=null) {
                    val overlap = predInstance.predecessors.asSequence()
                        .map { getInstance(it)!! }
                        .any { it.node == predInstance.node }
                    if (overlap) {
                        return create(pred, node, entryNo)
                    } else {
                        (instance as? PseudoInstance)?.predecessors?.add(predInstance.handle.toComparableHandle())
                        (instance as? ProcessNodeInstance.Builder<*,*>)?.predecessors?.add(predInstance.handle.toComparableHandle())
                        return instance
                    }
                }
            }
            return instance ?: create(pred, node, entryNo)
        }

        fun createPredecessorsFor(handle: Handle<SecureObject<ProcessNodeInstance<*>>>) {
            val targetInstance = getInstance(handle) ?: throw IllegalArgumentException("No such node exists")
            val interestedNodes = targetInstance.node.transitivePredecessors()
            val toProcess= ArrayDeque<IProcessNodeInstance>()
            toProcess.addAll(processInstance.childNodes.map { it.withPermission() }.filter { it.node in interestedNodes })
            while (toProcess.isEmpty()) {
                val child = toProcess.removeFirst()
                if (child.node in interestedNodes && child.entryNo<=targetInstance.entryNo) {
                    for (successorNodeId in child.node.successors) {
                        val successorNode = interestedNodes.get(successorNodeId)
                        if (successorNode!=null) {
                            toProcess.add(getOrCreate(child.handle, successorNode, child.entryNo))
                        }
                    }
                }
            }
        }

    }
}
