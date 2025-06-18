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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.util.PrefixCompactFragmentSerializer
import nl.adaptivity.util.multiplatform.toCharArray
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable(XmlResultType.Serializer::class)
class XmlResultType : XPathHolder, IPlatformXmlResultType {
    override val bodyStreamReader: XmlReader
        get() = getXmlReader()

    @OptIn(XmlUtilInternal::class)
    constructor(
        name: String?,
        path: String? = null,
        content: CharArray? = null,
        originalNSContext: IterableNamespaceContext = SimpleNamespaceContext()
    ) : super(name, path, content, originalNSContext)

    @OptIn(XmlUtilInternal::class)
    constructor(
        name: String?,
        path: String? = null,
        content: CharSequence?,
        nsContext: IterableNamespaceContext/* = SimpleNamespaceContext()*/
    ) : this(name, path, content?.toCharArray(), nsContext)

    override fun copy(
        name: String,
        path: String?,
        content: CharArray?,
        originalNSContext: Iterable<Namespace>
    ): XmlResultType {
        @OptIn(XmlUtilInternal::class)
        return XmlResultType(name, path, content, originalNSContext as? IterableNamespaceContext ?: SimpleNamespaceContext(originalNSContext))
    }

    override fun serialize(out: XmlWriter) {
        XML { autoPolymorphic = true }.encodeToWriter(out, serializer(), this)
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
    class Builder(
        var name: String? = null,
        var path: String? = null,
        var content: CharArray? = CharArray(0),
        nsContext: Iterable<Namespace> = emptyList(),
    ) {

        val nsContext = nsContext.toMutableList()

        constructor(orig: IXmlResultType) : this(orig.name, orig.path, orig.content?.copyOf(), orig.originalNSContext)

        fun build(): XmlResultType {
            @OptIn(XmlUtilInternal::class)
            return XmlResultType(name, path, content, SimpleNamespaceContext(nsContext))
        }

    }

    /** Dummy serializer that is just used to get the annotations on the type. */
    @Serializable
    @XmlSerialName(value = ELEMENTLOCALNAME, namespace = Engine.NAMESPACE, prefix = Engine.NSPREFIX)
    private class SerialDelegate private constructor(
        @SerialName("name") val name: String? = null,
        @SerialName("xpath") val _xpath: String? = null,
        @SerialName("path") val _path: String? = null,
        @XmlValue override val content: @Serializable(PrefixCompactFragmentSerializer::class) CompactFragment,
    ) : XPathHolderSerializer.SerialDelegateBase {
        override val xpath: String? get() = _xpath ?: _path

        constructor(
            name: String? = null,
            xpath: String? = null,
            content: CompactFragment,
        ): this(name, xpath, null, content)
    }

    private class Serializer : XPathHolderSerializer<XmlResultType, SerialDelegate>(SerialDelegate.serializer()) {
        override val descriptor = SerialDescriptor(
            "nl.adaptivity.process.processModel.XmlResultType",
            delegateSerializer.descriptor
        )

        override fun deserialize(decoder: Decoder): XmlResultType {
            val (data, extNamespaces) = deserializeCommon(decoder)

            return XmlResultType(data.name, data.xpath, data.content.content, extNamespaces)
        }

        override fun serialize(encoder: Encoder, value: XmlResultType) {
            val delegate = SerialDelegate(value.name, value.getPath(), value.fragment)
            delegateSerializer.serialize(encoder, delegate)
        }

    }

    companion object {

        const val ELEMENTLOCALNAME = "result"

        @Deprecated(
            "Use normal factory method",
            ReplaceWith("XmlResultType(import)", "nl.adaptivity.process.processModel.XmlResultType")
        )
        operator fun get(import: IXmlResultType) = XmlResultType(import)
    }

}

fun XmlResultType(import: IXmlResultType): XmlResultType {
    if (import is XmlResultType) {
        return import
    }
    val originalNSContext = import.originalNSContext

    @OptIn(XmlUtilInternal::class)
    return XmlResultType(
        import.getName(), import.getPath(), content = null,
        originalNSContext = SimpleNamespaceContext(originalNSContext)
    )
}
