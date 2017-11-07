/*
 * Copyright (c) 2017.
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
 * @param  T parameter that should help with compile time handle differentiation
 */
interface Handle<out T: @JvmWildcard Any?> {

  val handleValue: Long
  val valid:Boolean get() = handleValue>=0
}

interface ComparableHandle<out T: @JvmWildcard Any?> : Handle<T>, Comparable<ComparableHandle<@kotlin.UnsafeVariance T>> {
  override fun compareTo(other: ComparableHandle<@kotlin.UnsafeVariance T>) = handleValue.compareTo(other.handleValue)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T:Any?> Handle(handleValue:Long):Handle<T> = Handles.handle(handleValue)