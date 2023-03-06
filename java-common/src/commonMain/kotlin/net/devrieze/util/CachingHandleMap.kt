/*
 * Copyright (c) 2018.
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

import net.devrieze.util.CachingHandleMap.WrappingIterator
import nl.adaptivity.util.multiplatform.*


/**
 * A [HandleMap] that caches its results. Note that the [remove][WrappingIterator.remove] method on the iterator is not
 * efficient if the elements do not implement Handle as it will loop through the cache to find the underlying value.

 * @author Paul de Vrieze
 *
 * @param  V The type of the elements in the map.
 * @param T The transaction type that is used
 * @property delegate The underlying handlemap
 * @param cacheSize How many elements should be stored in the cache
 * @property handleAssigner Function that may update the value. If it returns null, the value will not be cached.
 */
open class CachingHandleMap<V : Any, T : Transaction>(
    protected open val delegate: MutableTransactionedHandleMap<V, T>,
    cacheSize: Int,
    val handleAssigner: (T, V, Handle<V>) -> V?) : AbstractTransactionedHandleMap<V, T>(), Closeable, AutoCloseable {

    constructor(
        delegate: MutableTransactionedHandleMap<V, T>,
        cacheSize: Int
    ) : this(delegate, cacheSize, { _, v, h -> HANDLE_AWARE_ASSIGNER(v, h) })

    private open inner class WrappingIterator(
        private val transaction: T,
        protected open val iterator: Iterator<V>
    ) : MutableAutoCloseableIterator<V> {
        private var last: V? = null

        override final fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override final fun next(): V {
            val result = iterator.next()
            putCache(transaction, result)
            last = result
            return result
        }

        override fun close() {
            val it = iterator
            if (it is AutoCloseable) {
                it.close()
            }
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    private inner class WrappingMutableIterator(transaction: T, iterator: MutableIterator<V>) : WrappingIterator(
        transaction, iterator
    ), MutableIterator<V>, AutoCloseableIterator<V> {
        override val iterator: MutableIterator<V> get() = super.iterator as MutableIterator<V>
        private var last: V? = null

        override fun remove() {
            if (last != null) {
                synchronizedCompat(cacheHandles) {
                    val last = last
                    if (last is Handle<*>) {
                        @Suppress("UNCHECKED_CAST")
                        invalidateCache(last as Handle<V>)
                    } else {
                        for (i in cacheHandles.indices) {
                            val value = cacheValues[i]
                            if (value != null && value == last) {
                                cacheHandles[i] = -1
                                cacheValues[i] = null
                                break
                            }
                        }
                    }
                }
            }
            iterator.remove()
        }

        override fun close() {
            val it = iterator
            if (it is AutoCloseable) {
                it.close()
            }
        }

    }

    private inner class WrappingIterable(
        private val transaction: T,
        private val delegateIterable: Iterable<V>
    ) : MutableIterable<V> {

        override fun iterator(): MutableIterator<V> {
            return WrappingIterator(transaction, delegateIterable.iterator())
        }
    }

    private var cacheHead = 0
    internal val cacheHandles: LongArray
    internal val cacheValues: Array<V?>
    private val pendingHandles : MutableSet<Handle<V>> = createCachingMapHandleSet()

    init {
        cacheHandles = LongArray(cacheSize)
        @Suppress("UNCHECKED_CAST")
        cacheValues = arrayOfNulls<Any>(cacheSize) as Array<V?>
    }

    override fun <W : V> put(transaction: T, value: W): Handle<W> {
        val handle = delegate.put(transaction, value)


        putCache(transaction, handle, value)
        return handle
    }

    @Suppress("UNCHECKED_CAST")
    protected fun putCache(transaction: T, pValue: V) {
        if (pValue is Handle<*>) {
            putCache(transaction, pValue as Handle<V>, pValue)
        } else if (pValue is ReadableHandleAware<*>) {
            putCache(transaction, (pValue as ReadableHandleAware<V>).handle, pValue)
        }
    }

    private fun putCache(transaction: T, handle: Handle<V>, nonUpdatedValue: V?) {
        if (nonUpdatedValue != null) { // never store null
            if (handle.isValid) {
                val updatedValue = handleAssigner(transaction, nonUpdatedValue, handle)

                // Don't cache if the updated value has a handle that does not match
                if (updatedValue == null ||
                    (updatedValue is ReadableHandleAware<*> &&
                        updatedValue.handle != handle)
                ) return

                synchronizedCompat(cacheHandles) {
                    transaction.addRollbackHandler { invalidateCache(handle) }
                    val pos = cacheHead
                    val handleValue = handle.handleValue
                    if (cacheHandles[pos] != handleValue) {
                        removeFromCache(handleValue)
                    }
                    cacheHandles[pos] = handleValue
                    cacheValues[pos] = updatedValue
                    cacheHead = (pos + 1) % cacheValues.size

                }
            }
        }
    }

    fun getFromDelegate(transaction: T, pHandle: Handle<V>): V? {
        pendingHandles.add(pHandle) // internal locking so no locking needed here
        try {
            return delegate[transaction, pHandle]
        } finally {
            pendingHandles.remove(pHandle)
        }
    }

    private fun getFromCache(handle: Long): V? {
        synchronizedCompat(cacheHandles) {
            for (i in cacheHandles.indices) {
                if (cacheHandles[i] == handle) {
                    return cacheValues[i]
                }
            }
            return null
        }
    }

    override fun get(transaction: T, handle: Handle<V>): V? {
        if (!handle.isValid) return null
        var value: V?
        synchronizedCompat(cacheHandles) {
            value = getFromCache(handle.handleValue)
            if (value != null) {
                return value
            }
            value = delegate[transaction, handle]
            return value?.apply { storeInCache(transaction, handle, this) }
        }
    }

    override fun contains(transaction: T, handle: Handle<V>): Boolean {
        if (getFromCache(handle.handleValue) != null) {
            return true
        }
        return delegate.contains(transaction, handle)
    }

    override fun containsElement(transaction: T, element: Any): Boolean {
        if (element is Handle<*>) {
            @Suppress("UNCHECKED_CAST")
            return contains(transaction, element as Handle<V>)
        }
        return delegate.containsElement(transaction, element)
    }

    override fun set(transaction: T, handle: Handle<V>, value: V): V? {
        invalidateCache(handle)
        delegate[transaction, handle] = value
        return storeInCache(transaction, handle, delegate[transaction, handle] as V)
    }

    private fun storeInCache(transaction: T, pHandle: Handle<V>, pV: V): V {
        if (!isPending(pHandle)) {
            val handle = pHandle.handleValue
            synchronizedCompat(cacheHandles) {
                removeFromCache(handle) // remove whatever old value was there
                putCache(transaction, pHandle, pV)
            }
        }
        return pV
    }

    private fun removeFromCache(handle: Long) {
        for (i in cacheHandles.indices) {
            if (cacheHandles[i] == handle) {
                cacheHandles[i] = -1
                cacheValues[i] = null
                assert(getFromCache(handle) == null)
                return
            }
        }
    }

    private fun isPending(handle: Handle<V>): Boolean {
        return pendingHandles.contains(handle)
    }

    override fun invalidateCache(handle: Handle<V>) {
        removeFromCache(handle.handleValue)
    }

    override fun invalidateCache() {
        synchronizedCompat(cacheHandles) {
            cacheHandles.fill(-1)
            cacheValues.fill(null)
            cacheHead = 0
        }
    }

    @Deprecated("")
    fun getUncached(transaction: T, pHandle: Handle<V>): V? {
        return delegate[transaction, pHandle].apply {
            if (this != null)
                storeInCache(transaction, pHandle, this)
            else {
                removeFromCache(pHandle.handleValue)
            }
        }
    }

    override fun remove(transaction: T, handle: Handle<V>): Boolean {
        removeFromCache(handle.handleValue)
        return delegate.remove(transaction, handle)
    }

    override fun clear(transaction: T) {
        invalidateCache()
        delegate.clear(transaction)
    }

    override fun iterator(transaction: T, readOnly: Boolean): MutableIterator<V> {
        return WrappingIterator(transaction, delegate.iterator(transaction, readOnly))
    }

    override fun iterable(transaction: T): MutableIterable<V> {
        return WrappingIterable(transaction, delegate.iterable(transaction))
    }

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

internal expect fun <V> createCachingMapHandleSet():MutableSet<Handle<V>>
