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
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable

@Serializable
abstract class CompositeActivityBase : ActivityBase, CompositeActivity {

    @Transient
    override val childModel: ChildProcessModel<ProcessNode>

    constructor(builder: CompositeActivity.ModelBuilder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper) {
        childModel = buildHelper.childModel(builder)
    }

    constructor(builder: CompositeActivity.ReferenceBuilder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        super(builder, buildHelper) {
        childModel = buildHelper.childModel(builder.childId ?: throw IllegalProcessModelException("Missing childId for reference"))
    }

    override fun builder(): CompositeActivity.ReferenceBuilder {
        return ReferenceBuilder(this)
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = visitor.visitActivity(this)

    companion object {
        const val ATTR_CHILDID = "childId"
    }


    @Serializable
    open class ReferenceBuilder : BaseBuilder, MessageActivity.Builder, CompositeActivity.ReferenceBuilder,
                                  SimpleXmlDeserializable {
        final override var childId: String? = null

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType> = emptyList(),
            results: Collection<IXmlResultType> = emptyList(),
            childId: String? = null,
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
            null,
            condition,
            name,
            x,
            y,
            multiInstance
                            ) {
            this.childId = childId
        }

        @Deprecated("Don't use when possible")
        internal constructor(node: Activity) : super(node) {
            childId = node.childModel?.id
        }


        constructor(node: CompositeActivity) : super(node) {
            childId = node.childModel?.id ?: throw IllegalProcessModelException("Missing child id in composite activity")
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R = when (childId) {
            null -> visitor.visitActivity(this as MessageActivity.Builder)
            else -> visitor.visitActivity(this as CompositeActivity.ReferenceBuilder)
        }
    }

}
