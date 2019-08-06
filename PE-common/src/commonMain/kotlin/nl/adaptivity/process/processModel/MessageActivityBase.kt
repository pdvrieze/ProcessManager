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
import nl.adaptivity.process.util.Identifiable

@Serializable
abstract class MessageActivityBase : ActivityBase, MessageActivity {

    constructor(builder: MessageActivity.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper)

    override fun builder(): MessageActivity.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = visitor.visitActivity(this)

    open class Builder : ActivityBase.BaseBuilder, MessageActivity.Builder {
//        var message = activity.message
        constructor(activity: MessageActivity) : super(activity)


        @Deprecated("Don't use when possible")
        internal constructor(activity: Activity) : super(activity)

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            message: XmlMessage? = null,
            condition: Condition? = null,
            name: String? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            multiInstance: Boolean = false
                   ) : super(
            id,
            predecessor,
            successor,
            label,
            defines,
            results,
            message,
            condition,
            name,
            x,
            y,
            multiInstance
                            )
    }
}
