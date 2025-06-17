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

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.messaging.RESTMethod
import nl.adaptivity.process.messaging.SOAPMethod
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.ICompactFragment

@Serializable(IXmlMessageSerializer::class)
interface IXmlMessage {

    val targetMethod: InvokableMethod

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.endpoint.serviceName?.getLocalPart()"))
    val serviceName: String?
        get() = targetMethod.endpoint.serviceName?.getLocalPart()

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.endpoint.serviceName?.getNamespaceURI()"))
    val serviceNS: String?
        get() = targetMethod.endpoint.serviceName?.getNamespaceURI()

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.endpoint.serviceName"))
    val service: QName?
        get() = targetMethod.endpoint.serviceName

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.endpoint.endpointName"))
    val endpoint: String?
        get() = targetMethod.endpoint.endpointName

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.endpoint"))
    val endpointDescriptor: EndpointDescriptor?
        get() = targetMethod.endpoint

    @Deprecated("Use targetMethod", ReplaceWith("targetMethod.operation"))
    val operation: String?
        get() = (targetMethod as? SOAPMethod)?.operation

    val messageBody: ICompactFragment

    @Deprecated("Use targetServicce", ReplaceWith("targetMethod.endpoint.endpointLocation?.toString()"))
    val url: String?
        get() = targetMethod.endpoint.endpointLocation?.toString()

    @Deprecated("Use targetMethod",
        ReplaceWith("(targetMethod as? RESTMethod)?.method", "nl.adaptivity.process.messaging.RESTMethod")
    )
    val method: String?
        get() = (targetMethod as? RESTMethod)?.method


    val contentType: String?
        get() = targetMethod.contentType

    override fun toString(): String
}

object IXmlMessageSerializer: DelegatingSerializer<IXmlMessage, XmlMessage>(XmlMessage.Companion) {
    override val descriptor: SerialDescriptor = SerialDescriptor(IXmlMessage::class.qualifiedName!!, delegateSerializer.descriptor)

    override fun fromDelegate(delegate: XmlMessage): IXmlMessage = delegate

    override fun IXmlMessage.toDelegate(): XmlMessage = XmlMessage.from(this)
}
