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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package nl.adaptivity.process.processModel

import kotlinx.serialization.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.util.multiplatform.toCharArray
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XmlResultType.Companion::class)
class XmlResultType : XPathHolder, IPlatformXmlResultType {
    override val bodyStreamReader: XmlReader
        get() = getXmlReader()

    constructor(
        name: String?,
        path: String? = null,
        content: CharArray? = null,
        originalNSContext: Iterable<Namespace> = emptyList()
    ) : super(name, path, content, originalNSContext)

    constructor(
        name: String?,
        path: String? = null,
        content: CharSequence?,
        nsContext: Iterable<Namespace> = emptyList()
    ) : this(name, path, content?.toCharArray(), nsContext)

    override fun serializeStartElement(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME)
    }

    override fun serializeEndElement(out: XmlWriter) {
        out.endTag(ELEMENTNAME)
    }

    override fun serialize(out: XmlWriter) {
        XML { autoPolymorphic = true }.encodeToWriter(out, Companion, this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "XmlResultType(content=$contentString, namespaces=(${originalNSContext.joinToString()}), name=$name, path=${getPath()})"
    }

    @ProcessModelDSL
    class Builder constructor(
        var name: String? = null,
        var path: String? = null,
        var content: CharArray? = CharArray(0),
        nsContext: Iterable<Namespace> = emptyList(),
    ) {

        val nsContext = nsContext.toMutableList()

        constructor(orig: IXmlResultType) : this(orig.name, orig.path, orig.content?.copyOf(), orig.originalNSContext)

        fun build(): XmlResultType {
            return XmlResultType(name, path, content, nsContext)
        }

    }

    /** Dummy serializer that is just used to get the annotations on the type. */
    @Serializable
    @XmlSerialName(value = XmlResultType.ELEMENTLOCALNAME, namespace = Engine.NAMESPACE, prefix = Engine.NSPREFIX)
    private class XmlResultTypeAnnotationHelper {}

    companion object : XPathHolderSerializer<XmlResultType>() {
        override val descriptor = buildClassSerialDescriptor("XmlResultType") {
            annotations = XmlResultTypeAnnotationHelper.serializer().descriptor.annotations
            element<String>("name")
            element<String>("xpath")
            element<String>("namespaces")
            element<String>("content")
        }

        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader): XmlResultType {
            return XML { autoPolymorphic = true }.decodeFromReader(this, reader)
        }

        const val ELEMENTLOCALNAME = "result"
        private val ELEMENTNAME = QName(
            Engine.NAMESPACE,
            ELEMENTLOCALNAME, Engine.NSPREFIX
        )

        @Deprecated(
            "Use normal factory method",
            ReplaceWith("XmlResultType(import)", "nl.adaptivity.process.processModel.XmlResultType")
        )
        operator fun get(import: IXmlResultType) = XmlResultType(import)

        override fun deserialize(decoder: Decoder): XmlResultType {
            val data = PathHolderData(this)
            data.deserialize(descriptor, decoder, XmlResultType)
            return XmlResultType(data.name, data.path, data.content, data.namespaces)
        }

        override fun serialize(encoder: Encoder, value: XmlResultType) {
            serialize(descriptor, encoder, value)
        }
    }

}

fun XmlResultType(import: IXmlResultType): XmlResultType {
    if (import is XmlResultType) {
        return import
    }
    val originalNSContext: Iterable<Namespace> = import.originalNSContext
    return XmlResultType(
        import.getName(), import.getPath(), content = null,
        originalNSContext = originalNSContext
    )
}
