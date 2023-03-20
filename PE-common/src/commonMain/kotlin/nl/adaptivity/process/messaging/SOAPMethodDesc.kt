package nl.adaptivity.process.messaging

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xmlutil.QName

class SOAPMethodDesc(
    override val serviceName: QName,
    override val endpointName: String,
    override val operation: String,
    override val url: String? = null
) : SOAPMethod {
    override val endpointLocation: URI? get() = url?.toUri()

    constructor(endpointDescriptor: EndpointDescriptor, operation: String) :
        this(
            serviceName = requireNotNull(endpointDescriptor.serviceName),
            endpointName = requireNotNull(endpointDescriptor.endpointName),
            operation = operation,
            url = endpointDescriptor.endpointLocation?.toString()
        )

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return other !=null && serviceName == other.serviceName && endpointName == other.endpointName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SOAPMethodDesc

        if (serviceName != other.serviceName) return false
        if (endpointName != other.endpointName) return false
        if (operation != other.operation) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceName.hashCode()
        result = 31 * result + endpointName.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }


}

class RESTMethodDesc(
    override val method: String,
    override val url: String,
    override val contentType: String

) : RESTMethod {
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
