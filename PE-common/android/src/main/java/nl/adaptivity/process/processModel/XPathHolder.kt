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

import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.xml.CombiningNamespaceContext
import nl.adaptivity.xml.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

actual abstract class XPathHolder : XMLContainer {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    actual var _name: String? = null

    //  @Volatile private var path: XPathExpression? = null // This is merely a cache.
    private var pathString: String? = null

    // TODO support a functionresolver
    @Volatile
    @Transient
    private var path: XPathExpression? = null
        get() {
            field?.let { return it }
            return if (pathString == null) {
                return SELF_PATH
            } else {
                XPathFactory.newInstance().newXPath().apply {
                    if (originalNSContext.iterator().hasNext()) {
                        namespaceContext = SimpleNamespaceContext.from(originalNSContext)
                    }
                }.compile(pathString)
            }.apply { field = this }
        }

    val xPath: XPathExpression? get() = path

    actual constructor() : super()

    actual constructor(name: String?,
                       path: String?,
                       content: CharArray?,
                       originalNSContext: Iterable<Namespace>) : super(originalNSContext, content ?: CharArray(0)) {
        _name = name
        pathString = path
    }

    actual fun getName() = _name ?: throw NullPointerException("Name not set")

    actual fun setName(value:String) { _name = value }

    actual fun getPath(): String? {
        return pathString
    }

    @Deprecated("This should be immutable")
    actual fun setPath(namespaceContext: Iterable<Namespace>, value: String?) {
        if (pathString != null && pathString == value) {
            return
        }
        path = null
        pathString = value
        @Suppress("DEPRECATION")
        updateNamespaceContext(namespaceContext)
        assert(value == null || xPath != null)
    }

    @Deprecated("")
    fun setNamespaceContext(namespaceContext: Iterable<Namespace>) {
        setContent(namespaceContext, content)

        path = null // invalidate the cached path expression
    }

    actual override fun deserializeAttribute(attributeNamespace: String?, attributeLocalName: String, attributeValue: String): Boolean {
        when (attributeLocalName) {
            "name"                       -> {
                _name = attributeValue
                return true
            }
            "path", "xpath"              -> {
                pathString = attributeValue
                return true
            }
            XMLConstants.XMLNS_ATTRIBUTE -> return true
            else                         -> return false
        }
    }

    @Throws(XmlException::class)
    actual override fun deserializeChildren(reader: XmlReader) {

        super.deserializeChildren(reader)
        val namespaces = TreeMap<String, String>()
        val gatheringNamespaceContext = CombiningNamespaceContext(SimpleNamespaceContext.from(originalNSContext),
                                                                  GatheringNamespaceContext(
                                                                      reader.namespaceContext, namespaces))
        visitNamespaces(gatheringNamespaceContext)
        if (namespaces.size > 0) {
            addNamespaceContext(SimpleNamespaceContext(namespaces))
        }
    }

    @Throws(XmlException::class)
    actual override fun serializeAttributes(out: XmlWriter) {
        super.serializeAttributes(out)
        if (pathString != null) {
            val namepaces = TreeMap<String, String>()
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

    @Throws(XmlException::class)
    actual override fun visitNamespaces(baseContext: NamespaceContext) {
        path = null
        if (pathString != null) {
            visitXpathUsedPrefixes(pathString, baseContext)
        }
        super.visitNamespaces(baseContext)
    }

    actual override fun visitNamesInAttributeValue(referenceContext: NamespaceContext, owner: QName, attributeName: QName, attributeValue: CharSequence) {
        if (Constants.MODIFY_NS_STR == owner.getNamespaceURI() && (XMLConstants.NULL_NS_URI == attributeName.getNamespaceURI() || XMLConstants.DEFAULT_NS_PREFIX == attributeName.getPrefix()) && "xpath" == attributeName.getLocalPart()) {
            visitXpathUsedPrefixes(attributeValue, referenceContext)
        }
    }

    actual companion object {

        private val SELF_PATH: XPathExpression

        init {
            try {
                SELF_PATH = XPathFactory.newInstance().newXPath().compile(".")
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            }

        }

        @JvmStatic
        @Throws(XmlException::class)
        actual fun <T : XPathHolder> deserialize(reader: XmlReader, result: T): T {
            return result.deserializeHelper(reader)
        }
    }
}


internal actual fun visitXpathUsedPrefixes(path: CharSequence?, namespaceContext: NamespaceContext) {
    if (path != null && path.isNotEmpty()) {
        try {
            val xpf = XPathFactory.newInstance()
            val xpath = xpf.newXPath()
            xpath.namespaceContext = namespaceContext
            xpath.compile(path.toString())
        } catch (e: XPathExpressionException) {
            Logger.getLogger(XPathHolder::class.java.simpleName).log(Level.WARNING, "The path used is not valid (" + path + ") - " + e.message, e)
        }

    }
}
