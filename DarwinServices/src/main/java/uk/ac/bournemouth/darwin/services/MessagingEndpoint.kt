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
package uk.ac.bournemouth.darwin.services

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.MessagingRegistry.messenger
import nl.adaptivity.process.messaging.GenericEndpoint
import jakarta.servlet.ServletConfig
import nl.adaptivity.process.util.Constants
import nl.adaptivity.rest.annotations.HttpMethod
import nl.adaptivity.rest.annotations.RestMethod
import java.lang.StringBuilder
import java.net.URI
import java.util.ArrayList
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.namespace.QName

class MessagingEndpoint : GenericEndpoint {
    @XmlRootElement(namespace = Constants.DARWIN_NS, name = "endpoint")
    class XmlEndpointDescriptor private constructor(service: QName?,
                                endpoint: String?,
                                location: URI?) {
        @XmlAttribute(name = "service")
        private val service: QName? = service

        @XmlAttribute(name = "endpoint")
        private val endpoint: String? = endpoint

        @XmlAttribute(name = "url")
        private var location: URI? = location

        constructor(): this(null, null, null)

        constructor(endpoint: EndpointDescriptor):
            this (endpoint.serviceName, endpoint.endpointName, endpoint.endpointLocation)
    }

    override val endpointLocation: URI? = null

    override val serviceName: QName get() = Companion.serviceName
    override val endpointName: String get() = Companion.endpointName

    private var endpointDescriptor: EndpointDescriptor? = null

    override fun isSameService(other: EndpointDescriptor?): Boolean {
        return other != null && other.serviceName.run {
            this != null &&
                namespaceURI == Constants.DARWIN_NS &&
                localPart == SERVICE_LOCALNAME
        } && endpointName == other.endpointName

    }

    override fun initEndpoint(config: ServletConfig) {
        val messenger = messenger
        endpointDescriptor = run {
            val path = StringBuilder(config.servletContext.contextPath)
            path.append("/endpoints")
            messenger.registerEndpoint(
                Companion.serviceName,
                Companion.endpointName,
                URI.create(path.toString())
            )
        }
    }

    @get:RestMethod(
        method = HttpMethod.GET,
        path = "/endpoints"
    )
    @get:XmlElementWrapper(
        name = "endpoints",
        namespace = Constants.DARWIN_NS
    )
    val endpoints: List<XmlEndpointDescriptor>
        get() {
            val messenger = messenger
            val endpoints = messenger.registeredEndpoints
            val result = ArrayList<XmlEndpointDescriptor>(endpoints.size)
            for (endpoint in endpoints) {
                result.add(XmlEndpointDescriptor(endpoint))
            }
            return result
        }

    override fun destroy() {
        if (endpointDescriptor != null) {
            val messenger = messenger
            if (messenger != null) {
                messenger.unregisterEndpoint(endpointDescriptor!!)
            }
            endpointDescriptor = null
        }
    }

    companion object {
        val endpointName = "messaging"

        const val SERVICE_LOCALNAME = "messaging"

        val serviceName = QName(Constants.DARWIN_NS, SERVICE_LOCALNAME)
    }
}
