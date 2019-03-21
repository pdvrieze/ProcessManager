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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName(XmlDefineType.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
@XmlDeserializer(XmlDefineType.Factory::class)
class XmlDefineType : XPathHolder, IXmlDefineType {

    class Factory : XmlDeserializerFactory<XmlDefineType> {

        override fun deserialize(reader: XmlReader): XmlDefineType {
            return XmlDefineType.deserialize(reader)
        }
    }


    @ProcessModelDSL
    class Builder {

        var name: String? = null
        var path: String? = null
        var content: CharArray
        var nsContext: MutableList<Namespace>

        internal constructor() {
            name = null
            path = null
            content = CharArray(0)
            nsContext = ArrayList<Namespace>()
        }

        internal constructor(orig: IXmlResultType) {
            name = orig.getName()
            path = orig.getPath()
            content = orig.content?.copyOf() ?: CharArray(0)
            nsContext = ArrayList<Namespace>()
            val origContext = orig.originalNSContext
            if (origContext != null) {
                for (ns in origContext) {
                    nsContext.add(ns)
                }
            }
        }

        fun build(): XmlResultType {
            return XmlResultType(name, path, content, nsContext)
        }
    }

    private var _refNode: String? = null

    private var _refName: String? = null

    constructor() {}

    constructor(name: String?,
                refNode: String?,
                refName: String?,
                path: String?,
                content: CharArray?,
                originalNSContext: Iterable<Namespace>) : super(name, path, content,
                                                                originalNSContext) {
        this._refNode = refNode
        this._refName = refName
    }

    @Transient
    override val elementName: QName
        get() = ELEMENTNAME

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean {
        when (attributeLocalName) {
            "refnode" -> {
                setRefNode(attributeValue)
                return true
            }
            "refname" -> {
                setRefName(attributeValue)
                return true
            }
            else      -> return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
        }
    }

    override fun serializeStartElement(out: XmlWriter) {
        out.smartStartTag(QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX))
    }

    override fun serializeEndElement(out: XmlWriter) {
        out.endTag(QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX))
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        out.writeAttribute("refnode", getRefNode())
        out.writeAttribute("refname", getRefName())
    }

    /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IXmlDefineType#getRefNode()
       */
    override fun getRefNode(): String? {
        return _refNode
    }

    /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IXmlDefineType#setRefNode(String)
     */
    override fun setRefNode(value: String?) {
        this._refNode = value
    }

    /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.XmlImportType#getName()
     */
    override fun getRefName(): String? {
        return _refName
    }

    /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.XmlImportType#setName(java.lang.String)
     */
    override fun setRefName(value: String?) {
        this._refName = value
    }

    @Serializer(forClass = XmlDefineType::class)
    companion object : XPathHolderSerializer<XmlDefineType>() {

        override val descriptor =
            simpleSerialClassDesc<XmlDefineType>("name" to StringSerializer,
                                                 "refnode" to StringSerializer,
                                                 "refname" to StringSerializer,
                                                 "xpath" to StringSerializer,
                                                 "namespaces" to Namespace.list,
                                                 "content" to StringSerializer)

        const val ELEMENTLOCALNAME = "define"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader): XmlDefineType {
            return XPathHolder.deserialize(reader, XmlDefineType())
        }

        @Deprecated("Use normal factory method",
                    ReplaceWith("XmlDefineType(export)", "nl.adaptivity.process.processModel.XmlDefineType"))
        @kotlin.jvm.JvmStatic
        operator fun get(export: IXmlDefineType) = XmlDefineType(export)

        override fun deserialize(decoder: Decoder): XmlDefineType {
            val data = DefineTypeData()
            data.deserialize(descriptor, decoder)
            return XmlDefineType(data.name, data.refNode, data.refName, data.path, data.content,
                                 data.namespaces ?: emptyList())
        }

        override fun writeAdditionalAttributes(writer: XmlWriter, data: XmlDefineType) {
            super.writeAdditionalAttributes(writer, data)
            writer.writeAttribute("refname", data.getRefName())
            writer.writeAttribute("refnode", data.getRefNode())
        }

        override fun writeAdditionalValues(out: KOutput, desc: KSerialClassDesc, data: XmlDefineType) {
            super.writeAdditionalValues(out, desc, data)
            out.encodeNullableStringElement(desc, desc.getElementIndex("refname"), data.getRefName())
            out.encodeNullableStringElement(desc, desc.getElementIndex("refnode"), data.getRefNode())
        }

        override fun serialize(encoder: Encoder, obj: XmlDefineType) {
            serialize(descriptor, encoder, obj)
        }

        private class DefineTypeData(var refNode: String? = null,
                                     var refName: String? = null) : PathHolderData<XmlDefineType>(this) {

            override fun readAdditionalChild(desc: SerialDescriptor, decoder: CompositeDecoder, index: Int) {
                val name = desc.getElementName(index)
                when (name) {
                    "refnode" -> refNode = decoder.readNullableString(desc, index)
                    "refname" -> refName = decoder.readNullableString(desc, index)
                    else      -> super.readAdditionalChild(desc, decoder, index)
                }
            }

            override fun handleAttribute(attributeLocalName: String, attributeValue: String) {
                when (attributeLocalName) {
                    "refnode" -> refNode = attributeValue
                    "refname" -> refName = attributeValue
                    else      -> super.handleAttribute(attributeLocalName, attributeValue)
                }
            }
        }

    }
}

fun XmlDefineType(export: IXmlDefineType): XmlDefineType {
    if (export is XmlDefineType) {
        return export
    }
    return XmlDefineType(export.getName(), export.getRefNode(), export.getRefName(), export.getPath(), export.content,
                         export.originalNSContext ?: emptyList<Namespace>())
}