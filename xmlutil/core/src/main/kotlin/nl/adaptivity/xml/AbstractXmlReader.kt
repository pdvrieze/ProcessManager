/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import net.devrieze.util.kotlin.asString
import net.devrieze.util.kotlin.matches
import nl.adaptivity.xml.AbstractXmlReader.Companion.toQname
import javax.xml.namespace.QName
import nl.adaptivity.xml.XmlStreaming.EventType


/**
 * Created by pdvrieze on 16/11/15.
 */

abstract class AbstractXmlReader : XmlReader {

  companion object {
    @JvmStatic

    fun CharSequence.toQname(): QName {
      val split = indexOf('}')
      val localname: String
      val nsUri: String?
      if (split >= 0) {
        if (this[0] != '{') throw IllegalArgumentException("Not a valid qname literal")
        nsUri = substring(1, split)
        localname = substring(split + 1)
      } else {
        nsUri = null
        localname = toString()
      }
      return QName(nsUri, localname)
    }
  }

  @Throws(XmlException::class)
  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?) {
    if (eventType !== type) {
      throw XmlException("Unexpected event type Found: $eventType expected $type")
    }
    if (namespace != null) {
      if (!(namespace matches namespaceUri)) {
        throw XmlException("Namespace uri's don't match: expected=$namespace found=$namespaceUri")
      }
    }
    if (name != null) {
      if (!(name matches localName)) {
        throw XmlException("Local names don't match: expected=$name found=$localName")
      }
    }
  }

  @Throws(XmlException::class)
  override fun isEndElement() = eventType === EventType.END_ELEMENT

  @Throws(XmlException::class)
  override fun isCharacters() = eventType === EventType.TEXT

  @Throws(XmlException::class)
  override fun isStartElement(): Boolean = eventType === EventType.START_ELEMENT

  @Throws(XmlException::class)
  override fun isWhitespace() = eventType === EventType.IGNORABLE_WHITESPACE || eventType === EventType.TEXT && isXmlWhitespace(text)

  override val name:QName
    @Throws(XmlException::class)
    get() = qname(namespaceUri, localName, prefix)

  @Throws(XmlException::class)
  override fun getAttributeName(i: Int): QName =
        qname(getAttributeNamespace(i), getAttributeLocalName(i), getAttributePrefix(i))
}
