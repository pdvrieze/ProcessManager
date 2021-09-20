/*
 * Copyright (c) 2019.
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

interface HandleMap<V:Any> : Iterable<V> {

    /**
     * Determine whether the given object is contained in the map. If the object
     * implements [HandleAware] a shortcut is applied instead of looping
     * through all values.

     * @param element The object to check.
     *
     * @return `true` if it does.
     */
    fun containsElement(element: @UnsafeVariance V): Boolean

    override operator fun iterator(): Iterator<V>

    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    fun isEmpty(): Boolean = ! iterator().hasNext()

    operator fun contains(handle: Handle<V>): Boolean

    /**
     * Determine whether the given handle is contained in the map.

     * @param handle The handle to check.
     *
     * @return `true` if it does.
     */
    @Deprecated("Don't use untyped handles", ReplaceWith("contains(Handle(handle))", "net.devrieze.util.Handle"))
    operator fun contains(handle: Long): Boolean = contains(Handle(handle))

    operator fun get(handle: Handle<V>): V?

    /**
     * Request the handle map to invalidate any caches it has for this item
     */
    fun invalidateCache(handle:Handle<V>) = Unit

    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    fun getSize():Int

    companion object {

        const val NULL_HANDLE: Long = 0
    }

}

interface MutableHandleMap<V:Any>: HandleMap<V>, MutableIterable<V> {
    override operator fun iterator(): MutableIterator<V>

    /**
     * Put a new walue into the map. This is thread safe.

     * @param value The value to put into the map.
     *
     * @return The handle for the value.
     */
    fun <W : V> put(value: W): Handle<W>

    @Deprecated(
        "Don't use untyped handles",
        ReplaceWith("set(Handles.handle(handle), value)", "net.devrieze.util.Handles")
    )
    operator fun set(handle: Long, value: V): V? = set(Handle(handle), value)

    operator fun set(handle: Handle<V>, value: V): V?

    fun remove(handle: Handle<V>): Boolean

    /** Remove all elements */
    fun clear()

}

fun <T> HANDLE_AWARE_ASSIGNER(@Suppress("UNUSED_PARAMETER") transaction: Transaction, value:T, handle: Handle<T>):T? {
    (value as? ReadableHandleAware<*>)?.let { if (it.handle ==handle) return value } // no change needed
    (value as? MutableHandleAware<*>)?.let { it.apply { setHandleValue(handle.handleValue) }} // The handle has been set
    return null
}

fun <T> HANDLE_AWARE_ASSIGNER(value:T, handle: Handle<T>):T? {
    (value as? ReadableHandleAware<*>)?.let { if (it.handle ==handle) return value } // no change needed
    (value as? MutableHandleAware<*>)?.let { it.apply { setHandleValue(handle.handleValue) }} // The handle has been set
    return null
}

class HandleNotFoundException: Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}


inline fun <V:Any> MutableHandleMap<V>.getOrPut(key: Handle<V>, defaultValue: ()-> V): V {
    val value = get(key)
    return if (value==null) {
        val newValue = defaultValue()
        set(key, newValue)
        newValue
    } else {
        value
    }
}
