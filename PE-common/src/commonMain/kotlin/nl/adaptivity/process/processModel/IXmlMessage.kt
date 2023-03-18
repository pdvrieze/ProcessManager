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

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.xml.QName
import nl.adaptivity.xmlutil.util.ICompactFragment

interface IXmlMessage {

    val serviceName: String?

    val serviceNS: String?

    val service: QName?

    val endpoint: String?

    val endpointDescriptor: EndpointDescriptor?

    val operation: String?

    val messageBody: ICompactFragment

    val serviceAuthData: ServiceAuthData?

    val url: String?

    val method: String?

    val contentType: String

    fun setType(type: String)

    override fun toString(): String

    companion object : DelegatingSerializer<IXmlMessage, XmlMessage>(XmlMessage) {
        override fun fromDelegate(delegate: XmlMessage): IXmlMessage = delegate

        override fun IXmlMessage.toDelegate(): XmlMessage = XmlMessage.from(this)
    }
}
