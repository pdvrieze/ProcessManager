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

import net.devrieze.util.collection.replaceBy
import java.util.*
import java.util.function.Predicate


actual abstract class ObservableCollectionBase<C : MutableCollection<T>, T, S : ObservableCollectionBase<C, T, S>>
constructor(protected val delegate: C, observers: Iterable<(S) -> Unit> = emptyList()) : Collection<T> by delegate,
                                                                                         MutableCollection<T> {

    val observers = observers.toMutableArraySet()

    protected open class ObservableIterator<T>(protected val base: ObservableCollectionBase<*,T,*>, protected open val delegate: MutableIterator<T>) :
        Iterator<T> by delegate, MutableIterator<T> {

        override fun remove() {
            delegate.remove()
            base.triggerObservers()
        }

    }

    abstract fun triggerObservers()

    actual fun replaceBy(elements: Iterable<T>): Boolean {
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

    actual override fun addAll(elements: Collection<T>): Boolean = delegate.addAll(elements).apply { if (this) triggerObservers() }

    override fun removeIf(filter: Predicate<in T>): Boolean = delegate.removeIf(filter).apply { if (this) triggerObservers() }

    actual override fun add(element: T): Boolean = delegate.add(element).apply { if (this) triggerObservers() }

    actual override fun clear() {
        if (delegate.isNotEmpty()) {
            delegate.clear(); triggerObservers()
        }
    }

    actual override fun contains(element: T): Boolean = delegate.contains(element)

    actual override fun iterator(): MutableIterator<T> = ObservableIterator(this, delegate.iterator())

    actual override fun remove(element: T): Boolean = delegate.remove(element).apply { if (this) triggerObservers() }

    actual override fun removeAll(elements: Collection<T>): Boolean =
        delegate.removeAll(elements).apply { if (this) triggerObservers() }

    actual override fun retainAll(elements: Collection<T>): Boolean =
        delegate.retainAll(elements).apply { if (this) triggerObservers() }
}

actual class ObservableCollection<T>
actual constructor(delegate: MutableCollection<T>, observers: Iterable<(ObservableCollection<T>) -> Unit>) :
    ObservableCollectionBase<MutableCollection<T>, T, ObservableCollection<T>>(delegate, observers) {

    actual constructor(delegate: MutableCollection<T>, vararg observers: (ObservableCollection<T>) -> Unit) :
        this(delegate, observers.toList())

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }
}

actual class ObservableSet<T>
actual constructor(delegate: MutableSet<T>, observers: Iterable<(ObservableSet<T>) -> Unit>) :
    ObservableCollectionBase<MutableSet<T>, T, ObservableSet<T>>(delegate, observers), MutableSet<T> {

    actual constructor(delegate: MutableSet<T>, vararg observers: (ObservableSet<T>) -> Unit) :
        this(delegate, observers.toList())

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }

    override fun spliterator(): Spliterator<T> = delegate.spliterator()

    override fun toString(): String = joinToString(prefix = "ObservableSet[", postfix = "]")
}

actual class ObservableList<T>
actual constructor(delegate: MutableList<T>, observers: Iterable<(ObservableList<T>) -> Unit>) :
    ObservableCollectionBase<MutableList<T>, T, ObservableList<T>>(delegate, observers), List<T> by delegate,
    MutableList<T> {

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

    actual constructor(delegate: MutableList<T>, vararg observers: (ObservableList<T>) -> Unit) :
        this(delegate, observers.toList())

    override fun triggerObservers() {
        observers.forEach { it(this) }
    }

    override fun spliterator(): Spliterator<T> = delegate.spliterator()
    actual override fun iterator() = super.iterator()

    actual override fun listIterator(): MutableListIterator<T> = ObservableListIterator(delegate.listIterator())

    actual override fun listIterator(index: Int): MutableListIterator<T> = ObservableListIterator(delegate.listIterator(index))

    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return ObservableList(
            delegate.subList(
                fromIndex,
                toIndex
                            )
                             ).apply { observers.replaceBy(this@ObservableList.observers) }
    }

    actual override fun add(index: Int, element: T) {
        delegate.add(index, element)
        triggerObservers()
    }

    actual override fun addAll(index: Int, elements: Collection<T>) =
        delegate.addAll(index, elements).apply { if (this) triggerObservers() }

    actual override fun removeAt(index: Int) = delegate.removeAt(index).apply { triggerObservers() }

    actual override fun set(index: Int, element: T) = delegate.set(index, element).apply { triggerObservers() }

    actual override fun contains(element: T) = delegate.contains(element)

    actual override fun containsAll(elements: Collection<T>) = delegate.containsAll(elements)

    actual override val size: Int get() = delegate.size

    actual override fun isEmpty() = delegate.isEmpty()

    override fun toString(): String = joinToString(prefix = "ObservableList[", postfix = "]")
}
