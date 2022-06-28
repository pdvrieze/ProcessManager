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

import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.CombinedNamespaceContext
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.util.xml.NamespaceAddingStreamReader
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReader


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
@OptIn(XmlUtilInternal::class)
abstract class XMLContainer
private constructor(
    override var namespaces: IterableNamespaceContext,
    override var content: CharArray
) : ICompactFragment {

    constructor(namespaces: Iterable<Namespace>, content: CharArray) : this(
        SimpleNamespaceContext.from(namespaces),
        content
                                                                           )
    override val isEmpty: Boolean
        get() = content.isEmpty()

    override val contentString: String
        get() = buildString(content.size) { content.forEach { append(it) } }

    val originalNSContext: Iterable<Namespace>
        get() = namespaces

    constructor() : this(emptyList(), CharArray(0))

    @Suppress("unused")
    constructor(fragment: ICompactFragment) : this(fragment.namespaces, fragment.content)

    open fun deserializeChildren(reader: XmlReader) {
        if (reader.hasNext()) {
            if (reader.next() !== EventType.END_ELEMENT) {
                val content = reader.siblingsToFragment()
                setContent(content.namespaces, content.content)
            }
        }
    }

    fun setContent(originalNSContext: Iterable<Namespace>, content: CharArray) {
        this.namespaces = SimpleNamespaceContext.from(originalNSContext)
        this.content = content
    }

    @Deprecated("XMLContainer should be immutable")
    protected fun updateNamespaceContext(additionalContext: Iterable<Namespace>) {
        val nsmap = mutableMapOf<String, String>()
        val context = when ((namespaces as? SimpleNamespaceContext)?.size) {
            0    -> SimpleNamespaceContext.from(additionalContext)
            else -> CombinedNamespaceContext(namespaces, SimpleNamespaceContext.from(additionalContext))
        }
        val gatheringNamespaceContext = MyGatheringNamespaceContext(nsmap, context)
        visitNamespaces(gatheringNamespaceContext)

        namespaces = SimpleNamespaceContext(nsmap)
    }

    internal fun addNamespaceContext(namespaceContext: SimpleNamespaceContext) {
        namespaces = when ((namespaces as? SimpleNamespaceContext)?.size) {
            0    -> namespaceContext
            else -> (namespaces + namespaceContext)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun visitNamesInElement(source: XmlReader) {
        assert(source.eventType === EventType.START_ELEMENT)
        visitNamespace(source, source.prefix)

        for (i in source.attributeCount - 1 downTo 0) {
            val attrName = source.getAttributeName(i)
            visitNamesInAttributeValue(source.namespaceContext, source.name, attrName, source.getAttributeValue(i))
        }
    }

    protected open fun visitNamesInAttributeValue(
        referenceContext: NamespaceContext,
        owner: QName,
        attributeName: QName,
        attributeValue: CharSequence
                                                 ) {
        // By default, there are no special attributes
    }

    @Suppress("UnusedReturnValue", "UNUSED_PARAMETER")
    protected fun visitNamesInTextContent(parent: QName?, textContent: CharSequence): List<QName> {
        return emptyList()
    }

    protected open fun visitNamespaces(baseContext: NamespaceContext) {
        val xsr = NamespaceAddingStreamReader(baseContext, this.getXmlReader())

        visitNamespacesInContent(xsr, null)
    }

    @Throws(XmlException::class)
    private fun visitNamespacesInContent(xsr: XmlReader, parent: QName?) {
        while (xsr.hasNext()) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (xsr.next()) {
                EventType.START_ELEMENT -> {
                    visitNamesInElement(xsr)
                    visitNamespacesInContent(xsr, xsr.name)
                }
                EventType.TEXT          -> {
                    visitNamesInTextContent(parent, xsr.text)
                }
                else -> { /* ignore */ }
            }
        }
    }

    @Throws(XmlException::class)
    private fun serializeBody(out: XmlWriter) {
        if (content.isNotEmpty()) {
            val contentReader = getXmlReader().asSubstream()
            while (contentReader.hasNext()) {
                contentReader.next()
                contentReader.writeCurrent(out)
            }
        }

    }

    @Throws(XmlException::class)
    protected open fun serializeAttributes(out: XmlWriter) {
        // No attributes by default
    }

    @Throws(XmlException::class)
    protected abstract fun serializeStartElement(out: XmlWriter)

    @Throws(XmlException::class)
    protected abstract fun serializeEndElement(out: XmlWriter)

    override fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XMLContainer

        val orderedNamespaces = namespaces.sortedBy { it.prefix }
        val orderedOtherNamespaces = other.namespaces.sortedBy { it.prefix }

        if (orderedNamespaces != orderedOtherNamespaces) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespaces.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    companion object {

        @Throws(XmlException::class)
        protected fun visitNamespace(reader: XmlReader, prefix: CharSequence?) {
            if (prefix != null) {
                reader.namespaceContext.getNamespaceURI(prefix.toString())
            }
        }

    }
}
