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
import net.devrieze.util.collection.replaceByNotNull
import net.devrieze.util.collection.setOfNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.QName


interface Split<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT>, JoinSplit<NodeT, ModelT> {

    val predecessor: Identifiable?

    override fun builder(): Builder<NodeT, ModelT>

    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : JoinSplit.Builder<NodeT, ModelT> {

        override fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitSplit(this)

        var predecessor: Identifiable?

        @Transient
        override val predecessors: Set<Identified>
            get() = setOfNotNull(predecessor?.identifier)

        override var successors: MutableSet<Identified>

        fun setSuccessors(value: Iterable<Identified>) = successors.replaceBy(value)

        override fun addSuccessor(identifier: Identifier) {
            successors.add(identifier)
        }


        override fun addPredecessor(identifier: Identifier) {
            val p = predecessor
            if (p !=null) {
                if (p.identifier == identifier) return
                throw IllegalStateException("Predecessor already set")
            }
            predecessor = identifier
        }

        override fun removeSuccessor(identifier: Identifiable) {
            successors.remove(identifier)
        }

        override fun removePredecessor(identifier: Identifiable) {
            if (predecessor?.id == identifier.id) predecessor = null
        }

    }

    companion object {

        const val ELEMENTLOCALNAME = "split"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    }
    // No methods beyond JoinSplit
}
