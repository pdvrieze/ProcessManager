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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable(IXmlDefineType.Serializer::class)
interface IXmlDefineType {

    val content: CompactFragment

    val refNode: String?

    val refName: String?


    val name: String

    val path: String?

    /**
     * Get the namespace context that defines the "missing" namespaces in the content.
     * @return
     */
    val originalNSContext: IterableNamespaceContext get() = content.namespaces

    fun copy(
        name: String = this.name,
        refNode: String? = this.refNode,
        refName: String? = this.refName,
        path: String? = this.path,
        content: CharArray? = this.content.content,
        nsContext: IterableNamespaceContext = originalNSContext
    ): IXmlDefineType

    fun copy(
        name: String = this.name,
        refNode: String? = this.refNode,
        refName: String? = this.refName,
        path: String? = this.path,
        content: CompactFragment = this.content,
    ): IXmlDefineType

    private class Serializer : DelegatingSerializer<IXmlDefineType, XmlDefineType>("nl.adaptivity.process.processModel.IXmlDefineType", XmlDefineType.serializer()) {

        override fun fromDelegate(delegate: XmlDefineType): IXmlDefineType = delegate
        override fun IXmlDefineType.toDelegate(): XmlDefineType =
            this as? XmlDefineType ?: XmlDefineType(this)
    }

}

val IXmlDefineType.refNode: String?
    inline get() = refNode

val IXmlDefineType.refName: String?
    inline get() = refName

val IXmlDefineType.name: String
    inline get() = name


object IXmlDefineTypeListSerializer : KSerializer<List<IXmlDefineType>> {

    val delegate = ListSerializer(XmlDefineType.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor("IXmlDefineType.List", delegate.descriptor)

    override fun deserialize(decoder: Decoder): List<IXmlDefineType> {
        return delegate.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<IXmlDefineType>) {
        delegate.serialize(encoder, value.map(::XmlDefineType))
    }
}
