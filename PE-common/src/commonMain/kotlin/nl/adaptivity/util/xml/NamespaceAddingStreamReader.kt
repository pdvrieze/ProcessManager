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

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XmlDelegatingReader

/**
 * A streamreader that adds a namespace context as source for looking up namespace information for prefixes present
 * in the original, but do not have attached namespace information.
 *
 * Created by pdvrieze on 31/10/15.
 */
class NamespaceAddingStreamReader(private val lookupSource: NamespaceContext, source: XmlReader) :
    XmlDelegatingReader(source) {

    override val namespaceURI: String
        get() {
            val namespaceURI = delegate.namespaceURI
            return if (namespaceURI.isNotEmpty()) namespaceURI else lookupSource.getNamespaceURI(delegate.prefix) ?: ""
        }

    @OptIn(XmlUtilInternal::class)
    override val namespaceContext: IterableNamespaceContext
        get() = CombiningNamespaceContext(delegate.namespaceContext, lookupSource)

    override fun require(type: EventType, namespace: String?, name: String?) {
        if (type !== eventType ||
            namespace != null && namespace != this.namespaceURI ||
            name != null && name != name
        ) {
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
                    i
                                                                )) && localName == getAttributeLocalName(i)
            ) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    override fun getAttributeNamespace(index: Int): String {
        val attributeNamespace = delegate.getAttributeNamespace(index)
        return if (attributeNamespace.isNotEmpty()) attributeNamespace else lookupSource.getNamespaceURI(
            delegate.getAttributePrefix(index)
                                                                                                        ) ?: ""
    }

    @OptIn(XmlUtilInternal::class)
    private class CombiningNamespaceContext(
        val primary: NamespaceContext,
        val secondary: NamespaceContext
    ) : IterableNamespaceContext, NamespaceContextImpl {

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

        @OptIn(XmlUtilInternal::class)
        override fun freeze(): IterableNamespaceContext = when {
            primary is SimpleNamespaceContext &&
                secondary is SimpleNamespaceContext -> this

            primary !is IterableNamespaceContext ->
                (secondary as? IterableNamespaceContext)?.freeze() ?: SimpleNamespaceContext()

            secondary !is IterableNamespaceContext ||
                ! secondary.iterator().hasNext() ->
                primary.freeze()

            !primary.iterator().hasNext() -> secondary.freeze()

            else -> {
                val frozenPrimary = primary.freeze()
                val frozenSecondary = secondary.freeze()
                if (frozenPrimary === primary && frozenSecondary == secondary) {
                    this
                } else {
                    @Suppress("DEPRECATION")
                    CombiningNamespaceContext(primary.freeze(), secondary.freeze())
                }
            }
        }

        @OptIn(XmlUtilInternal::class)
        override fun iterator(): Iterator<Namespace> {
            val p = (primary as? IterableNamespaceContext)?.run { freeze().asSequence() } ?: emptySequence()
            val s = (secondary as? IterableNamespaceContext)?.run { freeze().asSequence() } ?: emptySequence()

            return (p + s).iterator()
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

        override fun plus(secondary: FreezableNamespaceContext): FreezableNamespaceContext =
            @Suppress("DEPRECATION")
            CombiningNamespaceContext(this, secondary)
    }

}

