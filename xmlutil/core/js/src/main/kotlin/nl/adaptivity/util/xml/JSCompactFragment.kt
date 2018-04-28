/*
 * Copyright (c) 2017.
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

import nl.adaptivity.js.prototype
import nl.adaptivity.xml.*
import org.w3c.dom.DocumentFragment


typealias JSCompactFragment = CompactFragment

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
actual open class CompactFragment : XmlSerializable {

    class Factory : XmlDeserializerFactory<CompactFragment> {

        override fun deserialize(reader: XmlReader): CompactFragment {
            return Companion.deserialize(reader)
        }
    }

    actual val isEmpty: Boolean
        get() = content.isEmpty()


    actual val namespaces: IterableNamespaceContext

    @Deprecated("In javascript this is not efficient, use contentString")
    actual val content: CharArray
        get() = CharArray(contentString.length) { i -> contentString[i] }

    actual val contentString: String

    actual constructor(namespaces: Iterable<Namespace>, content: CharArray?) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = content?.toString() ?: ""
    }

    /** Convenience constructor for content without namespaces.  */
    actual constructor(content: String) : this(emptyList(), content)

    constructor(documentFragment: DocumentFragment):this(documentFragment.toString())

    /** Convenience constructor for content without namespaces.  */
    constructor(namespaces: Iterable<Namespace>, string: String) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = string
    }

    actual constructor(orig: CompactFragment) {
        namespaces = SimpleNamespaceContext.from(orig.namespaces)
        contentString = orig.contentString
    }

    actual constructor(content: XmlSerializable) {
        namespaces = SimpleNamespaceContext(emptyList())
        contentString = content.toString()
    }

    override fun serialize(out: XmlWriter) {
        XMLFragmentStreamReader.from(this).let { reader: XmlReader ->
            out.serialize(reader)
        }
    }

    actual fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || prototype != other.prototype) return false

        val that = other as CompactFragment?

        if (namespaces != that!!.namespaces) return false
        return contentString == that.contentString

    }

    override fun hashCode(): Int {
        var result = namespaces.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("{namespaces=[")
            namespaces.joinTo(this) { "\"${it.prefix} -> ${it.namespaceURI}" }

            append("], content=")
                .append(contentString)
                .append('}')
        }
    }


    companion object {

        val FACTORY = Factory()

        fun deserialize(reader: XmlReader): CompactFragment {
            return reader.siblingsToFragment()
        }
    }
}
