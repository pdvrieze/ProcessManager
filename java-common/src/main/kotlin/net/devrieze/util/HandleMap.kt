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


interface HandleMap<V> {

  interface HandleAware<T> {

    val handle: Handle<@JvmWildcard T>

    fun setHandleValue(handleValue: Long)
  }

  operator fun iterator(): Iterator<V>

  /**
   * Determine whether the given object is contained in the map. If the object
   * implements [HandleAware] a shortcut is applied instead of looping
   * through all values.

   * @param element The object to check.
   * *
   * @return `true` if it does.
   */
  operator fun contains(element: V): Boolean

  fun isEmpty(): Boolean

  fun containsHandle(handle: Handle<out V>): Boolean

  /**
   * Determine whether the given handle is contained in the map.

   * @param handle The handle to check.
   * *
   * @return `true` if it does.
   */
  operator fun contains(handle: Long): Boolean

  /**
   * Put a new walue into the map. This is thread safe.

   * @param value The value to put into the map.
   * *
   * @return The handle for the value.
   */
  fun <W : V> put(value: W): Handle<W>

  /**
   * Get the element with the given handle.

   * @param handle The handle
   * *
   * @return The element corresponding to the given handle.
   */
  operator fun get(handle: Long): V?

  operator fun get(handle: Handle<out V>): V?

  operator fun set(handle: Long, value: V): V?

  operator fun set(handle: Handle<out V>, value: V): V?

  fun getSize():Int

  fun remove(handle: Handle<out V>): Boolean

  companion object {

    const val NULL_HANDLE: Long = 0
  }

}