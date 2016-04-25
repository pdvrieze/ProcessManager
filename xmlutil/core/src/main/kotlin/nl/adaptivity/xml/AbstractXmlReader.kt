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
      return buildString {
        var type: EventType? = null
        val result:StringBuilder = this

        while ((next().apply { type=this }) !== EventType.END_ELEMENT) {
          when (type) {
            EventType.COMMENT              -> {} // ignore
            EventType.IGNORABLE_WHITESPACE ->
              // ignore whitespace starting the element.
              if (length != 0) append(text)

            EventType.TEXT,
            EventType.CDSECT               -> result.append(text)
            else                           -> throw XmlException("Found unexpected child tag")
          }//ignore

        }

      }
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.skipElement() {
      require(EventType.START_ELEMENT, null, null)
      while (hasNext() && this.next() !== EventType.END_ELEMENT) {
        if (eventType === EventType.START_ELEMENT) {
          skipElement()
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
      require(EventType.START_ELEMENT, null, null)

      return buildString {
        while ((next()) !== EventType.END_ELEMENT) {
          when (eventType) {
            EventType.COMMENT,
            EventType.PROCESSING_INSTRUCTION -> { }
            EventType.TEXT,
            EventType.CDSECT                 -> append(text)
            else                             -> throw XmlException("Expected text content or end tag, found: $eventType")
          }/* Ignore */
        }
      }
    }

    @JvmStatic
    @Throws(XmlException::class)
    fun XmlReader.toCharArrayWriter(): CharArrayWriter {
      return CharArrayWriter().apply {
        XmlStreaming.newWriter(this).use { out ->
          while (hasNext()) {
            XmlUtil.writeCurrentEvent(this@toCharArrayWriter, out)
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
      while (isIgnorable() && hasNext()) {
        next()
      }
    }

    @Throws(XmlException::class)
    @JvmStatic
    fun XmlReader.isIgnorable(): Boolean {
      val type = eventType
// Before start, means ignore the "current event"
      when (type) {
        EventType.COMMENT,
        EventType.START_DOCUMENT,
        EventType.END_DOCUMENT,
        EventType.PROCESSING_INSTRUCTION,
        EventType.DOCDECL,
        EventType.IGNORABLE_WHITESPACE -> return true
        EventType.TEXT                 -> return XmlUtil.isXmlWhitespace(text)
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
      val reader: XmlReader = this
      skipPreamble()
      if (hasNext()) {
        require(EventType.START_ELEMENT, null, null)
        next()
        return siblingsToFragment()
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
      
      val caw = CharArrayWriter()
      if (!isStarted) {
        if (hasNext()) {
          next()
        } else {
          return CompactFragment("")
        }
      }

      val startLocation = locationInfo
      try {

        val missingNamespaces = TreeMap<String, String>()
        val gatheringContext: GatheringNamespaceContext? = null
        // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
        val initialDepth = depth - if (eventType === EventType.START_ELEMENT) 1 else 0
        var type: EventType? = eventType
        while (type !== EventType.END_DOCUMENT && type !== EventType.END_ELEMENT && depth >= initialDepth) {
          if (type === EventType.START_ELEMENT) {
            val out = XmlStreaming.newWriter(caw)
            XmlUtil.writeCurrentEvent(this, out) // writes the start tag
            XmlUtil.addUndeclaredNamespaces(this, out, missingNamespaces)
            XmlUtil.writeElementContent(missingNamespaces, this ,  out) // writes the children and end tag
            out.close()
          } else if (type === EventType.TEXT || type === EventType.IGNORABLE_WHITESPACE || type === EventType.CDSECT) {
            caw.append(XmlUtil.xmlEncode(text.toString()))
          }
          type = if (hasNext()) next() else null
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
      for (i in namespaceStart..namespaceEnd - 1) {
        if (StringUtil.isEqual(getNamespacePrefix(i), prefix)) {
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
      when (eventType) {
        EventType.CDSECT, EventType.TEXT -> if (!isWhitespace()) {
          throw XmlException("Content found where not expected [" + locationInfo + "] Text:'" + text + "'")
        }
        EventType.COMMENT                -> {} // we never mind comments.
        EventType.START_ELEMENT          -> throw XmlException("Element found where not expected [" + locationInfo + "]: " + name)
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
      return isElement(EventType.START_ELEMENT, elementname.namespaceURI, elementname.localPart, elementname.prefix)
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
      return isElement(type, elementname.namespaceURI, elementname.localPart, elementname.prefix)
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
    fun XmlReader.isElement(elementNamespace: CharSequence, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
      return isElement(EventType.START_ELEMENT, elementNamespace, elementName, elementPrefix)
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
    fun XmlReader.isElement(type: EventType, elementNamespace: CharSequence, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
      if (eventType !== type) {
        return false
      }
      var expNs: CharSequence? = elementNamespace
      if (expNs != null && expNs.length == 0) {
        expNs = null
      }
      if (localName != elementName) {
        return false
      }

      if (StringUtil.isNullOrEmpty(elementNamespace)) {
        if (StringUtil.isNullOrEmpty(elementPrefix!!)) {
          return StringUtil.isNullOrEmpty(prefix)
        } else {
          return elementPrefix == prefix
        }
      } else {
        return StringUtil.isEqual(expNs!!, namespaceUri)
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

