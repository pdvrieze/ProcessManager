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

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xmlutil.QName

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ChildProcessModel<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel<NodeT,ModelT>, Identifiable {

  @Deprecated("Not needed as childmodels are not nested")
  val ownerModel: RootProcessModel<NodeT, ModelT>? get() = rootModel

  override val rootModel: RootProcessModel<NodeT, ModelT>

  fun builder(rootBuilder: RootProcessModel.Builder<NodeT, ModelT>): Builder<NodeT, ModelT>

  companion object {
    const val ELEMENTLOCALNAME="childModel"
    val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)
  }

    interface Builder<NodeT : ProcessNode<NodeT, ModelT>, ModelT : ProcessModel<NodeT, ModelT>?> : ProcessModel.Builder<NodeT,ModelT> {
        val childIdBase: String get() = "child"
        var childId: String?
        fun buildModel(buildHelper: ProcessModel.BuildHelper<NodeT, ModelT>): ChildProcessModel<NodeT, ModelT>
    }

}