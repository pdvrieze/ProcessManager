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

import kotlinx.serialization.*
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.serialutil.readNullableString
import nl.adaptivity.util.multiplatform.JvmName
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xml.localPart
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xml.QName as DescQName
import nl.adaptivity.xmlutil.QName as XmlQName


/**
 *
 *
 * Java class for Message complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="Message">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <any processContents='lax'/>
 * </sequence>
 * <attribute name="serviceNS" type="{http://www.w3.org/2001/XMLSchema}string" />
 * <attribute name="endpoint" type="{http://www.w3.org/2001/XMLSchema}string" />
 * <attribute name="operation" type="{http://www.w3.org/2001/XMLSchema}string" />
 * <attribute name="serviceName" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 * <attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 * <attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 */
@Serializable(XmlMessage.Companion::class)
@XmlSerialName(XmlMessage.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
class XmlMessage : XMLContainer, IXmlMessage {

    override var service: DescQName? = null
    override var endpoint: String?
    override var operation: String?
    override var url: String?
    override var method: String?
    private var type: String? = contentType

    override val endpointDescriptor: EndpointDescriptor?
        get() = EndpointDescriptorImpl(service, endpoint, this.url?.toUri())

    override val contentType: String
        get() = type ?: "application/soap+xml"

    override var serviceName: String?
        get() = service?.localPart
        set(name) {
            service = name?.let { DescQName(service?.getNamespaceURI() ?: "", it) }
        }

    override var serviceNS: String?
        get() = service?.getNamespaceURI()
        set(namespace) {
            this.service = namespace?.let { DescQName(it, service?.getLocalPart() ?: "xx") }
        }

    @OptIn(XmlUtilInternal::class)
    override var namespaces: SimpleNamespaceContext
        get() = super.namespaces
        set(value) {
            super.namespaces = value
        }

    override var content: CharArray
        get() = super.content
        set(value) {
            super.content = value
        }

    @OptIn(XmlUtilInternal::class)
    override val messageBody: ICompactFragment
        get() = CompactFragment(namespaces, content)

    constructor() : this(service = null) { /* default constructor */
    }


    @OptIn(XmlUtilInternal::class)
    constructor(
        service: DescQName? = null,
        endpoint: String? = null,
        operation: String? = null,
        url: String? = null,
        method: String? = null,
        contentType: String? = null,
        messageBody: ICompactFragment? = null
    ) {
        this.service = service
        this.endpoint = endpoint
        this.operation = operation
        this.url = url
        this.method = method
        this.type = contentType
        messageBody?.let {
            namespaces = SimpleNamespaceContext(it.namespaces)
            content = it.content
        }
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

    override fun serializeStartElement(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME)
    }

    override fun serializeEndElement(out: XmlWriter) {
        out.endTag(ELEMENTNAME)
    }

    override fun serialize(out: XmlWriter) {
        XML { autoPolymorphic = true }.encodeToWriter(out, Companion, this)
    }

    override fun setType(type: String) {
        this.type = type
    }

    override fun toString(): String {
        return XML.encodeToString(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XmlMessage

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
        result = 31 * result + (endpoint?.hashCode() ?: 0)
        result = 31 * result + (operation?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (method?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object : XmlContainerSerializer<XmlMessage>() {
        override val descriptor: SerialDescriptor = serializer<XmlMessageData>().descriptor // TODO use concrete serializer instance

        private val nullStringSerializer = String.serializer().nullable
        private val typeIdx = descriptor.getElementIndex("type")
        private val serviceNSIdx = descriptor.getElementIndex("serviceNS")
        private val serviceNameIdx = descriptor.getElementIndex("serviceName")
        private val endpointIdx = descriptor.getElementIndex("endpoint")
        private val operationIdx = descriptor.getElementIndex("operation")
        private val urlIdx = descriptor.getElementIndex("url")
        private val methodIdx = descriptor.getElementIndex("method")

        const val ELEMENTLOCALNAME = "message"

        val ELEMENTNAME = XmlQName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)


        @JvmName("fromNullable")
        fun from(message: IXmlMessage?) = when (message) {
            null -> null
            else -> from(message)
        }

        fun from(message: IXmlMessage): XmlMessage {
            return message as? XmlMessage ?: XmlMessage(
                message.service,
                message.endpoint,
                message.operation,
                message.url,
                message.method,
                message.contentType,
                message.messageBody
            )
        }

        override fun deserialize(decoder: Decoder): XmlMessage {
            val data = XmlMessageData(this).apply {
                deserialize(descriptor, decoder, Companion)
            }

            return XmlMessage(data.service, data.endpoint, data.operation, data.url, data.method, data.contentType, data.fragment)
        }


        override fun serialize(encoder: Encoder, value: XmlMessage) {
            super.serialize(descriptor, encoder, value)
        }

        override fun writeAdditionalValues(encoder: CompositeEncoder, desc: SerialDescriptor, data: XmlMessage) {
            super.writeAdditionalValues(encoder, desc, data)
            encoder.encodeSerializableElement(desc, typeIdx, nullStringSerializer, data.contentType)
            encoder.encodeSerializableElement(desc, serviceNSIdx, nullStringSerializer, data.serviceNS)
            encoder.encodeSerializableElement(desc, serviceNameIdx, nullStringSerializer, data.serviceName)
            encoder.encodeSerializableElement(desc, endpointIdx, nullStringSerializer, data.endpoint)
            encoder.encodeSerializableElement(desc, operationIdx, nullStringSerializer, data.operation)
            encoder.encodeSerializableElement(desc, urlIdx, nullStringSerializer, data.url)
            encoder.encodeSerializableElement(desc, methodIdx, nullStringSerializer, data.method)
        }

        @Serializable
        @XmlSerialName(ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
        private class XmlMessageData : ContainerData<XmlMessage> {
            constructor(owner: XmlMessage.Companion) : super()

            var serviceName: String? = null
            var serviceNS: String? = null
            var endpoint: String? = null
            var operation: String? = null
            var url: String? = null
            var method: String? = null
            @SerialName("type")
            var contentType: String? = null

            val service: DescQName? get() = serviceName?.let { DescQName(serviceNS ?: "", it) }

            override fun handleAttribute(attributeLocalName: String, attributeValue: String) {
                return when (attributeLocalName) {
                    "serviceName" -> serviceName = attributeValue
                    "serviceNS"   -> serviceNS = attributeValue
                    "endpoint"    -> endpoint = attributeValue
                    "operation"   -> operation = attributeValue
                    "url"         -> url = attributeValue
                    "method"      -> method = attributeValue
                    "type"        -> contentType = attributeValue

                    else          -> super.handleAttribute(attributeLocalName, attributeValue)
                }
            }

            override fun readAdditionalChild(desc: SerialDescriptor, decoder: CompositeDecoder, index: Int) {
                val name = desc.getElementName(index)
                val value = decoder.readNullableString(desc, index)
                if (value != null) {
                    handleAttribute(name, value)
                }
            }
        }
    }

}

