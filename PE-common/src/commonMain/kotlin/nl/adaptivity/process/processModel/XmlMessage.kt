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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.messaging.*
import nl.adaptivity.process.messaging.RESTMethod
import nl.adaptivity.process.messaging.SOAPMethod
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
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

    override val targetMethod: InvokableMethod

    @OptIn(XmlUtilInternal::class)
    override val messageBody: CompactFragment
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
    ) : super(messageBody ?: CompactFragment("")) {
        if (service!=null && endpoint != null && operation != null) {
            targetMethod = SOAPMethodDesc(service, endpoint, operation, url)
        } else {
            requireNotNull(method) { "A service must have either soap info or rest info. Method missing."  }
            requireNotNull(url) { "A service must have either soap info or rest info. url missing."  }
            requireNotNull(contentType) { "For a Rest service the content type cannot be null" }
            val descriptor = EndpointDescriptorImpl(service, endpoint, url.toUri())
            targetMethod = RESTMethodDesc(descriptor, method, contentType)
        }
    }

    constructor(
        service: SOAPMethod,
        messageBody: ICompactFragment? = null
    ) : super(messageBody ?: CompactFragment("")) {
        this.targetMethod = service
    }

    constructor(
        service: RESTMethod,
        messageBody: ICompactFragment? = null
    ) : super(messageBody ?: CompactFragment("")) {
        this.targetMethod = service
    }

    constructor(baseMessage: IXmlMessage, newMessageBody: ICompactFragment) : super(newMessageBody) {
        targetMethod = baseMessage.targetMethod
    }

    fun serializeAttributes(out: XmlWriter) {
        // No attributes by default
        out.writeAttribute("type", targetMethod.contentType)
        targetMethod.endpoint.run {
            serviceName?.run {
                out.writeAttribute("serviceNS", namespaceURI)
                out.writeAttribute("serviceName", getLocalPart())
            }
            endpointName?.let { out.writeAttribute("endpoint", it) }
        }
        out.writeAttribute("operation", operation)
        out.writeAttribute("url", targetMethod.url)
        out.writeAttribute("method", (targetMethod as? RESTMethod)?.method)
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
        if (targetMethod != other.targetMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (operation?.hashCode() ?: 0)
        result = 31 * result + namespaces.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + targetMethod.hashCode()
        return result
    }

    @Serializable
    @XmlSerialName(ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
    private class SerialDelegate(
        @SerialName("serviceName") val serviceName: String? = null,
        @SerialName("serviceNS") val serviceNS: String? = null,
        @SerialName("endpoint") val endpoint: String? = null,
        @SerialName("operation") val operation: String? = null,
        @SerialName("url") val url: String? = null,
        @SerialName("method") val method: String? = null,
        @SerialName("type") val contentType: String? = null,
        @XmlValue val content: CompactFragment? = null
    ) {
        val service: DescQName? get() = serviceName?.let { DescQName(serviceNS ?: "", it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object : XmlContainerSerializer<XmlMessage>() {
        private val delegateSerializer = SerialDelegate.serializer()
        override val descriptor: SerialDescriptor = SerialDescriptor("XmlMessage", delegateSerializer.descriptor)

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


        @Suppress("DEPRECATION")
        override fun deserialize(decoder: Decoder): XmlMessage {
            val data = delegateSerializer.deserialize(decoder)
            return XmlMessage(data.service, data.endpoint, data.operation, data.url, data.method, data.contentType, data.content)
        }


        override fun serialize(encoder: Encoder, value: XmlMessage) {
            val endpoint = value.targetMethod.endpoint
            val serviceName = (value.targetMethod as? SOAPMethod)?.endpoint?.serviceName
            val delegate = SerialDelegate(
                serviceName = serviceName?.getLocalPart(),
                serviceNS = serviceName?.getNamespaceURI(),
                endpoint = endpoint.endpointName,
                operation = (value.targetMethod as? SOAPMethod)?.operation,
                url = endpoint.endpointLocation?.toString(),
                method = (value.targetMethod as? RESTMethod)?.method,
                contentType = value.contentType,
                content = CompactFragment(value.namespaces, value.content),
            )
            delegateSerializer.serialize(encoder, delegate)
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
            @SerialName("type") var contentType: String? = null

            val service: DescQName? get() = serviceName?.let { DescQName(serviceNS ?: "", it) }

        }
    }

}

