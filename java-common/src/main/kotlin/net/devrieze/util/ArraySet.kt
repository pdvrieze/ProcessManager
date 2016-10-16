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

package net.devrieze.util

import java.util.*

/**
 * Created by pdvrieze on 11/10/16.
 */

class ArraySet<T>(initCapacity:Int=10): AbstractSet<T>() {

  private inner class ArraySetIterator:MutableIterator<T> {
    private var pos=0

    override fun hasNext(): Boolean {
      return pos < size
    }

    override fun next(): T {
      if (! isInRange(pos)) throw NoSuchElementException("The iterator is at the end")
      val result = this@ArraySet[pos]
      pos++;
      return result
    }

    override fun remove() {
      remove(pos-1)
    }
  }

  private operator fun get(pos: Int): T {
    if (!isInRange(pos)) throw IndexOutOfBoundsException("This index is invalid")
    val offset = (head+pos)%buffer.size
    return buffer[offset] as T;
  }

  private fun isInRange(pos:Int):Boolean {
    if (head<=tail) {
      if (pos<head || pos>=tail) return false
    } else if (pos<head && pos>=tail) {
      return false
    }
    return true
  }

  private var buffer = arrayOfNulls<Any?>(Math.max(1,initCapacity))
  private var head =0
  private var tail = 0

  override val size: Int
    get() = (tail+buffer.size-head)%buffer.size

  override fun iterator(): MutableIterator<T> {
    return ArraySetIterator()
  }

  override fun contains(element: T) = if (indexOf(element)>=0) true else false

  override fun add(element: T): Boolean {
    if (contains(element)) { return false }

    val space = if (tail<head) (tail+buffer.size-head) else (tail-head)
    if (space + 1 >= buffer.size) {
      reserve(space)
    }
    buffer[tail] = element
    tail = (tail+1) %buffer.size
    return true
  }

  private fun reserve(reservation: Int) {
    if (reservation+1<size) { reserve(size+1) }
    val newBuffer = arrayOfNulls<Any?>(reservation)

    if (head <= tail) {
      System.arraycopy(buffer, head, newBuffer, 0, tail - head)
      tail-=head
      head=0
    } else {
      System.arraycopy(buffer, head, newBuffer, 0, buffer.size-head)
      System.arraycopy(buffer, 0, newBuffer, buffer.size-head, tail)
      tail += buffer.size-head
    }

  }

  override fun remove(element: T): Boolean {
    indexOf(element).let { pos ->
      if (pos<0) { return false; }
      remove(pos)
      return true
    }
  }

  fun remove(index:Int) = removeAtOffset((index+head)%buffer.size)

  private fun removeAtOffset(offset: Int): T {
    val result = this[offset]

    val bufferSize = buffer.size
    if (offset + 1 == tail) { // optimize removing the last element
      buffer[tail--] = null;
      if (tail < 0) tail += bufferSize
    } else if (offset == head) { // optimize removing the first element
      buffer[head++] = null;
      if (head >= bufferSize) head -= bufferSize
    } else if (head < tail) { // Default non-wrapped case, don't attempt to optimize smallest copy ___EEEOEEEE___
      System.arraycopy(buffer, offset + 1, buffer, offset, head - offset)
      buffer[tail--] = null
    } else if (offset < tail && offset < head) { // The offset is wrapped as well  EOE_____EEE
      System.arraycopy(buffer, offset + 1, buffer, offset, tail - 1)
      buffer[tail--] = null
    } else { // ofset>tail -> tail wrapped, we are in the head section EEE_____EOE
      System.arraycopy(buffer, head, buffer, head + 1, offset - head)
      buffer[head++] = null
    }
    return result
  }

  public fun indexOf(element: T):Int {
    if(head<=tail) {
      for(i in head until tail) {
        if (buffer[i]==element) { return i-head; }
      }
    } else {
      for(i in (head until buffer.size)) {
        if (buffer[i]==element) { return i-head; }
      }
      for(i in (0 until tail)) {
        if (buffer[i]==element) { return i+buffer.size-head; }
      }
    }
    return -1
  }

  override fun clear() {
    buffer.fill(null)
    head=0
    tail=0
  }
}