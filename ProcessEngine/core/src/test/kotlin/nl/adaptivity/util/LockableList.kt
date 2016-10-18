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

package nl.adaptivity.util

import java.util.*
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * Created by pdvrieze on 16/10/16.
 */
interface LockableList<T> : MutableList<T> {

  fun lock():Unit
  val locked:Boolean

}

class ArrayLockableList<T> : ArrayList<T>, LockableList<T> {
  constructor(initialCapacity: Int) : super(initialCapacity)
  constructor() : super()
  constructor(c: Collection<out T>) : super(c)

  override fun lock() {
    locked=true
  }

  override var locked: Boolean = false
    private set

  private inline fun checkLocked() {
    if (locked) throw IllegalStateException("The list is locked, modification is not allowed")
  }

  override fun clear() {
    checkLocked()
    super.clear()
  }

  override fun add(index: Int, element: T) {
    checkLocked()
    super.add(index, element)
  }

  override fun add(element: T): Boolean {
    checkLocked()
    return super.add(element)
  }

  override fun set(index: Int, element: T): T {
    checkLocked()
    return super.set(index, element)
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    checkLocked()
    return super.addAll(index, elements)
  }

  override fun addAll(elements: Collection<T>): Boolean {
    checkLocked()
    return super.addAll(elements)
  }

  override fun sort(c: Comparator<in T>?) {
    checkLocked()
    super.sort(c)
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    checkLocked()
    return super.removeAll(elements)
  }

  override fun replaceAll(operator: UnaryOperator<T>?) {
    checkLocked()
    super.replaceAll(operator)
  }

  override fun listIterator(): MutableListIterator<T> {
    if (locked) {
      return Collections.unmodifiableList(this).listIterator()
    }
    return super.listIterator()
  }

  override fun listIterator(index: Int): MutableListIterator<T> {
    if (locked) {
      return Collections.unmodifiableList(this).listIterator(index)
    }
    return super.listIterator(index)
  }

  override fun removeRange(fromIndex: Int, toIndex: Int) {
    checkLocked()
    super.removeRange(fromIndex, toIndex)
  }

  override fun removeIf(filter: Predicate<in T>?): Boolean {
    checkLocked()
    return super.removeIf(filter)
  }

  override fun iterator(): MutableIterator<T> {
    if (locked) {
      return Collections.unmodifiableList(this).iterator()
    }
    return super.iterator()
  }

  override fun removeAt(index: Int): T {
    checkLocked()
    return super.removeAt(index)
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
    if (locked) {
      return Collections.unmodifiableList(subList(fromIndex, toIndex))
    }
    return super.subList(fromIndex, toIndex)
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    checkLocked()
    return super.retainAll(elements)
  }

  override fun remove(element: T): Boolean {
    checkLocked()
    return super.remove(element)
  }
}