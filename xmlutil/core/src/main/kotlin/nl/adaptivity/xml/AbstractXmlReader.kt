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

import net.devrieze.util.StringUtil
import net.devrieze.util.kotlin.matches
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XmlUtil
import nl.adaptivity.util.xml.XmlUtil.writeElementContent
import nl.adaptivity.xml.AbstractXmlReader.Companion.toCharArrayWriter
import nl.adaptivity.xml.XmlStreaming.EventType
import nl.adaptivity.xml.XmlStreaming.EventType.*
import java.io.CharArrayWriter
import java.util.*
import javax.xml.namespace.QName


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

    @JvmStatic
    fun XmlReader.asSubstream(): XmlReader = SubstreamFilter(this)


    /**
     * Get the next text sequence in the reader. This will skip over comments and ignorable whitespace, but not tags.
     * Any tags encountered with cause an exception to be thrown.
     *
     * @param in The reader to read from.
     *
     * @return   The text found
     *
     * @throws XmlException If reading breaks, or an unexpected element was found.
     */
    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.allText(): CharSequence {
      val t = this
      return buildString {
        var type: EventType? = null

        while ((t.next().apply { type = this@apply }) !== EventType.END_ELEMENT) {
          when (type) {
            EventType.COMMENT              -> {
            } // ignore
            EventType.IGNORABLE_WHITESPACE ->
              // ignore whitespace starting the element.
              if (length != 0) append(t.text)

            EventType.TEXT,
            EventType.CDSECT               -> append(t.text)
            else                           -> throw XmlException("Found unexpected child tag")
          }//ignore

        }

      }
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.skipElement() {
      val t = this
      t.require(EventType.START_ELEMENT, null, null)
      while (t.hasNext() && t.next() !== EventType.END_ELEMENT) {
        if (t.eventType === EventType.START_ELEMENT) {
          t.skipElement()
        }
      }
    }

    inline fun <reified T : Any> XmlReader.deSerialize(): T {
      return deSerialize(T::class.java)
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun <T> XmlReader.deSerialize(type: Class<T>): T {
      val deserializer = type.getAnnotation(XmlDeserializer::class.java) ?: throw IllegalArgumentException("Types must be annotated with " + XmlDeserializer::class.java.name + " to be deserialized automatically")

      return type.cast(deserializer.value.java.newInstance().deserialize(this))
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.readSimpleElement(): CharSequence {
      val t = this
      t.require(EventType.START_ELEMENT, null, null)
      return buildString {

        while ((t.next()) !== EventType.END_ELEMENT) {
          when (t.eventType) {
            EventType.COMMENT,
            EventType.PROCESSING_INSTRUCTION -> {
            }
            EventType.TEXT,
            EventType.CDSECT                 -> append(t.text)
            else                             -> throw XmlException("Expected text content or end tag, found: ${t.eventType}")
          }/* Ignore */
        }

      }
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.toCharArrayWriter(): CharArrayWriter {
      val t =this
      return CharArrayWriter().apply {
        XmlStreaming.newWriter(this).use { out ->
          while (t.hasNext()) {
            XmlUtil.writeCurrentEvent(t, out)
          }
        }
      }
    }


    /**
     * Skil the preamble events in the stream reader
     * @param in The stream reader to skip
     */
    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.skipPreamble() {
      val r = this
      while (r.isIgnorable() && r.hasNext()) {
        r.next()
      }
    }

    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.isIgnorable(): Boolean {
      val r = this
      val type = r.eventType
// Before start, means ignore the "current event"
      when (type) {
        EventType.COMMENT,
        EventType.START_DOCUMENT,
        EventType.END_DOCUMENT,
        EventType.PROCESSING_INSTRUCTION,
        EventType.DOCDECL,
        EventType.IGNORABLE_WHITESPACE -> return true
        EventType.TEXT                 -> return XmlUtil.isXmlWhitespace(r.text)
        else                           -> return false
      }
    }

    /**
     * Differs from [.siblingsToFragment] in that it skips the current event.
     * @param reader
     * *
     * @return
     * *
     * @throws XmlException
     */
    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.elementContentToFragment(): CompactFragment {
      val r = this
      r.skipPreamble()
      if (r.hasNext()) {
        r.require(EventType.START_ELEMENT, null, null)
        r.next()
        return r.siblingsToFragment()
      }
      return CompactFragment("")
    }

    /**
     * Read the current element (and content) and all its siblings into a fragment.
     * @param in The source stream.
     * *
     * @return the fragment
     * *
     * @throws XmlException parsing failed
     */
    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.siblingsToFragment(): CompactFragment {
      val r = this
      val caw = CharArrayWriter()
      if (!r.isStarted) {
        if (r.hasNext()) {
          r.next()
        } else {
          return CompactFragment("")
        }
      }

      val startLocation = r.locationInfo
      try {

        val missingNamespaces = TreeMap<String, String>()
        val gatheringContext: GatheringNamespaceContext? = null
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = r.depth - if (r.eventType === EventType.START_ELEMENT) 1 else 0
        var type: EventType? = r.eventType
        while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && r.depth >= initialDepth) {
          if (type === EventType.START_ELEMENT) {
            val out = XmlStreaming.newWriter(caw)
            XmlUtil.writeCurrentEvent(r, out) // writes the start tag
            XmlUtil.addUndeclaredNamespaces(r, out, missingNamespaces)
            XmlUtil.writeElementContent(missingNamespaces, r,  out) // writes the children and end tag
            out.close()
          } else if (type === EventType.TEXT || type === EventType.IGNORABLE_WHITESPACE || type === EventType.CDSECT) {
            caw.append(XmlUtil.xmlEncode(r.text.toString()))
          }
          type = if (r.hasNext()) r.next() else null
        }
        return CompactFragment(SimpleNamespaceContext(missingNamespaces), caw.toCharArray())
      } catch (e: XmlException) {
        throw XmlException("Failure to parse children into string at " + startLocation, e)
      } catch (e: RuntimeException) {
        throw XmlException("Failure to parse children into string at " + startLocation, e)
      }

    }

    /** Determine whether the prefix was declared within this tag. This will return `false` if it was only declared
     * on a parent.
     */
    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.isPrefixDeclaredInElement(prefix: String): Boolean {
      val r = this
      for (i in r.namespaceStart..r.namespaceEnd - 1) {
        if (StringUtil.isEqual(r.getNamespacePrefix(i), prefix)) {
          return true
        }
      }
      return false
    }

    /**
     * Helper method that throws exceptions for "unexpected" tags.
     */
    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.unhandledEvent() {
      val r = this
      when (r.eventType) {
        EventType.CDSECT, EventType.TEXT -> if (!r.isWhitespace()) {
          throw XmlException("Content found where not expected [" + r.locationInfo + "] Text:'" + r.text + "'")
        }
        EventType.COMMENT                -> {} // we never mind comments.
        EventType.START_ELEMENT          -> throw XmlException("Element found where not expected [" + r.locationInfo + "]: " + r.name)
        EventType.END_DOCUMENT           -> throw XmlException("End of document found where not expected")
      }// ignore
    }


    /**
     * Check that the current state is a start element for the given name. The mPrefix is ignored.
     * @param in The stream reader to check
     * *
     * @param elementname The name to check against
     * *
     * @return `true` if it matches, otherwise `false`
     */

    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.isElement(elementname: QName): Boolean {
      return this.isElement(EventType.START_ELEMENT, elementname.namespaceURI, elementname.localPart, elementname.prefix)
    }

    /**
     * Check that the current state is a start element for the given name. The mPrefix is ignored.
     * @param in The stream reader to check
     * *
     * @param type
     * @param elementname The name to check against  @return `true` if it matches, otherwise `false`
     */

    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.isElement(type: EventType, elementname: QName): Boolean {
      return this.isElement(type, elementname.namespaceURI, elementname.localPart, elementname.prefix)
    }

    /**
     * Check that the current state is a start element for the given name. The mPrefix is ignored.
     * @param in The stream reader to check
     * *
     * @param elementNamespace  The namespace to check against.
     * *
     * @param elementName The local name to check against
     * *
     * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined
     * *
     * @return `true` if it matches, otherwise `false`
     */
    @Throws(XmlException::class)
    @JvmStatic
    @JvmOverloads
    fun XmlReader.isElement(elementNamespace: CharSequence?, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
      return this.isElement(EventType.START_ELEMENT, elementNamespace, elementName, elementPrefix)
    }

    /**
     * Check that the current state is a start element for the given name. The mPrefix is ignored.
     * @param in The stream reader to check
     * *
     * @param type The type to verify. Should be named so start or end element
     * @param elementNamespace  The namespace to check against.
     * *
     * @param elementName The local name to check against
     * *
     * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined    @return `true` if it matches, otherwise `false`
     */
    @Throws(XmlException::class)
    @JvmStatic
    @JvmOverloads
    fun XmlReader.isElement(type: EventType, elementNamespace: CharSequence?, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
      val r = this
      if (r.eventType !== type) {
        return false
      }
      val expNs: CharSequence? = elementNamespace?.let { if (it.isEmpty()) null else it }

      if (r.localName != elementName) {
        return false
      }

      if (elementNamespace.isNullOrEmpty()) {
        if (elementPrefix.isNullOrEmpty()) {
          return r.prefix.isNullOrEmpty()
        } else {
          return elementPrefix == r.prefix
        }
      } else {
        return expNs == r.namespaceUri
      }
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


/**
 * A class that filters an xml stream such that it will only contain expected elements.
 */
private class SubstreamFilter(delegate: XmlReader) : XmlBufferedReader(delegate) {

  @Throws(XmlException::class)
  override fun doPeek(): List<XmlEvent> {
    return super.doPeek().filter {
      when (it.eventType) {
        EventType.START_DOCUMENT, EventType.PROCESSING_INSTRUCTION, EventType.DOCDECL, EventType.END_DOCUMENT -> false
        else                                                                                                  -> true
      }
    }
  }
}

