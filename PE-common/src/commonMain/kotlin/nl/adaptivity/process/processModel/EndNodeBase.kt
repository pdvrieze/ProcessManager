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
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Created by pdvrieze on 24/11/15.
 */
abstract class EndNodeBase : ProcessNodeBase, EndNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: EndNode.Builder, newOwner: ProcessModel<*>, otherNodes: Iterable<ProcessNode.Builder>) :
        super(builder, newOwner, otherNodes)

    @Suppress("DEPRECATION")
    override val predecessor: Identified? get() = predecessors.singleOrNull()

    override val maxSuccessorCount: Int
        get() = 0

    override val successors: IdentifyableSet<Identified>
        get() = IdentifyableSet.empty<Identified>()


    override fun builder(): EndNode.Builder = Builder(this)

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitEndNode(this)
    }

    @Serializable
    @SerialName(EndNode.ELEMENTLOCALNAME)
    @XmlSerialName(EndNode.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    class SerialDelegate : ProcessNodeBase.SerialDelegate {
        val predecessor: Identifier?

        constructor(
            id: String?,
            label: String?,
            defines: Iterable<IXmlDefineType>?,
            results: Iterable<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean,
            predecessor: Identifier?
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
            base.predecessor?.identifier
        )

        constructor(base: EndNode.Builder) : this(
            base.id,
            base.label,
            base.defines,
            base.results,
            base.x,
            base.y,
            base.isMultiInstance,
            base.predecessor?.identifier
        )
    }

    open class Builder : ProcessNodeBase.Builder, EndNode.Builder {

        override val idBase: String get() = "end"

        final override var predecessor: Identifiable? = null

        constructor() : this(
            null,
            null,
            null,
            null,
            null,
            Double.NaN,
            Double.NaN,
            false
        )

        constructor(
            id: String?,
            predecessor: Identified?,
            label: String?,
            defines: Collection<IXmlDefineType>?,
            results: Collection<IXmlResultType>?,
            x: Double,
            y: Double,
            isMultiInstance: Boolean
        ) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
        }


        constructor(node: EndNode) : super(node) {
            this.predecessor = node.predecessor
        }

        internal constructor(serialDelegate: SerialDelegate): this(
            serialDelegate.id,
            serialDelegate.predecessor,
            serialDelegate.label,
            serialDelegate.defines ?: emptyList(),
            serialDelegate.results ?: emptyList(),
            serialDelegate.x,
            serialDelegate.y,
            serialDelegate.isMultiInstance
        )
    }
}
