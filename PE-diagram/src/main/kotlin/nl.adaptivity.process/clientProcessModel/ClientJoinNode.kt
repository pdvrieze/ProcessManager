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


abstract class ClientJoinNode<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> : JoinBase<NodeT, ModelT>, ClientJoinSplit<NodeT, ModelT> {

  abstract class Builder<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> : JoinBase.Builder<NodeT, ModelT>, ClientJoinSplit.Builder<NodeT, ModelT> {

    private var compat: Boolean = false

    constructor(compat: Boolean = false) {
      this.compat = compat
    }

    constructor(predecessors: Collection<Identified>, successor: Identified, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, min: Int, max: Int) : super(id, predecessors, successor, label, defines, results, min, max, x, y) {
      compat = false
    }

    constructor(node: Join<*, *>) : super(node) {
      if (node is ClientProcessNode<*, *>) {
        compat = node.isCompat()
      } else
        compat = false
    }

    abstract override fun build(newOwner: ModelT?): ClientJoinNode<NodeT, ModelT> /* {
      return new ClientJoinNode<NodeT, ModelT>(this, newOwner);
    }*/

    override fun isCompat() = compat

    override fun setCompat(value: Boolean) { compat = value }
  }


  private val mCompat: Boolean

  constructor(ownerModel: ModelT, compat: Boolean) : super(ownerModel) {
    mCompat = compat
  }

  constructor(ownerModel: ModelT, id: String, compat: Boolean) : super(ownerModel) {
    setId(id)
    mCompat = compat
  }

  protected constructor(orig: Join<*, *>, newOwner: ModelT, compat: Boolean) : super(orig.builder(), newOwner) {
    mCompat = compat
  }

  constructor(builder: Join.Builder<*, *>, newOwnerModel: ModelT) : super(builder, newOwnerModel) {
    mCompat = builder is Builder<*, *> && builder.isCompat()
  }

  abstract override fun builder(): Builder<NodeT, ModelT> /* {
    return new Builder<>(this);
  }*/

  override fun setId(id: String?) {
    super.setId(id)
  }

  override fun setLabel(value: String?) { super.setLabel(value) }

  override val maxSuccessorCount: Int
    get() = if (isCompat) Integer.MAX_VALUE else 1

  override fun isCompat(): Boolean {
    return mCompat
  }

  override fun setOwnerModel(newOwnerModel: ModelT) {
    super.setOwnerModel(newOwnerModel)
  }

  override fun setPredecessors(predecessors: Collection<Identifiable>) {
    super.setPredecessors(predecessors)
  }

  override fun removePredecessor(node: Identified) {
    super.removePredecessor(node)
  }

  override fun addPredecessor(nodeId: Identified) {
    super.addPredecessor(nodeId)
  }

  override fun addSuccessor(node: Identified) {
    super.addSuccessor(node)
  }

  override fun removeSuccessor(node: Identified) {
    super.removeSuccessor(node)
  }

  override fun setSuccessors(successors: Collection<Identified>) {
    super.setSuccessors(successors)
  }

}
