package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.xmlutil.QName

/** Abstract base interface for all services */
interface InvokableService {
}

/** Interface for SOAP services */
interface SOAPService: InvokableService, EndpointDescriptor {
    override val serviceName: QName
    override val endpointName: String
    val url: String?
}

interface RESTService: InvokableService {
    val method: String
    val url: String
    val contentType: String
}
