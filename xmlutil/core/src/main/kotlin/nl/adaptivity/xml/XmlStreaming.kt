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

import javax.xml.stream.XMLStreamException
import javax.xml.transform.Result
import javax.xml.transform.Source

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer


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
    override fun newReader(inputStream: InputStream, encoding: String): XmlReader {
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
    fun newReader(inputStream: InputStream, encoding: String): XmlReader
  }

  enum class EventType {
    START_DOCUMENT {
      override fun XmlReader.createEvent() =
            StartDocumentEvent(locationInfo, version, encoding, standalone)
    },
    START_ELEMENT {
      @Throws(XmlException::class)
      override fun XmlReader.createEvent() =
            StartElementEvent(locationInfo, namespaceUri, localName, prefix, attributes, namespaceDecls)
    },
    END_ELEMENT {
      @Throws(XmlException::class)
      override fun XmlReader.createEvent() =
            EndElementEvent(locationInfo, namespaceUri, localName, prefix)
    },
    COMMENT {
      override fun XmlReader.createEvent() =
            TextEvent(locationInfo, COMMENT, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.comment(textEvent.text)
    },
    TEXT {
      override fun XmlReader.createEvent() =
            TextEvent(locationInfo, TEXT, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.text(textEvent.text)
    },
    CDSECT {
      override fun XmlReader.createEvent() = TextEvent(locationInfo, CDSECT, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.cdsect(textEvent.text)
    },
    DOCDECL {
      override fun XmlReader.createEvent() = TextEvent(locationInfo, DOCDECL, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.docdecl(textEvent.text)
    },
    END_DOCUMENT {
      override fun XmlReader.createEvent() = EndDocumentEvent(locationInfo)
    },
    ENTITY_REF {
      override fun XmlReader.createEvent() = TextEvent(locationInfo, ENTITY_REF, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.entityRef(textEvent.text)
    },
    IGNORABLE_WHITESPACE {
      override fun XmlReader.createEvent() = TextEvent(locationInfo, IGNORABLE_WHITESPACE, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.ignorableWhitespace(textEvent.text)
    },
    ATTRIBUTE {
      @Throws(XmlException::class)
      override fun XmlReader.createEvent() = Attribute(locationInfo, namespaceUri, localName, prefix, text)
    },
    PROCESSING_INSTRUCTION {
      override fun XmlReader.createEvent() = TextEvent(locationInfo, PROCESSING_INSTRUCTION, text)

      @Throws(XmlException::class)
      override fun writeEvent(writer: XmlWriter, textEvent: TextEvent) = writer.processingInstruction(textEvent.text)
    };

    @Throws(XmlException::class)
    open fun writeEvent(writer: XmlWriter, textEvent: TextEvent): Unit = throw UnsupportedOperationException("This is not generally supported, only by text types")

    @Throws(XmlException::class)
    abstract fun XmlReader.createEvent(): XmlEvent
  }

  val START_DOCUMENT = EventType.START_DOCUMENT
  val START_ELEMENT = EventType.START_ELEMENT
  val END_ELEMENT = EventType.END_ELEMENT
  val COMMENT = EventType.COMMENT
  val TEXT = EventType.TEXT
  val CDSECT = EventType.CDSECT
  val DOCDECL = EventType.DOCDECL
  val ATTRIBUTE = EventType.ATTRIBUTE
  val END_DOCUMENT = EventType.END_DOCUMENT
  val ENTITY_REF = EventType.ENTITY_REF
  val IGNORABLE_WHITESPACE = EventType.IGNORABLE_WHITESPACE
  val PROCESSING_INSTRUCTION = EventType.PROCESSING_INSTRUCTION

  val CDATA = CDSECT
  val CHARACTERS = TEXT

  private var _factory: XmlStreamingFactory? = null

  private val factory: XmlStreamingFactory
    get() = _factory?:DefaultFactory.DEFAULTFACTORY

  @Throws(XmlException::class)
  @JvmOverloads fun newWriter(result: Result, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(result, repairNamespaces)
  }

  @Throws(XmlException::class)
  @JvmOverloads
  fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(outputStream, encoding, repairNamespaces)
  }

  @Throws(XmlException::class)
  @JvmOverloads
  fun newWriter(writer: Writer, repairNamespaces: Boolean = false): XmlWriter {
    return factory.newWriter(writer, repairNamespaces)
  }

  @Throws(XmlException::class)
  fun newReader(inputStream: InputStream, encoding: String): XmlReader {
    return factory.newReader(inputStream, encoding)
  }

  @Throws(XmlException::class)
  fun newReader(reader: Reader): XmlReader {
    return factory.newReader(reader)
  }

  @Throws(XmlException::class)
  fun newReader(source: Source): XmlReader {
    return factory.newReader(source)
  }

  fun setFactory(factory: XmlStreamingFactory) {
    _factory = factory
  }
}
