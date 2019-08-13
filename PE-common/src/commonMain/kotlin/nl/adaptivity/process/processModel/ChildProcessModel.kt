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
import nl.adaptivity.util.multiplatform.JvmDefault
import nl.adaptivity.xmlutil.QName

/**
 * Created by pdvrieze on 02/01/17.
 */
interface ChildProcessModel<out NodeT : ProcessNode> : ProcessModel<NodeT>, Identifiable {

    @Deprecated("Not needed as childmodels are not nested")
    val ownerModel: RootProcessModel<NodeT>?
        get() = rootModel

    override val rootModel: RootProcessModel<NodeT>

    fun builder(rootBuilder: RootProcessModel.Builder): Builder

    companion object {
        const val ELEMENTLOCALNAME = "childModel"
        val ELEMENTNAME =
            QName(ProcessConsts.Engine.NAMESPACE, Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)
    }

    interface Builder : ProcessModel.Builder {
        @JvmDefault
        val childIdBase: String
            get() = CHILD_ID_BASE
        var childId: String?

        @JvmDefault
        fun <NodeT : ProcessNode, ChildT : ChildProcessModel<NodeT>> buildModel(buildHelper: ProcessModel.BuildHelper<NodeT, *, *, ChildT>): ChildT =
            buildHelper.childModel(this)

        companion object {
            const val CHILD_ID_BASE = "child"
        }
    }

}
