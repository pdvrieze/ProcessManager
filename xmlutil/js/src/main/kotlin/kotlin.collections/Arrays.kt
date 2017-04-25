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

package kotlin.collections

fun <T> Array<T>.fill(element: T, fromIndex:Int, toIndex:Int) {
  if (fromIndex>toIndex) throw IllegalArgumentException("From index after to index")
  if (fromIndex<0) throw IndexOutOfBoundsException("From index out of bounds $fromIndex")
  if (toIndex>size) throw IndexOutOfBoundsException("To index out of bounds $toIndex>$size")
  for(i in fromIndex until toIndex) {
    this[i] = element
  }
}

fun String.toCharArray():CharArray {
  return CharArray(size) { this[it] }
}

fun CharArray.toCharSequence(): CharSequence {
  return object : CharSequence {
    override val length get() = this@toCharSequence.size

    override fun get(index: Int) = this@toCharSequence[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
      if (startIndex<0) { throw IndexOutOfBoundsException("$startIndex<0") }
      if (endIndex>length) { throw IndexOutOfBoundsException("$endIndex>$length") }
      if (startIndex>endIndex) { throw IllegalArgumentException("$startIndex>$endIndex") }
      return CharArraySubSequence(this@toCharSequence, startIndex, endIndex)
    }

    override fun toString() = buildString { append(this) }
  }
}

private class CharArraySubSequence(private val base:CharArray, private val startIndex:Int, private val endIndex:Int): CharSequence {

  override val length get() = endIndex - startIndex

  override fun get(index: Int) = base[startIndex + index]

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence
  {
    if (startIndex<0) { throw IndexOutOfBoundsException("$startIndex<0") }
    if (endIndex>length) { throw IndexOutOfBoundsException("$endIndex>$length") }
    if (startIndex>endIndex) { throw IllegalArgumentException("$startIndex>$endIndex") }
    return CharArraySubSequence(base, this.startIndex+startIndex, this.startIndex+endIndex)
  }

  override fun toString() = buildString { append(this) }
}