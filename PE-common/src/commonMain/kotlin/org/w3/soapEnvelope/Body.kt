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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.09.24 at 08:12:58 PM CEST
//


package org.w3.soapEnvelope

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.util.multiplatform.createUri
import nl.adaptivity.util.multiplatform.toUri
import nl.adaptivity.util.net.devrieze.serializers.URISerializer
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment


/**
 *
 *
 * Java class for Body complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="Body">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 * </sequence>
 * <anyAttribute processContents='lax' namespace='##other'/>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 *
 */
class Body<T: Any>(
    @XmlValue(true)
    val child: T,
    val encodingStyle: URI = createUri("http://www.w3.org/2003/05/soap-encoding"),
    val otherAttributes: Map<QName, String> = emptyMap(),
) {
    fun copy(
        encodingStyle: URI = this.encodingStyle,
        otherAttributes: Map<QName, String> = this.otherAttributes,
    ): Body<T> = Body(child, encodingStyle, otherAttributes)

    fun <U: Any> copy(
        child: U,
        encodingStyle: URI = this.encodingStyle,
        otherAttributes: Map<QName, String> = this.otherAttributes,
    ): Body<U> = Body(child, encodingStyle, otherAttributes)

    class Serializer<T: Any>(private val contentSerializer: KSerializer<T>): KSerializer<Body<T>> {

        @OptIn(ExperimentalSerializationApi::class, XmlUtilInternal::class)
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(Body::class.name) {
            annotations = SoapSerialObjects.bodyAnnotations
            element("encodingStyle", URISerializer.descriptor, SoapSerialObjects.encodingStyleAnnotations, true)
            element("otherAttributes", SoapSerialObjects.attrsSerializer.descriptor, isOptional = true)
            element("child", contentSerializer.descriptor)
        }

        override fun deserialize(decoder: Decoder): Body<T> {
            var encodingStyle: URI? = null
            var otherAttributes: Map<QName, String> = emptyMap()
            lateinit var child: T
            decoder.decodeStructure(descriptor) {
                if (decoder is XML.XmlInput) {
                    val reader: XmlReader = decoder.input
                    otherAttributes = reader.attributes.filter {
                        when {
                            it.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                                (it.prefix=="" && it.localName == XMLConstants.XMLNS_ATTRIBUTE) -> false
                            it.namespaceUri!= Envelope.NAMESPACE -> true
                            it.localName == "encodingStyle" -> { encodingStyle = it.value.toUri(); false }
                            else -> true
                        }
                    }.associate { QName(it.namespaceUri, it.localName, it.prefix) to it.value }

                    child = decodeSerializableElement(descriptor, 2, contentSerializer, null)
                    if (reader.nextTag()!=EventType.END_ELEMENT) throw SerializationException("Extra content in body")
                } else {
                    decodeElements(this) { idx ->
                        when (idx) {
                            0 -> encodingStyle = decodeSerializableElement(descriptor, idx, URISerializer, encodingStyle)
                            1 -> otherAttributes = decodeSerializableElement(
                                descriptor, idx,
                                SoapSerialObjects.attrsSerializer, otherAttributes)
                            2 -> child = decodeSerializableElement(descriptor, idx, contentSerializer)
                        }
                    }
                }
            }
            return Body(child)
        }

        override fun serialize(encoder: Encoder, value: Body<T>) {
            if (encoder is XML.XmlOutput) {
                val out = encoder.target
                out.smartStartTag(ELEMENTNAME) {
                    value.encodingStyle?.also { style ->
                        out.attribute(Envelope.NAMESPACE, "encodingStyle", Envelope.PREFIX, style.toString())
                    }
                    for ((aName, aValue) in value.otherAttributes) {
                        out.writeAttribute(aName,  aValue)
                    }
                    val child = value.child
                    when (child) {
                        is CompactFragment -> {
                            for (ns in child.namespaces) {
                                if (out.getNamespaceUri(ns.prefix) != ns.namespaceURI) {
                                    out.namespaceAttr(ns)
                                }
                            }
                            child.serialize(out)
                        }
                        else -> encoder.delegateFormat().encodeToWriter(out, contentSerializer, child)
                    }

                }
            } else {
                encoder.encodeStructure(descriptor) {
                    value.encodingStyle?.also { style ->
                        encodeSerializableElement(descriptor, 0, URISerializer, style)
                    }
                    if (value.otherAttributes.isNotEmpty() || shouldEncodeElementDefault(descriptor, 1)) {
                        encodeSerializableElement(descriptor, 1, SoapSerialObjects.attrsSerializer, value.otherAttributes)
                    }
                    encodeSerializableElement(descriptor, 2, contentSerializer, value.child)
                }
            }
        }

    }

    companion object {

        const val ELEMENTLOCALNAME = "Body"
        val ELEMENTNAME = QName(Envelope.NAMESPACE, ELEMENTLOCALNAME, Envelope.PREFIX)

    }

}
