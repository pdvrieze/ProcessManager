package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor

/** Abstract base interface for all services */
interface InvokableMethod {
    val endpoint: EndpointDescriptor
    val contentType: String?
    val url: String?
}

/** Interface for SOAP services */
interface SOAPMethod: InvokableMethod {
    override val contentType: String
        get() = "application/soap+xml"
    val operation: String
}

interface RESTMethod: InvokableMethod {
    val method: String
    override val url: String
    override val contentType: String
}
