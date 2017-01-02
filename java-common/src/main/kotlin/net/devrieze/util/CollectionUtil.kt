/*
 * Copyright (c) 2016.
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

package net.devrieze.util.collection

/*
 * Created by pdvrieze on 02/12/16.
 */

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(vararg elements: T) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceByNotNull(element: T?) {
  clear()
  element?.let { add(it) }
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Sequence<T>) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Iterable<T>) {
  clear()
  addAll(elements)
}

/**
 * Helper function to reset and set values in a set.
 */
fun <T, C: MutableCollection<in T>> C.replaceBy(elements: Iterator<T>) {
  replaceBy(elements.asSequence())
}
