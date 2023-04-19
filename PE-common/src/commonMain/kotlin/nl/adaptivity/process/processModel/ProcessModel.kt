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

import nl.adaptivity.process.engine.ProcessException
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identifier

@DslMarker
annotation class ProcessModelDSL

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ProcessModel<out NodeT : ProcessNode> {

    val rootModel: RootProcessModel<NodeT>

    val modelNodes: Collection<NodeT>

    val imports: Collection<IXmlResultType>

    val exports: Collection<IXmlDefineType>

    /**
     * Get the process node with the given id.
     * @param nodeId The node id to look up.
     */
    fun getNode(nodeId: Identifiable): NodeT?

    @ProcessModelDSL
    interface Builder {
        val rootBuilder: RootProcessModel.Builder
        val nodes: MutableList<ProcessNode.Builder>
        val imports: MutableList<IXmlResultType>
        val exports: MutableList<IXmlDefineType>

        fun startNodeBuilder(): StartNode.Builder = StartNodeBase.Builder()

        fun splitBuilder(): Split.Builder = SplitBase.Builder()

        fun joinBuilder(): Join.Builder = JoinBase.Builder()

        fun activityBuilder(): MessageActivity.RWBuilder = MessageActivityBase.Builder()

        fun compositeActivityBuilder(): CompositeActivity.ModelBuilder =
            ActivityBase.CompositeActivityBuilder(rootBuilder = this.rootBuilder)

        fun endNodeBuilder(): EndNode.Builder = EndNodeBase.Builder()

        fun startNodeBuilder(startNode: StartNode): StartNode.Builder = startNode.builder()

        fun eventNodeBuilder(eventNode: EventNode): EventNode.Builder = eventNode.builder()

        fun splitBuilder(split: Split): Split.Builder = split.builder()

        fun joinBuilder(join: Join): Join.Builder = join.builder()

        fun activityBuilder(activity: MessageActivity): MessageActivity.Builder = activity.builder()

        fun activityBuilder(activity: CompositeActivity): Activity.Builder =
            activity.builder()

        fun endNodeBuilder(endNode: EndNode): EndNode.Builder = endNode.builder()

        fun startNode(body: StartNode.Builder.() -> Unit): Identifiable {
            return nodeHelper(startNodeBuilder(), body)
        }

        fun startNode(id: String) = nodes.firstOrNull { it.id == id }?.let { it as StartNode.Builder }

        fun split(body: Split.Builder.() -> Unit): Identifiable {
            return nodeHelper(splitBuilder(), body)
        }

        fun split(id: String) = nodes.firstOrNull { it.id == id }?.let { it as Split.Builder }

        fun join(body: Join.Builder.() -> Unit): Identifiable {
            return nodeHelper(joinBuilder(), body)
        }

        fun join(id: String) = nodes.firstOrNull { it.id == id }?.let { it as Join.Builder }

        fun activity(body: MessageActivity.RWBuilder.() -> Unit): Identifiable {
            return nodeHelper(activityBuilder(), body)
        }

        fun activity(id: String) = nodes.firstOrNull { it.id == id }?.let { it as MessageActivity.Builder }

        fun compositeActivity(body: CompositeActivity.ModelBuilder.() -> Unit): Identifiable {
            val builder = compositeActivityBuilder()
            builder.apply(body)
            builder.ensureChildId().ensureId()
            rootBuilder.childModels.add(builder)
            nodes.add(builder)
            return Identifier(builder.id!!)
        }

        fun endNode(body: EndNode.Builder.() -> Unit): Identifiable {
            return nodeHelper(endNodeBuilder(), body)
        }

        fun endNode(id: String) = nodes.firstOrNull { it.id == id }?.let { it as EndNode.Builder }

        fun newId(base: String): String {
            return generateSequence(1) { it + 1 }
                .map { "$base$it" }
                .first { candidateId -> nodes.none { it.id == candidateId } }
        }

        fun <B : ChildProcessModel.Builder> B.ensureChildId(): B = apply {
            if (childId == null) {
                childId = rootBuilder.newChildId(this.childIdBase)
            }
        }

        fun <B : ProcessNode.Builder> B.ensureId(): B = apply {
            if (id == null) {
                id = this@Builder.newId(this.idBase)
            }
        }

        fun validate() {
            normalize(true)

            val nodeList = nodes.toList()
            val mark = IntArray(nodeList.size)

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
                            nodeMap[successor.id] ?: throw ProcessException("Missing node for id $successor.id")
                                       )
                    }
                    resetCurrent(nodeIdx)
                }
            }

            // First normalize pedantically

            // Check for cycles and mark each node as seen
            nodeList.indices.filter { nodeIdx ->
                val node = nodeList[nodeIdx]
                node.predecessors.isEmpty().also { empty ->
                    if (empty && node !is StartNode.Builder) throw ProcessException(
                        "Non-start node without predecessors found (${node.id})"
                                                                                   )
                }
            }.forEach(::visitSuccessors)

            mark.indices.firstOrNull { !seen(it) }?.let { idx ->
                throw ProcessException("Node \"${nodeList[idx].id}\" found that is not reachable from any start node")
            }

            // This DOES allow for multiple disconnected graphs when multiple start nodes are present.
        }

        fun normalize(pedantic: Boolean) {
            val nodeMap: MutableMap<String, ProcessNode.Builder> = mutableMapOf()
            nodes.asSequence()
                .filter { it.id != null }
                .associateByTo(nodeMap) { it.id!! }

            // First drop all missing predecessors and successors (before adding id's)
            val splitsToInject = mutableMapOf<String, Split.Builder>()
            nodes.forEach { nodeBuilder ->
                if (pedantic) { // Pedantic will throw exceptions on missing things
                    if (nodeBuilder is StartNode.Builder && !nodeBuilder.predecessors.isEmpty()) {
                        throw ProcessException("Start nodes have no predecessors")
                    }
                    if (nodeBuilder is EndNode.Builder && !nodeBuilder.successors.isEmpty()) {
                        throw ProcessException("End nodes have no successors")
                    }

                    nodeBuilder.predecessors.firstOrNull { it.id !in nodeMap }?.let { missingPred ->
                        throw ProcessException("The node ${nodeBuilder.id} has a missing predecessor (${missingPred.id})")
                    }

                    nodeBuilder.successors.firstOrNull { it.id !in nodeMap }?.let { missingSuc ->
                        throw ProcessException("The node ${nodeBuilder.id} has a missing successor (${missingSuc.id})")
                    }
                } else {
                    // Remove "missing" predecessors and successors
                    nodeBuilder.removeAllPredecessors { it.id !in nodeMap }
                    nodeBuilder.removeAllSuccessors { it.id !in nodeMap }
                }
            }

            // Then ensure all have ids

            // Ensure all nodes are linked up and have ids
            nodes.forEach { nodeBuilder ->
                nodeBuilder.id?.let(::Identifier) ?: if (pedantic) {
                    throw IllegalArgumentException("Node without id found")
                } else {
                    generateSequence(1) { it + 1 }
                        .map { "${nodeBuilder.idBase}$it" }
                        .first { it !in nodeMap }
                        .also {
                            nodeBuilder.id = it
                            nodeMap[it] = nodeBuilder
                        }
                }
            }

            // Then ensure they are linked up
            nodes.forEach { nodeBuilder ->
                val curIdentifier = Identifier(nodeBuilder.id!!)

                nodeBuilder.successors.asSequence()
                    .map { nodeMap[it.id]!! }
                    .forEach { successor ->
                        successor.addPredecessor(curIdentifier) // If existing, should ignore it
                    }

                nodeBuilder.predecessors.asSequence()
                    .map { nodeMap[it.id]!! }
                    .forEach { pred ->
                        if (pred !is Split.Builder && // It is not a split
                            pred.successors.isNotEmpty() && // It has a successor set
                            pred.successors.single().id != curIdentifier.id
                        ) { // which is not just the current one

                            splitsToInject.getOrPut(pred.id!!) {
                                splitBuilder().apply {
                                    addPredecessor(Identifier(pred.id!!))
                                    addSuccessor(pred.successors.single().identifier)
                                }
                            }.also { split ->
                                split.addSuccessor(curIdentifier)
                            }
                        } else {
                            pred.addSuccessor(curIdentifier) // If existing, should ignore it
                        }
                    }
            }

            /* This injects split nodes.
             * In principle the code is formatted as:
             *
             *          / RIGHT1
             *    LEFT |- RIGHT2
             *          \ RIGHT3
             *
             * The new split will be the "middle".
             *
             * The predecessor/successor mapping has created valid split nodes. These nodes need embedding though as
             * their predecessors/successors will not be aware of them
             */
            splitsToInject.mapTo(nodes) { (leftStr, middle) ->

                val leftId = Identifier(leftStr)
                val leftBuilder = nodeMap[leftStr]!!

                // Create a new identifier for this split, assign it to splitId
                val splitId = Identifier(this@Builder.newId(middle.idBase).also { middle.id = it })

                // While the split knows the successors, they need to be updated to have the split as predecessor
                middle.successors.asSequence()
                    .map { nodeMap[it.id] }
                    .filterNotNull()
                    .forEach { right ->
                        right.removePredecessor(leftId)
                        right.addPredecessor(splitId)
                    }

                // Make it an AND node
                middle.min = middle.successors.size
                middle.max = middle.min

                // Replace the old successors of the left node with the new injected split
                leftBuilder.removeAllSuccessors { true }
                leftBuilder.addSuccessor(splitId)
                middle
            }
        }

        companion object {
            const val SEEN = 0b01
            const val CURRENT = 0b10
        }

    }

    /** Interface that helps to collate all the elements needed to build child nodes and child models */
    interface BuildHelper<NodeT : ProcessNode, out ModelT : ProcessModel<NodeT>, out RootT : RootProcessModel<*>, out ChildT : ChildProcessModel<*>> {
        val newOwner: ModelT// TODO must this be nullable ?
        val pedantic: Boolean get() = false
        fun childModel(childId: String): ChildT
        fun childModel(builder: ChildProcessModel.Builder): ChildT
        fun node(
            builder: ProcessNode.Builder,
            otherNodes: Iterable<ProcessNode.Builder>
        ): NodeT

        fun <M : ProcessModel<NodeT>> withOwner(newOwner: M): BuildHelper<NodeT, M, RootT, ChildT>
        fun condition(condition: Condition): Condition = condition
    }

    companion object {
        /**
         * Function that helps to ensure that nodes in the model have id's and are added to the
         * builder for the model.
         */
        private fun <B : ProcessNode.Builder> Builder.nodeHelper(
            builder: B,
            body: B.() -> Unit
        ): Identifiable {

            return builder.apply(body).ensureId().apply { this@nodeHelper.nodes.add(this) }.let { Identifier(it.id!!) }
        }
    }
}
