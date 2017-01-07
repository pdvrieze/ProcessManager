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

package nl.adaptivity.process.util


class ReadonlyArrayIterator<T> @JvmOverloads constructor(private val data: Array<T>, initialPos: Int = 0) : ListIterator<T> {
  var pos = initialPos;

  init {
    if (initialPos<0 || initialPos>data.size) throw IndexOutOfBoundsException(initialPos.toString())
  }

  override fun hasNext() = pos<data.size

  override fun hasPrevious() = pos>0

  override fun next() = data[pos].apply { pos++ }

  override fun nextIndex() = pos

  override fun previous():T { pos--; return data[pos] }

  override fun previousIndex() = pos - 1
}