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
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Base class for start nodes. It knows about the data
 */
abstract class StartNodeBase<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>?> : ProcessNodeBase, StartNode {

    override val maxPredecessorCount: Int
        get() = 0

    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    final override val eventType: IEventNode.Type?

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
        eventType: IEventNode.Type? = null,
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
    ) {
        this.eventType = eventType
    }

    constructor(builder: StartNode.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        this(builder, buildHelper.newOwner)

    constructor(builder: StartNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner, emptyList()) {
        this.eventType = builder.eventType
    }

    override fun builder(): StartNode.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitStartNode(this)
    }

    @SerialName(StartNode.ELEMENTLOCALNAME)
    @XmlSerialName(StartNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @Serializable
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        @XmlElement(false)
        var eventType: IEventNode.Type? = null

        constructor(source: StartNode) :
            super(source.id, source.label, x = source.x, y = source.y, isMultiInstance = source.isMultiInstance) {
            eventType = source.eventType
        }

        constructor(source: StartNode.Builder) :
            super(source.id, source.label, x = source.x, y = source.y, isMultiInstance = source.isMultiInstance) {
            eventType = source.eventType
        }
    }

    open class Builder : ProcessNodeBase.Builder, StartNode.Builder {

        override val idBase: String
            get() = "start"

        final override var successor: Identifiable? = null

        final override val predecessors
            get() = emptySet<Identified>()

        final override var eventType: IEventNode.Type?

        constructor() : this(null, null, null, null, null, Double.NaN, Double.NaN, false)

        constructor(
            id: String? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = null,
            results: Collection<IXmlResultType>? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false,
            eventType: IEventNode.Type? = null,
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.successor = successor
            this.eventType = eventType
        }

        constructor(node: StartNode) : super(node) {
            successor = node.successor
            eventType = node.eventType
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
            eventType = serialDelegate.eventType
        )

    }

}
