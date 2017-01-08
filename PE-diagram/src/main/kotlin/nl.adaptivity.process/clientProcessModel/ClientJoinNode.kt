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
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.JoinBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


abstract class ClientJoinNode : JoinBase<NodeT, ModelT>, ClientJoinSplit {

  abstract class Builder : JoinBase.Builder<NodeT, ModelT>,
                           ClientJoinSplit.Builder {

    override var isCompat: Boolean = false

    constructor(id: String? = null,
                predecessors: Collection<Identified> = emptyList(),
                successor: Identified? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList(),
                min: Int = 1,
                max: Int = -1,
                compat: Boolean = false) : super(id, predecessors, successor, label, defines, results, min, max, x, y) {
      isCompat = false
    }

    constructor(node: Join<*, *>) : super(node) {
      isCompat = (node as? ClientProcessNode)?.isCompat ?: false
    }

    abstract override fun build(newOwner: ModelT): ClientJoinNode
  }


  override val isCompat: Boolean

  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  @Deprecated("Use builders")
  constructor(ownerModel: ModelT, compat: Boolean) : super(ownerModel) {
    isCompat = compat
  }

  @Deprecated("Use builders")
  constructor(ownerModel: ModelT, id: String, compat: Boolean) : super(ownerModel) {
    setId(id)
    isCompat = compat
  }

  protected constructor(orig: Join<*, *>, newOwner: ModelT, compat: Boolean) : super(orig.builder(), newOwner) {
    isCompat = compat
  }

  constructor(builder: Join.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel) {
    isCompat = builder is Builder && builder.isCompat
  }

  abstract override fun builder(): Builder

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
