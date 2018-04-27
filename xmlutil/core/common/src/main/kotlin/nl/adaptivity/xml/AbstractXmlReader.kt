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

package nl.adaptivity.xml


/**
 * Created by pdvrieze on 16/11/15.
 */

abstract class AbstractXmlReader: XmlReader {

  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?) {
    if (eventType !== type) {
      throw XmlException("Unexpected event type Found: $eventType expected $type")
    }
    if (namespace != null) {
      if (namespace != namespaceUri) {
        throw XmlException("Namespace uri's don't match: expected=$namespace found=$namespaceUri")
      }
    }
    if (name != null) {
      if (!(name == localName)) {
        throw XmlException("Local names don't match: expected=$name found=$localName")
      }
    }
  }

  override fun isEndElement() = eventType === EventType.END_ELEMENT

  override fun isCharacters() = eventType === EventType.TEXT

  override fun isStartElement(): Boolean = eventType === EventType.START_ELEMENT

  override fun isWhitespace() = eventType === EventType.IGNORABLE_WHITESPACE || eventType === EventType.TEXT && isXmlWhitespace(text)

  override val name: QName
    get() = qname(namespaceUri, localName, prefix)

  override fun getAttributeName(i: Int): QName =
      qname(getAttributeNamespace(i), getAttributeLocalName(i), getAttributePrefix(i))
}

