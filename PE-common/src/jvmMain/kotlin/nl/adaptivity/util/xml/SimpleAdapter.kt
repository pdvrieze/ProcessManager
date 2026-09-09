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

package nl.adaptivity.util.xml

import nl.adaptivity.util.kotlin.arrayMap
import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlUtilInternal
import org.w3c.dom.Attr
import org.w3c.dom.NamedNodeMap
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.bind.JAXBElement
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 11/04/16.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
class SimpleAdapter {

    internal var name: QName? = null

    @OptIn(XmlUtilInternal::class)
    internal var namespaceContext: IterableNamespaceContext = SimpleNamespaceContext()
        private set

    @XmlAnyAttribute
    internal val attributes: MutableMap<QName, Any> = HashMap()

    @XmlAnyElement(lax = true, value = W3CDomHandler::class)
    internal val children: MutableList<Any> = ArrayList()

    fun getAttributes(): Map<QName, Any> {
        return attributes
    }

    fun setAttributes(attributes: NamedNodeMap) {
        for (i in attributes.length - 1 downTo 0) {
            val attr = attributes.item(i) as Attr
            val prefix = attr.prefix
            if (prefix == null) {
                this.attributes[QName(attr.localName)] = attr.value
            } else if (XMLConstants.XMLNS_ATTRIBUTE != prefix) {
                this.attributes[QName(attr.namespaceURI, attr.localName, prefix)] = attr.value
            }
        }
    }

    @OptIn(XmlUtilInternal::class)
    fun beforeUnmarshal(unmarshaller: Unmarshaller, parent: Any) {
        if (parent is JAXBElement<*>) {
            name = parent.name
        }

        if (_failedReflection) {
            return
        }
        val context: Any?
        try {

            val getContext = _getContext
            if (getContext == null) {
                synchronized(javaClass) {
                    val mh = MethodHandles.lookup().unreflect(unmarshaller.javaClass.getMethod("getContext"))
                    _getContext = mh

                    context = mh.invoke(unmarshaller) as Any

                    _getAllDeclaredPrefixes = MethodHandles.lookup().unreflect(context.javaClass.getMethod("getAllDeclaredPrefixes"))
                    _getNamespaceURI = MethodHandles.lookup().unreflect(context.javaClass.getMethod("getNamespaceURI", String::class.java))

                }
            } else {
                context = getContext(unmarshaller)
            }

            if (context != null) {
                @Suppress("UNCHECKED_CAST")
                val prefixes = _getAllDeclaredPrefixes(context) as Array<String>
                if (prefixes.isNotEmpty()) {
                    val namespaces = prefixes.arrayMap { Companion._getNamespaceURI(context, it) as String }
                    namespaceContext = SimpleNamespaceContext(prefixes, namespaces)
                }
            }

        } catch (e: Throwable) {
            Logger.getAnonymousLogger().log(Level.FINE, "Could not retrieve namespace context from marshaller", e)
            _failedReflection = true
        }

    }

    companion object {

        @Volatile
        private var _getContext: MethodHandle? = null
        @Volatile
        private var _failedReflection = false
        private lateinit var _getAllDeclaredPrefixes: MethodHandle
        private lateinit var _getNamespaceURI: MethodHandle
    }
}
