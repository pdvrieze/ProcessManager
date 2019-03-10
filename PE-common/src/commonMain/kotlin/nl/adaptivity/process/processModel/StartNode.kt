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
import net.devrieze.util.collection.setOfNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.QName

//@Serializable
interface StartNode<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT> {

    val successor: Identifiable?

    //    @Serializable
    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode.IBuilder<NodeT, ModelT> {
        override fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitStartNode(this)

        @Transient
        var successor: Identifiable?

        @Transient
        override val successors: Set<Identified>
            get() = setOfNotNull(successor?.identifier)

        override val predecessors: Set<Identified>
            get() = emptySet()

        override fun addSuccessor(identifier: Identifier) {
            val s = successor
            if (s !=null) {
                if (s.identifier == identifier) return
                throw IllegalStateException("Successor already set")
            }
            successor = identifier
        }

        override fun addPredecessor(identifier: Identifier) {
            throw IllegalStateException("Endnodes have no predecessors")
        }

        override fun removeSuccessor(identifier: Identifiable) {
            if (successor?.id == identifier.id) successor = null
        }

        override fun removePredecessor(identifier: Identifiable) = Unit

    }

    override fun builder(): Builder<NodeT, ModelT>

    companion object {

        const val ELEMENTLOCALNAME = "start"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

    }
    // No special aspects.
}