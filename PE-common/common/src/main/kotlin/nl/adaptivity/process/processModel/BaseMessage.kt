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

package nl.adaptivity.process.processModel

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.xml.*
import nl.adaptivity.xml.XmlWriter


abstract class BaseMessage : XMLContainer, IXmlMessage {

    override var service: QName? = null
    override lateinit var endpoint: String
    override lateinit var operation: String
    override lateinit var url: String
    override lateinit var method: String
    private var type: String? = null

    override val elementName: QName
        get() = ELEMENTNAME

    override var serviceName: String?
        get() {
            return service?.localPart
        }
        set(name) = when (service) {
            null -> service = name?.let { QName(name) }
            else -> service = name?.let { QName(service!!.getNamespaceURI(), it) }
        }

    override var serviceNS: String?
        get() = if (service == null) null else service!!.getNamespaceURI()
        set(namespace) = if (service == null) {
            service = namespace?.let { QName(it, "xx") }
        } else {
            service = namespace?.let { QName(it, service!!.getLocalPart()) }
        }

    override val messageBody: ICompactFragment
        get() = CompactFragment(originalNSContext, content)

    override val contentType: String
        get() = type ?: "application/soap+xml"

    protected constructor() : super() {}

    constructor(service: QName?,
                endpoint: String,
                operation: String,
                url: String,
                method: String,
                contentType: String,
                messageBody: ICompactFragment) : super(messageBody) {
        this.service = service
        this.endpoint = endpoint
        this.operation = operation
        this.url = url
        this.method = method
        type = contentType
    }

    constructor(message: IXmlMessage) : this(message.service,
                                             message.endpoint,
                                             message.operation,
                                             message.url,
                                             message.method,
                                             message.contentType,
                                             message.messageBody) {
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        out.writeAttribute("type", contentType)
        out.writeAttribute("serviceNS", serviceNS)
        out.writeAttribute("serviceName", serviceName)
        out.writeAttribute("endpoint", endpoint)
        out.writeAttribute("operation", operation)
        out.writeAttribute("url", url)
        out.writeAttribute("method", method)
    }


    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
        if (XMLConstants.NULL_NS_URI == attributeNamespace) {
            val value = attributeValue.toString()
            when (attributeLocalName.toString()) {
                "endpoint"    -> {
                    endpoint = value
                    return true
                }
                "operation"   -> {
                    operation = value
                    return true
                }
                "url"         -> {
                    url = value
                    return true
                }
                "method"      -> {
                    method = value
                    return true
                }
                "type"        -> {
                    type = value
                    return true
                }
                "serviceNS"   -> {
                    serviceNS = value
                    return true
                }
                "serviceName" -> {
                    serviceName = value
                    return true
                }
            }
        }
        return false
    }

    override fun serializeStartElement(out: XmlWriter) {
        out.smartStartTag(elementName)
    }

    override fun serializeEndElement(out: XmlWriter) {
        out.endTag(elementName)
    }

    override fun setType(type: String) {
        this.type = type
    }

    override fun toString(): String {
        return XmlStreaming.toString(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseMessage) return false

        if (service != other.service) return false
        if (endpoint != other.endpoint) return false
        if (operation != other.operation) return false
        if (url != other.url) return false
        if (method != other.method) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = service?.hashCode() ?: 0
        result = 31 * result + endpoint.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    companion object {

        val ELEMENTNAME = QName(Engine.NAMESPACE, "message", "pm")
    }
}