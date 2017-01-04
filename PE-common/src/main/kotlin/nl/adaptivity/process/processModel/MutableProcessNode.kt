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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


interface MutableProcessNode<T : MutableProcessNode<T, M>, M : ProcessModel<T, M>> : ProcessNode<T, M> {

  fun setId(id: String)

  fun setOwnerModel(ownerModel: ModelCommon<T,M>)

  fun setPredecessors(predecessors: Collection<Identifiable>)

  fun removePredecessor(node: Identified)

  fun addPredecessor(nodeId: Identified)

  fun addSuccessor(node: Identified)

  fun removeSuccessor(node: Identified)

  fun setSuccessors(successors: Collection<Identified>)

}