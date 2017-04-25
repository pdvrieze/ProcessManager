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

package nl.adaptivity.xml

import nl.adaptivity.js.util.removeElementChildren
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.XMLConstants
import kotlin.browser.document
import kotlin.properties.Delegates

/**
 * Created by pdvrieze on 04/04/17.
 */
class JSDomWriter: XmlWriter
{
  private var docDelegate: Document? = null
  val target: Document
    get() {
      return docDelegate ?: throw XmlException("Document not created yet")
    }
  var currentElement: Element? = null

  override var depth: Int = 0
    private set

  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence)
  {
    val cur = currentElement ?: throw XmlException("Not in an element")
    when {
      namespacePrefix.isEmpty() -> cur.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, namespaceUri.toString())
      else                      -> cur.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "${XMLConstants.XMLNS_ATTRIBUTE}:${namespacePrefix.toString()}", namespaceUri.toString())
    }
  }

  override fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    when {
      currentElement==null && docDelegate==null -> {
        docDelegate = document.implementation.createDocument(namespace?.toString() ?: "", qname(prefix, localName))
        currentElement = docDelegate?.rootElement
        return
      }
      currentElement==null -> {
        if (target.childElementCount>0) {
          target.removeElementChildren()

        }
      }
    }

    target.createElementNS(namespace?.toString(), qname(prefix, localName)).let { elem ->
      currentElement?.appendChild(elem) ?: document.appendChild(elem)
      currentElement = elem
    }
  }

  override fun comment(text: CharSequence) {
    target.createComment(text.toString()).let { comment ->
      currentElement?.appendChild(comment) ?: throw XmlException("Not in an element")
    }
  }

  override fun text(text: CharSequence) {
    target.createTextNode(text.toString()).let { textNode ->
      currentElement?.appendChild(textNode) ?: throw XmlException("Not in an element")
    }
  }

  override fun cdsect(text: CharSequence) {
    target.createCDATASection(text.toString()).let { cdataSection ->
      currentElement?.appendChild(cdataSection) ?: throw XmlException("Not in an element")
    }
  }

  override fun entityRef(text: CharSequence) {
    TODO("Not implemented yet. Lacks Kotlin support")
  }

  override fun processingInstruction(text: CharSequence)
  {
    if (currentElement!=null) throw XmlException("Document already started")
    val split = text.indexOf(' ')
    val (target, data) = when  {
      split < 0 -> text.toString() to ""
      else      -> text.substring(0, split) to text.substring(split+1)
    }
    this.target.createProcessingInstruction(target, data).let { processInstr ->
      this.target.appendChild(processInstr)
    }
  }

  override fun ignorableWhitespace(text: CharSequence) {
    target.createTextNode(text.toString()).let { textNode ->
      currentElement?.appendChild(textNode) ?: throw XmlException("Not in an element")
    }
  }

  override fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence)
  {
    val cur = currentElement ?: throw XmlException("Not in an element")
    when {
      prefix.isNullOrEmpty() -> cur.setAttributeNS(namespace?.toString() ?: XMLConstants.NULL_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, namespace.toString())
      else                   -> cur.setAttributeNS(namespace?.toString() ?: XMLConstants.NULL_NS_URI, "${XMLConstants.XMLNS_ATTRIBUTE}:${prefix.toString()}", namespace.toString())
    }
  }

  override fun docdecl(text: CharSequence)
  {
    val textElems = text.split(" ", limit = 3)
    val qualifiedName = textElems[0]
    val publicId = if (textElems.size>1) textElems[1] else ""
    val systemId = if (textElems.size>2) textElems[2] else ""
    target.implementation.createDocumentType(qualifiedName, publicId, systemId).let { docType ->
      target.appendChild(docType)
    }
  }

  var  requestedVersion: String? = null
    private set

  var requestedEncoding: String? = null
    private set

  var requestedStandalone: Boolean? = null
    private set

  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?)
  {
    // Ignore everything for now as this cannot be set on a dom tree
    requestedVersion = version?.toString()
    requestedEncoding = encoding?.toString()
    requestedStandalone = standalone
  }

  override fun endDocument()
  {
    currentElement = null
  }

  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    currentElement = (currentElement ?: throw XmlException("No current element or no parent element")).parentElement
  }

  override fun getNamespaceUri(prefix: CharSequence): CharSequence?
  {
    return document.lookupNamespaceURI(prefix.toString())
  }

  override fun getPrefix(namespaceUri: CharSequence?): CharSequence?
  {
    return document.lookupPrefix(namespaceUri?.toString())
  }


}

private fun qname(prefix: CharSequence?, localName: CharSequence): String {
  when {
    prefix.isNullOrEmpty() -> return localName.toString()
    else                   -> return buildString { append(prefix).append(':').append(localName) }
  }
}