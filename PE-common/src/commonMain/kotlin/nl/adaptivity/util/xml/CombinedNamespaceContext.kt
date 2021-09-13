/*
 * Copyright (c) 2021.
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

import nl.adaptivity.xmlutil.*

@OptIn(XmlUtilInternal::class)
class CombinedNamespaceContext(
    val primary: NamespaceContext,
    val secondary: NamespaceContext
) : NamespaceContextImpl {

    override fun getNamespaceURI(prefix: String): String? {
        val namespaceURI = primary.getNamespaceURI(prefix)
        return if (namespaceURI == null || XMLConstants.NULL_NS_URI == namespaceURI) {
            secondary.getNamespaceURI(prefix)
        } else namespaceURI
    }

    override fun getPrefix(namespaceURI: String): String? {
        val prefix = primary.getPrefix(namespaceURI)
        return if (prefix == null || XMLConstants.NULL_NS_URI == namespaceURI && XMLConstants.DEFAULT_NS_PREFIX == prefix) {
            secondary.getPrefix(namespaceURI)
        } else prefix
    }

    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
        val prefixes1 = primary.prefixesFor(namespaceURI)
        val prefixes2 = secondary.prefixesFor(namespaceURI)
        val prefixes = hashSetOf<String>()
        while (prefixes1.hasNext()) {
            prefixes.add(prefixes1.next())
        }
        while (prefixes2.hasNext()) {
            prefixes.add(prefixes2.next())
        }
        return prefixes.iterator()
    }

}
