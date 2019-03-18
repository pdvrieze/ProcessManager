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

package nl.adaptivity.util.xml

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.Characters
import javax.xml.stream.events.XMLEvent


abstract class AbstractEventReader : XMLEventReader {

    override fun next(): XMLEvent = nextEvent()

    @Throws(XMLStreamException::class)
    override fun getElementText(): String {
        val result = StringBuilder()
        var event = nextEvent()
        while (!event.isEndElement) {
            if (event.isCharacters) {
                result.append((event as Characters).data)
            } else if (event.isAttribute) {
                // ignore
            } else {
                throw XMLStreamException("Unexpected child")
            }
            event = nextEvent()
        }
        return result.toString()
    }

    @Throws(XMLStreamException::class)
    override fun nextTag(): XMLEvent {
        var event = nextEvent()
        while (!event.isEndDocument) {
            if (event.isStartElement) {
                return event
            } else if (event.isEndElement) {
                return event
            } else if (event.isAttribute) { // ignore
            } else if (event.isCharacters) {
                if (!event.asCharacters().isIgnorableWhiteSpace) {
                    throw XMLStreamException("Non-whitespace text encountered")
                }
            } else {
                throw XMLStreamException("Unexpected tags encountered")
            }
            event = nextEvent()
        }
        throw XMLStreamException("Unexpected end of document")
    }

}
