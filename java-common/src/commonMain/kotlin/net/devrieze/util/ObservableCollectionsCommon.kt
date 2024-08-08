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

expect abstract class ObservableCollectionBase<C : MutableCollection<T>, T, S : ObservableCollectionBase<C, T, S>> :
    Collection<T>, MutableCollection<T> {

    override fun contains(element: T): Boolean
    override val size: Int
    override fun isEmpty(): Boolean
    override fun containsAll(elements: Collection<T>): Boolean

    fun replaceBy(elements: Iterable<T>): Boolean
    override fun addAll(elements: Collection<T>): Boolean
    override fun add(element: T): Boolean
    override fun clear()
    override fun iterator(): MutableIterator<T>
    override fun remove(element: T): Boolean
    override fun removeAll(elements: Collection<T>): Boolean
    override fun retainAll(elements: Collection<T>): Boolean
}

expect class ObservableCollection<T>
constructor(delegate: MutableCollection<T>, observers: Iterable<(ObservableCollection<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableCollection<T>, T, ObservableCollection<T>> {
    constructor(delegate: MutableCollection<T>, vararg observers: (ObservableCollection<T>) -> Unit)

    override fun contains(element: T): Boolean
    override fun containsAll(elements: Collection<T>): Boolean
    override fun isEmpty(): Boolean
    override fun iterator(): MutableIterator<T>
    override val size: Int
    override fun add(element: T): Boolean
    override fun addAll(elements: Collection<T>): Boolean
    override fun clear()
    override fun remove(element: T): Boolean
    override fun removeAll(elements: Collection<T>): Boolean
    override fun retainAll(elements: Collection<T>): Boolean
}

expect class ObservableSet<T>
constructor(delegate: MutableSet<T>, observers: Iterable<(ObservableSet<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableSet<T>, T, ObservableSet<T>>, MutableSet<T> {

    constructor(delegate: MutableSet<T>, vararg observers: (ObservableSet<T>) -> Unit)

    override fun contains(element: T): Boolean
    override fun containsAll(elements: Collection<T>): Boolean
    override fun isEmpty(): Boolean
    override fun iterator(): MutableIterator<T>
    override val size: Int
    override fun add(element: T): Boolean
    override fun addAll(elements: Collection<T>): Boolean
    override fun clear()
    override fun remove(element: T): Boolean
    override fun removeAll(elements: Collection<T>): Boolean
    override fun retainAll(elements: Collection<T>): Boolean
}

expect class ObservableList<T>
constructor(delegate: MutableList<T>, observers: Iterable<(ObservableList<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableList<T>, T, ObservableList<T>>, List<T>, MutableList<T> {

    constructor(delegate: MutableList<T>, vararg observers: (ObservableList<T>) -> Unit)

    override fun contains(element: T): Boolean
    override fun containsAll(elements: Collection<T>): Boolean
    override fun isEmpty(): Boolean
    override fun iterator(): MutableIterator<T>
    override val size: Int
    override fun add(element: T): Boolean
    override fun add(index: Int, element: T)
    override fun addAll(elements: Collection<T>): Boolean
    override fun addAll(index: Int, elements: Collection<T>): Boolean
    override fun clear()
    override fun remove(element: T): Boolean
    override fun removeAll(elements: Collection<T>): Boolean
    override fun retainAll(elements: Collection<T>): Boolean
    override fun get(index: Int): T
    override fun indexOf(element: T): Int
    override fun lastIndexOf(element: T): Int
    override fun listIterator(): MutableListIterator<T>
    override fun listIterator(index: Int): MutableListIterator<T>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T>
    override fun removeAt(index: Int): T
    override fun set(index: Int, element: T): T
}
