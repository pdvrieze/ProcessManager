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
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.*
import nl.adaptivity.serialutil.*
import nl.adaptivity.serialutil.CharArrayAsStringSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.serialutil.decodeStructure
import nl.adaptivity.serialutil.writeStructure

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)

open class XmlContainerSerializer<T : XMLContainer> {

    fun serialize(desc: SerialDescriptor, encoder: Encoder, data: T) {
        encoder.writeStructure(desc) {
            val childOut = this
            if (childOut is XML.XmlOutput) {
                val writer = childOut.target
                for ((prefix, nsUri) in data.namespaces) {
                    if (writer.getNamespaceUri(prefix) != nsUri) {
                        writer.namespaceAttr(prefix, nsUri)
                    }
                }
                writeAdditionalValues(childOut, desc, data)
                writer.serialize(data.getXmlReader())
            } else {
                childOut.encodeSerializableElement(
                    desc, desc.getElementIndex("namespaces"), Namespace.list,
                    data.namespaces.toList()
                                                  )
                childOut.encodeStringElement(desc, desc.getElementIndex("content"), data.contentString)
                writeAdditionalValues(childOut, desc, data)
            }
        }
    }

    open fun writeAdditionalValues(out: KOutput, desc: KSerialClassDesc, data: T) {}


    open fun getFilter(gatheringNamespaceContext: GatheringNamespaceContext): NamespaceGatherer {
        return NamespaceGatherer(gatheringNamespaceContext)
    }


    open class ContainerData<T : XMLContainer> {

        var content: CharArray? = null
        var namespaces: Iterable<Namespace> = emptyList()

        @Transient
        val fragment: ICompactFragment?
            get() = content?.let {
                CompactFragment(namespaces ?: emptyList(), it)
            }

        open fun handleAttribute(attributeLocalName: String, attributeValue: String) {
            throw SerializationException("Unknown attribute: $attributeLocalName")
        }

        fun deserialize(desc: SerialDescriptor, decoder: Decoder, owner: XmlContainerSerializer<in T>) {
            @Suppress("NAME_SHADOWING")
            decoder.decodeStructure(desc) {
                val input = this
                if (input is XML.XmlInput) {
                    val reader = input.input
                    for (i in 0 until reader.attributeCount) {
                        handleAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i))
                    }

                    val namespacesMap = mutableMapOf<String, String>()

                    val gatheringNamespaceContext = GatheringNamespaceContext(reader.namespaceContext, namespacesMap)

                    handleLastRootAttributeReadEvent(reader, gatheringNamespaceContext)

                    reader.next()
                    // We have finished the start element, now only read the content
                    // If we don't skip here we will read the element itself
                    val gatheringReader = FilteringReader(reader, owner.getFilter(gatheringNamespaceContext))

                    val frag = gatheringReader.siblingsToFragment()
                    content = frag.content

                    for ((prefix, nsUri) in frag.namespaces) {
                        namespacesMap[prefix] = nsUri
                    }

                    namespaces = SimpleNamespaceContext(namespacesMap)

                } else {
                    // TODO look at using the description to resolve the indices
                    loop@ while (true) {
                        val next = input.decodeElementIndex(desc)
                        when (next) {
                            KInput.READ_DONE -> break@loop
                            KInput.READ_ALL  -> TODO("Not yet supported")
                            else             -> when (desc.getElementName(next)) {
                                "namespaces" -> namespaces = input.decodeSerializableElement(desc, next, Namespace.list)
                                "content"    -> content = input.decodeSerializableElement(
                                    desc, 0,
                                    CharArrayAsStringSerializer
                                                                                         )
                                else         -> readAdditionalChild(desc, input, next)
                            }
                        }

                    }
                }
            }

        }

        open fun readAdditionalChild(desc: SerialDescriptor, decoder: CompositeDecoder, index: Int) {
            throw UnknownFieldException(index)
        }


        open fun handleLastRootAttributeReadEvent(
            reader: XmlReader,
            gatheringNamespaceContext: GatheringNamespaceContext
                                                 ) {
        }

    }

    protected class FilteringReader(
        val delegate: XmlReader,
        val filter: NamespaceGatherer
                                   ) : XmlReader by delegate {

        private val localPrefixes = mutableListOf<List<String>>(emptyList())

        init {
            delegate.eventType.handle()
        }

        private fun EventType.handle() {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (this) {
                EventType.START_ELEMENT -> {
                    localPrefixes.add((delegate.namespaceStart until delegate.namespaceEnd).map {
                        delegate.getNamespacePrefix(it)
                    })
                    textContent = StringBuilder()
                    filter.visitNamesInElement(delegate, localPrefixes)
                }
                EventType.TEXT,
                EventType.IGNORABLE_WHITESPACE,
                EventType.CDSECT        -> {
                    textContent?.append(delegate.text)
                }
                EventType.END_ELEMENT   -> {
                    textContent?.let { filter.visitNamesInTextContent(delegate.name, it) }

                    textContent = null
                    localPrefixes.removeAt(localPrefixes.lastIndex)
                }
            }

        }

        private var textContent: StringBuilder? = null
        override fun next(): EventType {
            return delegate.next().apply { handle() }
        }
    }

    open class NamespaceGatherer(val gatheringNamespaceContext: GatheringNamespaceContext) {

        open fun visitNamesInElement(source: XmlReader, localPrefixes: List<List<String>>) {
            assert(source.eventType === EventType.START_ELEMENT)

            val prefix = source.prefix
            val isLocal = localPrefixes.any { prefix in it }
            if (!isLocal) {
                gatheringNamespaceContext.getNamespaceURI(prefix)
            }

            for (i in source.attributeCount - 1 downTo 0) {
                val attrName = source.getAttributeName(i)
                visitNamesInAttributeValue(
                    source.namespaceContext, source.name, attrName,
                    source.getAttributeValue(i),
                    localPrefixes
                                          )
            }
        }

        open fun visitNamesInAttributeValue(
            referenceContext: NamespaceContext,
            owner: QName,
            attributeName: QName,
            attributeValue: CharSequence,
            localPrefixes: List<List<String>>
                                           ) {
            // By default there are no special attributes
        }

        @Suppress("UnusedReturnValue")
        open fun visitNamesInTextContent(parent: QName?, textContent: CharSequence): List<QName> {
            return emptyList()
        }

    }

}
