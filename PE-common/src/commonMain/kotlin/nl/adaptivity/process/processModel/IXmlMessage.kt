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

import kotlinx.serialization.*
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xml.QName

interface IXmlMessage {

    var serviceName: String?

    var serviceNS: String?

    var service: QName?

    var endpoint: String?

    val endpointDescriptor: EndpointDescriptor?

    var operation: String?

    val messageBody: ICompactFragment

    var url: String?

    var method: String?

    val contentType: String

    fun setType(type: String)

    override fun toString(): String

    @Serializer(forClass = IXmlMessage::class)
    companion object : KSerializer<IXmlMessage> {
        override val descriptor: SerialDescriptor
            get() = XmlMessage.descriptor


        override fun deserialize(decoder: Decoder): IXmlMessage {
            return XmlMessage.deserialize(decoder)
        }

        override fun serialize(encoder: Encoder, obj: IXmlMessage) {
            return XmlMessage.serialize(encoder, XmlMessage.from(obj))
        }
    }
}
