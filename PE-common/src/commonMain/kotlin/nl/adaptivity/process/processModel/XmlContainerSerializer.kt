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
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.serialutil.CharArrayAsStringSerializer
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)

@OptIn(XmlUtilInternal::class)
abstract class XmlContainerSerializer<T : XMLContainer>: KSerializer<T> {

    @OptIn(ExperimentalSerializationApi::class)
    fun serialize(desc: SerialDescriptor, encoder: Encoder, data: T) {
        encoder.encodeStructure(desc) {
            val childOut = this
            if (childOut is XML.XmlOutput) {
                val writer: XmlWriter = childOut.target
                val origIndentString = writer.indentString
                writer.indentString="" // We want to retain the structure, so don't reindent.

                for ((prefix, nsUri) in data.namespaces) {
                    if (writer.getNamespaceUri(prefix) != nsUri) {
                        writer.namespaceAttr(prefix, nsUri)
                    }
                }
                writeAdditionalValues(childOut, desc, data)

                if (! data.isEmpty) {
                    writer.serialize(data.getXmlReader())

                    // This ensures that indentation is not applied for the end tag
                    // (indentation should only happen if no content was written)
                    writer.ignorableWhitespace("")
                }

                writer.indentString = origIndentString
            } else {
                childOut.encodeSerializableElement(
                    desc, desc.getElementIndex("namespaces"), ListSerializer(Namespace),
                    data.namespaces.toList()
                )
                childOut.encodeStringElement(desc, desc.getElementIndex("content"), data.contentString)
                writeAdditionalValues(childOut, desc, data)
            }
        }
    }

    open fun writeAdditionalValues(out: CompositeEncoder, desc: SerialDescriptor, data: T) {}


    internal open fun getFilter(gatheringNamespaceContext: MyGatheringNamespaceContext): NamespaceGatherer {
        return NamespaceGatherer(gatheringNamespaceContext)
    }


    @Serializable
    protected open class ContainerData<T : XMLContainer> {

        var content: CharArray? = null
        var namespaces: Iterable<Namespace> = emptyList()

        val fragment: ICompactFragment?
            get() = content?.let {
                CompactFragment(namespaces, it)
            }

        open fun handleAttribute(attributeLocalName: String, attributeValue: String) {
            throw SerializationException("Unknown attribute: $attributeLocalName")
        }

        @OptIn(ExperimentalSerializationApi::class)
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

                    val gatheringNamespaceContext =
                        MyGatheringNamespaceContext(namespacesMap, reader.namespaceContext.freeze())

                    handleLastRootAttributeReadEvent(reader, gatheringNamespaceContext)

                    reader.next()
                    // We have finished the start element, now only read the content
                    // If we don't skip here we will read the element itself
                    val gatheringReader = FilteringReader(reader, owner.getFilter(gatheringNamespaceContext))

                    val frag = gatheringReader.siblingsToFragment()
                    content = frag.content

                    namespaces = SimpleNamespaceContext(namespacesMap)

                } else {
                    // TODO look at using the description to resolve the indices
                    loop@ while (true) {
                        when (val next = input.decodeElementIndex(desc)) {
                            CompositeDecoder.DECODE_DONE -> break@loop
                            else             -> when (desc.getElementName(next)) {
                                "namespaces" -> namespaces = input.decodeSerializableElement(desc, next, ListSerializer(Namespace))
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
            throw UnsupportedOperationException("No support to reading additional child at index: $index")
        }


        internal open fun handleLastRootAttributeReadEvent(
            reader: XmlReader,
            gatheringNamespaceContext: MyGatheringNamespaceContext
        ) {
        }

    }

    internal class FilteringReader(
        val delegate: XmlReader,
        val filter: NamespaceGatherer
    ) : XmlReader by delegate {

        private val localPrefixes = mutableListOf<List<String>>(emptyList())

        private var textContent: StringBuilder? = null

        init {
            delegate.eventType.handle()
        }

        private fun EventType.handle() {
            when (this) {
                EventType.START_ELEMENT -> {
                    localPrefixes.add(delegate.namespaceDecls.map { it.prefix })
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
                else -> { /* ignore */ }
            }

        }

        override fun next(): EventType {
            return delegate.next().apply { handle() }
        }
    }

    internal open class NamespaceGatherer(val gatheringNamespaceContext: MyGatheringNamespaceContext) {

        open fun visitNamesInElement(source: XmlReader, localPrefixes: List<List<String>>) {
            assert(source.eventType === EventType.START_ELEMENT)

            val sourcePrefix = source.prefix
            val isLocal = localPrefixes.any { sourcePrefix in it }
            if (!isLocal) {
                gatheringNamespaceContext.getNamespaceURI(sourcePrefix)
            }

            if (source.namespaceURI == Constants.MY_JBI_NS_STR && source.localName=="value") {
                val xpath = source.getAttributeValue(null, "xpath")
                if (xpath!=null) {
                    val namesInPath = mutableMapOf<String, String>()
                    val newContext = MyGatheringNamespaceContext(namesInPath, source.namespaceContext.freeze())
                    visitXpathUsedPrefixes(xpath, newContext)
                    for (prefix in namesInPath.keys) {
                        if (localPrefixes.none { prefix in it }) {
                            gatheringNamespaceContext.getNamespaceURI(prefix)
                        }
                    }
                }
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
