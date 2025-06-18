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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)

@OptIn(XmlUtilInternal::class, ExperimentalXmlUtilApi::class)
abstract class XmlContainerSerializer<T : XMLContainer>: KSerializer<T> {

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
