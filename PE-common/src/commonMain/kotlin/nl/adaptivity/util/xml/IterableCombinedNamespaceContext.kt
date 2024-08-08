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
class IterableCombinedNamespaceContext(
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

        primary !is IterableNamespaceContext    ->
            (secondary as? IterableNamespaceContext)?.freeze() ?: SimpleNamespaceContext()

        secondary !is IterableNamespaceContext ||
            ! secondary.iterator().hasNext()    ->
            primary.freeze()

        !primary.iterator().hasNext() -> secondary.freeze()

        else                                    -> {
            val frozenPrimary = primary.freeze()
            val frozenSecondary = secondary.freeze()
            if (frozenPrimary === primary && frozenSecondary == secondary) {
                this
            } else {
                @Suppress("DEPRECATION")
                (IterableCombinedNamespaceContext(primary.freeze(), secondary.freeze()))
            }
        }
    }

    @OptIn(XmlUtilInternal::class)
    override fun iterator(): Iterator<Namespace> {
        val p = (primary as? IterableNamespaceContext)?.run { freeze().asSequence() } ?: emptySequence()
        val s = (secondary as? IterableNamespaceContext)?.run { freeze().asSequence() } ?: emptySequence()

        return (p + s).iterator()
    }

    @Deprecated(
        "Don't use as unsafe",
        replaceWith = ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor")
    )
    @Suppress("OverridingDeprecatedMember")
    override fun getPrefixes(namespaceURI: String): Iterator<String> {
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
        (IterableCombinedNamespaceContext(this, secondary))
}
