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

package nl.adaptivity.process.util

/**
 * Check property given indices
 */
inline fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean):Boolean {
  forEachIndexed { i, t -> if (predicate(i, t)) return false }
  return true
}

inline fun <T> Sequence<T>.allIndexed(predicate: (Int, T) -> Boolean):Boolean {
  forEachIndexed { i, t -> if (predicate(i, t)) return false }
  return true
}

inline fun <T> Array<T>.allIndexed(predicate: (Int, T) -> Boolean):Boolean {
  forEachIndexed { i, t -> if (predicate(i, t)) return false }
  return true
}

expect fun <E> List<E>.toUnmodifyableList(): List<E>