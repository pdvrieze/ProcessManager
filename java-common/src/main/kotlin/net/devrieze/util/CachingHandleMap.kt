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

import java.io.Closeable
import java.io.IOException
import java.lang.ref.WeakReference
import java.sql.SQLException
import java.util.Arrays
import java.util.concurrent.CopyOnWriteArraySet


/**
 * A [OldDBHandleMap] that uses [WeakReferences][WeakReference] to store
 * its results. Note that the [remove][WrappingIterator.remove] method on
 * the iterator is not efficient if the elements do not implement Handle as it
 * will loop through the cache to find the underlying value.

 * @author Paul de Vrieze
 * *
 * @param  The type of the elements in the map.
 */
open class CachingHandleMap<V, T : Transaction>(protected open val delegate: OldTransactionedHandleMap<V, T>, cacheSize: Int) : AbstractTransactionedHandleMap<V, T>(), Closeable, AutoCloseable {


  private inner class WrappingIterator(private val transaction:T, private val iterator: MutableIterator<V>) : AutoCloseableIterator<V> {
    private var last: V? = null

    override fun hasNext(): Boolean {
      return iterator.hasNext()
    }

    override fun next(): V {
      val result = iterator.next()
      putCache(transaction, result)
      last = result
      return result
    }

    override fun remove() {
      if (last != null) {
        synchronized (mCacheHandles) {
          val last = last
          if (last is Handle<*>) {
            invalidateCache(last as Handle<V>)
          } else {
            for (i in mCacheHandles.indices) {
              val value = mCacheValues[i]
              if (value != null && value == last) {
                mCacheHandles[i] = -1
                mCacheValues[i] = null
                break
              }
            }
          }
        }
      }
      iterator.remove()
    }

    @Throws(IOException::class)
    override fun close() {
      if (iterator is AutoCloseable) {
        try {
          iterator.close()
        } catch (e: IOException) {
          throw e
        } catch (e: Exception) {
          throw RuntimeException(e)
        }

      }
    }

  }

  private inner class WrappingIterable(private val transaction:T, private val delegateIterable: MutableIterable<V>) : MutableIterable<V> {

    override fun iterator(): MutableIterator<V> {
      return WrappingIterator(transaction, delegateIterable.iterator())
    }
  }

  internal var mCacheHead = 0
  internal val mCacheHandles: LongArray
  internal val mCacheValues: Array<V?>
  internal val mPendingHandles = CopyOnWriteArraySet<Handle<V>>()

  init {
    mCacheHandles = LongArray(cacheSize)
    mCacheValues = arrayOfNulls<Any>(cacheSize) as Array<V?>
  }

  override fun newTransaction(): T {
    return delegate.newTransaction()
  }

  @Throws(SQLException::class)
  override fun <W : V> put(transaction: T, value: W): ComparableHandle<W> {
    val handle = delegate.put(transaction, value)
    putCache(transaction, handle, value)
    return handle
  }

  protected fun putCache(transaction: T, pValue: V) {
    if (pValue is Handle<*>) {
      putCache(transaction, pValue as Handle<V>, pValue)
    } else if (pValue is HandleMap.HandleAware<*>) {
      putCache(transaction, (pValue as HandleMap.HandleAware<V>).handle, pValue)
    }
  }

  private fun putCache(transaction: T, pHandle: Handle<V>, pValue: V?) {
    if (pValue != null) { // never store null
      synchronized (mCacheHandles) {
        transaction.addRollbackHandler({ invalidateCache(pHandle) })
        val pos = mCacheHead
        val handle = pHandle.handleValue
        if (mCacheHandles[pos] != handle) {
          removeFromCache(handle)
        }
        mCacheHandles[pos] = handle
        mCacheValues[pos] = pValue
        mCacheHead = (pos + 1) % mCacheValues.size
      }
    }
  }

  @Throws(SQLException::class)
  fun getFromDelegate(transaction: T, pHandle: Handle<V>): V? {
    mPendingHandles.add(pHandle) // internal locking so no locking needed here
    try {
      return delegate[transaction, pHandle]
    } finally {
      mPendingHandles.remove(pHandle)
    }
  }

  @Throws(SQLException::class)
  @Deprecated("Use the transaction version")
  fun getFromDelegate(pHandle: Handle<V>): V? {
    mPendingHandles.add(pHandle) // internal locking so no locking needed here
    try {
      return delegate[pHandle]
    } finally {
      mPendingHandles.remove(pHandle)
    }
  }

  private fun getFromCache(handle: Long): V? {
    synchronized (mCacheHandles) {
      for (i in mCacheHandles.indices) {
        if (mCacheHandles[i] == handle) {
          return mCacheValues[i]
        }
      }
      return null
    }
  }

  override fun get(pHandle: Long): V? {
    val handle = Handles.handle<V>(pHandle)
    if (isPending(handle)) { // don't cache pending values
      return delegate[handle]
    }
    val key = pHandle
    var `val`: V?
    synchronized (mCacheHandles) {
      `val` = getFromCache(pHandle)

      if (`val` != null) {
        return `val`
      }
      newTransaction().use { transaction ->
        val value = getFromDelegate(handle)
        return value?.apply { storeInCache(transaction, handle, this) }
      }
    }
  }

  @Throws(SQLException::class)
  override fun get(transaction: T, handle: Handle<V>): V? {
    var value: V?
    synchronized (mCacheHandles) {
      value = getFromCache(handle.handleValue)
      if (value != null) {
        return value
      }
      value = delegate[transaction, handle]
      return value?.apply { storeInCache(transaction, Handles.handle(handle), this) }
    }
  }

  @Throws(SQLException::class)
  override fun contains(transaction: T, handle: Handle<V>): Boolean {
    if (getFromCache(handle.handleValue) != null) {
      return true
    }
    return delegate.contains(transaction, handle)
  }

  @Throws(SQLException::class)
  override fun contains(transaction: T, element: Any): Boolean {
    if (element is Handle<*>) {
      return contains(transaction, element as Handle<V>)
    }
    return delegate.contains(transaction, element)
  }

  override fun getSize(): Int {
    return delegate.getSize()
  }

  override fun isEmpty(): Boolean {
    return delegate.isEmpty()
  }

  @Throws(SQLException::class)
  override fun set(transaction: T, handle: Handle<V>, value: V): V? {
    invalidateCache(handle)
    delegate.set(transaction, handle, value)
    return storeInCache(transaction, handle, value)
  }

  private fun storeInCache(transaction: T, pHandle: Handle<V>, pV: V): V {
    if (!isPending(pHandle)) {
      val handle = pHandle.handleValue
      synchronized (mCacheHandles) {
        removeFromCache(handle) // remove whatever old value was there
        putCache(transaction, pHandle, pV)
      }
    }
    return pV
  }

  private fun removeFromCache(handle: Long) {
    for (i in mCacheHandles.indices) {
      if (mCacheHandles[i] == handle) {
        mCacheHandles[i] = -1
        mCacheValues[i] = null
        assert(getFromCache(handle) == null)
        return
      }
    }
  }

  private fun isPending(handle: Handle<V>): Boolean {
    return mPendingHandles.contains(handle)
  }

  override fun invalidateCache(handle: Handle<V>) {
    removeFromCache(handle.handleValue)
  }

  override fun invalidateCache() {
    synchronized (mCacheHandles) {
      Arrays.fill(mCacheHandles, -1)
      Arrays.fill(mCacheValues, null)
      mCacheHead = 0
    }
  }

  @Deprecated("")
  @Throws(SQLException::class)
  fun getUncached(transaction: T, pHandle: ComparableHandle<V>): V? {
    return delegate[transaction, pHandle].apply {
      if (this!=null)
        storeInCache(transaction, pHandle, this)
      else {
        removeFromCache(pHandle.handleValue)
      }
    }
  }

  @Throws(SQLException::class)
  override fun remove(transaction: T, handle: Handle<V>): Boolean {
    removeFromCache(handle.handleValue)
    return delegate.remove(transaction, handle)
  }

  @Throws(SQLException::class)
  override fun clear(transaction: T) {
    invalidateCache()
    delegate.clear(transaction)
  }

  override fun iterator(transaction: T, readOnly: Boolean): AutoCloseableIterator<V> {
    return WrappingIterator(transaction, delegate.iterator(transaction, readOnly))
  }

  override fun iterable(transaction: T): MutableIterable<V> {
    return WrappingIterable(transaction, delegate.iterable(transaction))
  }

  @Throws(IOException::class)
  override fun close() {
    invalidateCache()
    if (delegate is Closeable) {
      (delegate as Closeable).close()
    } else if (delegate is AutoCloseable) {
      try {
        (delegate as AutoCloseable).close()
      } catch (e: Exception) {
        throw RuntimeException(e)
      }

    }
  }
}
