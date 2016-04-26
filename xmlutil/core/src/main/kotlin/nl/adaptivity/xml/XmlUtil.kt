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

/**
 * Created by pdvrieze on 13/04/16.
 */
@file:JvmName("XmlUtil")
@file:JvmMultifileClass
package nl.adaptivity.xml

import javax.xml.XMLConstants
import javax.xml.namespace.QName
import net.devrieze.util.kotlin.asString
import javax.xml.namespace.NamespaceContext

/** Determine whether the character is xml whitespace. */
fun isXmlWhitespace(char:Char) =
      char == '\u000A' || char =='\u0009' || char =='\u000d' || char == ' '

fun isXmlWhitespace(data: CharArray) = data.all { isXmlWhitespace(it) }

fun isXmlWhitespace(data: CharSequence) = data.all { isXmlWhitespace(it) }

fun qname(namespaceUri:CharSequence?, localname:CharSequence, prefix:CharSequence? = XMLConstants.DEFAULT_NS_PREFIX) =
      QName(namespaceUri.asString()?:XMLConstants.NULL_NS_URI,
            localname.asString(),
            prefix.asString()?:XMLConstants.DEFAULT_NS_PREFIX)


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
  if (prefix == null || XMLConstants.NULL_NS_URI == prefix) return localPart
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
    return QName(reference.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX), name, XMLConstants.DEFAULT_NS_PREFIX)
  }

}


fun CharSequence?.xmlEncode(): String? {
  if (this==null) return null

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
