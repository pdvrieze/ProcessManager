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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util

import java.util.*

/**
 * Created by pdvrieze on 11/10/16.
 */

class ArraySet<T>(initCapacity:Int=10): AbstractSet<T>() {

  private inner class ArraySetIterator(private var pos:Int = 0):MutableListIterator<T> {
    init {
      if (pos<0 || pos>size) throw IndexOutOfBoundsException()
    }

    override fun hasNext() = pos < size

    override fun hasPrevious() = pos>0

    override fun next(): T {
      if (pos>=size) throw NoSuchElementException("The iterator is at the end")
      return this@ArraySet[pos++]
    }

    override fun previous(): T {
      if (pos<=0) throw NoSuchElementException("The iterator is at the end")
      --pos
      return this@ArraySet[pos]
    }

    override fun nextIndex() = pos

    override fun previousIndex() = pos -1

    override fun remove() {
      removeAt(pos-1)
    }

    override fun add(element: T) {
      TODO("Implement add in listIterator for arraySet. It is not implemented yet. This depends on too much implementation details")
    }

    override fun set(element: T) {
      TODO("Not implemented yet. Due to Set semantics this is a bit tricky")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private var buffer = arrayOfNulls<Any?>(Math.max(2,initCapacity)) as Array<T?>

  private var firstElemIdx =0
  private var nextElemIdx = 0

  constructor(base: Iterable<T>) : this((base as? Collection)?.let { it.size +5 } ?: 10) {
    addAll(base)
  }

  constructor(base: Sequence<T>) : this() {
    addAll(base)
  }

  constructor(vararg items:T) : this(items.asList())

  operator fun get(pos: Int): T {
    if (pos<0 || pos>=size) throw IndexOutOfBoundsException("This index is invalid")
    val offset = (firstElemIdx +pos)%buffer.size
    return buffer[offset]!!
  }

  private fun isInRange(offset:Int):Boolean {
    if (firstElemIdx <= nextElemIdx) {
      if (offset< firstElemIdx || offset>= nextElemIdx) return false
    } else if (offset< firstElemIdx && offset>= nextElemIdx) {
      return false
    }
    return true
  }

  override val size: Int
    get() = (nextElemIdx +buffer.size- firstElemIdx)%buffer.size

  override fun iterator(): MutableIterator<T> = listIterator()

  fun listIterator() = listIterator(0)

  fun listIterator(initPos: Int) : MutableListIterator<T> = ArraySetIterator(initPos)

  override fun contains(element: T) = indexOf(element)>=0

  override fun add(element: T): Boolean {
    if (contains(element)) { return false }

    val space = size
    if (space + 2 >= buffer.size) {
      reserve(buffer.size *2)
    }
    buffer[nextElemIdx] = element
    nextElemIdx = (nextElemIdx +1) %buffer.size
    return true
  }

  private fun reserve(reservation: Int) {
    assert(reservation>1) { "The reservation was ${reservation} but should be larger than 1"}
    if (reservation+1<size) { reserve(size+1); return }
    @Suppress("UNCHECKED_CAST")
    val newBuffer = arrayOfNulls<Any?>(reservation) as Array<T?>

    if (firstElemIdx <= nextElemIdx) {
      System.arraycopy(buffer, firstElemIdx, newBuffer, 0, nextElemIdx - firstElemIdx)
      nextElemIdx -= firstElemIdx
    } else {
      System.arraycopy(buffer, firstElemIdx, newBuffer, 0, buffer.size- firstElemIdx)
      System.arraycopy(buffer, 0, newBuffer, buffer.size- firstElemIdx, nextElemIdx)
      nextElemIdx += buffer.size- firstElemIdx
    }
    buffer = newBuffer
    firstElemIdx =0

  }

  override fun remove(element: T): Boolean {
    indexOf(element).let { pos ->
      if (pos<0) { return false; }
      removeAt(pos)
      return true
    }
  }

  @Deprecated("Use removeAt instead", ReplaceWith("removeAt(index)"), DeprecationLevel.WARNING)
  fun remove(index:Int) = removeAt(index)

  fun removeAt(index:Int) = removeAtOffset((index+ firstElemIdx)%buffer.size)

  private fun removeAtOffset(offset: Int): T {
    val result = buffer[offset]!!

    val bufferSize = buffer.size
    if (offset + 1 == nextElemIdx) { // optimize removing the last element
      nextElemIdx--
      if (nextElemIdx < 0) nextElemIdx += bufferSize
      buffer[nextElemIdx] = null
    } else if (offset == firstElemIdx) { // optimize removing the first element
      buffer[firstElemIdx++] = null
      if (firstElemIdx >= bufferSize) firstElemIdx -= bufferSize
    } else if (firstElemIdx < nextElemIdx) { // Default non-wrapped case, don't attempt to optimize smallest copy ___EEEOEEEE___
      System.arraycopy(buffer, offset + 1, buffer, offset, nextElemIdx - offset -1)
      buffer[--nextElemIdx] = null
    } else if (offset < nextElemIdx && offset < firstElemIdx) { // The offset is wrapped as well  EOE_____EEE
      System.arraycopy(buffer, offset + 1, buffer, offset, nextElemIdx -offset - 1)
      buffer[--nextElemIdx] = null
    } else { // ofset>tail -> tail wrapped, we are in the head section EEE_____EOE
      System.arraycopy(buffer, firstElemIdx, buffer, firstElemIdx + 1, offset - firstElemIdx)
      buffer[firstElemIdx++] = null
    }
    return result
  }

  fun indexOf(element: T):Int {
    if(firstElemIdx <= nextElemIdx) {
      for(i in firstElemIdx until nextElemIdx) {
        if (buffer[i]==element) { return i- firstElemIdx; }
      }
    } else {
      for(i in (firstElemIdx until buffer.size)) {
        if (buffer[i]==element) { return i- firstElemIdx; }
      }
      for(i in (0 until nextElemIdx)) {
        if (buffer[i]==element) { return i+buffer.size- firstElemIdx; }
      }
    }
    return -1
  }

  override fun clear() {
    buffer.fill(null)
    firstElemIdx =0
    nextElemIdx =0
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
