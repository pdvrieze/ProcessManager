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

interface HandleMap<V:Any> : Iterable<V> {

  /**
   * Determine whether the given object is contained in the map. If the object
   * implements [HandleAware] a shortcut is applied instead of looping
   * through all values.

   * @param element The object to check.
   * *
   * @return `true` if it does.
   */
  fun containsElement(element: @UnsafeVariance V): Boolean

  override operator fun iterator(): Iterator<V>

  @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
  fun isEmpty(): Boolean

  operator fun contains(handle: Handle<out V>): Boolean

  /**
   * Determine whether the given handle is contained in the map.

   * @param handle The handle to check.
   * *
   * @return `true` if it does.
   */
  @Deprecated("Don't use untyped handles", ReplaceWith("contains(Handles.handle(handle))", "net.devrieze.util.Handles"))
  operator fun contains(handle: Long): Boolean

  operator fun get(handle: Handle<out V>): V?

  @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
  fun getSize():Int

  companion object {

    const val NULL_HANDLE: Long = 0
  }

}

interface MutableHandleMap<V:Any>: HandleMap<V>, MutableIterable<V> {
  override operator fun iterator(): MutableIterator<V>
  /**
   * Put a new walue into the map. This is thread safe.

   * @param value The value to put into the map.
   * *
   * @return The handle for the value.
   */
  fun <W : V> put(value: W): ComparableHandle<out W>

  @Deprecated("Don't use untyped handles", ReplaceWith("set(Handles.handle(handle), value)", "net.devrieze.util.Handles"))
  operator fun set(handle: Long, value: V): V?

  operator fun set(handle: Handle<out V>, value: V): V?

  fun remove(handle: Handle<out V>): Boolean

  /** Remove all elements */
  fun clear()

}

fun <T> HANDLE_AWARE_ASSIGNER(it:T, handle: Long):T {
  (it as? MutableHandleAware<*>)?.let { it.apply { setHandleValue(handle) }} ;
  return it
}