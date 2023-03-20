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
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.messaging.RESTMethod
import nl.adaptivity.process.messaging.SOAPMethod
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.util.ICompactFragment

interface IXmlMessage {

    val targetService: InvokableMethod

    @Deprecated("Use targetService")
    val serviceName: String?
        get() = (targetService as? SOAPMethod)?.serviceName?.getLocalPart()

    @Deprecated("Use targetService")
    val serviceNS: String?
        get() = (targetService as? SOAPMethod)?.serviceName?.namespaceURI

    @Deprecated("Use targetService")
    val service: QName?
        get() = (targetService as? SOAPMethod)?.serviceName

    @Deprecated("Use targetService")
    val endpoint: String?
        get() = (targetService as? SOAPMethod)?.endpointName

    @Deprecated("Use targetService")
    val endpointDescriptor: EndpointDescriptor?
        get() = (targetService as? SOAPMethod)

    val operation: String?
        get() = (targetService as? SOAPMethod)?.operation

    val messageBody: ICompactFragment

    @Deprecated("Use targetServicce")
    val url: String?
        get() = targetService.url

    @Deprecated("Use targetService")
    val method: String?
        get() = (targetService as? RESTMethod)?.method


    val contentType: String?
        get() = targetService.contentType

    override fun toString(): String

    companion object : DelegatingSerializer<IXmlMessage, XmlMessage>(XmlMessage) {
        override fun fromDelegate(delegate: XmlMessage): IXmlMessage = delegate

        override fun IXmlMessage.toDelegate(): XmlMessage = XmlMessage.from(this)
    }
}
