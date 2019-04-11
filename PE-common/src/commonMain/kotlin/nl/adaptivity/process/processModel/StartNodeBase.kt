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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag


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

    constructor(ownerModel: ModelT,
                successor: Identified? = null,
                id: String? = null,
                label: String? = null,
                x: Double = Double.NaN,
                y: Double = Double.NaN,
                defines: Collection<IXmlDefineType> = emptyList(),
                results: Collection<IXmlResultType> = emptyList())
        : super(ownerModel,
                emptyList(),
                listOfNotNull(successor),
                id, label, x, y, defines, results)

    constructor(builder: StartNode.Builder, buildHelper: ProcessModel.BuildHelper<*,*,*,*>) : this(builder, buildHelper.newOwner)

    constructor(builder: StartNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner)

    override abstract fun builder(): Builder

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(StartNode.ELEMENTNAME) {
            serializeAttributes(this)
            serializeChildren(this)
        }
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitStartNode(this)
    }


    @Serializable
    abstract class Builder : ProcessNodeBase.Builder, StartNode.Builder, SimpleXmlDeserializable {

        @Transient
        override val idBase: String
            get() = "start"

        @Transient
        override val elementName: QName
            get() = StartNode.ELEMENTNAME

        @Transient
        final override var successor: Identifiable? = null

        @Transient
        final override val predecessors
            get() = emptySet<Identified>()

        constructor() : this(successor = null)

        constructor(id: String? = null,
                    successor: Identifiable? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    multiInstance: Boolean = false) : super(id, label, defines,
                                                            results, x, y, multiInstance) {
            this.successor = successor
        }

        constructor(node: StartNode) : super(node) {
            successor = node.successor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (ProcessConsts.Engine.NAMESPACE == reader.namespaceURI) {
                when (reader.localName) {
                    "import" -> {
                        (results as MutableList).add(XmlResultType.deserialize(reader))
                        return true
                    }
                }
            }
            return false
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            return false
        }

    }

}
