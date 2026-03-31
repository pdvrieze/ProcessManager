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

import kotlinx.browser.document
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node

actual abstract class XPathHolder actual constructor(
    actual val name: String,
    actual val path: String?,
    content: ICompactFragment,
) {

    actual val content: CompactFragment = CompactFragment(content)

/*
    // TODO support a functionresolver
    @Volatile
    private var _path: XPathExpression? = null
        get() {
            field?.let { return it }
            return if (pathString == null) {
                return SELF_PATH
            } else {
                XPathFactory.newInstance().newXPath().apply {
                    if (originalNSContext != null) {
                        namespaceContext = SimpleNamespaceContext.from(originalNSContext)
                    }
                }.compile(pathString)
            }.apply { field = this }
        }

    val xPath: XPathExpression? get() = path
*/

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as XPathHolder

        if (name != other.name) return false
        if (path != other.path) return false
        if (this@XPathHolder.content != other.content) return false

        return true
    }

    actual override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + this@XPathHolder.content.hashCode()
        return result
    }


}


internal actual fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext) {
    if (! path.isNullOrEmpty()) {
        try {
            val d = document.implementation.createDocument(null, "bar")
//                    d.createExpression(path.toString(), { prefix -> namespaceContext.getNamespaceURI(prefix) } )
            d.evaluate(
                path.toString(), d, { prefix -> namespaceContext.getNamespaceURI(prefix) },
                XPathResult.ANY_TYPE
                      )
        } catch (e: Exception) {
            console.warn("The path used is not valid ($path) - ${e.message}", e)
        }

    }
}


typealias NamespaceResolver = (String) -> String?

@Suppress("UnsafeCastFromDynamic")
inline fun Document.createExpression(
    xpathText: String,
    noinline namespaceUrlMapper: NamespaceResolver? = null
                                    ): XPathExpression = asDynamic().createExpression(
    xpathText, namespaceUrlMapper
                                                                                     )

@Suppress("UnsafeCastFromDynamic")
inline fun Document.evaluate(
    xpathExpression: String,
    contextNode: Node,
    noinline namespaceResolver: NamespaceResolver?,
    resultType: Short
                            ): XPathResult =
    asDynamic().evaluate(xpathExpression, contextNode, namespaceResolver, resultType, null)

@Suppress("UnsafeCastFromDynamic")
inline fun Document.evaluate(
    xpathExpression: String,
    contextNode: Node,
    noinline namespaceResolver: NamespaceResolver?,
    resultType: Short,
    result: XPathResult
                            ): Unit =
    asDynamic().evaluate(xpathExpression, contextNode, namespaceResolver, resultType, result)

external interface XPathExpression {
    fun evaluate(contextNode: Node, type: Short, result: Nothing?): XPathResult
    fun evaluate(contextNode: Node, type: Short, result: XPathResult)
}

external class XPathResult {

    val booleanValue: Boolean
    val invalidIteratorState: Boolean
    val numberValue: Float
    val resultType: Short
    val singleNodeValue: Node?
    val snapshotLength: Int
    val stringValue: String

    fun iterateNext(): Node?

    fun snapshotItem(index: Int): Node

    companion object {
        val ANY_TYPE: Short = definedExternally// = 0
        val NUMBER_TYPE: Short = definedExternally// = 1
        val STRING_TYPE: Short = definedExternally// = 2
        val BOOLEAN_TYPE: Short = definedExternally// = 3
        val UNORDERED_NODE_ITERATOR_TYPE: Short = definedExternally// = 4
        val ORDERED_NODE_ITERATOR_TYPE: Short = definedExternally// = 5
        val UNORDERED_NODE_SNAPSHOT_TYPE: Short = definedExternally// = 6
        val ORDERED_NODE_SNAPSHOT_TYPE: Short = definedExternally// = 7
        val ANY_UNORDERED_NODE_TYPE: Short = definedExternally// = 8
        val FIRST_ORDERED_NODE_TYPE: Short = definedExternally// = 9
    }
}

fun XPathResult.toDocumentFragment(): DocumentFragment {
    when (resultType) {
        XPathResult.BOOLEAN_TYPE               -> return document.implementation.createDocument(null, "foo").let { d ->
            d.createDocumentFragment().apply { append(d.createTextNode(booleanValue.toString())) }
        }
        XPathResult.STRING_TYPE                -> return document.implementation.createDocument(null, "foo").let { d ->
            d.createDocumentFragment().apply { textContent?.let { t -> append(d.createTextNode(t)) } }
        }
        XPathResult.NUMBER_TYPE                -> return document.implementation.createDocument(null, "foo").let { d ->
            d.createDocumentFragment().apply { append(d.createTextNode(numberValue.toString())) }
        }
        XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
        XPathResult.ORDERED_NODE_ITERATOR_TYPE -> iterateNext()?.let { first ->
            val df = first.ownerDocument!!.createDocumentFragment()
            var node: Node? = first
            while (node != null) {
                df.append(node)
                node = iterateNext()
            }
            return df
        }
        XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE,
        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE -> if (snapshotLength > 0) {
            val df = snapshotItem(0).ownerDocument!!.createDocumentFragment()
            for (i in 0 until snapshotLength) {
                df.append(snapshotItem(i))
            }
            return df
        }
        XPathResult.ANY_UNORDERED_NODE_TYPE,
        XPathResult.FIRST_ORDERED_NODE_TYPE    -> singleNodeValue?.let { n ->
            return snapshotItem(0).ownerDocument!!.createDocumentFragment().apply { append(n) }
        }
    }
    return document.implementation.createDocument(null, "foo").createDocumentFragment()
}
