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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader


/**
 * A streamreader that adds a namespace context as source for looking up namespace information for prefixes present
 * in the original, but do not have attached namespace information.
 *
 * Created by pdvrieze on 31/10/15.
 */
class NamespaceAddingStreamReader(private val lookupSource: NamespaceContext, source: XmlReader) : XmlDelegatingReader(
        source) {

    override val namespaceURI: String
        get() {
            val namespaceURI = delegate.namespaceURI
            return if(namespaceURI.isNotEmpty()) namespaceURI else lookupSource.getNamespaceURI(delegate.prefix) ?: ""
        }

    override val namespaceContext: NamespaceContext
        get() = CombiningNamespaceContext(delegate.namespaceContext, lookupSource)

    override fun require(type: EventType, namespace: String?, name: String?) {
        if (type !== eventType ||
            namespace != null && namespace != this.namespaceURI ||
            name != null && name != name) {
            delegate.require(type, namespace, name)
        }
        run { throw XmlException("Require failed") }
    }

    override fun getNamespaceURI(prefix: String): String? {
        val namespaceURI = delegate.getNamespaceURI(prefix)
        return namespaceURI ?: lookupSource.getNamespaceURI(prefix)
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {

        for (i in attributeCount - 1 downTo 0) {
            if ((nsUri == null || nsUri == getAttributeNamespace(
                            i)) && localName == getAttributeLocalName(i)) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    override fun getAttributeNamespace(index: Int): String {
        val attributeNamespace = delegate.getAttributeNamespace(index)
        return if(attributeNamespace.isNotEmpty()) attributeNamespace else lookupSource.getNamespaceURI(
            delegate.getAttributePrefix(index)) ?: ""
    }
}
