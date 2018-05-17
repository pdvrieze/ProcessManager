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
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.CombiningNamespaceContext
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.CharArrayAsStringSerializer
import nl.adaptivity.xml.serialization.XML
import nl.adaptivity.xml.serialization.readNullableString
import nl.adaptivity.xml.serialization.writeNullableStringElementValue

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)
open class XPathHolderSerializer<T : XPathHolder> {
    open class PathHolderData<T: XPathHolder>(
        val owner: XPathHolderSerializer<T>,
        var name: String? = null,
        var path: String? = null,
        var content: CharArray? = null,
        var namespaces: Iterable<Namespace>? = null) {

        open fun load(desc: KSerialClassDesc,
                      input: KInput) {
            @Suppress("NAME_SHADOWING")
            val input = input.readBegin(desc)

            if (input is XML.XmlInput) {
                val reader = input.input
                for (i in 0 until reader.attributeCount) {
                    handleAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i))
                }
                val namespacesMap = mutableMapOf<String, String>()
                val referenceContext = SimpleNamespaceContext(reader.namespaceDecls.toList())
                val gatheringNamespaceContext = GatheringNamespaceContext(referenceContext, namespacesMap)

                if (!path.isNullOrEmpty()) {
                    visitXpathUsedPrefixes(path, gatheringNamespaceContext)
                }
                val namespaceEnd = reader.namespaceEnd
                reader.next()
                // We have finished the start element, now only read the content
                // If we don't skip here we will read the element itself
                val gatheringReader = FilteringReader(reader, owner.getFilter(gatheringNamespaceContext), namespaceEnd)

                val frag = gatheringReader.siblingsToFragment()
                content = frag.content

                for ((prefix, nsUri) in frag.namespaces) {
                    namespacesMap[prefix] = nsUri
                }

                namespaces = SimpleNamespaceContext(namespacesMap)

            } else {
                // TODO look at using the description to resolve the indices
                loop@ while (true) {
                    val next = input.readElement(desc)
                    when (next) {
                        KInput.READ_DONE -> break@loop
                        KInput.READ_ALL  -> TODO("Not yet supported")
                        0                -> name = input.readNullableString()
                        1                -> path = input.readNullableString()
                        2                -> namespaces =
                            input.readSerializableElementValue(desc, 2,
                                                               input.context.klassSerializer(Namespace::class).list)

                        3                -> content =
                            input.readSerializableElementValue(desc, 0, CharArrayAsStringSerializer)
                    }

                }
                input.readEnd(desc)
            }
        }

        open fun handleAttribute(attributeLocalName: String, attributeValue: String) {
            when (attributeLocalName) {
                "name"  -> name = attributeValue
                "path",
                "xpath" -> path = attributeValue
            }
        }
    }

    open fun getFilter(gatheringNamespaceContext: GatheringNamespaceContext): NamespaceGatherer {
        return XPathholderNamespaceGatherer(gatheringNamespaceContext)
    }

    fun save(desc: KSerialClassDesc, output: KOutput, data: T) {
        val childOut = output.writeBegin(desc)

        childOut.writeNullableStringElementValue(desc, desc.getElementIndex("name"), data._name)
        childOut.writeNullableStringElementValue(desc, desc.getElementIndex("xpath"), data.getPath())
        if (childOut is XML.XmlOutput) {
            val writer = childOut.target
            for ((prefix, nsUri) in data.namespaces) {
                if (writer.getNamespaceUri(prefix) != nsUri) {
                    writer.namespaceAttr(prefix, nsUri)
                }
            }
            writeAdditionalAttributes(writer, data)
            writer.serialize(data.getXmlReader())
        } else {
            childOut.writeSerializableElementValue(desc, desc.getElementIndex("namespaces"), Namespace.list,
                                                   data.namespaces.toList())
            childOut.writeStringElementValue(desc, desc.getElementIndex("content"), data.contentString)
            writeAdditionalValues(childOut, desc, data)
        }
    }

    open fun writeAdditionalAttributes(writer: XmlWriter, data: T) {}

    open fun writeAdditionalValues(out: KOutput, desc: KSerialClassDesc, data: T) {}

    open class NamespaceGatherer(val gatheringNamespaceContext: GatheringNamespaceContext) {

        open fun visitNamesInElement(source: XmlReader, localPrefixes: List<List<String>>) {
            assert(source.eventType === EventType.START_ELEMENT)

            val prefix = source.prefix
            val isLocal = localPrefixes.any { prefix in it }
            if (! isLocal) {
                gatheringNamespaceContext.getNamespaceURI(prefix)
            }

            for (i in source.attributeCount - 1 downTo 0) {
                val attrName = source.getAttributeName(i)
                visitNamesInAttributeValue(source.namespaceContext, source.name, attrName, source.getAttributeValue(i),
                                           localPrefixes)
            }
        }

        open fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                            owner: QName,
                                            attributeName: QName,
                                            attributeValue: CharSequence,
                                            localPrefixes: List<List<String>>) {
            // By default there are no special attributes
        }

        @Suppress("UnusedReturnValue")
        open fun visitNamesInTextContent(parent: QName?, textContent: CharSequence): List<QName> {
            return emptyList()
        }

    }

    internal open class XPathholderNamespaceGatherer(gatheringNamespaceContext: GatheringNamespaceContext) :
        NamespaceGatherer(gatheringNamespaceContext) {

        override fun visitNamesInAttributeValue(elementContext: NamespaceContext,
                                                owner: QName,
                                                attributeName: QName,
                                                attributeValue: CharSequence,
                                                localPrefixes: List<List<String>>) {
            if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
                val namesInPath = mutableMapOf<String, String>()
                val newContext = GatheringNamespaceContext(elementContext, namesInPath)
                visitXpathUsedPrefixes(attributeValue, newContext)
                for((prefix, nsUri) in namesInPath) {
                    if (localPrefixes.none { prefix in it }) {
                        gatheringNamespaceContext.getNamespaceURI(prefix)
                    }
                }

            }
        }

    }


    private class FilteringReader(val delegate: XmlReader,
                                  val filter: XPathHolderSerializer.NamespaceGatherer,
                                  initialNamespaceEnd: Int) : XmlReader by delegate {

        private val localPrefixes = mutableListOf<List<String>>(emptyList())

        init {
            delegate.eventType.handle()
        }

        fun EventType.handle() {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (this) {
                EventType.START_ELEMENT -> {
                    localPrefixes.add((delegate.namespaceStart until delegate.namespaceEnd).map { delegate.getNamespacePrefix(it) })
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


}
