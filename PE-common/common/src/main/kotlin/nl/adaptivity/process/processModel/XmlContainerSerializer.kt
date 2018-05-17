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
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.CharArrayAsStringSerializer
import nl.adaptivity.xml.serialization.XML

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)
open class XmlContainerSerializer<T : XMLContainer> {


    open fun getFilter(gatheringNamespaceContext: GatheringNamespaceContext): NamespaceGatherer {
        return NamespaceGatherer(gatheringNamespaceContext)
    }


    open class ContainerData<O : XmlContainerSerializer<T>, T : XMLContainer>(val owner: O) {
        var content: CharArray? = null
        var namespaces: Iterable<Namespace>? = null

        open fun handleAttribute(attributeLocalName: String, attributeValue: String) {}

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

                handleLastRootAttributeRead(reader, gatheringNamespaceContext)

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
                val contentIdx = desc.getElementIndex("content")
                val namespaceIdx = desc.getElementIndex("path")
                loop@ while (true) {
                    val next = input.readElement(desc)
                    when (next) {
                        KInput.READ_DONE -> break@loop
                        KInput.READ_ALL  -> TODO("Not yet supported")
                        else -> when (desc.getElementName(next)) {
                            "namespaces" -> namespaces =
                                input.readSerializableElementValue(desc, next,
                                                                   input.context.klassSerializer(Namespace::class).list)
                            "content" -> content = input.readSerializableElementValue(desc, 0, CharArrayAsStringSerializer)
                            else -> readAdditionalChild(desc, input, next)
                        }
                    }

                }
                input.readEnd(desc)
            }


        }

        open fun readAdditionalChild(desc: KSerialClassDesc, input: KInput, index: Int) {
            throw UnknownFieldException(index)
        }


        open fun handleLastRootAttributeRead(reader: XmlReader, gatheringNamespaceContext: GatheringNamespaceContext) {}

    }

    protected class FilteringReader(val delegate: XmlReader,
                                    val filter: NamespaceGatherer) : XmlReader by delegate {

        private val localPrefixes = mutableListOf<List<String>>(emptyList())

        init {
            delegate.eventType.handle()
        }

        fun EventType.handle() {
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
                visitNamesInAttributeValue(source.namespaceContext, source.name, attrName,
                                           source.getAttributeValue(i),
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

}