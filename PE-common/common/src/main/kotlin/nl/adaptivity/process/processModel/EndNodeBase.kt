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
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*


/**
 * Created by pdvrieze on 24/11/15.
 */
@Serializable
abstract class EndNodeBase<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> : ProcessNodeBase<T, M>, EndNode<T, M> {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: EndNode.Builder<*, *>, buildHelper: ProcessModel.BuildHelper<T, M>) :
        super(builder, buildHelper)

    @Suppress("DEPRECATION")
    @Serializable(with = Identifiable.Companion::class)
    override var predecessor: Identified?
        get() = if (predecessors.size == 0) null else predecessors.single()
        @Deprecated("Use builder")
        set(value) {
            setPredecessors(listOfNotNull(value))
        }

    @Transient
    override val maxSuccessorCount: Int get() = 0

    @Transient
    override val successors: IdentifyableSet<Identified>
        get() = IdentifyableSet.empty<Identified>()


    override abstract fun builder(): Builder<T, M>

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(EndNode.ELEMENTNAME) {
            serializeAttributes(this)
            serializeChildren(this)
        }
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        predecessor?.let { out.writeAttribute(ProcessNodeBase.ATTR_PREDECESSOR, it.id) }
    }

    override fun <R> visit(visitor: ProcessNode.Visitor<R>): R {
        return visitor.visitEndNode(this)
    }

    @Serializable
    abstract class Builder<T : ProcessNode<T, M>, M : ProcessModel<T, M>?> :
        ProcessNodeBase.Builder<T, M>,
        EndNode.Builder<T, M>,
        SimpleXmlDeserializable {

        @Transient
        override val idBase: String get() = "end"

        @Serializable(with = Identifiable.Companion::class)
        final override var predecessor: Identifiable? = null

        @Transient
        override val elementName: QName
            get() = EndNode.ELEMENTNAME

        constructor() : this(id = null)

        constructor(id: String? = null,
                    predecessor: Identified? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    isMultiInstance: Boolean = false) : super(id, label, defines, results, x, y, isMultiInstance) {
            this.predecessor = predecessor
        }


        constructor(node: EndNode<*, *>) : super(node) {
            this.predecessor = node.predecessor
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (ProcessConsts.Engine.NAMESPACE == reader.namespaceURI) {
                when (reader.localName) {
                    "export", XmlDefineType.ELEMENTLOCALNAME -> {
                        defines.add(XmlDefineType.deserialize(reader))
                        return true
                    }
                }
            }
            return false
        }

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            if (ProcessNodeBase.ATTR_PREDECESSOR == attributeLocalName) {
                predecessor = Identifier(attributeValue)
                return true
            }
            return super<ProcessNodeBase.Builder>.deserializeAttribute(attributeNamespace, attributeLocalName,
                                                                       attributeValue)
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            return false
        }

    }
}
