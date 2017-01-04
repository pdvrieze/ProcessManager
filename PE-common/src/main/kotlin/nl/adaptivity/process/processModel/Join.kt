/*
 * Copyright (c) 2016.
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


import net.devrieze.util.collection.replaceByNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import javax.xml.namespace.QName


interface Join<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessNode<T, M>, JoinSplit<T, M> {


  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : JoinSplit.Builder<T, M> {

    override fun build(newOwner: ModelCommon<T, M>): ProcessNode<T, M>

    var successor: Identifiable?
      get() = successors.firstOrNull()
      set(value) { successors.replaceByNotNull(value?.identifier) }

  }

  override fun builder(): Builder<T, M>

  companion object {

    const val ELEMENTLOCALNAME = "join"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    val PREDELEMNAME = QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX)
  }
  // No methods beyond JoinSplit
}