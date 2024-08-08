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

package nl.adaptivity.util

import nl.adaptivity.xmlutil.*


@OptIn(XmlUtilInternal::class)
internal class MyGatheringNamespaceContext(
    private val resultMap: MutableMap<String, String>,
    private vararg val parentContext: NamespaceContext
) : NamespaceContextImpl, IterableNamespaceContext {

    override fun iterator(): Iterator<Namespace> {
        return resultMap.map { nameSpace(it.key, it.value) }.iterator()
    }

    override fun getNamespaceURI(prefix: String): String? {
        return parentContext.asSequence()
            .mapNotNull { it.getNamespaceURI(prefix) }
            .firstOrNull()?.apply {
                if (!isEmpty() && prefix != XMLConstants.XMLNS_ATTRIBUTE) {
                    resultMap[prefix] = this
                }
            }
    }

    override fun getPrefix(namespaceURI: String): String? {
        return parentContext.asSequence()
            .mapNotNull { it.getPrefix(namespaceURI) }
            .firstOrNull()?.apply {
                if (namespaceURI != XMLConstants.XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XMLConstants.XML_NS_URI) {
                    resultMap[this] = namespaceURI
                }
            }
    }

    @Deprecated(
        "Don't use as unsafe",
        replaceWith = ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor")
    )
    @Suppress(
        "UNCHECKED_CAST",
        "DEPRECATION",
        "OverridingDeprecatedMember"
    )// Somehow this type has no proper generic parameter
    override fun getPrefixes(namespaceURI: String): Iterator<String> {
        return parentContext
            .flatMap { it.prefixesFor(namespaceURI).asSequence() }
            .apply {
                if (namespaceURI != XMLConstants.XMLNS_ATTRIBUTE_NS_URI && namespaceURI != XMLConstants.XML_NS_URI) {
                    for (prefix in this) {
                        resultMap[prefix] = namespaceURI
                    }
                }
            }.iterator()
    }
}

private fun nameSpace(uri: String, prefix: String): Namespace = object : Namespace {
    override val namespaceURI: String = uri
    override val prefix: String = prefix
}
