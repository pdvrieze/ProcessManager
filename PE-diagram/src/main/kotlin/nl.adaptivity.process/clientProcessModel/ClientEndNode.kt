/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


open class ClientEndNode<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>> : EndNodeBase<NodeT, ModelT>, EndNode<NodeT, ModelT>, ClientProcessNode<NodeT, ModelT> {

    open class Builder<T : ClientProcessNode<T, M>, M : ClientProcessModel<T, M>> : EndNodeBase.Builder<T, M>, ClientProcessNode.Builder<T, M> {

        constructor()

        constructor(id: String,
                    predecessor: Identified? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN) : super(id, predecessor, label, defines, results, x, y)

        constructor(node: EndNode<*, *>) : super(node)

        override fun build(newOwner: M): ClientEndNode<T, M> {
            return ClientEndNode(this, newOwner)
        }

        override var isCompat: Boolean
            get() = false
            set(compat) {
                if (compat) throw IllegalArgumentException("Compatibility not supported on end nodes.")
            }

    }

    constructor(ownerModel: ModelT) : super(Builder<NodeT, ModelT>(), ownerModel)

    constructor(ownerModel: ModelT, id: String) : super(Builder<NodeT, ModelT>(id), ownerModel)

    protected constructor(orig: EndNode<*, *>, newOwnerModel: ModelT) : super(orig.builder(), newOwnerModel)

    constructor(builder: EndNode.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel)

    override fun builder() = Builder<NodeT, ModelT>(this)

    override val isCompat: Boolean
        get() = false

    override fun setId(id: String) = super.setId(id)

    override fun setLabel(label: String?) = super.setLabel(label)

    override fun setOwnerModel(newOwnerModel: ModelT) = super.setOwnerModel(newOwnerModel)

    override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

    override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

    override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

    override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

    override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

    override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

}
