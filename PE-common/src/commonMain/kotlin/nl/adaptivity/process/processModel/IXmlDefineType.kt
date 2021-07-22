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
import nl.adaptivity.xmlutil.Namespace

@Serializable(IXmlDefineType.Serializer::class)
interface IXmlDefineType {

    val content: CharArray?

    /**
     * Gets the value of the node property.
     *
     * @return possible object is [String]
     */
    fun getRefNode(): String?

    /**
     * Sets the value of the node property.
     *
     * @param value allowed object is [String]
     */
    fun setRefNode(value: String?)

    /**
     * Gets the value of the name property.
     *
     * @return possible object is [String]
     */
    fun getRefName(): String?

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is [String]
     */
    fun setRefName(value: String?)

    /**
     * Gets the value of the paramName property.
     *
     * @return possible object is [String]
     */
    fun getName(): String

    /**
     * Sets the value of the paramName property.
     *
     * @param value allowed object is [String]
     */
    fun setName(value: String)

    /**
     * Gets the value of the path property.
     *
     * @return possible object is [String]
     */
    fun getPath(): String?

    /**
     * Sets the value of the path property.

     * @param namespaceContext
     *
     * @param value allowed object is [String]
     */
    fun setPath(namespaceContext: Iterable<Namespace>, value: String?)


    /**
     * Get the namespace context that defines the "missing" namespaces in the content.
     * @return
     */
    val originalNSContext: Iterable<Namespace>

    fun copy(
        name: String = getName(),
        refNode: String? = getRefNode(),
        refName: String? = getRefName(),
        path: String? = getPath(),
        content: CharArray? = this.content,
        nsContext: Iterable<Namespace> = originalNSContext
            ): IXmlDefineType

    companion object Serializer : KSerializer<IXmlDefineType> {
        override val descriptor: SerialDescriptor
            get() = XmlResultType.serializer().descriptor

        override fun deserialize(decoder: Decoder): IXmlDefineType {
            return XmlDefineType.serializer().deserialize(decoder)
        }

        override fun serialize(encoder: Encoder, value: IXmlDefineType) {
            XmlDefineType.serializer().serialize(encoder, XmlDefineType(value))
        }
    }

}

var IXmlDefineType.refNode: String?
    inline get() = getRefNode()
    inline set(value) {
        setRefNode(value)
    }

var IXmlDefineType.refName: String?
    inline get() = getRefName()
    inline set(value) {
        setRefName(value)
    }

var IXmlDefineType.name: String
    inline get() = getName()
    inline set(value) {
        setName(value)
    }


object IXmlDefineTypeListSerializer : KSerializer<List<IXmlDefineType>> {

    val delegate = ListSerializer(XmlDefineType)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<IXmlDefineType> {
        return delegate.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, obj: List<IXmlDefineType>) {
        delegate.serialize(encoder, obj.map(::XmlDefineType))
    }
}
