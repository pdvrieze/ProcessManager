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
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.*
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node

actual abstract class XPathHolder actual constructor(
    name: String?,
    path: String?,
    content: CharArray?,
    originalNSContext: IterableNamespaceContext
) : XMLContainer(pathNamespaces(originalNSContext, path), content ?: CharArray(0)) {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    actual var _name: String? = name

    //  @Volatile private var path: XPathExpression? = null // This is merely a cache.
    private val pathString: String? = path

/*
    // TODO support a functionresolver
    @Volatile
    private var path: XPathExpression? = null
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

    @OptIn(XmlUtilInternal::class)
    actual constructor() : this(null, null, null, SimpleNamespaceContext())

    actual fun getName() = _name ?: throw NullPointerException("Name not set")

    actual fun setName(value: String) {
        _name = value
    }

    actual fun getPath(): String? {
        return pathString
    }


    @OptIn(XmlUtilInternal::class)
    fun serializeAttributes(out: XmlWriter) {
        // No attributes by default
        if (pathString != null) {
            val namepaces = mutableMapOf<String, String>()
            // Have a namespace that gathers those namespaces that are not known already in the outer context
            val referenceContext = out.namespaceContext
            // TODO streamline this, the right context should not require the filtering on the output context later.
            val nsc = MyGatheringNamespaceContext(namepaces, referenceContext, SimpleNamespaceContext.from(originalNSContext))
            visitXpathUsedPrefixes(pathString, nsc)
            for ((key, value) in namepaces) {
                if (value != referenceContext.getNamespaceURI(key)) {
                    out.namespaceAttr(key, value)
                }
            }
            out.attribute(null, "xpath", null, pathString!!)

        }
        out.writeAttribute("name", _name)
    }

    fun visitNamespaces(baseContext: NamespaceContext) {
        if (pathString != null) {
            visitXpathUsedPrefixes(pathString, baseContext)
        }
    }


    private val namespaceResolver: NamespaceResolver = { prefix -> namespaces.getNamespaceURI(prefix) }

    fun applyData(payload: Node?): ProcessData = pathString.let { p ->
        when (p) {
            null -> ProcessData(_name!!, payload?.let { CompactFragment(it) } ?: CompactFragment(""))
            else -> {
                val realPayload = when (payload) {
                    null -> {
                        document.implementation.createDocument(null, "dummy").createDocumentFragment()
                    }
                    else -> payload
                }
                val result = realPayload.ownerDocument!!.evaluate(
                    p, realPayload, namespaceResolver,
                    XPathResult.ORDERED_NODE_ITERATOR_TYPE
                                                                 )
                ProcessData(_name!!, CompactFragment(result.toDocumentFragment()))
            }
        }
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        if (!super.equals(other)) return false

        other as XPathHolder

        if (_name != other._name) return false
        if (pathString != other.pathString) return false

        return true
    }

    actual override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (_name?.hashCode() ?: 0)
        result = 31 * result + (pathString?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun pathNamespaces(namespaceContext: IterableNamespaceContext, value: String?): Iterable<Namespace> {
            val p = value?: return namespaceContext
            val result = mutableMapOf<String, String>()
            val gatheringNamespaceContext = MyGatheringNamespaceContext(result, namespaceContext)
            visitXpathUsedPrefixes(p, gatheringNamespaceContext)

            return result.entries.map { (p, u) -> XmlEvent.NamespaceImpl(p, u)}
        }
    }

}


internal actual fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext) {
    if (path != null && path.isNotEmpty()) {
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
