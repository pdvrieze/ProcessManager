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

class UnmodifyableList<E>(private val delegate: List<E>): List<E> by delegate, MutableList<E> {

    private class UnmodifyableIterator<E>(val delegate: ListIterator<E>): MutableListIterator<E>, ListIterator<E> by delegate {
        override fun add(element: E): Unit = throw UnsupportedOperationException("Unmodifyable")

        override fun set(element: E): Unit = throw UnsupportedOperationException("Unmodifyable")

        override fun remove(): Unit = throw UnsupportedOperationException("Unmodifyable")
    }

    override fun iterator(): MutableIterator<E> = UnmodifyableIterator<E>(delegate.listIterator())

    override fun listIterator(): MutableListIterator<E> = UnmodifyableIterator<E>(delegate.listIterator())

    override fun listIterator(index: Int): MutableListIterator<E> = UnmodifyableIterator<E>(delegate.listIterator(index))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return UnmodifyableList(delegate.subList(fromIndex, toIndex))
    }

    override fun add(element: E): Boolean = throw UnsupportedOperationException("Unmodifyable")

    override fun add(index: Int, element: E): Unit = throw UnsupportedOperationException("Unmodifyable")

    override fun addAll(index: Int, elements: Collection<E>): Boolean =
        throw UnsupportedOperationException("Unmodifyable")

    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("Unmodifyable")

    override fun clear(): Unit = throw UnsupportedOperationException("Unmodifyable")

    override fun remove(element: E): Boolean = throw UnsupportedOperationException("Unmodifyable")

    override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("Unmodifyable")

    override fun removeAt(index: Int): E = throw UnsupportedOperationException("Unmodifyable")

    override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("Unmodifyable")

    override fun set(index: Int, element: E): E = throw UnsupportedOperationException("Unmodifyable")
}

actual fun <E> List<E>.toUnmodifyableList(): List<E> = UnmodifyableList(this)