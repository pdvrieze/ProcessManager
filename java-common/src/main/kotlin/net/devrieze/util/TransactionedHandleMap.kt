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

import java.sql.SQLException

/**
 * Interface for handlemaps that support transactions. [DBHandleMap] does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
interface TransactionedHandleMap<V: Any, T : Transaction> {

  @Throws(SQLException::class)
  fun <W : V> put(transaction: T, value: W): ComparableHandle<W>

  @Throws(SQLException::class)
  fun castOrGet(transaction: T, handle: Handle<out V>): V?

  @Throws(SQLException::class)
  operator fun get(transaction: T, handle: Handle<out V>): V?

  fun iterable(transaction: T): Iterable<V>

  @Throws(SQLException::class)
  fun containsElement(transaction: T, element: Any): Boolean

  @Throws(SQLException::class)
  fun contains(transaction: T, handle: Handle<out V>): Boolean

  @Throws(SQLException::class)
  fun containsAll(transaction: T, c: Collection<*>): Boolean

  fun invalidateCache(handle: Handle<out V>)

  fun invalidateCache()

  fun iterator(transaction: T, readOnly: Boolean): AutoCloseableIterator<V>

  fun withTransaction(transaction:T):HandleMap<V> = HandleMapForwarder(transaction, this)
}


inline fun <T:Transaction, V: Any, R> TransactionedHandleMap<V,T>.inTransaction(transaction: T, body: HandleMap<V>.()->R):R {
  return withTransaction(transaction).body()
}


interface MutableTransactionedHandleMap<V: Any, T:Transaction> : TransactionedHandleMap<V, T> {

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

  override fun withTransaction(transaction: T): MutableHandleMap<V> = MutableHandleMapForwarder(transaction, this)

}

open class  HandleMapForwarder<V: Any, T:Transaction>(val transaction: T, open val delegate: TransactionedHandleMap<V, T>) : HandleMap<V> {
  override fun containsElement(element: V) = delegate.containsElement(transaction, element)

  override fun iterator() = delegate.iterator(transaction, true)

  override fun isEmpty():Boolean { throw UnsupportedOperationException("Not available") }

  override fun contains(handle: Handle<out V>) = delegate.contains(transaction, handle)

  override fun contains(handle: Long) = delegate.contains(transaction, Handles.handle(handle))

  override fun get(handle: Handle<out V>) = delegate.get(transaction, handle)

  override fun getSize(): Int { throw UnsupportedOperationException("Not available") }

}

open class MutableHandleMapForwarder<V: Any, T:Transaction>(transaction: T, override val delegate: MutableTransactionedHandleMap<V, T>) : HandleMapForwarder<V,T>(transaction, delegate), MutableHandleMap<V> {

  override fun iterator() = delegate.iterator(transaction, false)

  override fun <W : V> put(value: W) = delegate.put(transaction, value)

  override fun set(handle: Long, value: V) = delegate.set(transaction, Handles.handle(handle), value)

  override fun set(handle: Handle<out V>, value: V) = delegate.set(transaction, handle, value)

  override fun remove(handle: Handle<out V>) = delegate.remove(transaction, handle)

  override fun clear() = delegate.clear(transaction)
}