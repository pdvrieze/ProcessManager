/*
 * Copyright (c) 2017.
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

import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.EventType

import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName

import nl.adaptivity.xml.isXmlWhitespace


/**
 * Simple baseclass for a delagting XmlReader.
 * Created by pdvrieze on 16/11/15.
 */
internal open class XmlDelegatingReader protected constructor(protected val delegate: XmlReader) : XmlReader
{

  @Throws(XmlException::class)
  override fun hasNext(): Boolean
  {
    return delegate.hasNext()
  }

  @Throws(XmlException::class)
  override fun next(): EventType
  {
    return delegate.next()
  }

  override val isStarted: Boolean
    get() = delegate.isStarted

  override val namespaceUri: CharSequence
    @Throws(XmlException::class)
    get() = delegate.namespaceUri

  override val localName: CharSequence
    @Throws(XmlException::class)
    get() = delegate.localName

  override val prefix: CharSequence
    @Throws(XmlException::class)
    get() = delegate.prefix

  override val name: QName
    @Throws(XmlException::class)
    get() = delegate.name

  @Throws(XmlException::class)
  override fun require(type: EventType, namespace: CharSequence?, name: CharSequence?)
  {
    delegate.require(type, namespace, name)
  }

  override val depth: Int
    get() = delegate.depth

  override val text: CharSequence
    get() = delegate.text

  override val attributeCount: Int
    @Throws(XmlException::class)
    get() = delegate.attributeCount

  @Throws(XmlException::class)
  override fun getAttributeNamespace(i: Int): CharSequence
  {
    return delegate.getAttributeNamespace(i)
  }

  @Throws(XmlException::class)
  override fun getAttributePrefix(i: Int): CharSequence
  {
    return delegate.getAttributePrefix(i)
  }

  @Throws(XmlException::class)
  override fun getAttributeLocalName(i: Int): CharSequence
  {
    return delegate.getAttributeLocalName(i)
  }

  @Throws(XmlException::class)
  override fun getAttributeName(i: Int): QName
  {
    return delegate.getAttributeName(i)
  }

  @Throws(XmlException::class)
  override fun getAttributeValue(i: Int): CharSequence
  {
    return delegate.getAttributeValue(i)
  }

  override val eventType: EventType
    @Throws(XmlException::class)
    get() = delegate.eventType

  @Throws(XmlException::class)
  override fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence): CharSequence?
  {
    return delegate.getAttributeValue(nsUri, localName)
  }

  override val namespaceStart: Int
    @Throws(XmlException::class)
    get() = delegate.namespaceStart

  override val namespaceEnd: Int
    @Throws(XmlException::class)
    get() = delegate.namespaceEnd

  @Throws(XmlException::class)
  override fun getNamespacePrefix(i: Int): CharSequence
  {
    return delegate.getNamespacePrefix(i)
  }

  @Throws(XmlException::class)
  override fun close()
  {
    delegate.close()
  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(i: Int): CharSequence
  {
    return delegate.getNamespaceUri(i)
  }

  @Throws(XmlException::class)
  override fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence?
  {
    return delegate.getNamespacePrefix(namespaceUri)
  }

  @Throws(XmlException::class)
  override fun isWhitespace(): Boolean
  {
    return delegate.isWhitespace()
  }

  @Throws(XmlException::class)
  override fun isEndElement(): Boolean
  {
    return delegate.isEndElement()
  }

  @Throws(XmlException::class)
  override fun isCharacters(): Boolean
  {
    return delegate.isCharacters()
  }

  @Throws(XmlException::class)
  override fun isStartElement(): Boolean
  {
    return delegate.isStartElement()
  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(prefix: CharSequence): String?
  {
    return delegate.getNamespaceUri(prefix)
  }

  override val locationInfo: String?
    get() = delegate.locationInfo

  override val namespaceContext: NamespaceContext
    @Throws(XmlException::class)
    get() = delegate.namespaceContext

  override val encoding: CharSequence?
    get() = delegate.encoding

  override val standalone: Boolean?
    get() = delegate.standalone

  override val version: CharSequence?
    get() = delegate.version
}
