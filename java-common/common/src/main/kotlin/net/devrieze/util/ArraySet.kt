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

package net.devrieze.util

import nl.adaptivity.util.multiplatform.arraycopy
import nl.adaptivity.util.multiplatform.fill

/**
 * Created by pdvrieze on 11/10/16.
 */

private typealias BufferPos = Int

private typealias OuterPos = Int

class ArraySet<T>(initCapacity: Int = 10) : AbstractMutableSet<T>() {

    private inner class ArraySetIterator(private var pos: OuterPos = 0) : MutableListIterator<T> {
        init {
            if (pos < 0 || pos > size) throw IndexOutOfBoundsException()
        }

        override fun hasNext() = pos < size

        override fun hasPrevious() = pos > 0

        override fun next(): T {
            if (pos >= size) throw NoSuchElementException("The iterator is at the end")
            return this@ArraySet[pos++]
        }

        override fun previous(): T {
            if (pos <= 0) throw NoSuchElementException("The iterator is at the end")
            --pos
            return this@ArraySet[pos]
        }

        override fun nextIndex() = pos

        override fun previousIndex() = pos - 1

        override fun remove() {
            removeAt(pos - 1)
        }

        override fun add(element: T) {
            add(element)
        }


        override fun set(element: T) {
            if (this@ArraySet[pos] == element) return
            val otherPos = indexOf(element)
            if (otherPos >= 0) {
                swap(otherPos, pos)
            } else {
                val previous = buffer[pos]!!
                buffer[pos] = element
                add(previous)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private var buffer = arrayOfNulls<Any?>(maxOf(2, initCapacity)) as Array<T?>

    private var firstElemIdx = 0
    private var nextElemIdx = 0

    constructor(base: Iterable<T>) : this((base as? Collection)?.let { it.size + 5 } ?: 10) {
        addAll(base)
    }

    constructor(base: Sequence<T>) : this() {
        addAll(base)
    }

    constructor(vararg items: T) : this(items.asList())

    operator fun get(pos: OuterPos): T {
        if (pos < 0 || pos >= size) throw IndexOutOfBoundsException("This index is invalid")
        val offset = (firstElemIdx + pos) % buffer.size
        return buffer[offset]!!
    }

    operator fun set(pos: OuterPos, value: T): T {
        return get(pos).apply {
            if (this != value) {
                if (!contains(value)) {
                    buffer[pos.toBufferPos()] = value
                } else { // It's already contained, so just remove the previous value, don't add the new one
                    removeAt(pos)
                }
            }
        }
    }

    private fun isInRange(offset: Int): Boolean {
        if (firstElemIdx <= nextElemIdx) {
            if (offset < firstElemIdx || offset >= nextElemIdx) return false
        } else if (offset < firstElemIdx && offset >= nextElemIdx) {
            return false
        }
        return true
    }

    override val size: Int
        get() = (nextElemIdx + buffer.size - firstElemIdx) % buffer.size

    override fun isEmpty() = size == 0

    override fun iterator(): MutableIterator<T> = listIterator()

    fun listIterator() = listIterator(0)

    fun listIterator(initPos: OuterPos): MutableListIterator<T> = ArraySetIterator(initPos)

    override fun contains(element: T) = indexOf(element) >= 0

    override fun add(element: T): Boolean {
        if (contains(element)) {
            return false
        }

        val space = size
        if (space + 2 >= buffer.size) {
            reserve(buffer.size * 2)
        }
        buffer[nextElemIdx] = element
        nextElemIdx = (nextElemIdx + 1) % buffer.size
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        reserve(size + elements.size)

        return elements.fold(false) { acc, element ->
            add(element) or acc
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val it = iterator()
        var changed = false
        while (it.hasNext()) {
            if (it.next() !in elements) {
                it.remove()
                changed = true
            }
        }
        return changed
    }

    private fun reserve(reservation: Int) {
        if (reservation <= 1) throw IllegalArgumentException(
            "The reservation was ${reservation} but should be larger than 1")
        if (reservation + 1 < size) {
            reserve(size + 1); return
        }
        @Suppress("UNCHECKED_CAST")
        val newBuffer = arrayOfNulls<Any?>(reservation) as Array<T?>

        if (firstElemIdx <= nextElemIdx) {
            arraycopy(buffer, firstElemIdx, newBuffer, 0, nextElemIdx - firstElemIdx)
            nextElemIdx -= firstElemIdx
        } else {
            arraycopy(buffer, firstElemIdx, newBuffer, 0, buffer.size - firstElemIdx)
            arraycopy(buffer, 0, newBuffer, buffer.size - firstElemIdx, nextElemIdx)
            nextElemIdx += buffer.size - firstElemIdx
        }
        buffer = newBuffer
        firstElemIdx = 0

    }

    override fun remove(element: T): Boolean {
        indexOf(element).let { pos ->
            if (pos < 0) {
                return false; }
            removeAt(pos)
            return true
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return elements.fold(false) { acc, element->
            remove(element) or acc
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun OuterPos.toBufferPos(): BufferPos = when {
        this < 0 || this >= size -> throw IndexOutOfBoundsException("Invalid position: $this")
        else                     -> (this + firstElemIdx) % buffer.size
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun BufferPos.toOuterPos(): OuterPos = (buffer.size + this - firstElemIdx) % buffer.size

    fun swap(first: OuterPos, second: OuterPos) {
        bufferSwap(first.toBufferPos(), second.toBufferPos())
    }

    private fun bufferSwap(firstPos: BufferPos, secondPos: BufferPos) {
        val firstValue = buffer[firstPos]
        buffer[firstPos] = buffer[secondPos]
        buffer[secondPos] = firstValue
    }

    @Deprecated("Use removeAt instead", ReplaceWith("removeAt(index)"), DeprecationLevel.WARNING)
    fun remove(index: OuterPos) = removeAt(index)

    fun removeAt(index: OuterPos) = removeAtOffset(index.toBufferPos())

    private fun removeAtOffset(offset: BufferPos): T {
        val result = buffer[offset] as T

        val bufferSize = buffer.size
        if (offset + 1 == nextElemIdx) { // optimize removing the last element
            nextElemIdx--
            if (nextElemIdx < 0) nextElemIdx += bufferSize
            buffer[nextElemIdx] = null
        } else if (offset == firstElemIdx) { // optimize removing the first element
            buffer[firstElemIdx++] = null
            if (firstElemIdx >= bufferSize) firstElemIdx -= bufferSize
        } else if (firstElemIdx < nextElemIdx) { // Default non-wrapped case, don't attempt to optimize smallest copy ___EEEOEEEE___
            arraycopy(buffer, offset + 1, buffer, offset, nextElemIdx - offset - 1)
            buffer[--nextElemIdx] = null
        } else if (offset < nextElemIdx && offset < firstElemIdx) { // The offset is wrapped as well  EOE_____EEE
            arraycopy(buffer, offset + 1, buffer, offset, nextElemIdx - offset - 1)
            buffer[--nextElemIdx] = null
        } else { // ofset>tail -> tail wrapped, we are in the head section EEE_____EOE
            arraycopy(buffer, firstElemIdx, buffer, firstElemIdx + 1, offset - firstElemIdx)
            buffer[firstElemIdx++] = null
        }
        return result
    }

    fun indexOf(element: T): OuterPos {
        if (firstElemIdx <= nextElemIdx) {
            for (i in firstElemIdx until nextElemIdx) {
                if (buffer[i] == element) {
                    return i.toOuterPos()
                }
            }
        } else {
            for (i in (firstElemIdx until buffer.size)) {
                if (buffer[i] == element) {
                    return i.toOuterPos()
                }
            }
            for (i in (0 until nextElemIdx)) {
                if (buffer[i] == element) {
                    return i.toOuterPos()
                }
            }
        }
        return -1
    }

    override fun clear() {


        fill(buffer, null)
        firstElemIdx = 0
        nextElemIdx = 0
    }
}


/**
 * Returns a mutable set containing all distinct elements from the given sequence.
 *
 * The returned set preserves the element iteration order of the original sequence.
 */
fun <T> Sequence<T>.toMutableArraySet(): MutableSet<T> {
    return ArraySet<T>().apply {
        for (item in this@toMutableArraySet) add(item)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Sequence<T>.toArraySet(): Set<T> = toMutableArraySet()


fun <T> Iterable<T>.toMutableArraySet(): MutableSet<T> = ArraySet<T>(this)

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Iterable<T>.toArraySet(): Set<T> = toMutableArraySet()
