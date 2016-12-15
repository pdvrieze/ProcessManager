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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util

import net.devrieze.util.collection.replaceBy
import java.util.*
import java.util.function.Predicate

class ObservableCollection<T>(delegate: MutableCollection<T>, observers: Iterable<ObservableCollection<T>.Observer> = emptyList()) : ObservableCollectionBase<MutableCollection<T>, T>(delegate, observers) {
  typealias Observer = (MutableCollection<T>)->Unit

  constructor(delegate: MutableCollection<T>, vararg observers: ObservableCollection<T>.Observer): this(delegate, observers.toList())

  override fun triggerObservers() {
    observers.forEach { it(this) }
  }
}

abstract class ObservableCollectionBase<C: MutableCollection<T>,T>(protected val delegate: C, observers: Iterable<ObservableCollectionBase<C,T>.ObserverT> = emptyList() ) : Collection<T> by delegate, MutableCollection<T> {

  val observers = observers.toMutableArraySet()

  open inner class ObservableIterator(protected open val delegate: MutableIterator<T>) : Iterator<T> by delegate, MutableIterator<T> {
    override fun remove() {
      delegate.remove()
      triggerObservers()
    }
  }

  typealias ObserverT = (C)->Unit

  abstract fun triggerObservers()

  override fun addAll(elements: Collection<T>) = delegate.addAll(elements).apply { if (this) triggerObservers() }

  override fun removeIf(filter: Predicate<in T>?) = delegate.removeIf(filter).apply { if (this) triggerObservers() }

  override fun add(element: T) = delegate.add(element).apply { if (this) triggerObservers() }

  override fun clear() {
    if (delegate.isNotEmpty()) { delegate.clear(); triggerObservers() }
  }

  override fun spliterator(): Spliterator<T> = delegate.spliterator()

  override fun iterator(): MutableIterator<T> = ObservableIterator(delegate.iterator())

  override fun remove(element: T) = delegate.remove(element).apply { if (this) triggerObservers() }

  override fun removeAll(elements: Collection<T>) = delegate.removeAll(elements).apply { if (this) triggerObservers() }

  override fun retainAll(elements: Collection<T>): Boolean = delegate.retainAll(elements).apply { if (this) triggerObservers() }
}

class ObservableSet<T>(delegate: MutableSet<T>, observers: Iterable<ObservableSet<T>.ObserverT> = emptyList()) : ObservableCollectionBase<MutableSet<T>, T>(delegate, observers), MutableSet<T> {
  typealias ObserverT = (MutableSet<T>)->Unit

  constructor(delegate: MutableSet<T>, vararg observers: ObservableSet<T>.ObserverT): this(delegate, observers.toList())

  override fun triggerObservers() {
    observers.forEach { it(this) }
  }

  override fun spliterator() = super<ObservableCollectionBase>.spliterator()
}

class ObservableList<T>(delegate: MutableList<T>, observers: Iterable<ObservableList<T>.ObserverT> = emptyList()) : ObservableCollectionBase<MutableList<T>, T>(delegate, observers), List<T> by delegate, MutableList<T> {

  private inner class ObservableListIterator(delegate: MutableListIterator<T>): ObservableIterator(delegate), ListIterator<T> by delegate, MutableListIterator<T> {
    override val delegate: MutableListIterator<T> get() = super.delegate as MutableListIterator
    override fun add(element: T) {
      delegate.add(element)
      triggerObservers()
    }

    override fun set(element: T) {
      delegate.set(element)
      triggerObservers()
    }
  }

  typealias ObserverT = (MutableList<T>)->Unit

  constructor(delegate: MutableList<T>, vararg observers: ObservableList<T>.ObserverT): this(delegate, observers.toList())

  override fun triggerObservers() {
    observers.forEach { it(this) }
  }

  override fun spliterator() = super<ObservableCollectionBase>.spliterator()
  override fun iterator() = super.iterator()

  override fun listIterator(): MutableListIterator<T> = ObservableListIterator(delegate.listIterator())

  override fun listIterator(index: Int): MutableListIterator<T> = ObservableListIterator(delegate.listIterator(index))

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
    return ObservableList(delegate.subList(fromIndex, toIndex)).apply { observers.replaceBy(this@ObservableList.observers) }
  }

  override fun add(index: Int, element: T) {
    delegate.add(index, element)
    triggerObservers()
  }

  override fun addAll(index: Int, elements: Collection<T>) = delegate.addAll(index, elements).apply { if (this) triggerObservers() }

  override fun removeAt(index: Int) = delegate.removeAt(index).apply { triggerObservers() }

  override fun set(index: Int, element: T) = delegate.set(index, element).apply { triggerObservers() }
}
