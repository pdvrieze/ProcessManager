/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine.impl

import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.util.CompactFragment
import javax.xml.namespace.QName

inline fun CompactFragment(generator: (XmlWriter) -> Unit): CompactFragment {
    // TODO make this actually collect namespaces "outside"
    val string = buildString {
        XmlStreaming.newWriter(this).use{ writer ->
            writer.smartStartTag(QName("dummy")) { generator(writer) }
        }
    }
    val actualStart = string.indexOf('>')+1
    val actualEnd = string.lastIndexOf('<')
    val content = string.substring(actualStart, actualEnd)
    return CompactFragment(emptyList(), content)
}
