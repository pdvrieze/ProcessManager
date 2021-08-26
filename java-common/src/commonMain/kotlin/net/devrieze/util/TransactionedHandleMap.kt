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

/**
 * Interface for handlemaps that support transactions. [DBHandleMap] does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
interface TransactionedHandleMap<V : Any, T : Transaction> {

    fun <W : V> put(transaction: T, value: W): ComparableHandle<W>

    fun castOrGet(transaction: T, handle: Handle<V>): V?

    operator fun get(transaction: T, handle: Handle<V>): V?

    fun iterable(transaction: T): Iterable<V>

    fun containsElement(transaction: T, element: Any): Boolean

    fun contains(transaction: T, handle: Handle<V>): Boolean

    fun containsAll(transaction: T, c: Collection<*>): Boolean

    fun invalidateCache(handle: Handle<V>)

    fun invalidateCache()

    fun iterator(transaction: T, readOnly: Boolean): Iterator<V>

    fun withTransaction(transaction: T): HandleMap<V> = HandleMapForwarder(transaction, this)
}


inline fun <T : Transaction, V : Any, R> TransactionedHandleMap<V, T>.inTransaction(
    transaction: T,
    body: HandleMap<V>.() -> R
): R {
    return withTransaction(transaction).body()
}


