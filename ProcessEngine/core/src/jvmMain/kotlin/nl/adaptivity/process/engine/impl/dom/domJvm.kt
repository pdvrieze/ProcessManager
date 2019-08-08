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

package nl.adaptivity.process.engine.impl.dom

import nl.adaptivity.process.engine.impl.Result
import nl.adaptivity.process.engine.impl.Source
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.util.ICompactFragment
import javax.xml.namespace.QName
import javax.xml.xpath.XPathFactory

actual typealias DOMResult = javax.xml.transform.dom.DOMResult
actual typealias DOMSource = javax.xml.transform.dom.DOMSource
actual typealias NodeList = org.w3c.dom.NodeList
actual typealias Node = org.w3c.dom.Node
actual typealias CharacterData = org.w3c.dom.CharacterData
actual typealias Text = org.w3c.dom.Text
actual typealias Element = org.w3c.dom.Element
actual typealias Document = org.w3c.dom.Document
actual typealias DocumentFragment = org.w3c.dom.DocumentFragment
actual typealias DocumentBuilder = javax.xml.parsers.DocumentBuilder
actual typealias DocumentBuilderFactory = javax.xml.parsers.DocumentBuilderFactory
actual fun newDocumentBuilderFactory() = javax.xml.parsers.DocumentBuilderFactory.newInstance()

actual typealias InputSource = org.xml.sax.InputSource
actual typealias XPathExpression = javax.xml.xpath.XPathExpression
actual typealias XPath = javax.xml.xpath.XPath
actual fun newXPath(): XPath = XPathFactory.newInstance().newXPath()

actual fun Result.newWriter() = XmlStreaming.newWriter(this)
actual fun Source.newReader() = XmlStreaming.newReader(this)

actual object XPathConstants {
    actual val NUMBER: QName = javax.xml.xpath.XPathConstants.NUMBER
    actual val STRING: QName = javax.xml.xpath.XPathConstants.STRING
    actual val BOOLEAN: QName = javax.xml.xpath.XPathConstants.BOOLEAN
    actual val NODESET: QName = javax.xml.xpath.XPathConstants.NODESET
    actual val NODE: QName = javax.xml.xpath.XPathConstants.NODE
    actual val DOM_OBJECT_MODEL: String = javax.xml.xpath.XPathConstants.DOM_OBJECT_MODEL
}

actual fun NodeList.toFragment(): ICompactFragment = DomUtil.nodeListToFragment(this)
actual fun Node.toFragment(): ICompactFragment = DomUtil.nodeToFragment(this)
actual fun ICompactFragment.toDocumentFragment(): DocumentFragment = DomUtil.childrenToDocumentFragment(getXmlReader())
