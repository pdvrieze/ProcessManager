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

import java.sql.SQLException


/**
 * Created by pdvrieze on 19/05/16.
 */
abstract class AbstractOldTransactionedHandleMap<V, T : Transaction> : OldTransactionedHandleMap<V, T> {

  @Deprecated("")
  @Throws(SQLException::class)
  override fun get(transaction: T, handle: Long): V? {
    return get(transaction, Handles.handle<V>(handle))
  }

  @Throws(SQLException::class)
  override fun castOrGet(transaction: T, handle: Handle<V>): V? {
    return get(transaction, handle)
  }

  @Deprecated("")
  override fun get(handle: Handle<V>): V? {
    try {
      newTransaction().use { t -> return get(t, handle) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }
//
//  @Deprecated("")
//  fun toArray(): Array<Any> {
//    return toArray(arrayOfNulls<Any>(getSize()))
//  }

  @Deprecated("")
  override fun iterator(): Iterator<V> {
    newTransaction().use { t -> return iterator(t, false) }
  }

  @Deprecated("")
  @Throws(SQLException::class)
  override fun contains(transaction: T, handle: Long): Boolean {
    return contains(transaction, Handles.handle<V>(handle))
  }

  @Deprecated("")
  override fun contains(handle: Long): Boolean {
    newTransaction().use { transaction ->
      return contains(transaction, Handles.handle<V>(handle))
    }
  }

  @Deprecated("")
  override operator fun contains(element: V): Boolean {
    newTransaction().use { t -> return contains(t, element as Any) }
  }

  @Deprecated("")
  fun containsAll(c: Collection<Any>): Boolean {
    try {
      newTransaction().use { t -> return containsAll(t, c) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Deprecated("")
  @Throws(SQLException::class)
  override fun containsAll(transaction: T, c: Collection<out Any>): Boolean {
    for (o in c) {
      if (!contains(transaction, o)) {
        return false
      }
    }
    return true
  }

  @Deprecated("")
  override fun containsHandle(handle: Handle<V>): Boolean {
    try {
      newTransaction().use { t -> return contains(t, handle) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Deprecated("")
  @Throws(SQLException::class)
  override fun set(transaction: T, handle: Long, value: V): V? {
    return set(transaction, Handles.handle<V>(handle), value)
  }

  @Deprecated("")
  override fun set(handle: Long, value: V): V? {
    return set(Handles.handle<V>(handle), value)
  }

  @Deprecated("")
  override fun set(handle: Handle<V>, value: V): V? {
    try {
      newTransaction().use { t -> return set(t, handle, value) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Deprecated("")
  override fun <W : V> put(value: W): Handle<W> {
    try {
      newTransaction().use { t -> return put(t, value) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Deprecated("")
  fun add(v: V): Boolean {
    return put(v) != null
  }

  @Deprecated("")
  fun addAll(c: Collection<V>): Boolean {
    var changed = false
    for (elem in c) {
      changed = add(elem) || changed
    }
    return changed
  }

  @Deprecated("")
  @Throws(SQLException::class)
  override fun remove(transaction: T, handle: Long): Boolean {
    return remove(transaction, Handles.handle<V>(handle))
  }

  @Deprecated("")
  override fun remove(handle: Handle<V>): Boolean {
    try {
      newTransaction().use { t -> return remove(t, handle) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Deprecated("")
  fun remove(handle: Long): Boolean {
    return remove(Handles.handle<V>(handle))
  }

  @Deprecated("")
  fun clear() {
    try {
      newTransaction().use { t -> clear(t) }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

}
