/*
 * Copyright (c) 2019.
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class CompositeActivityBase : ActivityBase, CompositeActivity {

    @Transient
    override final val childModel: ChildProcessModel<ProcessNode>

    constructor(builder: CompositeActivity.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper) {
        childModel = buildHelper.childModel(builder)
    }

    constructor(builder: CompositeActivity.ReferenceBuilder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper) {
        childModel = buildHelper.childModel(builder.childId ?: throw IllegalProcessModelException("Missing childId for reference"))
    }

    override fun builder(): CompositeActivity.ReferenceBuilder {
        return ActivityBase.Builder(this)
    }

    companion object {
        const val ATTR_CHILDID = "childId"
    }
}
