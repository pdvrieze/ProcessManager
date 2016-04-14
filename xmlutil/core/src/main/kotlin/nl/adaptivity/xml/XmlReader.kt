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

import nl.adaptivity.xml.XmlStreaming.EventType

import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName


/**
 * Created by pdvrieze on 15/11/15.
 */
interface XmlReader {

  /** Get the next tag. This must call next, not use the underlying stream.  */
  @Throws(XmlException::class)
  fun nextTag(): EventType

  @Throws(XmlException::class)
  operator fun hasNext(): Boolean

  @Throws(XmlException::class)
  operator fun next(): EventType?

  @Throws(XmlException::class)
  fun getNamespaceUri(): CharSequence

  @Throws(XmlException::class)
  fun getLocalName(): CharSequence

  @Throws(XmlException::class)
  fun getPrefix(): CharSequence

  @Throws(XmlException::class)
  fun getName(): QName

  @Throws(XmlException::class)
  fun require(type: EventType, namespace: CharSequence?, name: CharSequence?)

  val depth: Int

  val text: CharSequence

  val attributeCount: Int

  @Throws(XmlException::class)
  fun getAttributeNamespace(i: Int): CharSequence?

  @Throws(XmlException::class)
  fun getAttributePrefix(i: Int): CharSequence?

  @Throws(XmlException::class)
  fun getAttributeLocalName(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributeName(i: Int): QName

  @Throws(XmlException::class)
  fun getAttributeValue(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getEventType(): EventType

  @Throws(XmlException::class)
  fun getAttributeValue(nsUri: CharSequence, localName: CharSequence): CharSequence?

  @Throws(XmlException::class)
  fun getNamespaceStart(): Int

  @Throws(XmlException::class)
  fun getNamespaceEnd(): Int

  @Throws(XmlException::class)
  fun getNamespacePrefix(i: Int): CharSequence

  @Throws(XmlException::class)
  fun close()

  @Throws(XmlException::class)
  fun getNamespaceUri(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence?

  @Throws(XmlException::class)
  fun isWhitespace(): Boolean

  @Throws(XmlException::class)
  fun isEndElement(): Boolean

  @Throws(XmlException::class)
  fun isCharacters(): Boolean

  @Throws(XmlException::class)
  fun isStartElement(): Boolean

  @Throws(XmlException::class)
  fun getNamespaceUri(prefix: CharSequence): String?

  /** Get some information on the current location in the file. This is implementation dependent.  */
  fun getLocationInfo(): String

  @Throws(XmlException::class)
  fun getNamespaceContext(): NamespaceContext

  val encoding: CharSequence

  val standalone: Boolean?

  val version: CharSequence
}
