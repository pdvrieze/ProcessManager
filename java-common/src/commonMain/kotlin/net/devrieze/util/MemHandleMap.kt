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

import nl.adaptivity.util.net.devrieze.util.MutableHasForEach

expect open class MemHandleMap<V : Any>: MutableHandleMap<V> {
    constructor(handleAssigner: (V, Handle<V>) -> V? = ::HANDLE_AWARE_ASSIGNER)

    override fun clear()
    override fun iterator(): MutableIterator<V>
    override fun <W : V> put(value: W): Handle<W>
    override fun set(handle: Handle<V>, value: V): V?
    override fun remove(handle: Handle<V>): Boolean
    override fun containsElement(element: V): Boolean
    override fun contains(handle: Handle<V>): Boolean
    override fun get(handle: Handle<V>): V?
    override fun getSize(): Int
    override fun forEach(body: MutableHasForEach.ForEachReceiver<V>)
}
