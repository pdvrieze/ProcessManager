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
import nl.adaptivity.util.xml.CombinedNamespaceContext
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReader


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
@OptIn(XmlUtilInternal::class)
abstract class XMLContainer
private constructor(
    fragment: ICompactFragment
) : ICompactFragment {

    var fragment: CompactFragment = CompactFragment(fragment)
        private set

    override var namespaces: IterableNamespaceContext
        get() = fragment.namespaces
        set(value) { fragment = CompactFragment(value, fragment.content) }

    override var content: CharArray
        get() = fragment.content
        set(value) { fragment = CompactFragment(fragment.namespaces, value) }


    constructor(namespaces: Iterable<Namespace>, content: CharArray) :
        this(CompactFragment(namespaces, content))

    override val isEmpty: Boolean
        get() = content.isEmpty()

    override val contentString: String
        get() = buildString(content.size) { content.forEach { append(it) } }

    val originalNSContext: IterableNamespaceContext
        get() = namespaces

    constructor() : this(emptyList(), CharArray(0))

    @Deprecated("XMLContainer should be immutable")
    protected fun updateNamespaceContext(additionalContext: Iterable<Namespace>) {
        val nsmap = mutableMapOf<String, String>()
        val context = when ((namespaces as? SimpleNamespaceContext)?.size) {
            0    -> SimpleNamespaceContext.from(additionalContext)
            else -> CombinedNamespaceContext(namespaces, SimpleNamespaceContext.from(additionalContext))
        }
        val gatheringNamespaceContext = MyGatheringNamespaceContext(nsmap, context)

        namespaces = SimpleNamespaceContext(nsmap)
    }

    protected open fun visitNamesInAttributeValue(
        referenceContext: NamespaceContext,
        owner: QName,
        attributeName: QName,
        attributeValue: CharSequence
    ) {
        // By default, there are no special attributes
    }

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

        protected fun visitNamespace(reader: XmlReader, prefix: CharSequence?) {
            if (prefix != null) {
                reader.namespaceContext.getNamespaceURI(prefix.toString())
            }
        }

    }
}
