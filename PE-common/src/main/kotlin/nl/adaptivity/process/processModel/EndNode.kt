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
import nl.adaptivity.process.util.Identifiable

import javax.xml.namespace.QName


interface EndNode<T : ProcessNode<T, M>, M : ProcessModel<T, M>> : ProcessNode<T, M> {

  interface Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>> : ProcessNode.Builder<T, M> {
    override fun build(newOwner: M): EndNode<T, M>
    var predecessor: Identifiable?
  }

  fun setDefines(exports: Collection<@JvmWildcard IXmlDefineType>)

  var predecessor: Identifiable?

  companion object {

    const val ELEMENTLOCALNAME = "end"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

  }

}