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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializable
import javax.xml.xpath.XPathExpression

@Serializable(with = IXmlResultType.Serializer::class)
actual interface IXmlResultType {

    actual val content: CharArray?

    /**
     * The value of the name property.
     */
    actual fun getName(): String

    actual fun setName(value: String)

    /**
     * Gets the value of the path property.
     *
     * @return possible object is [String]
     */
    actual fun getPath(): String?

    val xPath: XPathExpression?

    /**
     * Sets the value of the path property.
     *
     * @param namespaceContext
     *
     * @param value allowed object is [String]
     */
    actual fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

    actual val bodyStreamReader: XmlReader

    /**
     * Get the namespace context for evaluating the xpath expression.
     * @return the context
     */
    actual val originalNSContext: Iterable<Namespace>

    actual companion object Serializer : KSerializer<IXmlResultType> {
        override val descriptor: SerialDescriptor
            get() = XmlResultType.serializer().descriptor

        override fun deserialize(decoder: Decoder): IXmlResultType {
            return XmlResultType.serializer().deserialize(decoder)
        }

        override fun serialize(encoder: Encoder, obj: IXmlResultType) {
            XmlResultType.serializer().serialize(encoder, XmlResultType(obj))
        }
    }
}
