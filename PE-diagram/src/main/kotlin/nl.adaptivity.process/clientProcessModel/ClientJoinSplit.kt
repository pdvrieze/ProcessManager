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


import nl.adaptivity.process.processModel.JoinSplit


interface ClientJoinSplit<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> : JoinSplit<NodeT, ModelT>, ClientProcessNode<NodeT, ModelT> {

  interface Builder<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> : JoinSplit.Builder<NodeT, ModelT>, ClientProcessNode.Builder<NodeT, ModelT> {

    override fun build(newOwner: ModelT): ClientJoinSplit<NodeT, ModelT>
  }

  override fun builder(): Builder<NodeT, ModelT>
}