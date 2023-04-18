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
import nl.adaptivity.xmlutil.util.ICompactFragment

interface IXmlMessage {

    val targetService: InvokableMethod

    @Deprecated("Use targetService", ReplaceWith("targetService.endpoint.serviceName?.getLocalPart()"))
    val serviceName: String?
        get() = targetService.endpoint.serviceName?.getLocalPart()

    @Deprecated("Use targetService", ReplaceWith("targetService.endpoint.serviceName?.getNamespaceURI()"))
    val serviceNS: String?
        get() = targetService.endpoint.serviceName?.getNamespaceURI()

    @Deprecated("Use targetService", ReplaceWith("targetService.endpoint.serviceName"))
    val service: QName?
        get() = targetService.endpoint.serviceName

    @Deprecated("Use targetService", ReplaceWith("targetService.endpoint.endpointName"))
    val endpoint: String?
        get() = targetService.endpoint.endpointName

    @Deprecated("Use targetService", ReplaceWith("targetService.endpoint"))
    val endpointDescriptor: EndpointDescriptor?
        get() = targetService.endpoint

    @Deprecated("Use targetService", ReplaceWith("targetService.operation"))
    val operation: String?
        get() = (targetService as? SOAPMethod)?.operation

    val messageBody: ICompactFragment

    @Deprecated("Use targetServicce", ReplaceWith("targetService.endpoint.endpointLocation?.toString()"))
    val url: String?
        get() = targetService.endpoint.endpointLocation?.toString()

    @Deprecated("Use targetService",
        ReplaceWith("(targetService as? RESTMethod)?.method", "nl.adaptivity.process.messaging.RESTMethod")
    )
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
