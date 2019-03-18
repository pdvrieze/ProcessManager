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

import nl.adaptivity.util.xml.SimpleAdapter
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlStreaming
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.dom.DOMResult


/**
 * Created by pdvrieze on 11/04/16.
 */
open class JAXBAdapter : XmlAdapter<SimpleAdapter, XmlSerializable>() {

    @Throws(Exception::class)
    override fun unmarshal(v: SimpleAdapter): XmlSerializable {
        throw UnsupportedOperationException()
    }

    @Throws(Exception::class)
    override fun marshal(v: XmlSerializable): SimpleAdapter {


        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val document = dbf.newDocumentBuilder().newDocument()
        val content = document.createDocumentFragment()
        val xof = XMLOutputFactory.newFactory()
        val out = xof.createXMLStreamWriter(DOMResult(content))

        v.serialize(XmlStreaming.newWriter(DOMResult(content)))
        val childCount = content.childNodes.length
        if (childCount == 0) {
            return SimpleAdapter()
        } else if (childCount == 1) {
            val result = SimpleAdapter()
            val child = content.firstChild
            if (child is Element) {
                result.setAttributes(child.getAttributes())
                var child2: Node? = child.getFirstChild()
                while (child2 != null) {
                    result.children.add(child2)
                    child2 = child2.nextSibling
                }
            } else {
                result.children.add(child)
            }
            return result
        } else { // More than one child
            val result = SimpleAdapter()
            var child: Node? = content.firstChild
            while (child != null) {
                result.children.add(child)
                child = child.nextSibling
            }
            return result
        }
    }
}
