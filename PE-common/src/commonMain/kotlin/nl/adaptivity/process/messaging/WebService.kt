package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.xmlutil.QName

/** Abstract base interface for all services */
interface WebService {
}

/** Interface for SOAP services */
interface SOAPService: EndpointDescriptor {
    override val serviceName: QName

    val serviceNamespace: String
    override val endpointName: String
}
