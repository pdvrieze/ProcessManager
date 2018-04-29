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

import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.util.multiplatform.createUri
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*


/**
 * Simple pojo implementation of [EndpointDescriptor] that supports
 * serialization through [JAXB].
 *
 * @author Paul de Vrieze
 */
@XmlDeserializer(EndpointDescriptorImpl.Factory::class)
class EndpointDescriptorImpl : EndpointDescriptor, XmlSerializable, SimpleXmlDeserializable {

    internal lateinit var serviceLocalName: String

    internal lateinit var serviceNamespace: String

    override lateinit var endpointName: String

    override var endpointLocation: URI? = null

    override val elementName: QName
        get() = ELEMENTNAME

    internal var endpointLocationString: String
        get() = endpointLocation!!.toString()
        set(location) {
            endpointLocation = createUri(location)
        }

    override val serviceName: QName
        get() = QName(serviceNamespace, serviceLocalName)

    class Factory : XmlDeserializerFactory<EndpointDescriptorImpl> {

        override fun deserialize(reader: XmlReader): EndpointDescriptorImpl {
            return Companion.deserialize(reader)
        }
    }

    constructor() {}

    constructor(serviceName: QName, endpointName: String, endpointLocation: URI) {
        serviceLocalName = serviceName.localPart
        serviceNamespace = serviceName.namespaceURI
        this.endpointName = endpointName
        this.endpointLocation = endpointLocation
    }

    override fun deserializeChild(reader: XmlReader): Boolean {
        return false // No children
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
        return false // No child text
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
        when (attributeLocalName.toString()) {
            "endpointLocation" -> {
                endpointLocation = createUri(attributeValue.toString())
                return true
            }
            "endpointName"     -> {
                endpointName = attributeValue.toString()
                return true
            }
            "serviceLocalName" -> {
                serviceLocalName = attributeValue.toString()
                return true
            }
            "serviceNS"        -> {
                serviceNamespace = attributeValue.toString()
                return true
            }
        }
        return false
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
        // do nothing
    }

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME) {
            writeAttribute("endpointLocation", endpointLocation?.toString())
            writeAttribute("endpointName", endpointName)
            writeAttribute("serviceLocalName", serviceLocalName)
            writeAttribute("serviceNS", serviceNamespace)
        }
    }

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return other!=null && serviceNamespace == other.serviceName.getNamespaceURI() && serviceLocalName == other.serviceName.getLocalPart() && endpointName == other.endpointName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EndpointDescriptorImpl) return false

        if (serviceLocalName != other.serviceLocalName) return false
        if (serviceNamespace != other.serviceNamespace) return false
        if (endpointName != other.endpointName) return false
        if (endpointLocation != other.endpointLocation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceLocalName.hashCode()
        result = 31 * result + serviceNamespace.hashCode()
        result = 31 * result + endpointName.hashCode()
        result = 31 * result + (endpointLocation?.hashCode() ?: 0)
        return result
    }

    companion object {

        val MY_JBI_NS = "http://adaptivity.nl/jbi"
        val ELEMENTLOCALNAME = "endpointDescriptor"
        val ELEMENTNAME = QName(MY_JBI_NS,
                                ELEMENTLOCALNAME, "jbi")


        private fun deserialize(reader: XmlReader): EndpointDescriptorImpl {
            return EndpointDescriptorImpl().deserializeHelper(reader)
        }
    }

}
