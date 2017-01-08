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

import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.SplitBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.

 * @param <NodeT> The type of ProcessNode used.
</NodeT> */
abstract class ClientSplitNode(builder: Split.Builder<*, *>, newOwnerModel: DrawableProcessModel?) : SplitBase<DrawableProcessNode, DrawableProcessModel?>(builder, newOwnerModel), DrawableProcessNode {

  abstract class Builder : SplitBase.Builder<DrawableProcessNode, DrawableProcessModel?>, DrawableProcessNode.Builder {

    constructor() {}

    constructor(id: String?,
                predecessor: Identified? = null,
                successors: Collection<Identified>,
                label: String?,
                defines: Collection<IXmlDefineType>,
                results: Collection<IXmlResultType>,
                x: Double,
                y: Double,
                min: Int,
                max: Int) : super(id, predecessor, successors, label, defines, results, x, y, min, max) {}

    constructor(node: Split<*, *>) : super(node) {}

    abstract override fun build(newOwner: DrawableProcessModel?): ClientSplitNode

    override var isCompat:Boolean
      get() = false
      set(compat: Boolean) {
        if (compat) throw IllegalArgumentException("Split nodes cannot be compatible with their own absense")
      }
  }

  abstract override fun builder(): Builder /* {
    return new Builder<>(this);
  }*/

  override val maxSuccessorCount: Int
    get() = Integer.MAX_VALUE

  override val isCompat: Boolean
    get() = false

  override fun setId(id: String) = super.setId(id)

  override fun setLabel(label: String?) = super.setLabel(label)

  override fun setOwnerModel(newOwnerModel: DrawableProcessModel?) = super.setOwnerModel(newOwnerModel)

  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

}
