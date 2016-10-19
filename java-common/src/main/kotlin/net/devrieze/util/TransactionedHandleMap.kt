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
 * Interface for handlemaps that support transactions. [DBHandleMap] does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
interface TransactionedHandleMap<V, T : Transaction> {

  @Throws(SQLException::class)
  fun <W : V> put(transaction: T, value: W): ComparableHandle<W>

  @Throws(SQLException::class)
  fun castOrGet(transaction: T, handle: Handle<out V>): V?

  @Throws(SQLException::class)
  operator fun get(transaction: T, handle: Handle<out V>): V?

  fun iterable(transaction: T): Iterable<V>

  @Throws(SQLException::class)
  fun contains(transaction: T, element: Any): Boolean

  @Throws(SQLException::class)
  fun contains(transaction: T, handle: Handle<out V>): Boolean

  @Throws(SQLException::class)
  fun containsAll(transaction: T, c: Collection<*>): Boolean

  fun invalidateCache(handle: Handle<out V>)

  fun invalidateCache()

  fun iterator(transaction: T, readOnly: Boolean): AutoCloseableIterator<V>

  fun newTransaction(): T
}

interface MutableTransactionedHandleMap<V, T:Transaction> : TransactionedHandleMap<V, T> {

  override fun iterator(transaction: T, readOnly: Boolean): MutableAutoCloseableIterator<V>

  @Throws(SQLException::class)
  fun remove(transaction: T, handle: Handle<out V>): Boolean

  override fun iterable(transaction: T): MutableIterable<V>


  /**
   * Set the value for the handle
   * @return The previous value, or null if none.
   */
  @Throws(SQLException::class)
  operator fun set(transaction: T, handle: Handle<out V>, value: V): V?

  @Throws(SQLException::class)
  fun clear(transaction: T)

}