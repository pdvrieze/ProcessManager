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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.util.MyGatheringNamespaceContext

abstract class XPathHolderSerializer<T : XPathHolder> : XmlContainerSerializer<T>() {

    protected open class PathHolderData<T : XPathHolder>(
        val owner: XPathHolderSerializer<in T>,
        var name: String? = null,
        var path: String? = null
    ) : ContainerData<T>() {

        override fun handleLastRootAttributeReadEvent(
            reader: XmlReader,
            gatheringNamespaceContext: MyGatheringNamespaceContext
        ) {
            if (!path.isNullOrEmpty()) {
                visitXpathUsedPrefixes(path, gatheringNamespaceContext)
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun readAdditionalChild(desc: SerialDescriptor, decoder: CompositeDecoder, index: Int) {
            when (desc.getElementName(index)) {
                "name"  -> name = decoder.decodeSerializableElement(desc, index, nullStringSerializer)
//                "path",
//                "xpath" -> path = decoder.readNullableString(desc, index)
                else    -> super.readAdditionalChild(desc, decoder, index)
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

    override fun getFilter(gatheringNamespaceContext: MyGatheringNamespaceContext): NamespaceGatherer {
        return XPathholderNamespaceGatherer(gatheringNamespaceContext)
    }

    open fun writeAdditionalAttributes(writer: XmlWriter, data: T) {}

    @OptIn(ExperimentalSerializationApi::class)
    override fun writeAdditionalValues(out: CompositeEncoder, desc: SerialDescriptor, data: T) {
        out.encodeSerializableElement(desc, desc.getElementIndex("name"), nullStringSerializer, data._name)
        out.encodeSerializableElement(desc, desc.getElementIndex("xpath"), nullStringSerializer, data.getPath())
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
