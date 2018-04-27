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

package nl.adaptivity.util.xml

import nl.adaptivity.xml.EventType
import nl.adaptivity.xml.NamespaceContext
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader


/**
 * A streamreader that adds a namespace context as source for looking up namespace information for prefixes present
 * in the original, but do not have attached namespace information.
 *
 * Created by pdvrieze on 31/10/15.
 */
class NamespaceAddingStreamReader(private val lookupSource: NamespaceContext, source: XmlReader) : XmlDelegatingReader(
        source) {

    override val namespaceUri: CharSequence
        get() {
            val namespaceURI = delegate.namespaceUri
            return namespaceURI ?: lookupSource.getNamespaceURI(delegate.prefix?.toString()) ?: ""
        }

    override val namespaceContext: NamespaceContext
        get() = CombiningNamespaceContext(delegate.namespaceContext, lookupSource)

    override fun require(type: EventType, namespaceURI: CharSequence?, localName: CharSequence?) {
        if (type !== eventType ||
            namespaceURI != null && namespaceURI != namespaceUri ||
            localName != null && localName != localName) {
            delegate.require(type, namespaceURI, localName)
        }
        run { throw XmlException("Require failed") }
    }

    override fun getNamespaceUri(prefix: CharSequence): String? {
        val namespaceURI = delegate.getNamespaceUri(prefix)
        return namespaceURI ?: lookupSource.getNamespaceURI(prefix.toString())
    }

    override fun getAttributeValue(namespaceURI: CharSequence?, localName: CharSequence): CharSequence? {

        for (i in attributeCount - 1 downTo 0) {
            if ((namespaceURI == null || namespaceURI == getAttributeNamespace(
                            i)) && localName == getAttributeLocalName(i)) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    override fun getAttributeNamespace(index: Int): CharSequence {
        val attributeNamespace = delegate.getAttributeNamespace(index)
        return attributeNamespace ?: lookupSource.getNamespaceURI(delegate.getAttributePrefix(index)?.toString()) ?: ""
    }
}
