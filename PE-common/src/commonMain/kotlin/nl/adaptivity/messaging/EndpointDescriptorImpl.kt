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

package nl.adaptivity.messaging

import kotlinx.serialization.Serializable
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.util.multiplatform.createUri
import nl.adaptivity.util.net.devrieze.serializers.URISerializer
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xml.localPart
import nl.adaptivity.xml.namespaceURI
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.QName as XmlQName


/**
 * Simple pojo implementation of [EndpointDescriptor] that supports
 * serialization through [JAXB].
 *
 * @author Paul de Vrieze
 */
@Serializable
@XmlSerialName(EndpointDescriptorImpl.ELEMENTLOCALNAME, EndpointDescriptorImpl.MY_JBI_NS, "jbi")
class EndpointDescriptorImpl private constructor(
    internal var serviceLocalName: String?,

    internal var serviceNamespace: String?,
    override var endpointName: String?,
    @Serializable(URISerializer::class) override var endpointLocation: URI?
) : EndpointDescriptor {

    constructor(
        serviceName: QName?,
        endpointName: String?,
        endpointLocation: URI?
    ): this(serviceName?.localPart, serviceName?.namespaceURI, endpointName, endpointLocation)

    internal var endpointLocationString: String
        get() = endpointLocation!!.toString()
        set(location) {
            endpointLocation = createUri(location)
        }

    override val serviceName: QName?
        get() = serviceLocalName?.let { QName(serviceNamespace ?: XMLConstants.NULL_NS_URI, it) }

    constructor() : this(null, null, null)

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return other != null &&
            serviceNamespace == other.serviceName?.getNamespaceURI() &&
            serviceLocalName == other.serviceName?.getLocalPart() &&
            endpointName == other.endpointName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EndpointDescriptorImpl

        if (endpointName != other.endpointName) return false
        if (endpointLocation != other.endpointLocation) return false
        if (serviceLocalName != other.serviceLocalName) return false
        if (serviceNamespace != other.serviceNamespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = endpointName?.hashCode() ?: 0
        result = 31 * result + (endpointLocation?.hashCode() ?: 0)
        result = 31 * result + (serviceLocalName?.hashCode() ?: 0)
        result = 31 * result + (serviceNamespace?.hashCode() ?: 0)
        return result
    }

    companion object {

        const val MY_JBI_NS = "http://adaptivity.nl/jbi"
        const val ELEMENTLOCALNAME = "endpointDescriptor"
        val ELEMENTNAME = XmlQName(MY_JBI_NS, ELEMENTLOCALNAME, "jbi")

    }

}
