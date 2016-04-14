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

import nl.adaptivity.xml.XmlEvent.NamespaceImpl
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

  val namespaceUri: CharSequence
    @Throws(XmlException::class) get

  val localName: CharSequence
    @Throws(XmlException::class) get

  val prefix: CharSequence
    @Throws(XmlException::class) get

  open val name: QName
    @Throws(XmlException::class)
    get

  open val isStarted:Boolean

  @Throws(XmlException::class)
  fun require(type: EventType, namespace: CharSequence?, name: CharSequence?)

  val depth: Int

  val text: CharSequence

  val attributeCount: Int
    @Throws(XmlException::class)
    get

  @Throws(XmlException::class)
  fun getAttributeNamespace(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributePrefix(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributeLocalName(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributeName(i: Int): QName

  @Throws(XmlException::class)
  fun getAttributeValue(i: Int): CharSequence

  val eventType: EventType
    @Throws(XmlException::class) get

  @Throws(XmlException::class)
  fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence): CharSequence?

  val namespaceStart: Int
    @Throws(XmlException::class) get

  val namespaceEnd: Int
    @Throws(XmlException::class) get

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
  val locationInfo: String?

  val namespaceContext: NamespaceContext
    @Throws(XmlException::class) get

  val encoding: CharSequence

  val standalone: Boolean?

  val version: CharSequence
}

val XmlReader.attributes: Array<out XmlEvent.Attribute> get() =
      Array<XmlEvent.Attribute>(attributeCount) { i ->
        XmlEvent.Attribute(locationInfo,
                           getAttributeNamespace(i),
                           getAttributeLocalName(i),
                           getAttributePrefix(i),
                           getAttributeValue(i))
      }

val XmlReader.namespaceDecls: Array<out Namespace> get() =
      Array<Namespace>(namespaceEnd - namespaceStart) { i ->
        val nsIndex = namespaceStart + i
        NamespaceImpl(getNamespacePrefix(nsIndex), getNamespaceUri(nsIndex))
      }
