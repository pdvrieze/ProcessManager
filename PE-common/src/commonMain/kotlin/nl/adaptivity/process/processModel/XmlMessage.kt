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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.messaging.*
import nl.adaptivity.serialutil.readNullableString
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import kotlin.jvm.JvmName
import nl.adaptivity.xmlutil.QName as DescQName
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

    override var namespaces: IterableNamespaceContext
        get() = super.namespaces
        set(value) {
            super.namespaces = value
        }

    override var content: CharArray
        get() = super.content
        set(value) {
            super.content = value
        }

    override val targetService: InvokableMethod

    @OptIn(XmlUtilInternal::class)
    override val messageBody: ICompactFragment
        get() = CompactFragment(namespaces, content)

    constructor() : this(service = null) { /* default constructor */
    }


    @Deprecated("Use one of the functions explicitly taking a service kind")
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
        if (service!=null && endpoint != null && operation != null) {
            targetService = SOAPMethodDesc(service, endpoint, operation, url)
        } else {
            requireNotNull(method) { "A service must have either soap info or rest info. Method missing."  }
            requireNotNull(url) { "A service must have either soap info or rest info. url missing."  }
            requireNotNull(contentType) { "For a Rest service the content type cannot be null" }
            val descriptor = EndpointDescriptorImpl(service, endpoint, url.toUri())
            targetService = RESTMethodDesc(descriptor, method, contentType)
        }
        messageBody?.let {
            namespaces = SimpleNamespaceContext(it.namespaces)
            content = it.content
        }
    }

    constructor(
        service: SOAPMethod,
        messageBody: ICompactFragment? = null
    ) {
        this.targetService = service
        messageBody ?: CompactFragment("").let {
            this.content = it.content
            this.namespaces = it.namespaces
        }
    }

    constructor(
        service: RESTMethod,
        messageBody: ICompactFragment? = null
    ) {
        this.targetService = service
        messageBody ?: CompactFragment("").let {
            this.content = it.content
            this.namespaces = it.namespaces
        }
    }

    constructor(baseMessage: IXmlMessage, newMessageBody: ICompactFragment) {
        targetService = baseMessage.targetService
        content = newMessageBody.content
        namespaces = newMessageBody.namespaces
    }

    override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        out.writeAttribute("type", targetService.contentType)
        targetService.endpoint.run {
            serviceName?.run {
                out.writeAttribute("serviceNS", namespaceURI)
                out.writeAttribute("serviceName", getLocalPart())
            }
            endpointName?.let { out.writeAttribute("endpoint", it) }
        }
        out.writeAttribute("operation", operation)
        out.writeAttribute("url", targetService.url)
        out.writeAttribute("method", (targetService as? RESTMethod)?.method)
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

    override fun toString(): String {
        return XML.encodeToString(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XmlMessage

        if (operation != other.operation) return false
        if (namespaces != other.namespaces) return false
        if (!content.contentEquals(other.content)) return false
        if (targetService != other.targetService) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (operation?.hashCode() ?: 0)
        result = 31 * result + namespaces.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + targetService.hashCode()
        return result
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object : XmlContainerSerializer<XmlMessage>() {
        override val descriptor: SerialDescriptor = SerialDescriptor("XmlMessage", serializer<XmlMessageData>().descriptor) // TODO use concrete serializer instance

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
            return message as? XmlMessage ?: XmlMessage(message, message.messageBody)
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
            val targetService = data.targetService

            encoder.encodeSerializableElement(desc, typeIdx, nullStringSerializer, data.contentType)
            if (targetService !is RESTMethod) {
                encoder.encodeSerializableElement(desc, serviceNSIdx, nullStringSerializer,
                    data.targetService.endpoint.serviceName?.getNamespaceURI()
                )
                encoder.encodeSerializableElement(desc, serviceNameIdx, nullStringSerializer,
                    data.targetService.endpoint.serviceName?.getLocalPart()
                )
                encoder.encodeSerializableElement(desc, endpointIdx, nullStringSerializer,
                    data.targetService.endpoint.endpointName
                )
            }
            if (targetService is SOAPMethod) {
                encoder.encodeSerializableElement(desc, operationIdx, nullStringSerializer, targetService.operation)
            }
            encoder.encodeSerializableElement(desc, urlIdx, nullStringSerializer, targetService.endpoint.endpointLocation?.toString())
            if (targetService is RESTMethod) {
                encoder.encodeSerializableElement(desc, methodIdx, nullStringSerializer, targetService.method)
            }
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

