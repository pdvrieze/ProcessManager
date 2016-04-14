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

import net.devrieze.util.kotlin.matches
import nl.adaptivity.xml.XmlStreaming.*
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 16/11/15.
 */
abstract class AbstractXmlReader : XmlReader {

  @Throws(XmlException::class)
  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?) {
    if (getEventType() !== type) {
      throw XmlException("Unexpected event type Found:" + getEventType() + " expected " + type)
    }
    if (namespace != null) {
      if (!(namespace matches getNamespaceUri())) {
        throw XmlException("Namespace uri's don't match: expected=$namespace found=${getNamespaceUri()}")
      }
    }
    if (name != null) {
      if (!(name matches getLocalName())) {
        throw XmlException("Local names don't match: expected=$name found=${getLocalName()}")
      }
    }
  }

  @Throws(XmlException::class)
  override fun isEndElement() = getEventType() === END_ELEMENT

  @Throws(XmlException::class)
  override fun isCharacters() = getEventType() === CHARACTERS

  @Throws(XmlException::class)
  override fun isStartElement(): Boolean = getEventType() === CHARACTERS

  @Throws(XmlException::class)
  override fun isWhitespace() = getEventType() === IGNORABLE_WHITESPACE || getEventType() === TEXT && isXmlWhitespace(text)

  @Throws(XmlException::class)
  override fun getName() = qname(getNamespaceUri(), getLocalName(), getPrefix())

  @Throws(XmlException::class)
  override fun getAttributeName(i: Int): QName =
        qname(getAttributeNamespace(i), getAttributeLocalName(i), getAttributePrefix(i))
}
