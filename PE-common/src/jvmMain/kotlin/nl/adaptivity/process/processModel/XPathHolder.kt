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
import nl.adaptivity.util.MyGatheringNamespaceContext
import nl.adaptivity.xmlutil.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

actual abstract class XPathHolder actual constructor(
    name: String?,
    path: String?,
    content: CharArray?,
    originalNSContext: IterableNamespaceContext,
) : XMLContainer(originalNSContext.freeze(), content ?: CharArray(0)) {
    /**
     * @see nl.adaptivity.process.processModel.IXmlResultType#setName(java.lang.String)
     */
    actual var _name: String? = name

    //  @Volatile private var path: XPathExpression? = null // This is merely a cache.
    private val pathString: String? = path

    // TODO support a functionresolver
    @Volatile
    private var path: XPathExpression? = null
        get() {
            field?.let { return it }
            return if (pathString == null) {
                return SELF_PATH
            } else {
                XPathFactory.newInstance().newXPath().apply {
                    if (originalNSContext.iterator().hasNext()) {
                        namespaceContext = originalNSContext
                    }
                }.compile(pathString)
            }.apply { field = this }
        }

    val xPath: XPathExpression? get() = path

    @OptIn(XmlUtilInternal::class)
    actual constructor() : this(null, null, null, SimpleNamespaceContext())

    actual fun getName() = _name ?: throw NullPointerException("Name not set")

    actual fun setName(value: String) {
        _name = value
    }

    @XmlAttribute(name = "xpath")
    actual fun getPath(): String? {
        return pathString
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
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

        private val SELF_PATH: XPathExpression

        init {
            try {
                SELF_PATH = XPathFactory.newInstance().newXPath().compile(".")
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            }

        }

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
    if (! path.isNullOrEmpty()) {
        try {
            val xpf = XPathFactory.newInstance()
            val xpath = xpf.newXPath()
            xpath.namespaceContext = namespaceContext
            xpath.compile(path.toString())
        } catch (e: XPathExpressionException) {
            Logger.getLogger(XPathHolder::class.java.simpleName).log(
                Level.WARNING,
                "The path used is not valid (" + path + ") - " + e.message,
                e
            )
        }

    }
}
