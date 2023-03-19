package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xmlutil.QName

class SOAPServiceDesc(
    override val serviceName: QName,
    override val endpointName: String,
    override val url: String? = null
) : SOAPService {
    override val endpointLocation: URI? get() = url?.toUri()

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return other !=null && serviceName == other.serviceName && endpointName == other.endpointName
    }
}

class RESTServiceDesc(
    override val method: String,
    override val url: String,
    override val contentType: String

) : RESTService {

}
