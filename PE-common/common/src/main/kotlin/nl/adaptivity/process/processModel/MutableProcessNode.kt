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

import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified

@Deprecated("Use builders instead of mutable process models")
interface MutableProcessNode<NodeT : MutableProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT> {

  @Deprecated("Use builders instead of mutable process models")
  fun setId(id: String)

  @Deprecated("Use builders instead of mutable process models")
  fun setOwnerModel(newOwnerModel: ModelT)

  @Deprecated("Use builders instead of mutable process models")
  fun setPredecessors(predecessors: Collection<Identifiable>)

  @Deprecated("Use builders instead of mutable process models")
  fun removePredecessor(predecessorId: Identified)

  @Deprecated("Use builders instead of mutable process models")
  fun addPredecessor(predecessorId: Identified)

  @Deprecated("Use builders instead of mutable process models")
  fun addSuccessor(successorId: Identified)

  @Deprecated("Use builders instead of mutable process models")
  fun removeSuccessor(successorId: Identified)

  @Deprecated("Use builders instead of mutable process models")
  fun setSuccessors(successors: Collection<Identified>)

}