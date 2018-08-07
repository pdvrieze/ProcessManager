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

package nl.adaptivity.util

import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.IOException
import org.w3c.dom.*
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object DomUtil {
    private val DEFAULT_FLAGS = FLAG_OMIT_XMLDECL

    /**
     * Create an [Element] with the given name. Depending on the prefix, and namespace it uses the "correct"
     * approach, with null namespace or prefix, or specified namespace and prefix.
     *
     * @param document  The owning document.
     * @param qName The name of the element.
     */
    @JvmStatic
    fun createElement(document: Document, qName: QName): Element {
        val root: Element
        if (XMLConstants.NULL_NS_URI == qName.namespaceURI || null == qName.namespaceURI) {
            root = document.createElement(qName.localPart)
        } else if (XMLConstants.DEFAULT_NS_PREFIX == qName.prefix) {
            root = document.createElementNS(qName.namespaceURI, qName.localPart)
        } else {
            root = document.createElementNS(qName.namespaceURI, qName.prefix + ':'.toString() + qName.localPart)
        }
        return root
    }

    /**
     * XPath processing does require either a document or a fragment to actually work. This method will
     * make this work. If the node is either that will be returned, otherwise, if it is the root node of the owner document,
     * the owner document is returned. Otherwise, a fragment will be created with a clone of the node.
     * @param node The node to attach if needed.
     * @return A document or documentfragment representing the given node (it may be a clone though)
     */
    @JvmStatic
    fun ensureAttached(node: Node?): Node? {
        if (node == null) {
            return null
        }
        if (node is Document || node is DocumentFragment) {
            return node
        }
        if (node.isSameNode(node.ownerDocument.documentElement)) {
            return node.ownerDocument
        }
        val frag = node.ownerDocument.createDocumentFragment()
        frag.appendChild(node.cloneNode(true))
        return frag
    }

    @JvmStatic
    fun isAttached(node: Node): Boolean {
        if (node is Document || node is DocumentFragment) {
            return true
        }
        val docElem = node.ownerDocument.documentElement
        if (docElem != null) {
            var curNode: Node? = node
            while (curNode != null) {
                if (docElem.isSameNode(curNode)) {
                    return true
                }
                curNode = curNode.parentNode
            }
        }
        return false
    }

    @Throws(IOException::class)
    @JvmStatic
    fun tryParseXml(inputStream: InputStream): Document? {
        return tryParseXml(InputSource(inputStream))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun tryParseXml(reader: Reader): Document? {
        return tryParseXml(InputSource(reader))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun tryParseXml(xmlString: String): Document? {
        return tryParseXml(StringReader(xmlString))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun tryParseXml(xmlSource: InputSource): Document? {
        try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()

            return db.parse(xmlSource)
        } catch (e: SAXException) {
            return null
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(IOException::class)
    @JvmStatic
    fun tryParseXmlFragment(reader: Reader): DocumentFragment {
        try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(InputSource(CombiningReader(StringReader("<elem>"), reader, StringReader("</elem>"))))
            val frag = doc.createDocumentFragment()
            val docelem = doc.documentElement
            var child: Node? = docelem.firstChild
            while (child != null) {
                frag.appendChild(child)
                child = docelem.firstChild
            }
            doc.removeChild(docelem)
            return frag
        } catch (e: ParserConfigurationException) {
            throw IOException(e)
        } catch (e: SAXException) {
            throw IOException(e)
        }

    }

    @JvmStatic
    fun toString(value: Node): String {
        return toString(value, DEFAULT_FLAGS)
    }

    @JvmStatic
    fun toString(value: Node, flags: Int): String {
        val out = StringWriter()
        try {
            val t = TransformerFactory
                .newInstance()
                .newTransformer()
            configure(t, flags)
            t.transform(DOMSource(value), StreamResult(out))
        } catch (e: TransformerException) {
            throw RuntimeException(e)
        }

        return out.toString()
    }

    @JvmStatic
    fun toString(nodeList: NodeList): String {
        return toString(nodeList, DEFAULT_FLAGS)
    }

    @JvmStatic
    fun toString(nodeList: NodeList, flags: Int): String {
        val out = StringWriter()
        try {
            val t = TransformerFactory
                .newInstance()
                .newTransformer()
            configure(t, flags)
            for (i in 0 until nodeList.length) {
                t.transform(DOMSource(nodeList.item(i)), StreamResult(out))
            }
        } catch (e: TransformerException) {
            throw RuntimeException(e)
        }

        return out.toString()
    }

    @JvmStatic
    fun toDocFragment(value: NodeList?): DocumentFragment? {
        if (value == null || value.length == 0) {
            return null
        }
        val document = value.item(0).ownerDocument
        val fragment = document.createDocumentFragment()
        for (i in 0 until value.length) {
            val n = value.item(i)
            if (n.ownerDocument !== document) {
                fragment.appendChild(document.adoptNode(n.cloneNode(true)))
            } else {
                fragment.appendChild(n.cloneNode(true))
            }
        }
        return fragment
    }

    @JvmStatic
    fun childrenToDocumentFragment(input: XmlReader): DocumentFragment {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc: Document
        try {
            doc = dbf.newDocumentBuilder().newDocument()
        } catch (e: ParserConfigurationException) {
            throw XmlException(e)
        }

        val documentFragment = doc.createDocumentFragment()
        val out = XmlStreaming.newWriter(DOMResult(documentFragment), true)
        while (input.hasNext() && input.next() !== EventType.END_ELEMENT) {
            input.writeCurrent(out)
            if (input.eventType === EventType.START_ELEMENT) {
                out.writeElementContent(null, input)
            }
        }
        return documentFragment
    }

    @JvmStatic
    fun childToNode(`in`: XmlReader): Node {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc: Document
        try {
            doc = dbf.newDocumentBuilder().newDocument()
        } catch (e: ParserConfigurationException) {
            throw XmlException(e)
        }

        val documentFragment = doc.createDocumentFragment()
        val out = XmlStreaming.newWriter(DOMResult(documentFragment), true)
        `in`.writeCurrent(out)
        if (`in`.eventType === EventType.START_ELEMENT) {
            out.writeElementContent(null, `in`)
        }
        return documentFragment.firstChild
    }

    @JvmStatic
    fun nodeListToFragment(nodeList: NodeList): ICompactFragment {
        when (nodeList.length) {
            0    -> return CompactFragment("")
            1    -> {
                val node = nodeList.item(0)
                return nodeToFragment(node)
            }
            else -> return nodeToFragment(toDocFragment(nodeList))
        }
    }

    @JvmStatic
    fun nodeToFragment(node: Node?): ICompactFragment {
        if (node == null) {
            return CompactFragment("")
        } else if (node is Text) {
            return CompactFragment(node.data)
        }
        return XmlStreaming.newReader(DOMSource(node)).siblingsToFragment()
    }

    @JvmStatic
    fun toDocFragment(value: List<Node>?): DocumentFragment? {
        if (value == null || value.size == 0) {
            return null
        }
        val document = value[0].ownerDocument
        val fragment = document.createDocumentFragment()
        for (n in value) {
            if (n.ownerDocument !== document) {
                fragment.appendChild(document.adoptNode(n.cloneNode(true)))
            } else {
                fragment.appendChild(n.cloneNode(true))
            }
        }
        return fragment
    }

    /**
     * Make a QName for the given parameters.
     * @param reference The node to use to look up the namespace that corresponds to the prefix.
     * @param name This is the full name of the element. That includes the prefix (or if no colon present) the default prefix.
     * @return The QName.
     */
    @JvmStatic
    fun asQName(reference: Node, name: String): QName {
        val colPos = name.indexOf(':')
        if (colPos >= 0) {
            val prefix = name.substring(0, colPos)
            return QName(reference.lookupNamespaceURI(prefix), name.substring(colPos + 1), prefix)
        } else {
            return QName(reference.lookupNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), name, XMLConstants.NULL_NS_URI)
        }

    }

    @JvmStatic
    fun getChild(parent: Element, name: QName): Element? {
        return getFirstChild(parent, name.namespaceURI, name.localPart)
    }

    @JvmStatic
    fun getFirstChild(parent: Element, namespaceURI: String?, localName: String): Element? {
        var child = getFirstChildElement(parent)
        while (child != null) {
            if (namespaceURI == null || namespaceURI.length == 0) {
                if ((child.namespaceURI == null || child.namespaceURI.length == 0) && localName == child.localName) {
                    return child
                }
            } else {
                if (namespaceURI == child.namespaceURI && localName == child.localName) {
                    return child
                }
            }
            child = getNextSiblingElement(child)
        }
        return null
    }

    @JvmStatic
    fun getNextSibling(sibling: Element, name: QName): Element? {
        return getNextSibling(sibling, name.namespaceURI, name.localPart)
    }

    @JvmStatic
    fun getNextSibling(sibling: Element, namespaceURI: String, localName: String): Element? {
        var child = getNextSiblingElement(sibling)
        while (child != null) {
            if (namespaceURI == child.namespaceURI && localName == child.localName) {
                return child
            }
            child = getNextSiblingElement(child)
        }
        return null
    }

    /**
     * Return the first child that is an element.
     *
     * @param parent The parent element.
     * @return The first element child, or `null` if there is none.
     */
    @JvmStatic
    fun getFirstChildElement(parent: Element): Element? {
        var child: Node? = parent.firstChild
        while (child != null) {
            if (child is Element) {
                return child
            }
            child = child.nextSibling
        }
        return null
    }

    /**
     * Return the next sibling that is an element.
     *
     * @param sibling The reference element.
     * @return The next element sibling, or `null` if there is none.
     */
    @JvmStatic
    fun getNextSiblingElement(sibling: Element): Element? {
        var child: Node? = sibling.nextSibling
        while (child != null) {
            if (child is Element) {
                return child
            }
            child = child.nextSibling
        }
        return null
    }

    @JvmStatic
    fun setAttribute(element: Element, name: QName, value: String) {
        if (name.namespaceURI == null || XMLConstants.NULL_NS_URI == name.namespaceURI) {
            element.setAttribute(name.localPart, value)
        } else if (name.prefix == null || XMLConstants.DEFAULT_NS_PREFIX == name.prefix) {
            element.setAttributeNS(name.namespaceURI, name.localPart, value)
        } else {
            element.setAttributeNS(name.namespaceURI, name.prefix + ':'.toString() + name.localPart, value)
        }
    }

    @JvmStatic
    fun getPrefix(node: Node?, namespaceURI: String?): String? {
        if (node == null) {
            return null
        }
        if (node is Element) {
            val attrs = node.attributes
            for (i in 0 until attrs.length) {
                val attr = attrs.item(i) as Attr
                if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI == attr.namespaceURI && attr.value == namespaceURI) {
                    return attr.name
                }
            }
        }
        val prefix = getPrefix(node.parentNode, namespaceURI)
        if (node.hasAttributes() && prefix != null) {
            if (prefix.isEmpty()) {
                if (node.attributes.getNamedItem(XMLConstants.XMLNS_ATTRIBUTE) != null) {
                    return null
                }
            } else {
                if (node.attributes.getNamedItemNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix) != null) {
                    return null
                }
            }
        }
        return prefix
    }

    internal fun configure(transformer: Transformer, flags: Int) {
        if (flags and FLAG_OMIT_XMLDECL != 0) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }
    }
}