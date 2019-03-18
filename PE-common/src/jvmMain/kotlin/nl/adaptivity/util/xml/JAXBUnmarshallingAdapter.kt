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

import nl.adaptivity.util.DomUtil
import nl.adaptivity.util.JAXBAdapter
import nl.adaptivity.xmlutil.XmlDeserializer
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlSerializable
import nl.adaptivity.xmlutil.XmlStreaming
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource


/**
 * Created by pdvrieze on 11/04/16.
 */
class JAXBUnmarshallingAdapter<T : XmlSerializable>(targetType: Class<T>) : JAXBAdapter() {

    private val factory: XmlDeserializerFactory<T>

    init {
        val factoryTypeAnn = targetType.getAnnotation(XmlDeserializer::class.java)
                             ?: throw IllegalArgumentException("For unmarshalling with this adapter to work, the type ${targetType.name} must have the ${XmlDeserializer::class.java.name} annotation")
        try {
            @Suppress("UNCHECKED_CAST")
            this.factory = factoryTypeAnn.value.java.newInstance() as XmlDeserializerFactory<T>
        } catch (e: InstantiationException) {
            throw IllegalArgumentException("The factory must have a visible no-arg constructor", e)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException("The factory must have a visible no-arg constructor", e)
        }

    }

    @Throws(Exception::class)
    override fun unmarshal(v: SimpleAdapter): T {
        try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val document = dbf.newDocumentBuilder().newDocument()

            val outerName = if (v.name == null) QName("value") else v.name
            val root: Element
            root = DomUtil.createElement(document, outerName!!)


            val sourceNamespaceContext = v.namespaceContext


            for (i in sourceNamespaceContext.size - 1 downTo 0) {
                val prefix = sourceNamespaceContext.getPrefix(i)
                val namespace = sourceNamespaceContext.getNamespaceURI(i)
                if (!// Not null namespace
                        // or xml mPrefix
                        (XMLConstants.NULL_NS_URI == namespace ||
                         XMLConstants.XML_NS_PREFIX == prefix ||
                         XMLConstants.XMLNS_ATTRIBUTE == prefix)) { // or xmlns mPrefix

                }

                if (XMLConstants.DEFAULT_NS_PREFIX == prefix) { // Set the default namespace, unless it is the null namespace
                    if (XMLConstants.NULL_NS_URI != namespace) {
                        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", namespace)
                    }
                } else if (XMLConstants.XMLNS_ATTRIBUTE != prefix) { // Bind the mPrefix, except for xmlns itself
                    root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:$prefix", namespace)
                }
            }


            for ((key, value) in v.attributes) {
                DomUtil.setAttribute(root, key, value as String)
            }
            for (child in v.children) {
                if (child is Node) {
                    root.appendChild(document.importNode(child, true))
                }
            }
            val reader = XmlStreaming.newReader(DOMSource(root))
            reader.nextTag()

            return factory.deserialize(reader)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

    }

}
