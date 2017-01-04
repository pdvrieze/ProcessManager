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

/**
 * Created by pdvrieze on 02/01/17.
 */
interface SubModel<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ModelCommon<T,M> {

  interface Builder<T : ProcessNode<T, M>, M : ProcessModelBase<T, M>?> : ModelCommon.Builder<T,M> {
    fun build(ownerNode: T, pedantic: Boolean = false)
  }

  val ownerNode: T
  val parent: ModelCommon<T,M> get() = ownerNode.ownerModel
  override val rootModel get() = parent.rootModel
}