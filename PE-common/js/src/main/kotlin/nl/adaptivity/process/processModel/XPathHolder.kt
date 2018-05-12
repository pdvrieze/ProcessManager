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

import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.CombiningNamespaceContext
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.*
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import kotlin.browser.document

actual abstract class XPathHolder : XMLContainer {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    actual var _name: String? = null

    //  @Volatile private var path: XPathExpression? = null // This is merely a cache.
    private var pathString: String? = null

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

    actual constructor() : super()

    actual constructor(name: String?,
                       path: String?,
                       content: CharArray?,
                       originalNSContext: Iterable<Namespace>) : super(originalNSContext, content ?: CharArray(0)) {
        _name = name
        setPath(originalNSContext, path)
    }

    actual fun getName() = _name ?: throw NullPointerException("Name not set")

    actual fun setName(value: String) {
        _name = value
    }

    actual fun getPath(): String? {
        return pathString
    }

    actual fun setPath(namespaceContext: Iterable<Namespace>, value: String?) {
        if (pathString != null && pathString == value) {
            return
        }
        pathString = value
        updateNamespaceContext(namespaceContext)
        assert(value == null)
    }

    actual override fun deserializeAttribute(attributeNamespace: String?,
                                             attributeLocalName: String,
                                             attributeValue: String) =
        when (attributeLocalName) {
            "name"                       -> {
                _name = attributeValue
                true
            }

            "path", "xpath"              -> {
                pathString = attributeValue
                true
            }

            XMLConstants.XMLNS_ATTRIBUTE -> true

            else                         -> false
        }

    actual
    override fun deserializeChildren(reader: XmlReader) {
        super.deserializeChildren(reader)
        val namespaces = mutableMapOf<String, String>()
        val gatheringNamespaceContext = CombiningNamespaceContext(SimpleNamespaceContext.from(originalNSContext),
                                                                  GatheringNamespaceContext(
                                                                      reader.namespaceContext, namespaces))
        visitNamespaces(gatheringNamespaceContext)
        if (namespaces.size > 0) {
            addNamespaceContext(SimpleNamespaceContext(namespaces))
        }
    }

    actual override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        if (pathString != null) {
            val namepaces = mutableMapOf<String, String>()
            // Have a namespace that gathers those namespaces that are not known already in the outer context
            val referenceContext = out.namespaceContext
            // TODO streamline this, the right context should not require the filtering on the output context later.
            val nsc = GatheringNamespaceContext(
                CombiningNamespaceContext(referenceContext, SimpleNamespaceContext
                    .from(originalNSContext)), namepaces)
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

    actual override fun visitNamespaces(baseContext: NamespaceContext) {
        if (pathString != null) {
            visitXpathUsedPrefixes(pathString, baseContext)
        }
        super.visitNamespaces(baseContext)
    }

    actual override fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                                   owner: QName,
                                                   attributeName: QName,
                                                   attributeValue: CharSequence) {
        if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
            visitXpathUsedPrefixes(attributeValue, referenceContext)
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
                val result = realPayload.ownerDocument!!.evaluate(p, realPayload, namespaceResolver,
                                                                  XPathResult.ORDERED_NODE_ITERATOR_TYPE)
                ProcessData(_name!!, CompactFragment(result.toDocumentFragment()))
            }
        }
    }


    actual companion object {


        @JvmStatic
        actual fun <T : XPathHolder> deserialize(reader: XmlReader, result: T): T {
            return result.deserializeHelper(reader)
        }

        protected actual fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext) {
            if (path != null && path.isNotEmpty()) {
                try {
                    val d = document.implementation.createDocument(null, "bar")
//                    d.createExpression(path.toString(), { prefix -> namespaceContext.getNamespaceURI(prefix) } )
                    d.evaluate(path.toString(), d, { prefix -> namespaceContext.getNamespaceURI(prefix) },
                               XPathResult.ANY_TYPE)
                } catch (e: Exception) {
                    console.warn("The path used is not valid ($path) - ${e.message}", e)
                }

            }
        }
    }
}

typealias NamespaceResolver = (String) -> String?

@Suppress("UnsafeCastFromDynamic")
inline fun Document.createExpression(xpathText: String,
                                     noinline namespaceUrlMapper: NamespaceResolver? = null): XPathExpression = asDynamic().createExpression(
    xpathText, namespaceUrlMapper)

@Suppress("UnsafeCastFromDynamic")
inline fun Document.evaluate(xpathExpression: String,
                             contextNode: Node,
                             noinline namespaceResolver: NamespaceResolver?,
                             resultType: Short): XPathResult =
    asDynamic().evaluate(xpathExpression, contextNode, namespaceResolver, resultType, null)

@Suppress("UnsafeCastFromDynamic")
inline fun Document.evaluate(xpathExpression: String,
                             contextNode: Node,
                             noinline namespaceResolver: NamespaceResolver?,
                             resultType: Short,
                             result: XPathResult): Unit =
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