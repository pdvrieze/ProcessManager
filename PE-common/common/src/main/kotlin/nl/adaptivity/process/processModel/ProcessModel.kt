/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.processModel

import kotlinx.serialization.Transient
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier

@DslMarker
annotation class ProcessModelDSL

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
    /**
     * Get the process node with the given id.
     * @param nodeId The node id to look up.
     *
     * @return The process node with the id.
     */
    fun getNode(nodeId: Identifiable): NodeT?

    fun getModelNodes(): Collection<NodeT>
    fun getImports(): Collection<IXmlResultType>
    fun getExports(): Collection<IXmlDefineType>

    @Transient
    val rootModel: RootProcessModel<NodeT, ModelT>

    @Transient
    val asM: ModelT
        get() {
            @Suppress("UNCHECKED_CAST")
            return this as ModelT
        }

    @ProcessModelDSL
    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
        val defaultPedantic get() = false
        val rootBuilder: RootProcessModel.Builder<NodeT, ModelT>
        val nodes: MutableList<ProcessNode.IBuilder<NodeT, ModelT>>
        val imports: MutableList<IXmlResultType>
        val exports: MutableList<IXmlDefineType>

        fun startNodeBuilder(): StartNode.Builder<NodeT, ModelT>
        fun splitBuilder(): Split.Builder<NodeT, ModelT>
        fun joinBuilder(): Join.Builder<NodeT, ModelT>
        fun activityBuilder(): Activity.Builder<NodeT, ModelT>
        fun compositeActivityBuilder(): Activity.ChildModelBuilder<NodeT, ModelT>
        fun endNodeBuilder(): EndNode.Builder<NodeT, ModelT>

        fun startNodeBuilder(startNode: StartNode<*, *>): StartNode.Builder<NodeT, ModelT>
        fun splitBuilder(split: Split<*, *>): Split.Builder<NodeT, ModelT>
        fun joinBuilder(join: Join<*, *>): Join.Builder<NodeT, ModelT>
        fun activityBuilder(activity: Activity<*, *>): Activity.Builder<NodeT, ModelT>
        fun endNodeBuilder(endNode: EndNode<*, *>): EndNode.Builder<NodeT, ModelT>

        fun startNode(body: StartNode.Builder<NodeT, ModelT>.() -> Unit): Identifiable {
            return nodeHelper(startNodeBuilder(), body)
        }

        fun startNode(id: String) = nodes.firstOrNull { it.id == id }?.let { it as StartNode.Builder }

        fun split(body: Split.Builder<NodeT, ModelT>.() -> Unit): Identifiable {
            return nodeHelper(splitBuilder(), body)
        }

        fun split(id: String) = nodes.firstOrNull { it.id == id }?.let { it as Split.Builder }

        fun join(body: Join.Builder<NodeT, ModelT>.() -> Unit): Identifiable {
            return nodeHelper(joinBuilder(), body)
        }

        fun join(id: String) = nodes.firstOrNull { it.id == id }?.let { it as Join.Builder }

        fun activity(body: Activity.Builder<NodeT, ModelT>.() -> Unit): Identifiable {
            return nodeHelper(activityBuilder(), body)
        }

        fun activity(id: String) = nodes.firstOrNull { it.id == id }?.let { it as Activity.Builder }

        fun compositeActivity(body: Activity.ChildModelBuilder<NodeT, ModelT>.() -> Unit): Identifiable {
            val builder = compositeActivityBuilder()
            builder.apply(body)
            builder.ensureChildId().ensureId()
            rootBuilder.childModels.add(builder)
            nodes.add(builder)
            return Identifier(builder.id!!)
        }

        fun endNode(body: EndNode.Builder<NodeT, ModelT>.() -> Unit): Identifiable {
            return nodeHelper(endNodeBuilder(), body)
        }

        fun endNode(id: String) = nodes.firstOrNull { it.id == id }?.let { it as EndNode.Builder }

        fun newId(base: String): String {
            return generateSequence(1,
                                    { it + 1 }).map { "${base}${it}" }.first { candidateId -> nodes.none { it.id == candidateId } }
        }

        fun <B : ChildProcessModel.Builder<*, *>> B.ensureChildId(): B = apply {
            if (childId == null) {
                childId = rootBuilder.newChildId(this.childIdBase)
            }
        }

        fun <B : ProcessNode.IBuilder<*, *>> B.ensureId(): B = apply {
            if (id == null) {
                id = this@Builder.newId(this.idBase)
            }
        }

        fun validate() {
            normalize(true)

            val nodeList = nodes.toList()
            val mark = IntArray(nodeList.size)
            val SEEN = 0b01
            val CURRENT = 0b10

            fun seen(idx: Int) = (mark[idx] and SEEN == SEEN)
            fun markSeen(idx: Int) {
                mark[idx] = mark[idx] or SEEN
            }

            fun current(idx: Int) = mark[idx] and CURRENT == CURRENT
            fun markCurrent(idx: Int) {
                mark[idx] = mark[idx] or CURRENT
            }

            fun resetCurrent(idx: Int) {
                mark[idx] = mark[idx] and CURRENT.inv()
            }

            val nodeMap = nodeList.indices.associateBy { nodeList[it].id }

            fun visitSuccessors(nodeIdx: Int) {
                if (!seen(nodeIdx)) {
                    if (current(nodeIdx)) throw ProcessException("Cycle in process model")
                    markSeen(nodeIdx)
                    markCurrent(nodeIdx)
                    val node = nodeList[nodeIdx]
                    for (successor in node.successors) {
                        visitSuccessors(
                            nodeMap[successor.id] ?: throw ProcessException("Missing node for id $successor.id"))
                    }
                    resetCurrent(nodeIdx)
                }
            }

            // First normalize pedantically

            // Check for cycles and mark each node as seen
            nodeList.indices.filter { nodeIdx ->
                val node = nodeList[nodeIdx]
                node.predecessors.isEmpty().also { empty ->
                    if (empty && node !is StartNode.Builder<NodeT, ModelT>) throw ProcessException(
                        "Non-start node without predecessors found (${node.id})")
                }
            }.forEach(::visitSuccessors)

            mark.indices.firstOrNull { !seen(it) }?.let { idx ->
                throw ProcessException("Node \"${nodeList[idx].id}\" found that is not reachable from any start node")
            }

            // This DOES allow for multiple disconnected graphs when multiple start nodes are present.
        }

        fun normalize(pedantic: Boolean) {
            val nodeMap = nodes.asSequence().filter { it.id != null }.associateBy { it.id }

            // Ensure all nodes are linked up and have ids
            var lastId = 1
            nodes.forEach { nodeBuilder ->
                val curIdentifier = nodeBuilder.id?.let(::Identifier) ?: if (pedantic) {
                    throw IllegalArgumentException("Node without id found")
                } else {
                    generateSequence(lastId) { lastId += 1; lastId }
                        .map { "node$it" }
                        .first { it !in nodeMap }
                        .apply { nodeBuilder.id = this }
                        .let(::Identifier)
                }

                if (pedantic) { // Pedantic will throw exceptions on missing things
                    if (nodeBuilder is StartNode.Builder<NodeT, ModelT> && !nodeBuilder.predecessors.isEmpty()) {
                        throw ProcessException("Start nodes have no predecessors")
                    }
                    if (nodeBuilder is EndNode.Builder<NodeT, ModelT> && !nodeBuilder.successors.isEmpty()) {
                        throw ProcessException("End nodes have no successors")
                    }

                    nodeBuilder.predecessors.firstOrNull { it.id !in nodeMap }?.let { missingPred ->
                        throw ProcessException(
                            "The node ${nodeBuilder.id} has a missing predecessor (${missingPred.id})")
                    }

                    nodeBuilder.successors.firstOrNull { it.id !in nodeMap }?.let { missingSuc ->
                        throw ProcessException("The node ${nodeBuilder.id} has a missing successor (${missingSuc.id})")
                    }
                } else {
                    // Remove "missing" predecessors and successors
                    nodeBuilder.predecessors.removeAll { it.id !in nodeMap }
                    nodeBuilder.successors.removeAll { it.id !in nodeMap }
                }

                nodeBuilder.predecessors.asSequence()
                    .map { nodeMap[it.id]!! }
                    .forEach { pred ->
                        pred.successors.add(curIdentifier) // If existing, should ignore it
                    }

                nodeBuilder.successors.asSequence()
                    .map { nodeMap[it.id]!! }
                    .forEach { successor ->
                        successor.predecessors.add(curIdentifier) // If existing, should ignore it
                    }
            }

            /* This injects split nodes.
             * In principle the code is formatted as:
             *
             *          / RIGHT1
             *    LEFT |- RIGHT2
             *          \ RIGHT3
             *
             * The new split will be the "middle"
             */

            nodes.asSequence()
                .filter { left -> left.successors.size > 1 && left !is Split.Builder<NodeT, ModelT> }
                .map { leftBuilder ->
                    splitBuilder().also { middle ->
                        // All successors of the left node will be successors of the new split.
                        middle.successors.addAll(leftBuilder.successors)

                        // Nodes without id can't have successors (as they have no predecessors
                        val leftId = Identifier(leftBuilder.id!!)

                        middle.predecessor = leftId

                        // Create a new identifier for this split, assign it to splitId
                        val splitId = Identifier(this@Builder.newId(middle.idBase).also { middle.id = it })

                        // For all the original successors of the left node remove the left node as predecessor and add the split
                        // as new predecessor instead
                        leftBuilder.successors.asSequence()
                            .map { nodeMap[it.id] }
                            .filterNotNull()
                            .forEach { right ->
                                right.predecessors.remove(leftId)
                                right.predecessors.add(splitId)
                            }

                        // Replace the old successors of the left node with the new injected split
                        leftBuilder.successors.replaceBy(splitId)

                    }
                }.toList().let { nodes.addAll(it) }
        }

    }

    /** Interface that helps collating all the elements needed to build child nodes and child models*/
    interface BuildHelper<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> {
        val newOwner: ModelT
        val pedantic: Boolean get() = false
        fun childModel(childId: String): ChildProcessModel<NodeT, ModelT>
        fun node(builder: ProcessNode.IBuilder<*, *>): NodeT
        fun withOwner(newOwner: ModelT): BuildHelper<NodeT, ModelT>
    }

    companion object {
        private fun <B : ProcessNode.IBuilder<NodeT, ModelT>, NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> ProcessModel.Builder<NodeT, ModelT>.nodeHelper(
            builder: B,
            body: B.() -> Unit): Identifiable {
            return builder.apply(body).ensureId().apply { this@nodeHelper.nodes.add(this) }.let { Identifier(it.id!!) }
        }
    }
}
