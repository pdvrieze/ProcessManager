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

abstract class ObservableCollectionBase<C : MutableCollection<T>, T, S : ObservableCollectionBase<C, T, S>>
constructor(protected val delegate: C, val observers: Iterable<(S) -> Unit> = emptyList()): MutableCollection<T> {

    override fun contains(element: T): Boolean = delegate.contains(element)
    override val size: Int = delegate.size
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun containsAll(elements: Collection<T>): Boolean = delegate.containsAll(elements)

    protected abstract fun triggerObservers()

    fun replaceBy(elements: Iterable<T>): Boolean {
        var hasChanges = false
        val oldElements = delegate.toMutableList()
        delegate.clear()
        for (element in elements) {
            if (hasChanges) {
                delegate.add(element)
            } else {
                if (!oldElements.remove(element)) {
                    hasChanges = true
                }
                delegate.add(element)
            }
        }
        if (!hasChanges && oldElements.isNotEmpty()) hasChanges = true
        if (hasChanges) {
            triggerObservers()
        }
        return hasChanges
    }

    override fun addAll(elements: Collection<T>): Boolean =
        delegate.addAll(elements).apply { if (this) triggerObservers() }

    override fun add(element: T): Boolean =
        delegate.add(element).apply { if (this) triggerObservers() }

    override fun clear() {
        if (delegate.isNotEmpty()) {
            delegate.clear(); triggerObservers()
        }
    }

    override fun iterator(): MutableIterator<T> = ObservableIterator(this, delegate.iterator())

    override fun remove(element: T): Boolean = delegate.remove(element).apply { if (this) triggerObservers() }

    override fun removeAll(elements: Collection<T>): Boolean =
        delegate.removeAll(elements).apply { if (this) triggerObservers() }

    override fun retainAll(elements: Collection<T>): Boolean =
        delegate.retainAll(elements).apply { if (this) triggerObservers() }

    protected open class ObservableIterator<T>(protected val base: ObservableCollectionBase<*,T,*>, protected open val delegate: MutableIterator<T>) :
        Iterator<T> by delegate, MutableIterator<T> {

        override fun remove() {
            delegate.remove()
            base.triggerObservers()
        }

    }
}

class ObservableCollection<T>
constructor(delegate: MutableCollection<T>, observers: Iterable<(ObservableCollection<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableCollection<T>, T, ObservableCollection<T>>(delegate, observers) {
    constructor(delegate: MutableCollection<T>, vararg observers: (ObservableCollection<T>) -> Unit) :
        this(delegate, observers.toList())

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return delegate.equals(other)
    }
}

class ObservableSet<T>(delegate: MutableSet<T>, observers: Iterable<(ObservableSet<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableSet<T>, T, ObservableSet<T>>(delegate, observers), MutableSet<T> {

    constructor(delegate: MutableSet<T>, vararg observers: (ObservableSet<T>) -> Unit):
        this(delegate, observers.toList())

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }

    override fun toString(): String = joinToString(prefix = "ObservableSet[", postfix = "]")

    override fun equals(other: Any?): Boolean = delegate.equals(other)

    override fun hashCode(): Int = delegate.hashCode()

}

class ObservableList<T>(delegate: MutableList<T>, observers: Iterable<(ObservableList<T>) -> Unit> = emptyList()) :
    ObservableCollectionBase<MutableList<T>, T, ObservableList<T>>(delegate, observers), MutableList<T> {

    constructor(delegate: MutableList<T>, vararg observers: (ObservableList<T>) -> Unit) :
        this(delegate, observers.toList())

    override fun add(index: Int, element: T) {
        delegate.add(index, element)
        triggerObservers()
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        delegate.addAll(index, elements).apply { if (this) triggerObservers() }

    override fun get(index: Int): T = delegate.get(index)

    override fun indexOf(element: T): Int = delegate.indexOf(element)

    override fun lastIndexOf(element: T): Int = delegate.lastIndexOf(element)

    override fun listIterator(): MutableListIterator<T> =
        ObservableListIterator(delegate.listIterator())

    override fun listIterator(index: Int): MutableListIterator<T> =
        ObservableListIterator(delegate.listIterator(index))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        ObservableList(delegate.subList(fromIndex, toIndex), this.observers)

    override fun removeAt(index: Int): T = delegate.removeAt(index).apply { triggerObservers() }

    override fun set(index: Int, element: T): T =
        delegate.set(index, element).apply { triggerObservers() }

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }

    override fun toString(): String = joinToString(prefix = "ObservableList[", postfix = "]")

    override fun equals(other: Any?): Boolean = delegate.equals(other)

    override fun hashCode(): Int = delegate.hashCode()

    private inner class ObservableListIterator(delegate: MutableListIterator<T>) :
        ObservableIterator<T>(this, delegate),
        ListIterator<T> by delegate,
        MutableListIterator<T> {
        override val delegate: MutableListIterator<T> get() = super.delegate as MutableListIterator
        override fun add(element: T) {
            delegate.add(element)
            triggerObservers()
        }

        override fun set(element: T) {
            delegate.set(element)
            triggerObservers()
        }

        override fun hasNext() = super.hasNext()

        override fun next() = super.next()
    }

}
