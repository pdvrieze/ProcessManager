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

import net.devrieze.util.db.OldDBHandleMap

import java.sql.SQLException


/**
 * Interface for handlemaps that support transactions. [OldDBHandleMap] does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
interface OldTransactionedHandleMap<V, T : Transaction> : HandleMap<V> {

  @Throws(SQLException::class)
  fun <W : V> put(transaction: T, value: W): ComparableHandle<W>


  @Deprecated("use typed {@link #get(Transaction, Handle)}")
  @Throws(SQLException::class)
  operator fun get(transaction: T, handle: Long): V?

  @Throws(SQLException::class)
  fun castOrGet(transaction: T, handle: Handle<V>): V?

  @Throws(SQLException::class)
  operator fun get(transaction: T, handle: Handle<V>): V?


  @Deprecated("use typed {@link #get(Transaction, Handle)}")
  @Throws(SQLException::class)
  operator fun set(transaction: T, handle: Long, value: V): V?

  /**
   * Set the value for the handle
   * @return The previous value, or null if none.
   */
  @Throws(SQLException::class)
  operator fun set(transaction: T, handle: Handle<V>, value: V): V?

  fun iterable(transaction: T): MutableIterable<V>

  @Throws(SQLException::class)
  fun contains(transaction: T, element: Any): Boolean

  @Throws(SQLException::class)
  fun contains(transaction: T, handle: Handle<V>): Boolean

  @Throws(SQLException::class)
  fun containsAll(transaction: T, c: Collection<out Any>): Boolean


  @Deprecated("use typed {@link #get(Transaction, Handle)}")
  @Throws(SQLException::class)
  fun contains(transaction: T, handle: Long): Boolean

  @Throws(SQLException::class)
  fun remove(transaction: T, handle: Handle<V>): Boolean


  @Deprecated("use typed {@link #get(Transaction, Handle)}")
  @Throws(SQLException::class)
  fun remove(transaction: T, handle: Long): Boolean

  fun invalidateCache(handle: Handle<V>)

  fun invalidateCache()

  @Throws(SQLException::class)
  fun clear(transaction: T)

  fun iterator(transaction: T, readOnly: Boolean): AutoCloseableIterator<V>

  fun newTransaction(): T
}

/**
 * Interface for handlemaps that support transactions. [OldDBHandleMap] does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
interface TransactionedHandleMap<V, T : Transaction> {

  @Throws(SQLException::class)
  fun <W : V> put(transaction: T, value: W): ComparableHandle<W>

  @Throws(SQLException::class)
  fun castOrGet(transaction: T, handle: Handle<V>): V?

  @Throws(SQLException::class)
  operator fun get(transaction: T, handle: Handle<V>): V?

  /**
   * Set the value for the handle
   * @return The previous value, or null if none.
   */
  @Throws(SQLException::class)
  operator fun set(transaction: T, handle: Handle<V>, value: V): V?

  fun iterable(transaction: T): Iterable<V>

  @Throws(SQLException::class)
  fun contains(transaction: T, element: Any): Boolean

  @Throws(SQLException::class)
  fun contains(transaction: T, handle: Handle<V>): Boolean

  @Throws(SQLException::class)
  fun containsAll(transaction: T, c: Collection<*>): Boolean

  @Throws(SQLException::class)
  fun remove(transaction: T, handle: Handle<V>): Boolean


  fun invalidateCache(handle: Handle<V>)

  fun invalidateCache()

  @Throws(SQLException::class)
  fun clear(transaction: T)

  fun iterator(transaction: T, readOnly: Boolean): AutoCloseableIterator<V>

  fun newTransaction(): T
}
