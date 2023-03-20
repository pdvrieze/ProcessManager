package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.xmlutil.QName

/** Abstract base interface for all services */
interface InvokableMethod {
    val contentType: String?
    val url: String?
}

/** Interface for SOAP services */
interface SOAPMethod: InvokableMethod, EndpointDescriptor {
    override val serviceName: QName
    override val endpointName: String
    override val contentType: String
        get() = "application/soap+xml"
    val operation: String
}

interface RESTMethod: InvokableMethod {
    val method: String
    override val url: String
    override val contentType: String
}
