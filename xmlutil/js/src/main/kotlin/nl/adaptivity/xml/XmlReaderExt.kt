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

package nl.adaptivity.xml

import nl.adaptivity.util.xml.CompactFragment
import java.io.CharArrayWriter
import java.util.*
import kotlin.browser.document

/**
 * Functions that work on both js/jvm but have different implementations
 */

/**
 * Read the current element (and content) and all its siblings into a fragment.
 * @param in The source stream.
 *
 * @return the fragment
 *
 * @throws XmlException parsing failed
 */
fun XmlReader.siblingsToFragment(): CompactFragment
{
  val dest = (this as JSDomReader).delegate.createDocumentFragment()
  if (!isStarted) {
    if (hasNext()) {
      next()
    } else {
      return CompactFragment(dest)
    }
  }

  val startLocation = locationInfo
  try {

    val missingNamespaces = mapOf<String, String>()
    val gatheringContext: GatheringNamespaceContext? = null
    // If we are at a start tag, the depth will already have been increased. So in that case, reduce one.
    val initialDepth = depth - if (eventType === XmlStreaming.EventType.START_ELEMENT) 1 else 0
    var type: XmlStreaming.EventType? = eventType
    while (type !== XmlStreaming.EventType.END_DOCUMENT && type !== XmlStreaming.EventType.END_ELEMENT && depth >= initialDepth) {
      if (type === XmlStreaming.EventType.START_ELEMENT) {
        val out = XmlStreaming.newWriter(caw)
        writeCurrent(out) // writes the start tag
        out.addUndeclaredNamespaces(this, missingNamespaces)
        out.writeElementContent(missingNamespaces, this) // writes the children and end tag
        out.close()
      } else if (type === XmlStreaming.EventType.TEXT || type === XmlStreaming.EventType.IGNORABLE_WHITESPACE || type === XmlStreaming.EventType.CDSECT) {
        // TODO if empty, ignore ignorable whitespace.
        caw.append(text.xmlEncode())
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
