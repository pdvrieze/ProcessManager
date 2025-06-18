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
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader

@Serializable(with = IXmlResultType.Serializer::class)
interface IXmlResultType {

    val content: CharArray?

    /**
     * The value of the name property.
     */
    fun getName(): String

    fun setName(value: String)

    /**
     * Gets the value of the path property.
     *
     * @return possible object is [String]
     */
    fun getPath(): String?

    /**
     * A reader for the underlying body stream.
     */
    val bodyStreamReader: XmlReader

    /**
     * Get the namespace context for evaluating the xpath expression.
     * @return the context
     */
    val originalNSContext: Iterable<Namespace>

    fun copy(name: String = this.name, path: String? = this.path, content: CharArray? = this.content, originalNSContext: Iterable<Namespace> = this.originalNSContext): IXmlResultType

    private class Serializer : DelegatingSerializer<IXmlResultType, XmlResultType>(XmlResultType.serializer()) {
        override val descriptor: SerialDescriptor = SerialDescriptor("nl.adaptivity.process.processModel.IXmlResultType", delegateSerializer.descriptor)

        override fun fromDelegate(delegate: XmlResultType): IXmlResultType = delegate

        override fun IXmlResultType.toDelegate(): XmlResultType {
            return this as? XmlResultType ?: XmlResultType(this)
        }
    }

}


val IXmlResultType.path: String?
    inline get() = getPath()

var IXmlResultType.name: String
    inline get(): String = getName()
    inline set(value) {
        setName(value)
    }

fun IXmlResultType.getOriginalNSContext(): Iterable<Namespace> = originalNSContext

object IXmlResultTypeListSerializer : KSerializer<List<IXmlResultType>> {
    val delegate = ListSerializer(XmlResultType.serializer())

    override val descriptor: SerialDescriptor =
        SerialDescriptor(IXmlResultType.serializer().descriptor.serialName+".list",delegate.descriptor)

    override fun deserialize(decoder: Decoder): List<IXmlResultType> {
        return delegate.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<IXmlResultType>) {
        delegate.serialize(encoder, value.map(::XmlResultType))
    }
}
