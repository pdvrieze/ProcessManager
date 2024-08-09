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

actual open class MemHandleMap<V : Any>
actual constructor(private val handleAssigner: (V, Handle<V>) -> V?) : MutableHandleMap<V> {
    private var nextHandle: Long = 0L
    private val backingMap: MutableMap<Handle<*>, V> = mutableMapOf()

    actual override fun containsElement(element: V): Boolean = when (element) {
        is ReadableHandleAware<*> -> backingMap.containsKey(element.handle)
        else -> backingMap.containsValue(element)
    }

    actual override fun contains(handle: Handle<V>): Boolean {
        return backingMap.containsKey(handle)
    }

    actual override fun get(handle: Handle<V>): V? {
        return backingMap[handle]
    }

    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    @Suppress("OverridingDeprecatedMember")
    actual override fun getSize(): Int {
        return backingMap.size
    }

    actual override fun iterator(): MutableIterator<V> {
        return backingMap.values.iterator()
    }

    actual override fun forEach(body: MutableHasForEach.ForEachReceiver<V>) {
        MutableHasForEach.forEach(backingMap.values.iterator(), body)
    }

    actual override fun <W : V> put(value: W): Handle<W> {
        val handle1 = nextHandle++
        val handle = if (handle1 < 0) Handle.invalid() else Handle<W>(handle1)
        val storedValue =
            handleAssigner(value, handle) ?: throw IllegalArgumentException("Could not set a handle to the value")
        backingMap[handle] = storedValue
        return handle
    }

    actual override fun set(handle: Handle<V>, value: V): V? {
        return backingMap[handle].also {
            val storedValue =
                handleAssigner(value, handle) ?: throw IllegalArgumentException("Could not set a handle to the value")
            backingMap[handle] = storedValue
        }
    }

    actual override fun remove(handle: Handle<V>): Boolean {
        return backingMap.remove(handle) != null
    }

    actual override fun clear() {
        return backingMap.clear()
    }
}
