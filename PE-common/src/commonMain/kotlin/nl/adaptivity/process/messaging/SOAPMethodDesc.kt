package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xmlutil.QName

class SOAPMethodDesc(
    serviceName: QName,
    endpointName: String,
    override val operation: String,
    override val url: String? = null
) : SOAPMethod {
    override val endpoint: EndpointDescriptor = EndpointDescriptorImpl(serviceName, endpointName, url?.toUri())

    constructor(endpointDescriptor: EndpointDescriptor, operation: String) :
        this(
            serviceName = requireNotNull(endpointDescriptor.serviceName),
            endpointName = requireNotNull(endpointDescriptor.endpointName),
            operation = operation,
            url = endpointDescriptor.endpointLocation?.toString()
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SOAPMethodDesc

        if (endpoint.serviceName != other.endpoint.serviceName) return false
        if (endpoint.endpointName != other.endpoint.endpointName) return false
        if (operation != other.operation) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = endpoint.serviceName.hashCode()
        result = 31 * result + endpoint.endpointName.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }


}

class RESTMethodDesc(
    override val endpoint: EndpointDescriptor,
    override val method: String,
    override val contentType: String
) : RESTMethod {
    constructor(
        serviceName: QName,
        method: String,
        url: String,
        contentType: String
    ) : this(EndpointDescriptorImpl(serviceName, null, url.toUri()), method, contentType)

    override val url: String
        get() = endpoint.endpointLocation.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RESTMethodDesc

        if (method != other.method) return false
        if (url != other.url) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
