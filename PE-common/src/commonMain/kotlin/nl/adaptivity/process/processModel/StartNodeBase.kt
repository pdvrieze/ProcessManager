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
@Serializable
abstract class StartNodeBase<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>?> : ProcessNodeBase, StartNode {

    @Transient
    override val maxPredecessorCount: Int
        get() = 0

    @Transient
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
        results: Collection<IXmlResultType> = emptyList()
    ) : super(
        ownerModel,
        emptyList(),
        listOfNotNull(successor),
        id, label, x, y, defines, results
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

    @SerialName(StartNode.ELEMENTLOCALNAME)
    @XmlSerialName(StartNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @Serializable
    open class Builder : ProcessNodeBase.Builder, StartNode.Builder {

        @Transient
        override val idBase: String
            get() = "start"

        @Transient
        final override var successor: Identifiable? = null

        @Transient
        final override val predecessors
            get() = emptySet<Identified>()

        constructor() : this(successor = null)

        constructor(
            id: String? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = emptyList(),
            results: Collection<IXmlResultType>? = emptyList(),
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.successor = successor
        }

        constructor(node: StartNode) : super(node) {
            successor = node.successor
        }

        constructor(serialDelegate: SerialDelegate): this(
            id = serialDelegate.id,
            label = serialDelegate.label,
            defines = serialDelegate.defines,
            results = serialDelegate.results,
            x = serialDelegate.x,
            y = serialDelegate.y,
            isMultiInstance = serialDelegate.isMultiInstance,
        )

    }

}
