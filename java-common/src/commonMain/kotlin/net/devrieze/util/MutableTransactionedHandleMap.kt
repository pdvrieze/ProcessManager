/*
 * Copyright (c) 2021.
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

interface MutableTransactionedHandleMap<V : Any, T : Transaction> : TransactionedHandleMap<V, T> {

    override fun iterator(transaction: T, readOnly: Boolean): MutableIterator<V>

    fun remove(transaction: T, handle: Handle<V>): Boolean

    override fun iterable(transaction: T): MutableIterable<V>


    /**
     * Set the value for the handle
     * @return The previous value, or null if none.
     */
    operator fun set(transaction: T, handle: Handle<V>, value: V): V?

    fun clear(transaction: T)

    override fun withTransaction(transaction: T): MutableHandleMap<V> = MutableHandleMapForwarder(transaction, this)

}

inline fun <T : Transaction, V : Any, R> MutableTransactionedHandleMap<V, T>.inWriteTransaction(
    transaction: T,
    body: MutableHandleMap<V>.() -> R
): R {
    return withTransaction(transaction).body()
}
