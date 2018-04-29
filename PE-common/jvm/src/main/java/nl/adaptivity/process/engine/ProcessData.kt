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

package nl.adaptivity.process.engine

import net.devrieze.util.Named
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.util.DomUtil
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ExtXmlDeserializable
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.xml.*
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.w3c.dom.NodeList


/** Class to represent data attached to process instances.  */
actual class ProcessData actual constructor(name: String?, value: ICompactFragment) : Named, ExtXmlDeserializable, XmlSerializable {
    override var name = name
        private set

    actual var content: ICompactFragment
        private set

    override val elementName: QName
        get() = ELEMENTNAME

    // Property accessors start
    val contentFragment: DocumentFragment
        get() = DomUtil.childrenToDocumentFragment(contentStream)

    actual val contentStream: XmlReader
        get() = content.getXmlReader()

    init {
        this.content = value
    }

    @Deprecated("")
    constructor(name: String, value: NodeList?) : this(name, if (value == null || value.length <= 1) toNode(
        value) else DomUtil.toDocFragment(value)) {
    }

    override fun copy(name: String?): ProcessData = copy(name, content)

    fun copy(name:String?, value: ICompactFragment): ProcessData = ProcessData(name, value)

    /**
     * @see .ProcessData
     */
    @Deprecated("Initialise with compact fragment instead.")
    constructor(name: String, value: Node?) : this(name,
                                                   toCompactFragment(
                                                       value)) {
    }

    @Deprecated("")
    constructor(name: String, value: List<Node>) : this(name, DomUtil.toDocFragment(value)) {
    }


    override fun deserializeChildren(reader: XmlReader) {
        val expected = EventType.END_ELEMENT
        if (reader.next() !== expected) {
            content = reader.siblingsToFragment()
        }
    }

    override fun deserializeAttribute(attributeNamespace: CharSequence,
                                      attributeLocalName: CharSequence,
                                      attributeValue: CharSequence): Boolean {
        if (attributeLocalName.toString() == "name") {
            name = attributeValue.toString()
            return true
        }
        return false
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
        // nothing
    }

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME)
        out.writeAttribute("name", name)
        content.serialize(out)
        out.endTag(ELEMENTNAME)
    }

    override fun newWithName(name: String): Named {
        return ProcessData(name, content)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessData

        if (name != other.name) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + content.hashCode()
        return result
    }

    actual companion object {

        actual val ELEMENTLOCALNAME = "value"
        actual val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)

        private fun toCompactFragment(value: Node?): ICompactFragment {
            return DomUtil.nodeToFragment(value)
        }

        private fun toNode(value: NodeList?): Node? {
            if (value == null || value.length == 0) {
                return null
            }
            assert(value.length == 1)
            return value.item(0)
        }
        // Property acccessors end

        actual fun missingData(name: String): ProcessData {
            return ProcessData(name, CompactFragment(""))
        }

        actual fun deserialize(reader: XmlReader): ProcessData {
            return ProcessData(null, CompactFragment("")).deserializeHelper(reader)
        }
    }
}
