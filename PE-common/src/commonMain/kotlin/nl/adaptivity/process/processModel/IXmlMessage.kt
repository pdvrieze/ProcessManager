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

import foo.FakeSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.serialutil.DelegatingSerializer
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

    @FakeSerializer(forClass = IXmlMessage::class)
    companion object : DelegatingSerializer<IXmlMessage, XmlMessage>(XmlMessage) {
        override fun fromDelegate(delegate: XmlMessage): IXmlMessage = delegate

        override fun IXmlMessage.toDelegate(): XmlMessage = XmlMessage.from(this)
    }
}
