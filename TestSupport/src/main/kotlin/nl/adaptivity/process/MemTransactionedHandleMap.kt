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

package nl.adaptivity.process

import net.devrieze.util.*

import java.sql.SQLException


/**
 * Created by pdvrieze on 09/12/15.
 */
open class MemTransactionedHandleMap<T: Any, TR : StubTransaction>(handleAssigner: (T, Long)->T = ::HANDLE_AWARE_ASSIGNER) : MemHandleMap<T>(handleAssigner=handleAssigner), net.devrieze.util.MutableTransactionedHandleMap<T, TR> {

  interface TransactionFactory<TR : StubTransaction> {
    fun newTransaction(): TR
  }

  private class IteratorWrapper<T>(private val delegate: Iterator<T>, private val readOnly: Boolean) : MutableAutoCloseableIterator<T> {

    override fun remove() {
      if (readOnly) throw UnsupportedOperationException("The iterator is read-only")
    }

    override fun next(): T {
      return delegate.next()
    }

    override fun hasNext(): Boolean {
      return delegate.hasNext()
    }

    override fun close() {
      // Do nothing
    }
  }

  @Throws(SQLException::class)
  override fun <W : T> put(transaction: TR, value: W): ComparableHandle<W> {
    val put = put(value)
    return Handles.handle(put)
  }

  @Throws(SQLException::class)
  override fun get(transaction: TR, handle: Handle<out T>): T? {
    return get(handle)
  }

  @Throws(SQLException::class)
  override fun castOrGet(transaction: TR, handle: Handle<out T>): T? {
    return get(handle)
  }

  @Throws(SQLException::class)
  override fun set(transaction: TR, handle: Handle<out T>, value: T): T? {
    return set(handle, value)
  }

  override fun iterable(transaction: TR): MutableIterable<T> {
    return this
  }

  override fun iterator(transaction: TR, readOnly: Boolean): MutableAutoCloseableIterator<T> {
    return IteratorWrapper(iterator(), readOnly)
  }

  @Throws(SQLException::class)
  override fun containsAll(transaction: TR, c: Collection<*>) =
        c.all { it !=null && contains(it) }

  @Throws(SQLException::class)
  override fun containsElement(transaction: TR, element: Any): Boolean {
    return contains(element)
  }

  @Throws(SQLException::class)
  override fun contains(transaction: TR, handle: Handle<out T>): Boolean {
    return contains(handle)
  }

  @Throws(SQLException::class)
  override fun remove(transaction: TR, handle: Handle<out T>): Boolean {
    return remove(handle)
  }

  override fun invalidateCache(handle: Handle<out T>) { /* No-op */
  }

  override fun invalidateCache() { /* No-op */
  }

  @Throws(SQLException::class)
  override fun clear(transaction: TR) {
    clear()
  }

  override fun withTransaction(transaction: TR): MutableHandleMap<T> {
    return MutableHandleMapForwarder(transaction, this)
  }
}