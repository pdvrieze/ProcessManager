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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel


import nl.adaptivity.process.ProcessConsts.Engine

import javax.xml.namespace.QName


interface StartNode<T : ProcessNode<T, M>, M : ProcessModel<T, M>> : ProcessNode<T, M> {

  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>> : ProcessNode.Builder<T, M> {
    override fun build(newOwner: M): StartNode<T, M>
  }

  override fun builder(): Builder<T, M>

  companion object {

    const val ELEMENTLOCALNAME = "start"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

  }
  // No special aspects.
}