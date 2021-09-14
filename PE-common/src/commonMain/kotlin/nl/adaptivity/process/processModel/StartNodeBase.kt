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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Base class for start nodes. It knows about the data
 */
abstract class StartNodeBase<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>?> : ProcessNodeBase, StartNode {

    override val maxPredecessorCount: Int
        get() = 0

    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    constructor(
        ownerModel: ModelT,
        successor: Identified? = null,
        id: String? = null,
        label: String? = null,
        x: Double = Double.NaN,
        y: Double = Double.NaN,
        defines: Collection<IXmlDefineType> = emptyList(),
        results: Collection<IXmlResultType> = emptyList(),
        isMultiInstance: Boolean,
    ) : super(
        ownerModel,
        emptyList(),
        listOfNotNull(successor),
        id,
        label,
        x,
        y,
        defines,
        results,
        isMultiInstance
    )

    constructor(builder: StartNode.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        this(builder, buildHelper.newOwner)

    constructor(builder: StartNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner, emptyList())

    override fun builder(): StartNode.Builder = Builder()

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitStartNode(this)
    }

    @SerialName(StartNode.ELEMENTLOCALNAME)
    @XmlSerialName(StartNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @Serializable
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        constructor(source: StartNode) :
            super(source.id, source.label, x = source.x, y = source.y, isMultiInstance = source.isMultiInstance)

        constructor(source: StartNode.Builder) :
            super(source.id, source.label, x = source.x, y = source.y, isMultiInstance = source.isMultiInstance)
    }

    open class Builder : ProcessNodeBase.Builder, StartNode.Builder {

        override val idBase: String
            get() = "start"

        final override var successor: Identifiable? = null

        final override val predecessors
            get() = emptySet<Identified>()

        constructor() : this(null, null, null, null, null, Double.NaN, Double.NaN, false)

        constructor(
            id: String?,
            successor: Identifiable?,
            label: String?,
            defines: Collection<IXmlDefineType>?,
            results: Collection<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.successor = successor
        }

        constructor(node: StartNode) : super(node) {
            successor = node.successor
        }

        constructor(serialDelegate: SerialDelegate) : this(
            id = serialDelegate.id,
            successor = null,
            label = serialDelegate.label,
            defines = serialDelegate.defines,
            results = serialDelegate.results,
            x = serialDelegate.x,
            y = serialDelegate.y,
            isMultiInstance = serialDelegate.isMultiInstance,
        )

    }

}
