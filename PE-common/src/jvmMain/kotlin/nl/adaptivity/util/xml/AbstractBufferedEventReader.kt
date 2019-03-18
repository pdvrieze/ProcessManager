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

import nl.adaptivity.xmlutil.isXmlWhitespace
import java.util.*
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent


abstract class AbstractBufferedEventReader : AbstractEventReader() {
    private var peekBuffer: ArrayDeque<XMLEvent> = ArrayDeque()

    protected val isPeekBufferEmpty: Boolean
        get() = peekBuffer.isEmpty()

    @Throws(XMLStreamException::class)
    override fun nextEvent(): XMLEvent {
        if (!peekBuffer.isEmpty()) {
            return peekBuffer.removeFirst()
        }
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        peek()
        return peekBuffer.removeFirst()
    }

    override fun hasNext(): Boolean {
        if (!peekBuffer.isEmpty()) {
            return true
        }
        try {
            return peek() != null
        } catch (e: XMLStreamException) {
            throw RuntimeException(e)
        }

    }

    protected fun stripWhiteSpaceFromPeekBuffer() {
        while (peekBuffer.size > 0 && peekBuffer.peekLast().isCharacters && isXmlWhitespace(
                peekBuffer.peekLast()
                    .asCharacters()
                    .data)) {
            peekBuffer.removeLast()
        }
    }

    protected fun peekFirst(): XMLEvent {
        return peekBuffer.peekFirst()
    }

    protected fun add(event: XMLEvent) {
        peekBuffer.addLast(event)
    }

    protected fun addAll(events: Collection<XMLEvent>) {
        peekBuffer.addAll(events)
    }

    @Throws(XMLStreamException::class)
    override fun close() {
        peekBuffer.clear()
        peekBuffer = ArrayDeque()
    }

}
