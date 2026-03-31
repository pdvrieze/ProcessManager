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
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

abstract class XPathHolderSerializer<T : XPathHolder, D: XPathHolderSerializer.SerialDelegateBase>(
    protected val delegateSerializer: KSerializer<D>
) : KSerializer<T> {

    interface SerialDelegateBase {
        val xpath: String?
        val content: CompactFragment
    }

    protected fun deserializeCommon(decoder: Decoder): Pair<D, IterableNamespaceContext> {
        val data: D
        val extNamespaces: IterableNamespaceContext

        when (decoder) {
            is XML.XmlInput -> {
                val nsContext = decoder.input.namespaceContext.freeze()
                data = delegateSerializer.deserialize(decoder)

                extNamespaces = when (val xpath = data.xpath)  {
                    null -> data.content.namespaces
                    else -> extNamespaces(data.content, xpath, nsContext)
                }
            }

            else -> {
                data = delegateSerializer.deserialize(decoder)
                extNamespaces = data.content.namespaces
            }
        }
        return data to extNamespaces
    }

    fun extNamespaces(fragment: ICompactFragment, xpath: String, parentContext: NamespaceContext): IterableNamespaceContext {
        val namespaces = buildMap {
            for ((p, n) in fragment.namespaces) {
                put(p, n)
            }

            val newContext = MyGatheringNamespaceContext(this, parentContext)
            visitXpathUsedPrefixes(xpath, newContext)
        }.map { XmlEvent.NamespaceImpl(it.key, it.value) }

        @OptIn(XmlUtilInternal::class)
        return SimpleNamespaceContext(namespaces)
    }


}

