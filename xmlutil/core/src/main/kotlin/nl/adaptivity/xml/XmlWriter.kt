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

@file:JvmName("XmlWriterUtil")
package nl.adaptivity.xml

import net.devrieze.util.StringUtil
import net.devrieze.util.kotlin.asString
import nl.adaptivity.util.xml.XmlDelegatingWriter
import nl.adaptivity.xml.XmlStreaming.EventType
import org.w3c.dom.Node
import java.io.Closeable
import javax.xml.XMLConstants
import javax.xml.XMLConstants.*
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.transform.dom.DOMSource


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


@Throws(XmlException::class)
fun XmlWriter.addUndeclaredNamespaces(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
  undeclaredPrefixes(reader, missingNamespaces)
}

@Throws(XmlException::class)
private fun XmlWriter.undeclaredPrefixes(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
  assert(reader.eventType === EventType.START_ELEMENT)
  val prefix = StringUtil.toString(reader.prefix)
  if (prefix != null) {
    if (!missingNamespaces.containsKey(prefix)) {
      val uri = reader.namespaceUri
      if (getNamespaceUri(prefix)== uri && reader.isPrefixDeclaredInElement(prefix)) {
        return
      } else if (uri.length > 0) {
        if (getNamespaceUri(prefix) != uri) {
          missingNamespaces.put(prefix, uri.toString())
        }
      }
    }
  }
}


@Throws(XmlException::class)
fun XmlWriter.writeChild(child: XmlSerializable?) {
  child?.serialize(this)
}

@Throws(XmlException::class)
fun XmlWriter.writeChild(node: Node) {
  serialize(node)
}

@Throws(XmlException::class)
fun writeChildren(out: XmlWriter, children: Iterable<XmlSerializable>?) {
  children?.forEach { out.writeChild(it) }
}


fun XmlWriter.serialize(node: Node) {
  this.serialize(XmlStreaming.newReader(DOMSource(node)))
}

fun XmlWriter.serialize(reader: XmlReader) {
  while (reader.hasNext()) {
    val eventType = reader.next() ?: break
    when (eventType) {
      EventType.START_DOCUMENT,
      EventType.PROCESSING_INSTRUCTION,
      EventType.DOCDECL,
      EventType.END_DOCUMENT -> {
        if (depth <= 0) {
          writeCurrentEvent(reader)
        }
      }
      else                   -> writeCurrentEvent(reader)
    }
  }
}


@Throws(XmlException::class)
fun XmlWriter.writeCurrentEvent(reader: XmlReader) {
  when (reader.eventType) {
    EventType.START_DOCUMENT         -> startDocument(null, reader.encoding, reader.standalone)
    EventType.START_ELEMENT          -> {
      startTag(reader.namespaceUri, reader.localName, reader.prefix)
      run {
        for (i in reader.namespaceIndices) {
          namespaceAttr(reader.getNamespacePrefix(i), reader.getNamespaceUri(i))
        }
      }
      run {
        for (i in reader.attributeIndices) {
          attribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                         null, reader.getAttributeValue(i))
        }
      }
    }
    EventType.END_ELEMENT            -> endTag(reader.namespaceUri, reader.localName,
                                                    reader.prefix)
    EventType.COMMENT                -> comment(reader.text)
    EventType.TEXT                   -> text(reader.text)
    EventType.ATTRIBUTE              -> attribute(reader.namespaceUri, reader.localName,
                                                       reader.prefix, reader.text)
    EventType.CDSECT                 -> cdsect(reader.text)
    EventType.DOCDECL                -> docdecl(reader.text)
    EventType.END_DOCUMENT           -> endDocument()
    EventType.ENTITY_REF             -> entityRef(reader.text)
    EventType.IGNORABLE_WHITESPACE   -> ignorableWhitespace(reader.text)
    EventType.PROCESSING_INSTRUCTION -> processingInstruction(reader.text)
    else                             -> throw XmlException("Unsupported element found")
  }
}


/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes.
 */
@Throws(XmlException::class)
fun XmlWriter.smartStartTag(qName: QName) {
  smartStartTag(qName.namespaceURI, qName.localPart, qName.prefix)
}

/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes.
 */
@Throws(XmlException::class)
fun XmlWriter.smartStartTag(nsUri:CharSequence?, localName: CharSequence, prefix: CharSequence?=null) {
  if (nsUri==null) {
    val namespace = namespaceContext.getNamespaceURI(prefix.asString()?: DEFAULT_NS_PREFIX) ?: NULL_NS_URI
    startTag(namespace, localName, prefix)
  } else {
    var writeNs = false

    val namespace: String = nsUri.toString()
    val usedPrefix = getPrefix(namespace) ?: run {writeNs=true; prefix ?: DEFAULT_NS_PREFIX}

    startTag(namespace, localName, usedPrefix)

    if (writeNs) this.namespaceAttr(usedPrefix, namespace)
  }
}

@Throws(XmlException::class)
fun XmlWriter.writeSimpleElement(qName: QName, value: CharSequence?) {
  writeSimpleElement(qName.namespaceURI, qName.localPart, qName.prefix, value)
}

@Throws(XmlException::class)
fun XmlWriter.writeSimpleElement(nsUri:CharSequence?, localName: CharSequence, prefix: CharSequence?, value: CharSequence?) {
  smartStartTag(nsUri, localName, prefix)
  if (!value.isNullOrEmpty()) {
    text(value.toString())
  }
  endTag(nsUri, localName, prefix)
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: String, value: CharSequence?) {
  value?.let { attribute(null, name, null, it) }
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: String, value: Any?) {
  value?.let {attribute(null, name, null, it.toString())}
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: QName, value: CharSequence?) {
  value?.let { attribute(name.namespaceURI, name.localPart, name.prefix, value) }
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: String, value: Double) {
  if (!java.lang.Double.isNaN(value)) {
    attribute(null, name, null, java.lang.Double.toString(value))
  }
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: String, value: Long) {
  attribute(null, name, null, java.lang.Long.toString(value))
}

@Throws(XmlException::class)
fun XmlWriter.writeAttribute(name: String, value: QName?) {
  if (value != null) {
    var prefix: String?
    if (value.namespaceURI != null) {
      if (value.prefix != null && value.namespaceURI == namespaceContext.getNamespaceURI(value.prefix)) {
        prefix = value.prefix
      } else {
        prefix = namespaceContext.getPrefix(value.namespaceURI)
        if (prefix == null) {
          prefix = value.prefix
          namespaceAttr(prefix!!, value.namespaceURI)
        }
      }
    } else { // namespace not specified
      prefix = value.prefix
      if (prefix?.let {namespaceContext.getNamespaceURI(it) } ==null) throw IllegalArgumentException("Cannot determine namespace of qname")
    }
    attribute(null, name, null, prefix + ':' + value.localPart)
  }
}

@Throws(XmlException::class)
fun XmlWriter.endTag(predelemname: QName) {
  this.endTag(predelemname.namespaceURI, predelemname.localPart, predelemname.prefix)
}

fun XmlWriter.filterSubstream(): XmlWriter {
  return SubstreamFilterWriter(this)
}


@Throws(XmlException::class)
private fun undeclaredPrefixes(reader: XmlReader,
                               reference: XmlWriter,
                               missingNamespaces: MutableMap<String, String>) {
  assert(reader.eventType === EventType.START_ELEMENT)
  val prefix = StringUtil.toString(reader.prefix)
  if (prefix != null) {
    if (!missingNamespaces.containsKey(prefix)) {
      val uri = reader.namespaceUri
      if (StringUtil.isEqual(reference.getNamespaceUri(prefix)!!,
                             uri) && reader.isPrefixDeclaredInElement(prefix)) {
        return
      } else if (uri.length > 0) {
        if (!StringUtil.isEqual(reference.getNamespaceUri(prefix)!!, uri)) {
          missingNamespaces.put(prefix, uri.toString())
        }
      }
    }
  }
}


/**
 * TODO make it package protected once XmlUtil is moved.
 */
@Deprecated("should be more private")
@Throws(XmlException::class)
fun XmlWriter.writeElementContent(missingNamespaces: MutableMap<String, String>?, reader: XmlReader) {
  while (reader.hasNext()) {
    val type = reader.next()
    reader.writeCurrent(this)
    when (type) {
      EventType.START_ELEMENT -> {
        if (missingNamespaces != null) addUndeclaredNamespaces(reader, missingNamespaces)

        writeElementContent(missingNamespaces, reader)
      }
      EventType.END_ELEMENT -> return
      else -> {}// ignore
    }
  }
}


private class SubstreamFilterWriter(delegate: XmlWriter) : XmlDelegatingWriter(delegate) {

  override fun processingInstruction(text: CharSequence) { /* ignore */ }

  override fun endDocument() { /* ignore */ }

  override fun docdecl(text: CharSequence) { /* ignore */ }

  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?) { /* ignore */ }
}
