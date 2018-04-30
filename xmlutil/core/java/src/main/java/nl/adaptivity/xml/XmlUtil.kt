/*
 * Copyright (c) 2018.
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
@file:JvmName("XmlUtilJava")
@file:JvmMultifileClass
package nl.adaptivity.xml

import net.devrieze.util.kotlin.asString
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


