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

import nl.adaptivity.util.net.devrieze.util.HasForEach
import nl.adaptivity.util.net.devrieze.util.MutableHasForEach

open class MutableHandleMapForwarder<V : Any, T : Transaction>(
    transaction: T,
    override val delegate: MutableTransactionedHandleMap<V, T>
) : HandleMapForwarder<V, T>(transaction, delegate), MutableHandleMap<V> {

    override fun iterator(): MutableIterator<V> = delegate.iterator(transaction, false)

    override fun <W : V> put(value: W) = delegate.put(transaction, value)

    override fun set(handle: Handle<V>, value: V) = delegate.set(transaction, handle, value)

    override fun remove(handle: Handle<V>) = delegate.remove(transaction, handle)

    override fun clear() = delegate.clear(transaction)

    override fun forEach(body: MutableHasForEach.ForEachReceiver<V>) = delegate.forEach(transaction, body)

    override fun forEach(body: HasForEach.ForEachReceiver<V>) {
        super<HandleMapForwarder>.forEach(body)
    }
}
