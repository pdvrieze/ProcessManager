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

import nl.adaptivity.util.multiplatform.URI

object Handles {

    private val INVALID get() = SimpleHandle<Any>(-1L)

    private class SimpleHandle<T> constructor(override val handleValue: Long) : ComparableHandle<T> {

        override val valid: Boolean
            get() = handleValue >= 0L

        override fun toString(): String {
            return "H:$handleValue"
        }

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

    fun <T> getInvalid(): ComparableHandle<T> {
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
        return if (handle < 0) getInvalid() else SimpleHandle(
            handle)
    }

    fun <T> handle(handle: Handle<T>): ComparableHandle<T> {
        return if (handle is ComparableHandle<*>) {
            handle as ComparableHandle<T>
        } else SimpleHandle(handle.handleValue)
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
            Handles.handle(path.substring(slashPos + 1))
        } else {
            Handles.handle(path)
        }
    }

}
