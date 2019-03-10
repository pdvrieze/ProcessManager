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

import org.jetbrains.annotations.Contract

/**
 * Collection class that allows accessing the entries in map.entry collection as a collection of values.
 * @author Paul de Vrieze
 */
class ValueCollection<T>(private val backingCollection: Collection<Map.Entry<*, T>>) : AbstractCollection<T>() {

    @Contract(pure = true)
    override operator fun contains(element: T): Boolean {
        return backingCollection.any { it == element }
    }

    @Contract(pure = true)
    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    @Contract(pure = true)
    override fun equals(other: Any?): Boolean {
        if (other !is Iterable<*>) {
            return false
        }
        val myIterator = iterator()
        val otherIterator = other.iterator()
        while (myIterator.hasNext()) {
            if (!(otherIterator.hasNext() && myIterator.next() == otherIterator.next())) {
                return false
            }
        }
        return !otherIterator.hasNext()
    }

    @Contract(pure = true)
    override fun hashCode(): Int {
        var result = 0
        for (value in this) {
            result = result * 31 + value.hashCode()
        }
        return result
    }

    @Contract(pure = true)
    override fun isEmpty(): Boolean {
        return backingCollection.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return ValueIterator(backingCollection.iterator())
    }

    @get:Contract(pure = true)
    override val size: Int get() = backingCollection.size


}