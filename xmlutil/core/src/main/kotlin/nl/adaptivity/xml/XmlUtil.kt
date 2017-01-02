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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by pdvrieze on 13/04/16.
 */
@file:JvmName("XmlUtil")
@file:JvmMultifileClass
package nl.adaptivity.xml

import net.devrieze.util.kotlin.asString
import nl.adaptivity.xml.XmlStreaming.EventType
import java.util.*
import javax.xml.XMLConstants.DEFAULT_NS_PREFIX
import javax.xml.XMLConstants.NULL_NS_URI
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.transform.Result
import javax.xml.transform.Source

/** Determine whether the character is xml whitespace. */
fun isXmlWhitespace(char:Char) =
      char == '\u000A' || char =='\u0009' || char =='\u000d' || char == ' '

fun isXmlWhitespace(data: CharArray) = data.all { isXmlWhitespace(it) }

fun isXmlWhitespace(data: CharSequence) = data.all { isXmlWhitespace(it) }

fun qname(namespaceUri:CharSequence?, localname:CharSequence, prefix:CharSequence? = DEFAULT_NS_PREFIX) =
      QName(namespaceUri.asString()?: NULL_NS_URI,
            localname.asString(),
            prefix.asString()?: DEFAULT_NS_PREFIX)


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

fun QName.toCName(): String {
  if (prefix == null || NULL_NS_URI == prefix) return localPart
  return "$prefix:$localPart"
}


/**
 * Convert a prefixed element name (CNAME) to a qname. If there is no prefix, the default prefix is used.
 * @param reference The namespace context to use to resolve the name.
 *
 * @param name The name to resolve
 *
 * @return A resolved qname.
 */
fun NamespaceContext.asQName(name: String): QName {
  val reference: NamespaceContext = this
  val colPos = name.indexOf(':')
  if (colPos >= 0) {
    val prefix = name.substring(0, colPos)
    return QName(reference.getNamespaceURI(prefix), name.substring(colPos + 1), prefix)
  } else {
    return QName(reference.getNamespaceURI(DEFAULT_NS_PREFIX), name, DEFAULT_NS_PREFIX)
  }

}

fun CharSequence?.xmlEncode(): String? {
  return this?.xmlEncode()
}

@JvmName("xmlEncodeNotNull")
fun CharSequence.xmlEncode(): String {

  return buildString {
    for (c in this@xmlEncode) {
      when (c) {
        '<'  -> append("&lt;")
        '>'  -> append("&gt;")
        '&'  -> append("&amp;")
        else -> append(c)
      }
    }
  }
}


private class NamespaceInfo(val prefix: String, val url: String)

@Throws(XmlException::class)
fun cannonicallize(source: Source, result: Result) {


  fun addNamespace(collectedNS: MutableMap<String, NamespaceInfo>, prefix: String, namespaceURI: String) {
    if (namespaceURI != NULL_NS_URI && namespaceURI !in collectedNS) {
      collectedNS[namespaceURI] = NamespaceInfo(prefix, namespaceURI)
    }
  }

  val collectedNS = HashMap<String, NamespaceInfo>()
  XmlStreaming.newReader(source).use { reader ->
    while (reader.hasNext()) {
      when (reader.next()) {
        EventType.START_ELEMENT -> {
          addNamespace(collectedNS, reader.prefix.toString(), reader.namespaceUri.toString())

          for (i in reader.attributeCount - 1 downTo 0) {
            addNamespace(collectedNS,
                         reader.getAttributePrefix(i).toString(),
                         reader.getAttributeNamespace(i).toString())
          }
        }
        else                    -> {
        } /* Do nothing*/
      }// ignore
    }
  }

  // TODO add wrapper methods that get stream readers and writers analogous to the event writers and readers
  XmlStreaming.newWriter(result, true).use { writer ->

    XmlStreaming.newReader(source).use { reader ->

      var first = true
      while (reader.hasNext()) {
        when (reader.next()) {
        // TODO extract the default elements to a separate method that is also used to copy StreamReader to StreamWriter without events.
          EventType.START_ELEMENT -> {
            if (first) {
              var needsDecl = false
              var namespaceInfo = collectedNS[reader.namespaceUri]
              if (namespaceInfo != null) {
                if (first && reader.prefix == DEFAULT_NS_PREFIX) { // If the first element in the reader has a default prefix, leave this so
                  namespaceInfo = NamespaceInfo("", namespaceInfo.url)
                  collectedNS[namespaceInfo.url] = namespaceInfo
                }
                if (writer.getNamespaceUri(namespaceInfo.prefix) != reader.namespaceUri) {
                  needsDecl = true
                }
                writer.setPrefix(namespaceInfo.prefix, namespaceInfo.url)
                writer.startTag(namespaceInfo.prefix, reader.localName.toString(), namespaceInfo.url)
              } else { // no namespace info (probably no namespace at all)
                writer.startTag(reader.namespaceUri, reader.localName, reader.prefix)
              }

              if (first) {
                first = false
                for (ns in collectedNS.values) {
                  writer.setPrefix(ns.prefix, ns.url)
                  writer.namespaceAttr(ns.prefix, ns.url)
                }
              } else {
                if (needsDecl && namespaceInfo != null) {
                  writer.namespaceAttr(namespaceInfo.prefix, namespaceInfo.url)
                }
              }
            } else { // not first

              writer.startTag(reader.namespaceUri, reader.localName, null)
            }
            val ac = reader.attributeCount
            for (i in 0..ac - 1) {
              writer.attribute(reader.getAttributeNamespace(i),
                               reader.getAttributeLocalName(i),
                               null,
                               reader.getAttributeValue(i))
            }
          }
          else                    -> reader.writeCurrent(writer)
        }
      }
    }
  }
}
