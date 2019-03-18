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

package nl.adaptivity.util.activation

import nl.adaptivity.process.engine.NormalizedMessage

import javax.activation.DataHandler


class AttachmentMap(private val message: NormalizedMessage) : AbstractMap<String, DataHandler>() {


    private inner class Entry(override val key: String) : MutableMap.MutableEntry<String, DataHandler> {

        override val value: DataHandler
            get() {
                return message.getAttachment(key)
            }

        override fun setValue(value: DataHandler): DataHandler {
            val result = message.getAttachment(key)
            message.addAttachment(key, value)
            return result
        }

    }

    private inner class EntryIterator : MutableIterator<MutableMap.MutableEntry<String, DataHandler>> {

        private val backingIterator: Iterator<String>

        init {
            backingIterator = message.attachmentNames.iterator()
        }

        override fun hasNext(): Boolean {
            return backingIterator.hasNext()
        }

        override fun next(): Entry {

            val next = backingIterator.next()
            return Entry(next)
        }

        override fun remove() {
            (backingIterator as MutableIterator).remove()
        }

    }

    private inner class EntrySet : AbstractSet<Map.Entry<String, DataHandler>>() {

        private var _size = -1

        override val size: Int
            get() {
                if (_size < 0) {
                    _size = message.attachmentNames.size
                }
                return _size
            }

        fun remove(element: MutableMap.MutableEntry<String, DataHandler>): Boolean {
            return this@AttachmentMap.remove(element.key) != null
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, DataHandler>> {
            return EntryIterator()
        }

        fun removeAll(elements: Collection<MutableMap.MutableEntry<String, DataHandler>>): Boolean {
            var result = false
            for (o in elements) {
                result = result or (this@AttachmentMap.remove(o.key) != null)
            }
            return result
        }

    }

    override val entries: Set<Map.Entry<String, DataHandler>>
        get() = EntrySet()


    override fun containsKey(key: String): Boolean {
        return keys.contains(key)
    }

    override operator fun get(key: String): DataHandler? {
        return message.getAttachment(key)
    }

    override val keys: MutableSet<String>
        get() = message.attachmentNames as MutableSet<String>


    fun remove(key: String): DataHandler? {
        return message.getAttachment(key).also {
            message.removeAttachment(key)
        }
    }

}
