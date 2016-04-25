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
import nl.adaptivity.util.xml.XmlUtil
import nl.adaptivity.xml.AbstractXmlReader.Companion.toCharArrayWriter
import nl.adaptivity.xml.XmlStreaming.EventType
import java.io.CharArrayWriter
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

