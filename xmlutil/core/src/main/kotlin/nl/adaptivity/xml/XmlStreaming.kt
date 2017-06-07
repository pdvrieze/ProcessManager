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

import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.JavaCompactFragment
import nl.adaptivity.xml.XmlEvent.*
import nl.adaptivity.xml.EventType
import nl.adaptivity.xml.XmlStreaming.deSerialize
import nl.adaptivity.xml.jvm.deSerialize
import nl.adaptivity.xml.jvm.toCharArrayWriter
import java.io.*
import java.util.*
import javax.xml.transform.Result
import javax.xml.transform.Source


/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
object XmlStreaming {


  private val serviceLoader:ServiceLoader<XmlStreamingFactory> by lazy {
    val service = XmlStreamingFactory::class.java
    ServiceLoader.load(service, service.classLoader)
  }

  private var _factory: XmlStreamingFactory? = null

  private val factory: XmlStreamingFactory
    get() {
      return _factory ?: serviceLoader.first().apply { _factory = this }
    }

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
  fun newReader(inputStream: InputStream, encoding: String): XmlReader {
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

/*
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val START_DOCUMENT: EventType = EventType.START_DOCUMENT
  @JvmField@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val START_ELEMENT : EventType = EventType.START_ELEMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val END_ELEMENT : EventType = EventType.END_ELEMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val COMMENT : EventType = EventType.COMMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val CDSECT : EventType = EventType.CDSECT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val DOCDECL : EventType = EventType.DOCDECL
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val ATTRIBUTE : EventType = EventType.ATTRIBUTE
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val END_DOCUMENT : EventType = EventType.END_DOCUMENT
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val ENTITY_REF : EventType = EventType.ENTITY_REF
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val IGNORABLE_WHITESPACE : EventType = EventType.IGNORABLE_WHITESPACE
  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val PROCESSING_INSTRUCTION : EventType = EventType.PROCESSING_INSTRUCTION

  @JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  val CDATA = EventType.CDSECT

  @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  @JvmField val TEXT = EventType.TEXT
  @Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
  @JvmField val CHARACTERS = EventType.TEXT
*/
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

  fun toString(value: XmlSerializable): String {
    return StringWriter().apply {
      XmlStreaming.newWriter(this).use { writer ->
        value.serialize(writer)
      }
    }.toString()
  }

}


/** Flag to indicate that the xml declaration should be omitted, when possible.  */
const val FLAG_OMIT_XMLDECL = 1
const val FLAG_REPAIR_NS = 2
const val DEFAULT_FLAGS = FLAG_OMIT_XMLDECL


inline fun<reified T : Any>  deserialize(input:InputStream) = deSerialize(input, T::class.java)

inline fun<reified T : Any>  deserialize(input:Reader) = deSerialize(input, T::class.java)

inline fun<reified T : Any>  deserialize(input:String) = deSerialize(input, T::class.java)

fun CompactFragment(content:String): CompactFragment = JavaCompactFragment(content)
fun CompactFragment(namespaces:Iterable<Namespace>, content:CharArray?): CompactFragment = JavaCompactFragment(namespaces, content ?: kotlin.CharArray(0))
fun CompactFragment(namespaces:Iterable<Namespace>, content:String?): CompactFragment = JavaCompactFragment(namespaces, content?.toCharArray() ?: kotlin.CharArray(0))



@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.START_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val START_DOCUMENT: EventType = EventType.START_DOCUMENT
@JvmField@Deprecated("Don't use it", ReplaceWith("EventType.START_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val START_ELEMENT : EventType = EventType.START_ELEMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_ELEMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val END_ELEMENT : EventType = EventType.END_ELEMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.COMMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val COMMENT : EventType = EventType.COMMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val CDSECT : EventType = EventType.CDSECT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.DOCDECL", "nl.adaptivity.xml.XmlStreaming.EventType"))
val DOCDECL : EventType = EventType.DOCDECL
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ATTRIBUTE", "nl.adaptivity.xml.XmlStreaming.EventType"))
val ATTRIBUTE : EventType = EventType.ATTRIBUTE
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.END_DOCUMENT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val END_DOCUMENT : EventType = EventType.END_DOCUMENT
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.ENTITY_REF", "nl.adaptivity.xml.XmlStreaming.EventType"))
val ENTITY_REF : EventType = EventType.ENTITY_REF
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.IGNORABLE_WHITESPACE", "nl.adaptivity.xml.XmlStreaming.EventType"))
val IGNORABLE_WHITESPACE : EventType = EventType.IGNORABLE_WHITESPACE
@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.PROCESSING_INSTRUCTION", "nl.adaptivity.xml.XmlStreaming.EventType"))
val PROCESSING_INSTRUCTION : EventType = EventType.PROCESSING_INSTRUCTION

@JvmField @Deprecated("Don't use it", ReplaceWith("EventType.CDSECT", "nl.adaptivity.xml.XmlStreaming.EventType"))
val CDATA = CDSECT

@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
@JvmField val TEXT = EventType.TEXT
@Deprecated("Don't use it", ReplaceWith("EventType.TEXT", "nl.adaptivity.xml.XmlStreaming.EventType"))
@JvmField val CHARACTERS = EventType.TEXT
