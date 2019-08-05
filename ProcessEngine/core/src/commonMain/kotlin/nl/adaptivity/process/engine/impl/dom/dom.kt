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
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.ICompactFragment

expect class DOMResult : Result {
    constructor(node: Node)
}
expect fun Result.newWriter(): XmlWriter

expect fun Source.newReader(): XmlReader
expect class DOMSource : Source {
    constructor(node: Node)
}
expect interface NodeList {
    fun getLength(): Int
}

expect interface Text : CharacterData
expect interface CharacterData : Node
expect interface Node {
    fun getOwnerDocument(): Document
    fun getFirstChild(): Node?
    fun getNextSibling(): Node?
    fun appendChild(child: Node): Node
}

expect interface Element : Node
expect interface Document {
    fun createTextNode(text: String): Text
    fun createDocumentFragment(): DocumentFragment
    fun createElement(tagname: String): Element
    fun adoptNode(source: Node): Node
}

expect interface DocumentFragment : Node {
}

expect abstract class DocumentBuilder {
    abstract fun newDocument(): Document
}

expect abstract class DocumentBuilderFactory {
    fun isNamespaceAware(): Boolean
    fun setNamespaceAware(value: Boolean)
    abstract fun newDocumentBuilder(): DocumentBuilder
}

var DocumentBuilderFactory.isNamespaceAware
    inline get() = isNamespaceAware()
    inline set(value) {
        setNamespaceAware(value)
    }

expect fun newDocumentBuilderFactory(): DocumentBuilderFactory

expect class InputSource
expect interface XPathExpression {
    fun evaluate(source: Any, returnType: QName):Any
    fun evaluate(source: InputSource, returnType: QName):Any
}
expect interface XPath {
    fun getNamespaceContext(): NamespaceContext
    fun setNamespaceContext(context: NamespaceContext)
    fun compile(expression: String): XPathExpression
}

expect fun newXPath(): XPath

expect object XPathConstants {
    val NUMBER: QName
    val STRING: QName
    val BOOLEAN: QName
    val NODESET: QName
    val NODE: QName
    val DOM_OBJECT_MODEL: String
}

expect fun NodeList.toFragment(): ICompactFragment
