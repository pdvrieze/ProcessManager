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
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.*


expect abstract class XPathHolder : XMLContainer {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    var _name: String?

    constructor()

    constructor(name: String?,
                path: String?,
                content: CharArray?,
                originalNSContext: Iterable<Namespace>)

    fun getName(): String

    fun setName(value: String)

    fun getPath(): String?

    fun setPath(namespaceContext: Iterable<out Namespace>, value: String?)

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean

    override fun deserializeChildren(reader: XmlReader)

    override fun serializeAttributes(out: XmlWriter)

    override fun visitNamespaces(baseContext: NamespaceContext)

    override fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                            owner: QName,
                                            attributeName: QName,
                                            attributeValue: CharSequence)

    companion object {

        @JvmStatic
        fun <T : XPathHolder> deserialize(reader: XmlReader, result: T): T

        protected fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext)
    }
}

inline fun <T> XPathHolder.Companion.load(desc: KSerialClassDesc,
                                          input: KInput,
                                          factory: (name: String?, path: String?, content: CharArray?, originalNSContext: Iterable<Namespace>?) -> T): T {
    @Suppress("NAME_SHADOWING")
    val input = input.readBegin(desc)

    var name: String? = null
    var path: String? = null
    var content: CharArray? = null
    var namespaces: Iterable<Namespace>? = null

    if (input is XML.XmlInput) {
        val reader = input.input
        for (i in 0 until reader.attributeCount) {
            when (reader.getAttributeLocalName(i)) {
                "name"  -> name = reader.getAttributeValue(i)
                "path",
                "xpath" -> path = reader.getAttributeValue(i)
            }
        }
        val frag = reader.siblingsToFragment()
        content = frag.content
        namespaces = frag.namespaces
    } else {
        // TODO look at using the description to resolve the indices
        loop@ while (true) {
            val next = input.readElement(desc)
            when (next) {
                KInput.READ_DONE -> break@loop
                KInput.READ_ALL  -> TODO("Not yet supported")
                0                -> name = input.readNullableString()
                1                -> path = input.readNullableString()
                2                -> namespaces = input.readSerializableElementValue(desc, 2,
                                                                                    input.context.klassSerializer(
                                                                                        Namespace::class).list)
                3                -> content = input.readSerializableElementValue(desc, 0,
                                                                                 CharArrayAsStringSerializer)
            }

        }
        input.readEnd(desc)
    }
    return factory(name, path, content, namespaces)
}

fun XPathHolder.Companion.save(desc: KSerialClassDesc, output: KOutput, data: XPathHolder) {
    val childOut = output.writeBegin(desc)

    childOut.writeNullableStringElementValue(desc, 0, data._name)
    childOut.writeNullableStringElementValue(desc, 1, data.getPath())
    if (childOut is XML.XmlOutput) {
        data.serialize(childOut.target)
    } else {
        childOut.writeSerializableElementValue(desc, 0, Namespace.list, data.namespaces.toList())
        childOut.writeStringElementValue(desc, 1, data.contentString)
    }
}
