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

import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

actual abstract class XPathHolder actual constructor(
    actual val name: String,
    actual val path: String?,
    content: ICompactFragment,
) {

    actual val content: CompactFragment = CompactFragment(content)

    // TODO support a functionresolver
    @Volatile
    private var _path: XPathExpression? = null
        get() {
            field?.let { return it }
            return if (path == null) {
                return SELF_PATH
            } else {
                XPathFactory.newInstance().newXPath().apply {
                    if (this@XPathHolder.content.namespaces.iterator().hasNext()) {
                        namespaceContext = this@XPathHolder.content.namespaces
                    }
                }.compile(path)
            }.apply { field = this }
        }

    val xPath: XPathExpression? get() = _path


    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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

    companion object {

        private val SELF_PATH: XPathExpression

        init {
            try {
                SELF_PATH = XPathFactory.newInstance().newXPath().compile(".")
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            }

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
