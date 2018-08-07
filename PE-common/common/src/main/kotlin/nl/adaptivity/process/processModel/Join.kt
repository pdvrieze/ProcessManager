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


import net.devrieze.util.collection.replaceBy
import net.devrieze.util.collection.setOfNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.QName


interface Join<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT>, JoinSplit<NodeT, ModelT> {

    val successor: Identifiable?

    val conditions: Map<Identifier, Condition?>

    /**
     * Does this join support multi-merge (in other words, is it allowed to fire of new threads after an initial instance
     * has finalised.
     */
    val isMultiMerge: Boolean

    override fun builder(): Builder<NodeT, ModelT>

    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplit.Builder<NodeT, ModelT> {

        override fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

        override var predecessors: MutableSet<Identified>

        var conditions: MutableMap<Identifier, String?>

        var successor: Identifiable?

        override val successors: Set<Identified> get() = setOfNotNull(successor?.identifier)

        var isMultiMerge: Boolean

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitJoin(this)

        fun predecessors(vararg values: Identifiable) {
            values.forEach {
                predecessors.add(
                    it.identifier ?: throw NullPointerException("Missing identifier for predecessor $it"))
            }
        }

        fun setPredecessors(value: Iterable<Identified>) = predecessors.replaceBy(value)

        override fun addSuccessor(identifier: Identifier) {
            val s = successor
            if (s !=null) {
                if (s.identifier == identifier) return
                throw IllegalStateException("Successor already set")
            }
            successor = identifier
        }

        override fun addPredecessor(identifier: Identifier) {
            predecessors.add(identifier)
        }

        override fun removeSuccessor(identifier: Identifiable) {
            if (successor?.id == identifier.id) successor = null
        }

        override fun removePredecessor(identifier: Identifiable) {
            predecessors.remove(identifier)
        }

    }

    companion object {

        const val ELEMENTLOCALNAME = "join"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
        val PREDELEMNAME = QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX)
    }
    // No methods beyond JoinSplit
}