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

import java.io.Closeable
import javax.xml.namespace.NamespaceContext


/**
 * Created by pdvrieze on 15/11/15.
 */
interface XmlWriter: Closeable, AutoCloseable {

  val depth: Int

  @Throws(XmlException::class)
  fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence)

  @Throws(XmlException::class)
  fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence)

  @Throws(XmlException::class)
  override fun close()

  /**
   * Flush all state to the underlying buffer
   */
  @Throws(XmlException::class)
  fun flush()

  /**
   * Write a start tag.
   * @param namespace The namespace to use for the tag.
   * *
   * @param localName The local name for the tag.
   * *
   * @param prefix The prefix to use, or `null` for the namespace to be assigned automatically
   */
  @Throws(XmlException::class)
  fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence? = null)

  /**
   * Write a comment.
   * @param text The comment text
   */
  @Throws(XmlException::class)
  fun comment(text: CharSequence)

  /**
   * Write text.
   * @param text The text content.
   */
  @Throws(XmlException::class)
  fun text(text: CharSequence)

  /**
   * Write a CDATA section
   * @param text The text of the section.
   */
  @Throws(XmlException::class)
  fun cdsect(text: CharSequence)

  @Throws(XmlException::class)
  fun entityRef(text: CharSequence)

  @Throws(XmlException::class)
  fun processingInstruction(text: CharSequence)

  @Throws(XmlException::class)
  fun ignorableWhitespace(text: CharSequence)

  @Throws(XmlException::class)
  fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence)

  @Throws(XmlException::class)
  fun docdecl(text: CharSequence)

  @Throws(XmlException::class)
  fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?)

  @Throws(XmlException::class)
  fun endDocument()

  @Throws(XmlException::class)
  fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?=null)

  val namespaceContext: NamespaceContext

  @Throws(XmlException::class)
  fun getNamespaceUri(prefix: CharSequence): CharSequence?

  @Throws(XmlException::class)
  fun getPrefix(namespaceUri: CharSequence?): CharSequence?
}
