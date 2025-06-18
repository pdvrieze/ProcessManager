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
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.process.processModel.XmlDefineType.SerialDelegate
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

abstract class XPathHolderSerializer<T : XPathHolder, D: XPathHolderSerializer.SerialDelegateBase>(
    protected val delegateSerializer: KSerializer<D>
) : XmlContainerSerializer<T>() {

    interface SerialDelegateBase {
        val xpath: String?
        val content: CompactFragment
    }

    protected open class PathHolderData<T : XPathHolder>(
        val owner: XPathHolderSerializer<in T, *>,
        var name: String? = null,
        var path: String? = null
    ) : ContainerData<T>() {

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


    override fun getFilter(gatheringNamespaceContext: MyGatheringNamespaceContext): XmlContainerSerializer.NamespaceGatherer {
        return XPathholderNamespaceGatherer(gatheringNamespaceContext)
    }


    internal open class XPathholderNamespaceGatherer(gatheringNamespaceContext: MyGatheringNamespaceContext) :
        NamespaceGatherer(gatheringNamespaceContext) {

        override fun visitNamesInAttributeValue(
            referenceContext: NamespaceContext,
            owner: QName,
            attributeName: QName,
            attributeValue: CharSequence,
            localPrefixes: List<List<String>>
        ) {
            if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
                val namesInPath = mutableMapOf<String, String>()
                val newContext = MyGatheringNamespaceContext(namesInPath, referenceContext)
                visitXpathUsedPrefixes(attributeValue, newContext)
                for (prefix in namesInPath.keys) {
                    if (localPrefixes.none { prefix in it }) {
                        gatheringNamespaceContext.getNamespaceURI(prefix)
                    }
                }

            }
        }

    }


}

private val nullStringSerializer = String.serializer().nullable
