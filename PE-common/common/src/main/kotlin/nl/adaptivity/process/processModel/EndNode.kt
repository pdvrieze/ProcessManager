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

import net.devrieze.util.collection.replaceByNotNull
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.QName


interface EndNode<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode<NodeT, ModelT> {

  interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessNode.IBuilder<NodeT, ModelT> {
    override fun build(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ProcessNode<NodeT, ModelT>

    var predecessor: Identifiable?
      get() = predecessors.firstOrNull()
      set(value) { predecessors.replaceByNotNull(value?.identifier) }

    override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitEndNode(this)

  }

  override fun builder(): Builder<NodeT, ModelT>

  fun setDefines(exports: Collection<IXmlDefineType>)

  var predecessor: Identified?

  companion object {

    const val ELEMENTLOCALNAME = "end"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

  }

}
