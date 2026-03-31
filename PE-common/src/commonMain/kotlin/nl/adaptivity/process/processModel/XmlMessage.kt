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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.messaging.*
import nl.adaptivity.process.messaging.RESTMethod
import nl.adaptivity.process.messaging.SOAPMethod
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
 * ```xml
 * <complexType name="Message">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <any processContents='lax'/>
 *       </sequence>
 *       <attribute name="serviceNS" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="endpoint" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="operation" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="serviceName" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       <attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * ```
 */
@Serializable(XmlMessage.Companion::class)
@XmlSerialName(XmlMessage.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
class XmlMessage private constructor(
    override val targetMethod: InvokableMethod,
    messageBody: ICompactFragment,
) : IXmlMessage {

    override val messageBody = CompactFragment(messageBody)

    constructor(
        service: SOAPMethod,
        messageBody: ICompactFragment? = null
    ) : this(targetMethod = service, messageBody= messageBody ?: CompactFragment(""))

    constructor(
        service: RESTMethod,
        messageBody: ICompactFragment? = null
    ) : this(targetMethod = service, messageBody = messageBody ?: CompactFragment(""))

    constructor(baseMessage: IXmlMessage, newMessageBody: ICompactFragment) : this(baseMessage.targetMethod, newMessageBody)

    fun serialize(out: XmlWriter) {
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

        return targetMethod == other.targetMethod
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + targetMethod.hashCode()
        return result
    }

    internal object DummyMethod : InvokableMethod {
        override val endpoint: EndpointDescriptor
            get() = object : EndpointDescriptor {
                override val serviceName: Nothing? get() = null
                override val endpointName: Nothing? get() = null
                override val endpointLocation: Nothing? get() = null

                override fun isSameService(other: EndpointDescriptor?): Boolean = other == DummyMethod
            }
        override val contentType: String? get() = null
        override val url: String? get() = null
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
        val targetMethod: InvokableMethod get() = when {
            serviceNS == null -> throw SerializationException("Null service")
            serviceName == null -> throw SerializationException("Null service namespace")

            endpoint != null -> when {
                operation == null -> throw SerializationException("Soap method operation is null")
                else -> SOAPMethodDesc(DescQName(serviceNS, serviceName), endpoint, operation, url)
            }

            url == null -> throw SerializationException("Null URL for rest method")
            method == null -> throw SerializationException("Method missing")
            contentType == null -> throw SerializationException("ContentType missing")

            else -> RESTMethodDesc(
                serviceName = DescQName(serviceNS, serviceName),
                method = method,
                url = url,
                contentType = contentType,
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object : KSerializer<XmlMessage> {
        private val delegateSerializer = SerialDelegate.serializer()
        override val descriptor: SerialDescriptor = SerialDescriptor("XmlMessage", delegateSerializer.descriptor)

        const val ELEMENTLOCALNAME = "message"

        val ELEMENTNAME = XmlQName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

        val EMPTY: XmlMessage = XmlMessage(DummyMethod, CompactFragment(""))


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
            return when (val method = data.targetMethod) {
                is SOAPMethod -> XmlMessage(method, data.content)
                is RESTMethod -> XmlMessage(method as RESTMethod, data.content)
                else -> error("Unsupported method type")
            }
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
                content = value.messageBody,
            )
            delegateSerializer.serialize(encoder, delegate)
        }

    }

}

