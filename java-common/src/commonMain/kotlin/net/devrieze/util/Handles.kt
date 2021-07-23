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

@file:JvmName("Handles")
package net.devrieze.util

import kotlin.jvm.JvmName
import nl.adaptivity.util.multiplatform.URI


private val INVALID get() = SimpleHandle<Any>(-1L)

private class SimpleHandle<T> constructor(override val handleValue: Long) : ComparableHandle<T> {

    override fun toString(): String = "H:$handleValue"

    override fun compareTo(other: ComparableHandle<T>): Int {
        return handleValue.compareTo(other.handleValue)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SimpleHandle<*>

        if (handleValue != other.handleValue) return false

        return true
    }

    override fun hashCode(): Int {
        return handleValue.hashCode()
    }

}

fun <T> getInvalidHandle(): ComparableHandle<T> {
    @Suppress("UNCHECKED_CAST")
    return INVALID as ComparableHandle<T>
}

/**
 * Get a very simple Handle implementation.
 *
 * @param handle The handle
 * @return a Handle<T> object corresponding to the handle.
 */
fun <T> handle(handle: Long): ComparableHandle<T> {
    return if (handle < 0) getInvalidHandle() else SimpleHandle(handle)
}

@Deprecated("Use extension function", ReplaceWith("handle.toComparableHandle()", "net.devrieze.util.toComparableHandle"))
fun <T> handle(handle: Handle<T>): ComparableHandle<T> {
    return handle.toComparableHandle()
}

fun <T> Handle<T>.toComparableHandle(): ComparableHandle<T> = when(this) {
    is ComparableHandle<*> -> this as ComparableHandle<T>
    else -> SimpleHandle(handleValue)
}

/**
 * Convenience method that will parse the handle from a string
 * @param handle The string for the handle
 * @param T
 * @return
 */
fun <T> handle(handle: String): ComparableHandle<T> {
    return handle(handle.toLong())
}

fun <T> handle(handle: URI): ComparableHandle<T> {
    val path = handle.getPath()
    val slashPos = path.lastIndexOf('/')
    return if (slashPos > 0) {
        handle(path.substring(slashPos + 1))
    } else {
        handle(path)
    }
}
