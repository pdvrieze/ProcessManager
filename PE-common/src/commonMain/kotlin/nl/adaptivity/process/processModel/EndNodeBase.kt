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
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Created by pdvrieze on 24/11/15.
 */
abstract class EndNodeBase : ProcessNodeBase, EndNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: EndNode.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes) {
        eventType = builder.eventType ?: IEventNode.Type.TERMINATE
    }

    override val predecessor: Identified? get() = predecessors.singleOrNull()

    override val maxSuccessorCount: Int
        get() = 0

    override val successors: IdentifyableSet<Identified>
        get() = IdentifyableSet.empty<Identified>()

    final override val eventType: IEventNode.Type

    override fun builder(): EndNode.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitEndNode(this)
    }

    @Serializable
    @SerialName(EndNode.ELEMENTLOCALNAME)
    @XmlSerialName(EndNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        val predecessor: Identifier?
        var eventType: IEventNode.Type = IEventNode.Type.TERMINATE

        constructor(
            id: String?,
            label: String?,
            defines: Iterable<IXmlDefineType>?,
            results: Iterable<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
            predecessor: Identifier?,
            eventType: IEventNode.Type,
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
        }

        constructor(base: EndNode) : this(
            base.id,
            base.label,
            base.defines,
            base.results,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.eventType
        )

        constructor(base: EndNode.Builder) : this(
            base.id,
            base.label,
            base.defines,
            base.results,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier,
            base.eventType ?: IEventNode.Type.TERMINATE,
        )
    }

    open class Builder : ProcessNodeBase.Builder, EndNode.Builder {

        override val idBase: String get() = "end"

        final override var predecessor: Identifiable? = null

        override var eventType: IEventNode.Type? = null

        constructor(
            id: String? = null,
            predecessor: Identified? = null,
            label: String? = null,
            defines: Collection<IXmlDefineType>? = null,
            results: Collection<IXmlResultType>? = null,
            x: Double = Double.NaN,
            y: Double = Double.NaN,
            isMultiInstance: Boolean = false,
            eventType: IEventNode.Type = IEventNode.Type.TERMINATE
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
            this.eventType = eventType
        }


        constructor(node: EndNode) : super(node) {
            this.predecessor = node.predecessor
            this.eventType = node.eventType
        }

        internal constructor(serialDelegate: SerialDelegate) : this(
            serialDelegate.id,
            serialDelegate.predecessor,
            serialDelegate.label,
            serialDelegate.defines ?: emptyList(),
            serialDelegate.results ?: emptyList(),
            serialDelegate.x,
            serialDelegate.y,
            serialDelegate.isMultiInstance,
            serialDelegate.eventType
        )
    }
}
