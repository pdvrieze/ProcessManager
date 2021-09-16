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

import nl.adaptivity.process.util.Identifiable

abstract class CompositeActivityBase : ActivityBase, CompositeActivity {

    private var _childModel: ChildProcessModel<ProcessNode>? = null

    override val childModel: ChildProcessModel<ProcessNode>
        get() = _childModel!!

    constructor(
        builder: CompositeActivity.ModelBuilder,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder, buildHelper.newOwner, otherNodes) {
        _childModel = buildHelper.childModel(builder)
    }

    constructor(
        builder: CompositeActivity.ReferenceBuilder,
        buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder, buildHelper.newOwner, otherNodes) {
        _childModel = buildHelper.childModel(
            builder.childId ?: throw IllegalProcessModelException("Missing childId for reference")
        )
    }

    override fun builder(): CompositeActivity.ReferenceBuilder {
        return ReferenceBuilder(this)
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R = visitor.visitActivity(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as CompositeActivityBase

        if (childModel != other.childModel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + childModel.hashCode()
        return result
    }

    open class ReferenceBuilder : BaseBuilder, CompositeActivity.ReferenceBuilder {
        final override var childId: String? = null

        constructor(
            id: String?,
            predecessor: Identifiable?,
            successor: Identifiable?,
            label: String?,
            defines: Collection<IXmlDefineType>?,
            results: Collection<IXmlResultType>?,
            childId: String?,
            condition: Condition?,
            name: String?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean
       ) : super(
            id,
            predecessor,
            successor,
            label,
            defines,
            results,
            condition,
            name,
            x,
            y,
            isMultiInstance
        ) {
            this.childId = childId
        }

        @Suppress("DEPRECATION")
        constructor(node: CompositeActivity) : this(
            node.id,
            node.predecessor,
            node.successor,
            node.label,
            node.defines,
            node.results,
            node.childModel?.id,
            node.condition,
            node.name,
            node.x,
            node.y,
            node.isMultiInstance,
        )

        constructor(node: SerialDelegate) : this(
            id = node.id,
            predecessor = node.predecessor,
            successor = null,
            label = node.label,
            defines = node.defines,
            results = node.results,
            childId = node.childId?.id,
            condition = node.elementCondition,
            name = node.name,
            x = node.x,
            y = node.y,
            isMultiInstance = node.isMultiInstance,
        )

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R = when (childId) {
            null -> visitor.visitActivity(this as MessageActivity.Builder)
            else -> visitor.visitActivity(this as CompositeActivity.ReferenceBuilder)
        }
    }

}
