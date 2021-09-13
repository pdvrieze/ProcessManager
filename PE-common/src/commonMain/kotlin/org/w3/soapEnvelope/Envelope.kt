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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 *
 *
 * Java class for Envelope complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="Envelope">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <element ref="{http://www.w3.org/2003/05/soap-envelope}Header" minOccurs="0"/>
 * <element ref="{http://www.w3.org/2003/05/soap-envelope}Body"/>
 * </sequence>
 * <anyAttribute processContents='lax' namespace='##other'/>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 *
 * @property otherAttributes A map that contains attributes that aren't bound to any typed property
 *                   on this class.
 *                   the map is keyed by the name of the attribute and the value is the string value
 *                   of the attribute. the map returned by this method is live, and you can add new
 *                   attribute by updating the map directly. Because of this design, there's no
 *                   setter.
 */
@Serializable(Envelope.Serializer::class)
class Envelope<T : Any>(
    val body: Body<T>,
    val header: Header = Header(),
    val otherAttributes: Map<QName, String> = emptyMap(),
) {

    val elementName: QName
        get() = ELEMENTNAME

    constructor(content: T) : this(Body<T>(content))

    public class Serializer<T : Any>(private val bodyContentSerializer: KSerializer<T>) : KSerializer<Envelope<T>> {
        private val bodySerializer: KSerializer<Body<T>> = Body.Serializer(bodyContentSerializer)

        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(Envelope::class.qualifiedName!!, bodyContentSerializer.descriptor) {
                annotations = SoapSerialObjects.envelopeAnnotations
                element("otherAttributes", SoapSerialObjects.attrsSerializer.descriptor, isOptional = true)
                element<Header>("header", isOptional = true)
                element("body", bodySerializer.descriptor)
            }

        override fun deserialize(decoder: Decoder): Envelope<T> {
            var header = Header()
            lateinit var body: Body<T>
            var otherAttributes: Map<QName, String> = mutableMapOf()
            var encodingStyle: URI? = null
            if (decoder is XML.XmlInput) {
                val reader: XmlReader = decoder.input
                otherAttributes = reader.attributes.filter {
                    when {
                        it.prefix == XMLConstants.XMLNS_ATTRIBUTE ||
                            (it.prefix == "" && it.localName == XMLConstants.XMLNS_ATTRIBUTE) -> false
                        it.namespaceUri != Envelope.NAMESPACE                                 -> true
                        it.localName == "encodingStyle"                                       -> {
                            encodingStyle = URI(it.value); false
                        }
                        else                                                                  -> true
                    }
                }.associate { QName(it.namespaceUri, it.localName, it.prefix) to it.value }

                reader.require(EventType.START_ELEMENT, NAMESPACE, ELEMENTLOCALNAME)
                reader.next()
                do {
                    when {
                        reader.eventType == EventType.START_ELEMENT -> {
                            when {
                                reader.namespaceURI != NAMESPACE            -> reader.elementContentToFragment() // ignore but process
                                reader.localName == Header.ELEMENTLOCALNAME ->
                                    header = decoder.decodeSerializableValue(Header.serializer())

                                reader.localName == Body.ELEMENTLOCALNAME   ->
                                    body = decoder.decodeSerializableValue(bodySerializer)
                                else                                        -> reader.elementContentToFragment() // Ignore again
                            }
                        }
                        reader.eventType.isIgnorable -> reader.next()
                        else                    -> throw SerializationException("Found unexpected event in xml stream")
                    }

                } while (reader.eventType != EventType.END_ELEMENT)

                encodingStyle?.let { s ->
                    if (body.encodingStyle == null) {
                        body = body.copy(encodingStyle = s)
                    }
                }
            } else {
                decoder.decodeStructure(descriptor) {
                    decodeElements(this) { idx ->
                        when (idx) {
                            0 -> otherAttributes =
                                decodeSerializableElement(descriptor, idx, SoapSerialObjects.attrsSerializer)
                            1 -> header = decodeSerializableElement(descriptor, idx, Header.serializer())
                            2 -> body = decodeSerializableElement(descriptor, idx, bodySerializer)
                        }
                    }
                }
            }
            return Envelope(body, header, otherAttributes)
        }

        override fun serialize(encoder: Encoder, value: Envelope<T>) {
            if (encoder is XML.XmlOutput) {
                val writer: XmlWriter = encoder.target
                writer.smartStartTag(ELEMENTNAME) {
                    for ((aName, aValue) in value.otherAttributes) {
                        val prefixForNs = when (aName.namespaceURI) {
                            ""   -> ""
                            else -> writer.getPrefix(aName.namespaceURI)
                        } ?: run {
                            writer.namespaceAttr(aName.prefix, aName.namespaceURI)
                            aName.prefix
                        }
                        writer.attribute(aName.namespaceURI, aName.localPart, prefixForNs, aValue)
                    }
                    encoder.delegateFormat().encodeToWriter(writer, bodySerializer, value.body)
                }
            } else {
                encoder.encodeStructure(descriptor) {
                    encodeSerializableElement(
                        descriptor,
                        0,
                        SoapSerialObjects.attrsSerializer,
                        value.otherAttributes
                    )
                    if (value.header.blocks.isNotEmpty() || value.header.otherAttributes.isNotEmpty()) {
                        encodeSerializableElement(descriptor, 1, Header.serializer(), value.header)
                    }
                    encodeSerializableElement(descriptor, 2, bodySerializer, value.body)
                }
            }
        }
    }

    companion object {

        const val NAMESPACE = "http://www.w3.org/2003/05/soap-envelope"

        const val ELEMENTLOCALNAME = "Envelope"

        const val PREFIX = "soap"

        val ELEMENTNAME = QName(NAMESPACE, ELEMENTLOCALNAME, PREFIX)

        const val MIMETYPE = "application/soap+xml"

        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader): Envelope<out ICompactFragment> {
            return XML { indent=2; autoPolymorphic = true }.decodeFromReader(serializer(CompactFragmentSerializer), reader)
        }

    }

}

internal object SoapSerialObjects {
    @Serializable
    private class AnnotationHelper(
        @XmlSerialName(Envelope.ELEMENTLOCALNAME, Envelope.NAMESPACE, Envelope.PREFIX)
        val envelope: String,
        @XmlSerialName(Header.ELEMENTLOCALNAME, Envelope.NAMESPACE, Envelope.PREFIX)
        val header: String,
        @XmlSerialName(Body.ELEMENTLOCALNAME, Envelope.NAMESPACE, Envelope.PREFIX)
        val body: String,
        @XmlSerialName("encodingStyle", Envelope.NAMESPACE, Envelope.PREFIX)
        val encodingStyle: String,
        @XmlValue(true)
        val value: String,
    )

    val envelopeAnnotations = AnnotationHelper.serializer().descriptor.run {
        getElementAnnotations(getElementIndex("envelope"))
    }

    val headerAnnotations = AnnotationHelper.serializer().descriptor.run {
        getElementAnnotations(getElementIndex("header"))
    }

    val bodyAnnotations = AnnotationHelper.serializer().descriptor.run {
        getElementAnnotations(getElementIndex("body"))
    }

    val encodingStyleAnnotations = AnnotationHelper.serializer().descriptor.run {
        getElementAnnotations(getElementIndex("encodingStyle"))
    }

    val valueAnnotations = AnnotationHelper.serializer().descriptor.run {
        getElementAnnotations(getElementIndex("value"))
    }

    object attrsSerializer : KSerializer<Map<QName, String>> {
        private val default = MapSerializer(QNameSerializer, String.serializer())

        override val descriptor: SerialDescriptor = SerialDescriptor("attrsSerializer", default.descriptor)

        override fun deserialize(decoder: Decoder): Map<QName, String> {
            return if (decoder is XML.XmlInput) {
                decoder.input.attributes
                    .asSequence()
                    .filter { !(it.prefix == "xmlns" || (it.prefix == "" && it.localName == "xmlns")) }
                    .associate { QName(it.namespaceUri, it.localName, it.prefix) to it.value }
                    .toMap()
            } else {
                default.deserialize(decoder)
            }
        }

        override fun serialize(encoder: Encoder, value: Map<QName, String>) {
            if (encoder is XML.XmlOutput) {
                val out = encoder.target
                for ((name, aValue) in value) {
                    out.attribute(name.namespaceURI, name.localPart, name.prefix, aValue)
                }
            } else {
                default.serialize(encoder, value)
            }
        }
    }

}
