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
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.serialutil.encodeNullableStringElement
import nl.adaptivity.serialutil.readNullableString

open class XPathHolderSerializer<T : XPathHolder> : XmlContainerSerializer<T>() {
    open class PathHolderData<T : XPathHolder>(val owner: XPathHolderSerializer<in T>,
                                               var name: String? = null,
                                               var path: String? = null) :
        ContainerData<T>() {

        override fun handleLastRootAttributeReadEvent(reader: XmlReader,
                                                      gatheringNamespaceContext: GatheringNamespaceContext) {
            if (!path.isNullOrEmpty()) {
                visitXpathUsedPrefixes(path, gatheringNamespaceContext)
            }
        }

        override fun readAdditionalChild(desc: SerialDescriptor, decoder: CompositeDecoder, index: Int) {
            when (desc.getElementName(index)) {
                "name" -> name = decoder.readNullableString(desc, index)
                "path",
                "xpath" -> path = decoder.readNullableString(desc, index)
                else   -> super.readAdditionalChild(desc, decoder, index)
            }
        }

        override fun handleAttribute(attributeLocalName: String, attributeValue: String) {
            when (attributeLocalName) {
                "name"  -> name = attributeValue
                "path",
                "xpath" -> path = attributeValue
            }
        }
    }

    override fun getFilter(gatheringNamespaceContext: GatheringNamespaceContext): NamespaceGatherer {
        return XPathholderNamespaceGatherer(gatheringNamespaceContext)
    }

    open fun writeAdditionalAttributes(writer: XmlWriter, data: T) {}

    override fun writeAdditionalValues(out: KOutput, desc: KSerialClassDesc, data: T) {
        out.encodeNullableStringElement(desc, desc.getElementIndex("name"), data._name)
        out.encodeNullableStringElement(desc, desc.getElementIndex("xpath"), data.getPath())
    }

    internal open class XPathholderNamespaceGatherer(gatheringNamespaceContext: GatheringNamespaceContext) :
        NamespaceGatherer(gatheringNamespaceContext) {

        override fun visitNamesInAttributeValue(elementContext: NamespaceContext,
                                                owner: QName,
                                                attributeName: QName,
                                                attributeValue: CharSequence,
                                                localPrefixes: List<List<String>>) {
            if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
                val namesInPath = mutableMapOf<String, String>()
                val newContext = GatheringNamespaceContext(elementContext, namesInPath)
                visitXpathUsedPrefixes(attributeValue, newContext)
                for ((prefix, nsUri) in namesInPath) {
                    if (localPrefixes.none { prefix in it }) {
                        gatheringNamespaceContext.getNamespaceURI(prefix)
                    }
                }

            }
        }

    }


}
