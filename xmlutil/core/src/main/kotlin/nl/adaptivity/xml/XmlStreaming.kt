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

import nl.adaptivity.xml.XmlEvent.*
import nl.adaptivity.xml.XmlStreaming.EventType
import nl.adaptivity.xml.XmlStreaming.deSerialize
import java.io.*

import javax.xml.stream.XMLStreamException
import javax.xml.transform.Result
import javax.xml.transform.Source


/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
object XmlStreaming {

  private class DefaultFactory : XmlStreamingFactory {

    @Throws(XmlException::class)
    override fun newWriter(writer: Writer, repairNamespaces: Boolean): XmlWriter {
      try {
        return StAXWriter(writer, repairNamespaces)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    @Throws(XmlException::class)
    override fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter {
      try {
        return StAXWriter(outputStream, encoding, repairNamespaces)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    @Throws(XmlException::class)
    override fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter {
      try {
        return StAXWriter(result, repairNamespaces)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    @Throws(XmlException::class)
    override fun newReader(reader: Reader): XmlReader {
      try {
        return StAXReader(reader)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    @Throws(XmlException::class)
    override fun newReader(inputStream: InputStream, encoding: String?): XmlReader {
      try {
        return StAXReader(inputStream, encoding)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    @Throws(XmlException::class)
    override fun newReader(source: Source): XmlReader {
      try {
        return StAXReader(source)
      } catch (e: XMLStreamException) {
        throw XmlException(e)
      }
    }

    companion object {
      internal val DEFAULTFACTORY = DefaultFactory()
    }
  }


  enum class EventType {
    START_DOCUMENT {
      override fun createEvent(reader:XmlReader) = reader.run {
        StartDocumentEvent(locationInfo, version, encoding, standalone) }

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.startDocument(reader.version, reader.encoding, reader.standalone)
    },
    START_ELEMENT {
      @Throws(XmlException::class)
      override fun createEvent(reader:XmlReader) = reader.run {
        StartElementEvent(locationInfo, namespaceUri, localName, prefix, attributes, namespaceDecls)}

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) {
        writer.startTag(reader.namespaceUri, reader.localName, reader.prefix)
        for (i in reader.namespaceStart..reader.namespaceEnd - 1) {
          writer.namespaceAttr(reader.getNamespacePrefix(i), reader.getNamespaceUri(i))
        }
        for (i in 0..reader.attributeCount - 1) {
          writer.attribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), null, reader.getAttributeValue(i))
        }
      }
    },
    END_ELEMENT {
      @Throws(XmlException::class)
      override fun createEvent(reader:XmlReader) = reader.run {
        EndElementEvent(locationInfo, namespaceUri, localName, prefix)}

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endTag(reader.namespaceUri, reader.localName, reader.prefix)
    },
    COMMENT {
      override fun createEvent(reader:XmlReader) = reader.run {
        TextEvent(locationInfo, COMMENT, text)}

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.comment(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.comment(reader.text)
    },
    TEXT {
      override fun createEvent(reader:XmlReader) = reader.run {
        TextEvent(locationInfo, TEXT, text)}

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.text(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.text(reader.text)
    },
    CDSECT {
      override fun createEvent(reader:XmlReader) =  reader.run {TextEvent(locationInfo, CDSECT, text)}

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.cdsect(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.cdsect(reader.text)
    },
    DOCDECL {
      override fun createEvent(reader:XmlReader) =  reader.run {TextEvent(locationInfo, DOCDECL, text)}

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.docdecl(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.docdecl(reader.text)
    },
    END_DOCUMENT {
      override fun createEvent(reader:XmlReader) = reader.run { EndDocumentEvent(locationInfo) }

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.endDocument()
    },
    ENTITY_REF {
      override fun createEvent(reader:XmlReader) = reader.run { TextEvent(locationInfo, ENTITY_REF, text) }

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.entityRef(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.entityRef(reader.text)
    },
    IGNORABLE_WHITESPACE {
      override fun createEvent(reader:XmlReader) =  reader.run { TextEvent(locationInfo, IGNORABLE_WHITESPACE, text) }

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.ignorableWhitespace(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.ignorableWhitespace(reader.text)
    },
    ATTRIBUTE {
      @Throws(XmlException::class)
      override fun createEvent(reader:XmlReader) = reader.run { Attribute(locationInfo, namespaceUri, localName, prefix, text) }

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.attribute(reader.namespaceUri, reader.localName, reader.prefix, reader.text)
    },
    PROCESSING_INSTRUCTION {
      override fun createEvent(reader:XmlReader) = TextEvent(reader.locationInfo, PROCESSING_INSTRUCTION, reader.text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.processingInstruction(textEvent.text)

      override fun writeEvent(writer: XmlWriter, reader: XmlReader) =
            writer.processingInstruction(reader.text)
    };

    @Throws(XmlException::class)
    open fun writeEvent(writer: XmlWriter, textEvent: TextEvent): Unit = throw UnsupportedOperationException("This is not generally supported, only by text types")

    @Throws(XmlException::class)
    abstract fun writeEvent(writer:XmlWriter, reader:XmlReader)

    @Throws(XmlException::class)
    abstract fun createEvent(reader:XmlReader): XmlEvent

  }


  interface XmlStreamingFactory {

    @Throws(XmlException::class)
    fun newWriter(writer: Writer, repairNamespaces: Boolean): XmlWriter

    @Throws(XmlException::class)
    fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean): XmlWriter

    @Throws(XmlException::class)
    fun newWriter(result: Result, repairNamespaces: Boolean): XmlWriter

    @Throws(XmlException::class)
    fun newReader(source: Source): XmlReader

    @Throws(XmlException::class)
    fun newReader(reader: Reader): XmlReader

    @Throws(XmlException::class)
    fun newReader(inputStream: InputStream, encoding: String?): XmlReader
  }

  private var _factory: XmlStreamingFactory? = null

  private val factory: XmlStreamingFactory
    get() = _factory?:DefaultFactory.DEFAULTFACTORY

  @Throws(XmlException::class)
  @JvmStatic
  @JvmOverloads fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(result, repairNamespaces)
  }

  @Throws(XmlException::class)
  @JvmOverloads
  @JvmStatic
  fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(outputStream, encoding, repairNamespaces)
  }

  @Throws(XmlException::class)
  @JvmOverloads
  @JvmStatic
  fun newWriter(writer: Writer, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(writer, repairNamespaces)
  }

  @Throws(XmlException::class)
  @JvmStatic
  fun newReader(inputStream: InputStream, encoding: String?): XmlReader {
    return factory.newReader(inputStream, encoding)
  }

  @Throws(XmlException::class)
  @JvmStatic
  fun newReader(reader: Reader): XmlReader {
    return factory.newReader(reader)
  }

  @Throws(XmlException::class)
  @JvmStatic
  fun newReader(source: Source): XmlReader {
    return factory.newReader(source)
  }

  @JvmStatic
  fun setFactory(factory: XmlStreamingFactory?) {
    _factory = factory
  }


  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val START_DOCUMENT: EventType = EventType.START_DOCUMENT
  @JvmField@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val START_ELEMENT : EventType = EventType.START_ELEMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val END_ELEMENT :EventType = EventType.END_ELEMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val COMMENT :EventType = EventType.COMMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val CDSECT :EventType = EventType.CDSECT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val DOCDECL :EventType = EventType.DOCDECL
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val ATTRIBUTE :EventType = EventType.ATTRIBUTE
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val END_DOCUMENT :EventType = EventType.END_DOCUMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val ENTITY_REF :EventType = EventType.ENTITY_REF
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val IGNORABLE_WHITESPACE :EventType = EventType.IGNORABLE_WHITESPACE
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val PROCESSING_INSTRUCTION :EventType = EventType.PROCESSING_INSTRUCTION

  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val CDATA = CDSECT

  @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  @JvmField val TEXT = EventType.TEXT
  @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  @JvmField val CHARACTERS = EventType.TEXT

  @JvmStatic
  @Throws(XmlException::class)
  fun <T> deSerialize(input: InputStream, type: Class<T>): T {
    return XmlStreaming.newReader(input, "UTF-8").deSerialize(type)
  }

  @JvmStatic
  @Throws(XmlException::class)
  fun <T> deSerialize(input: Reader, type: Class<T>): T {
    return XmlStreaming.newReader(input).deSerialize(type)
  }

  @JvmStatic
  @Throws(XmlException::class)
  fun <T> deSerialize(input: String, type: Class<T>): T {
    return XmlStreaming.newReader(StringReader(input)).deSerialize(type)
  }


  @JvmStatic
  @Throws(XmlException::class)
  fun <T> deSerialize(reader: Source, type: Class<T>): T {
    return XmlStreaming.newReader(reader).deSerialize(type)
  }

  @JvmStatic
  @Throws(XmlException::class)
  fun toCharArray(content: Source): CharArray {
    return XmlStreaming.newReader(content).toCharArrayWriter().toCharArray()
  }

  @JvmStatic
  @Throws(XmlException::class)
  fun toString(source: Source): String {
    return XmlStreaming.newReader(source).toCharArrayWriter().toString()
  }

}


/** Flag to indicate that the xml declaration should be omitted, when possible.  */
const val FLAG_OMIT_XMLDECL = 1
const val FLAG_REPAIR_NS = 2
const val DEFAULT_FLAGS = FLAG_OMIT_XMLDECL


inline fun<reified T : Any>  deserialize(input:InputStream) = deSerialize(input, T::class.java)

inline fun<reified T : Any>  deserialize(input:Reader) = deSerialize(input, T::class.java)

inline fun<reified T : Any>  deserialize(input:String) = deSerialize(input, T::class.java)


@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val START_DOCUMENT: EventType = EventType.START_DOCUMENT
@JvmField@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val START_ELEMENT : EventType = EventType.START_ELEMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val END_ELEMENT :EventType = EventType.END_ELEMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val COMMENT :EventType = EventType.COMMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val CDSECT :EventType = EventType.CDSECT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "nl.adaptivity.xml.XmlStreaming.EventType"))
val DOCDECL :EventType = EventType.DOCDECL
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "nl.adaptivity.xml.XmlStreaming.EventType"))
val ATTRIBUTE :EventType = EventType.ATTRIBUTE
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val END_DOCUMENT :EventType = EventType.END_DOCUMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "nl.adaptivity.xml.XmlStreaming.EventType"))
val ENTITY_REF :EventType = EventType.ENTITY_REF
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "nl.adaptivity.xml.XmlStreaming.EventType"))
val IGNORABLE_WHITESPACE :EventType = EventType.IGNORABLE_WHITESPACE
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "nl.adaptivity.xml.XmlStreaming.EventType"))
val PROCESSING_INSTRUCTION :EventType = EventType.PROCESSING_INSTRUCTION

@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val CDATA = CDSECT

@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
@JvmField val TEXT = EventType.TEXT
@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
@JvmField val CHARACTERS = EventType.TEXT
