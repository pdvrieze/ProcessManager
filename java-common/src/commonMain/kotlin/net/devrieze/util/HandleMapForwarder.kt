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

open class HandleMapForwarder<V : Any, T : Transaction>(
    val transaction: T,
    open val delegate: TransactionedHandleMap<V, T>
) : HandleMap<V> {
    override fun containsElement(element: V) = delegate.containsElement(transaction, element)

    @Deprecated("Not safe for use")
    override fun iterator() = delegate.iterator(transaction, true)

    override fun forEach(body: HasForEach.ForEachReceiver<V>) {
        delegate.forEach(transaction, body)
    }

    override fun contains(handle: Handle<V>) = delegate.contains(transaction, handle)

    override fun get(handle: Handle<V>) = delegate.get(transaction, handle)

    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    @Suppress("OverridingDeprecatedMember")
    override fun getSize(): Int {
        throw UnsupportedOperationException("Not available")
    }

    override fun invalidateCache(handle: Handle<V>) = delegate.invalidateCache(handle)
}
