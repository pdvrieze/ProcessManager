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

import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified

open class ClientStartNode : StartNodeBase<NodeT, ModelT>, ClientProcessNode {

    open class Builder : StartNodeBase.Builder<NodeT, ModelT>, ClientProcessNode.Builder {

        constructor() {}

        constructor(compat: Boolean) {
            this.isCompat = compat
        }

        constructor(successor: Identified?, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>) : super(id, successor, label, defines, results, x, y) {}

        constructor(node: StartNode<*, *>) : super(node) {
            if (node is ClientStartNode) {
                isCompat = node.isCompat
            } else {
                isCompat = false
            }
        }

        override fun build(newOwner: ModelT): ClientStartNode {
            return ClientStartNode(this, newOwner)
        }

        override var isCompat = false
    }

    override val isCompat: Boolean

    constructor(ownerModel: ModelT, compat: Boolean) : super(ownerModel) {
        isCompat = compat
    }

    constructor(ownerModel: ModelT, id: String, compat: Boolean) : super(ownerModel, id=id) {
        isCompat = compat
    }

    protected constructor(orig: ClientStartNode, newOwnerModel: ModelT, compat: Boolean) : super(orig.builder(), newOwnerModel) {
        isCompat = compat
    }

    constructor(builder: StartNode.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel) {
        if (builder is Builder) {
            isCompat = builder.isCompat
        } else {
            isCompat = false
        }
    }

    override fun builder(): Builder {
        return Builder(this)
    }

    override val maxSuccessorCount: Int
        get() = if (isCompat) Integer.MAX_VALUE else 1

    override fun setId(id: String) = super.setId(id!!)

    override fun setLabel(label: String?) = super.setLabel(label)

    override fun setOwnerModel(newOwnerModel: ModelT) = super.setOwnerModel(newOwnerModel)

    override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

    override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

    override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

    override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

    override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

    override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

}
