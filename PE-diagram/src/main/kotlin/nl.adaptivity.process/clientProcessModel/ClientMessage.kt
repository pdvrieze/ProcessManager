/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.processModel.BaseMessage
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.*
import org.w3c.dom.*

import javax.xml.namespace.QName

import nl.adaptivity.process.clientProcessModel.RootClientProcessModel.Companion.NS_PM

class ClientMessage : BaseMessage {

    constructor() : super() {}

    @Throws(XmlException::class)
    constructor(service: QName, endpoint: String, operation: String, url: String, method: String, contentType: String,
                messageBody: Node) : super(service, endpoint, operation, url, method, contentType, messageBody) {
    }

    @Throws(XmlException::class)
    constructor(message: IXmlMessage) : super(message) {
    }

    override fun getEndpointDescriptor(): EndpointDescriptor? {
        throw UnsupportedOperationException("Not supported for clientmessages yet")
    }

    @Throws(XmlException::class)
    override fun serializeStartElement(out: XmlWriter) {
        out.smartStartTag(elementName)
    }

    @Throws(XmlException::class)
    override fun serializeEndElement(out: XmlWriter) {
        out.endTag(elementName)
    }

    override val elementName: QName
        get() = ELEMENTNAME

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
        out.startTag(RootClientProcessModel.NS_PM, "message", null)
        if (serviceNS != null) out.attribute(null, "serviceNS", null, serviceNS!!)
        if (serviceName != null) out.attribute(null, "serviceName", null, serviceName!!)
        if (endpoint != null) out.attribute(null, "endpoint", null, endpoint)
        if (operation != null) out.attribute(null, "operation", null, operation)
        if (url != null) out.attribute(null, "url", null, url)
        if (contentType != null) out.attribute(null, "type", null, contentType)
        if (method != null) out.attribute(null, "method", null, method)
        // TODO don't do this through DOM
        serialize(out, messageBodyNode)

        out.endTag(RootClientProcessModel.NS_PM, "message", null)
    }

    @Throws(XmlException::class)
    private fun serialize(out: XmlWriter, node: Node) {
        when (node.nodeType) {
            Node.ELEMENT_NODE -> serializeElement(out, node as Element)
            Node.CDATA_SECTION_NODE -> out.cdsect((node as CDATASection).data)
            Node.COMMENT_NODE -> out.comment((node as Comment).data)
            Node.ENTITY_REFERENCE_NODE -> out.entityRef((node as EntityReference).localName)
            Node.TEXT_NODE -> out.text((node as Text).data)
        }

    }

    @Throws(XmlException::class)
    private fun serializeElement(out: XmlWriter, node: Element) {
        out.namespaceAttr(node.prefix, node.namespaceURI)
        out.startTag(node.namespaceURI, node.localName, null)
        val attrs = node.attributes
        for (i in 0..attrs.length - 1) {
            val attr = attrs.item(i) as Attr
            out.attribute(attr.namespaceURI, attr.localName, null, attr.value)
        }
        var child: Node? = node.firstChild
        while (child != null) {
            serialize(out, node)
            child = child.nextSibling
        }
        out.endTag(node.namespaceURI, node.localName, null)
    }

    companion object {


        val ELEMENTNAME = QName(RootClientProcessModel.NS_PM, "message", "pm")

        fun from(message: IXmlMessage?): ClientMessage? {
            if (message == null) {
                return null
            }
            if (message is ClientMessage) {
                return message
            }
            try {
                return ClientMessage(message)
            } catch (e: XmlException) {
                throw RuntimeException(e)
            }

        }
    }

}
