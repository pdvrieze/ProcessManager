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

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ChildProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT,ModelT>, Identifiable {

  interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.Builder<NodeT,ModelT> {
    val childIdBase: String get() = "child"
    var childId: String?
    fun buildModel(ownerModel: RootProcessModel<NodeT, ModelT>, pedantic: Boolean = defaultPedantic): ChildProcessModel<NodeT, ModelT>
  }

  override val id: String?

  @Deprecated("Use the object itself", ReplaceWith("this"))
  val childId: Identifiable? get() = this

  val ownerModel: RootProcessModel<NodeT, ModelT>

  override val rootModel: RootProcessModel<NodeT, ModelT>? get() = ownerModel?.rootModel

  fun builder(rootBuilder: RootProcessModel.Builder<NodeT, ModelT>): Builder<NodeT, ModelT>

  companion object {
    const val ELEMENTLOCALNAME="childModel"
    val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)
  }

}