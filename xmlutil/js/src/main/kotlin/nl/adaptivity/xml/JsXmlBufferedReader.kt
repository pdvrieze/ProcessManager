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


typealias XmlBufferedReader = JsXmlBufferedReader
/**
 * Created by pdvrieze on 03/04/17.
 */
open class JsXmlBufferedReader(delegate:XmlReader): XmlBufferedReaderBase(delegate)
{
  private val peekBuffer = mutableListOf<XmlEvent>()

  override protected val hasPeekItems get() = peekBuffer.isNotEmpty()

  override protected fun peekFirst(): XmlEvent {
    return peekBuffer.first()
  }

  override protected fun peekLast(): XmlEvent {
    return peekBuffer.last()
  }

  override fun bufferRemoveLast() = peekBuffer.removeAt(peekBuffer.lastIndex)

  override fun bufferRemoveFirst() = peekBuffer.removeAt(0)

  override protected fun add(event: XmlEvent) {
    peekBuffer.add(event)
  }

  override protected fun addAll(events: Collection<XmlEvent>) {
    peekBuffer.addAll(events)
  }

  override fun close()
  {
    super.close()
    peekBuffer.clear()
  }
}