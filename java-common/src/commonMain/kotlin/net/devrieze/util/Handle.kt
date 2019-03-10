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

import nl.adaptivity.util.multiplatform.JvmDefault


interface Handle<out T : Any?> {

    val handleValue: Long

    @Deprecated("Use isValid", ReplaceWith("isValid"))
    @JvmDefault
    val valid: Boolean get() = isValid

    @JvmDefault
    val isValid get() = handleValue >= 0
}

interface ComparableHandle<out T: Any?> : Handle<T>, Comparable<ComparableHandle<@kotlin.UnsafeVariance T>> {
    @JvmDefault
    override fun compareTo(other: ComparableHandle<@kotlin.UnsafeVariance T>):Int {
        return handleValue.compareTo(other.handleValue)
    }
}

