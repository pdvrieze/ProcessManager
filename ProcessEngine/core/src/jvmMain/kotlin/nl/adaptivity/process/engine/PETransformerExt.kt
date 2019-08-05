/*
 * Copyright (c) 2019.
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

import nl.adaptivity.process.engine.impl.JAXBmarshal
import nl.adaptivity.process.engine.impl.dom.*
import nl.adaptivity.process.engine.impl.getClass
import nl.adaptivity.xmlutil.XmlStreaming
import javax.xml.bind.JAXBElement
import javax.xml.transform.Result
import javax.xml.transform.Source

fun PETransformer.transform(source: Source, result: Result) {
    transform(XmlStreaming.newReader(source), XmlStreaming.newWriter(result, true))
}


fun PETransformer.transform(node: Node): DocumentFragment? {
    val dbf = newDocumentBuilderFactory()
    val document: Document
    document = dbf.newDocumentBuilder().newDocument()
    val fragment = document.createDocumentFragment()
    val result = DOMResult(fragment)
    transform(DOMSource(node), result)
    return fragment
}


fun PETransformer.transform(content: List<*>): List<Node> {
    var document: Document? = null
    val result = ArrayList<Node>(content.size)
    for (obj in content) {
        if (obj is CharSequence) {
            if (document == null) {
                val dbf = newDocumentBuilderFactory()
                dbf.isNamespaceAware = true
                document = dbf.newDocumentBuilder().newDocument()!!
            }
            result.add(document.createTextNode(obj.toString()))
        } else if (obj is Node) {
            if (document == null) {
                document = obj.getOwnerDocument()
            }
            val v = transform(obj)
            if (v != null) {
                result.add(v)
            }
        } else if (obj is JAXBElement<*>) {
            if (document == null) {
                val dbf = newDocumentBuilderFactory()
                dbf.isNamespaceAware = true
                document = dbf.newDocumentBuilder().newDocument()!!
            }
            val jbe = obj as JAXBElement<*>?
            val df = document.createDocumentFragment()
            val domResult = DOMResult(df)
            JAXBmarshal(jbe!!, domResult)
            var n: Node? = df.getFirstChild()
            while (n != null) {
                val v = transform(n)
                if (v != null) {
                    result.add(v)
                }
                n = n.getNextSibling()
            }
        } else if (obj != null) {
            throw IllegalArgumentException(
                "The node " + obj.toString() + " of type ${obj.getClass()} is not understood")
        }
    }
    return result
}
