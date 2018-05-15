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
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.CombiningNamespaceContext
import nl.adaptivity.util.xml.NamespaceAddingStreamReader
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.*


expect abstract class XPathHolder : XMLContainer {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    var _name: String?

    constructor()

    constructor(name: String?,
                path: String?,
                content: CharArray?,
                originalNSContext: Iterable<Namespace>)

    fun getName(): String

    fun setName(value: String)

    fun getPath(): String?

    fun setPath(namespaceContext: Iterable<out Namespace>, value: String?)

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean

    override fun deserializeChildren(reader: XmlReader)

    override fun serializeAttributes(out: XmlWriter)

    override fun visitNamespaces(baseContext: NamespaceContext)

    override fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                            owner: QName,
                                            attributeName: QName,
                                            attributeValue: CharSequence)

    companion object {

        @JvmStatic
        fun <T : XPathHolder> deserialize(reader: XmlReader, result: T): T

    }
}

internal expect fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)


@PublishedApi
internal class PathHolderData(var name: String? = null,
                              var path: String? = null,
                              var content: CharArray? = null,
                              var namespaces: Iterable<Namespace>? = null) {
    fun load(input: KInput,
             desc: KSerialClassDesc,
             getFilter: (GatheringNamespaceContext) -> NamespaceGatherer) {
        @Suppress("NAME_SHADOWING")
        val input = input.readBegin(desc)

        if (input is XML.XmlInput) {
            val reader = input.input
            for (i in 0 until reader.attributeCount) {
                when (reader.getAttributeLocalName(i)) {
                    "name"  -> name = reader.getAttributeValue(i)
                    "path",
                    "xpath" -> path = reader.getAttributeValue(i)
                }
            }
            val namespacesMap = mutableMapOf<String, String>()
            val gatheringNamespaceContext = GatheringNamespaceContext(reader.namespaceContext, namespacesMap)

            if (!path.isNullOrEmpty()) {
                visitXpathUsedPrefixes(path, gatheringNamespaceContext)
            }
            reader.next()
            // We have finished the start element, now only read the content
            // If we don't skip here we will read the element itself
            val gatheringReader = FilteringReader(reader, getFilter(gatheringNamespaceContext))

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
}

internal inline fun <T> XPathHolder.Companion.load(desc: KSerialClassDesc,
                                          input: KInput,
                                          noinline getFilter: (GatheringNamespaceContext) -> NamespaceGatherer,
                                          factory: (name: String?, path: String?, content: CharArray?, originalNSContext: Iterable<Namespace>?) -> T): T {
    val data = PathHolderData().apply { load(input, desc, getFilter) }
    return factory(data.name, data.path, data.content, data.namespaces)
}

fun XPathHolder.Companion.save(desc: KSerialClassDesc, output: KOutput, data: XPathHolder) {
    val childOut = output.writeBegin(desc)

    childOut.writeNullableStringElementValue(desc, 0, data._name)
    childOut.writeNullableStringElementValue(desc, 1, data.getPath())
    if (childOut is XML.XmlOutput) {
        val writer = childOut.target
        for ((prefix, nsUri) in data.namespaces) {
            if (writer.getNamespaceUri(prefix) != nsUri) {
                writer.namespaceAttr(prefix, nsUri)
            }
        }
        writer.serialize(data.getXmlReader())
    } else {
        childOut.writeSerializableElementValue(desc, 2, Namespace.list, data.namespaces.toList())
        childOut.writeStringElementValue(desc, 3, data.contentString)
    }
}

private class FilteringReader(val delegate: XmlReader, val filter: NamespaceGatherer): XmlReader by delegate {
    private var textContent: StringBuilder? = null
    override fun next(): EventType {
        return delegate.next().also {
            when (it) {
                EventType.START_ELEMENT          -> {
                    textContent = StringBuilder()
                    filter.visitNamesInElement(this)
                }
                EventType.TEXT,
                EventType.IGNORABLE_WHITESPACE,
                EventType.CDSECT -> {
                    textContent?.append(text)
                }
                EventType.END_ELEMENT -> {
                    textContent?.let { filter.visitNamesInTextContent(name, it) }

                    textContent = null
                }
            }
        }
    }
}

open internal class NamespaceGatherer(val gatheringNamespaceContext: GatheringNamespaceContext) {

    open fun visitNamesInElement(source: XmlReader) {
        assert(source.eventType === EventType.START_ELEMENT)

        gatheringNamespaceContext.getNamespaceURI(source.prefix)

        for (i in source.attributeCount - 1 downTo 0) {
            val attrName = source.getAttributeName(i)
            visitNamesInAttributeValue(source.namespaceContext, source.name, attrName, source.getAttributeValue(i))
        }
    }

    open fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                                  owner: QName,
                                                  attributeName: QName,
                                                  attributeValue: CharSequence) {
        // By default there are no special attributes
    }

    @Suppress("UnusedReturnValue")
    open fun visitNamesInTextContent(parent: QName?, textContent: CharSequence): List<QName> {
        return emptyList()
    }

}

internal open class XPathholderNamespaceGatherer(gatheringNamespaceContext: GatheringNamespaceContext) :
    NamespaceGatherer(gatheringNamespaceContext) {

    override fun visitNamesInAttributeValue(referenceContext: NamespaceContext, owner: QName, attributeName: QName, attributeValue: CharSequence) {
        if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
            visitXpathUsedPrefixes(attributeValue, CombiningNamespaceContext(gatheringNamespaceContext, referenceContext))
        }
    }

}
