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
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Base class for start nodes. It knows about the data
 */
abstract class EventNodeBase<NodeT : ProcessNode, ModelT : ProcessModel<NodeT>?> : ProcessNodeBase, EventNode {

    final override val predecessor: Identifiable?
        get() = predecessors.singleOrNull()

    final override val successor: Identifiable?
        get() = successors.singleOrNull()

    final override val isThrowing: Boolean

    final override val eventType: IEventNode.Type


    constructor(
        ownerModel: ModelT,
        predecessor: Identified? = null,
        successor: Identified? = null,
        id: String? = null,
        label: String? = null,
        x: Double = Double.NaN,
        y: Double = Double.NaN,
//        defines: Collection<IXmlDefineType> = emptyList(),
        results: Collection<IXmlResultType> = emptyList(),
        isThrowing: Boolean = false,
        eventType: IEventNode.Type = IEventNode.Type.MESSAGE,
        isMultiInstance: Boolean = false,
    ) : super(
        _ownerModel = ownerModel,
        predecessors = listOfNotNull(predecessor),
        successors = listOfNotNull(successor),
        id = id,
        label = label,
        x = x,
        y = y,
        defines = emptyList(),
        results = results,
        isMultiInstance = isMultiInstance,
    ) {
        this.isThrowing = isThrowing
        this.eventType = eventType
    }

    constructor(builder: EventNode.Builder, buildHelper: ProcessModel.BuildHelper<*, *, *, *>) :
        this(builder, buildHelper.newOwner)

    constructor(builder: EventNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner, emptyList()) {
            this.isThrowing = builder.isThrowing
            this.eventType = builder.eventType ?: IEventNode.Type.MESSAGE
        }

    override fun builder(): EventNode.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitEventNode(this)
    }

    @SerialName(EventNode.ELEMENTLOCALNAME)
    @XmlSerialName(EventNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    @Serializable
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        val predecessor: Identifier?
        @SerialName("throwing")
        var isThrowing: Boolean = false

        @SerialName("type")
        @XmlElement(false)
        var eventType: IEventNode.Type

        constructor(
            id: String?,
            label: String?,
            defines: Iterable<IXmlDefineType>?,
            results: Iterable<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
            predecessor: Identifier?,
            isThrowing: Boolean,
            eventType: IEventNode.Type
        ) : super(
            id,
            label,
            defines?.map { XmlDefineType(it) },
            results?.map { XmlResultType(it) },
            x,
            y,
            isMultiInstance
        ) {
            this.predecessor = predecessor
            this.isThrowing = isThrowing
            this.eventType = eventType
        }

        constructor(base: EventNode) : this(
            base.id,
            base.label,
            base.defines,
            base.results,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.isThrowing,
            base.eventType,
        )

        constructor(base: EventNode.Builder) : this(
            base.id,
            base.label,
            base.defines,
            base.results,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.isThrowing,
            base.eventType ?: IEventNode.Type.MESSAGE,
        )
    }

    open class Builder : ProcessNodeBase.Builder, EventNode.Builder {

        override val idBase: String
            get() = "start"

        final override var predecessor: Identifiable? = null

        final override var successor: Identifiable? = null

        final override var isThrowing: Boolean
        final override var eventType: IEventNode.Type?

        constructor(
            id: String? = null,
            predecessor: Identifiable? = null,
            successor: Identifiable? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = null,
            results: Collection<IXmlResultType>? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false,
            isThrowing: Boolean = false,
            eventType: IEventNode.Type? = IEventNode.Type.MESSAGE,
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
            this.successor = successor
            this.isThrowing = isThrowing
            this.eventType = eventType
        }

        constructor(node: EventNode) : super(node) {
            successor = node.successor
            predecessor = node.predecessor
            isThrowing = node.isThrowing
            eventType = node.eventType
        }

        constructor(serialDelegate: SerialDelegate) : this(
            id = serialDelegate.id,
            predecessor = serialDelegate.predecessor,
            successor = null,
            label = serialDelegate.label,
            defines = serialDelegate.defines,
            results = serialDelegate.results,
            x = serialDelegate.x,
            y = serialDelegate.y,
            isMultiInstance = serialDelegate.isMultiInstance,
            isThrowing = serialDelegate.isThrowing,
            eventType = serialDelegate.eventType,
        )

    }

}
