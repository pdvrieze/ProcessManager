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

package nl.adaptivity.xml

import javax.xml.XMLConstants
import javax.xml.namespace.QName
import net.devrieze.util.kotlin.asString

/** Determine whether the character is xml whitespace. */
fun isXmlWhitespace(char:Char) =
      char == '\u000A' || char =='\u0009' || char =='\u000d' || char == ' '

fun isXmlWhitespace(data: CharArray) = data.all { isXmlWhitespace(it) }

fun isXmlWhitespace(data: CharSequence) = data.all { isXmlWhitespace(it) }

fun qname(namespaceUri:CharSequence?, localname:CharSequence, prefix:CharSequence? = XMLConstants.DEFAULT_NS_PREFIX) =
      QName(namespaceUri.asString()?:XMLConstants.NULL_NS_URI,
            localname.asString(),
            prefix.asString()?:XMLConstants.DEFAULT_NS_PREFIX)