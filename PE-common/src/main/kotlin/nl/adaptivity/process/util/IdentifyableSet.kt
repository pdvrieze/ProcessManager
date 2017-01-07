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

package nl.adaptivity.process.util

import net.devrieze.util.ArraySet
import net.devrieze.util.MutableReadMap
import net.devrieze.util.ReadMap
import net.devrieze.util.toArraySet
import uk.ac.bournemouth.kotlinsql.Database

import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.AbstractList
import kotlin.collections.RandomAccess


interface MutableIdentifyableSet<T: Identifiable> : IdentifyableSet<T>, MutableSet<T>, MutableReadMap<String, T> {
  override fun spliterator(): Spliterator<T>

  fun addAll(c: Iterable<T>) = c.map { add(it) }.reduce(Boolean::or)

  operator fun set(index: Int, value:T): T

  fun addAll(sequence: Sequence<T>) = sequence.map { add(it) }.reduce(Boolean::or)

  fun removeAt(index: Int): T

  override fun removeAll(elements: Collection<T>) = elements.map { remove(it) }.reduce(Boolean::or)

  override fun retainAll(elements: Collection<T>) = removeIf { it !in elements }

  override fun iterator(): MutableIterator<T>
}

interface IdentifyableSet<out T : Identifiable> : Set<T>, ReadMap<String, T>, RandomAccess, Cloneable {

  private class ReadonlyIterator<T> constructor(private val mIterator: ListIterator<T>) : ListIterator<T> {

    override fun hasNext() = mIterator.hasNext()

    override fun next() = mIterator.next()

    override fun hasPrevious() = mIterator.hasPrevious()

    override fun previous() = mIterator.previous()

    override fun nextIndex() = mIterator.nextIndex()

    override fun previousIndex() = mIterator.previousIndex()

  }


  private class ReadOnlyIdentifyableSet<T: Identifiable> private constructor(private val data: Array<T>): IdentifyableSet<T> {

    @Suppress("UNCHECKED_CAST")
    constructor(delegate: IdentifyableSet<T>): this(delegate.toTypedArray<Identifiable>() as Array<T>)



    override fun clone(): ReadOnlyIdentifyableSet<T> {
      return this
    }

    fun subList(fromIndex: Int, toIndex: Int) = ReadOnlyIdentifyableSet(data.copyOfRange(fromIndex, toIndex))

    override fun get(index: Int): T {
      return data[index]
    }

    override fun contains(element: T) = indexOf(element) >=0

    override fun indexOf(element: T) = data.indexOf(element)

    override fun lastIndexOf(element: T) = data.lastIndexOf(element)

    override val size: Int get() = data.size

    override fun listIterator(index: Int): ListIterator<T> {
      return ReadonlyArrayIterator(data, index)
    }

    override fun equals(o: Any?): Boolean {
      return o is IdentifyableSet<*> && o.size == data.size && o.allIndexed { i, otherElem -> data[i] == otherElem }
    }

    override fun hashCode() = data.hashCode()
  }

  private class BaseIdentifyableSet<V : Identifiable>(private val data: ArraySet<V> = ArraySet()) : MutableIdentifyableSet<V>, MutableSet<V> by data {

    constructor(initialcapacity: Int) : this(ArraySet(initialcapacity))

    constructor(c: Sequence<V>) : this(ArraySet(c))

    constructor(c: Iterable<V>): this(ArraySet(c))

    override fun get(index: Int) = data[index]

    override fun containsAll(elements: Collection<V>) = data.containsAll(elements)

    override fun isEmpty() = data.isEmpty()

    override fun listIterator(index: Int): MutableListIterator<V> {
      return data.listIterator(index)
    }

    override fun iterator(): MutableIterator<V> {
      return data.iterator()
    }

    override fun spliterator() = data.spliterator()

    override fun clone(): BaseIdentifyableSet<V> {
      return BaseIdentifyableSet(data.toArraySet())
    }

    override fun add(element: V): Boolean {
      val name = element.id

      if (name == null) {
        if (data.contains(element)) {
          return false
        }
      } else if (containsKey(name)) {
        return false
      }

      data.add(element)
      return true
    }

    override fun set(index: Int, value: V) = data.set(index, value)

    override fun removeAll(elements: Collection<V>) = data.removeAll(elements)

    override fun retainAll(elements: Collection<V>) = data.removeAll(elements)

    override fun removeAt(index: Int) = data.removeAt(index)

    override operator fun contains(element: V): Boolean {
      val elementId = element.id
      return if (elementId != null) {
        data.any { it.id == elementId }
      } else {
        data.any { it == element }
      }
    }

    override fun clear() {
      data.clear()
    }

    override fun values(): IdentifyableSet<V> {
      return ReadOnlyIdentifyableSet(this)
    }


    override fun equals(o: Any?): Boolean {
      if (this === o) {
        return true
      }
      if (o == null || javaClass != o.javaClass) {
        return false
      }

      val that = o as BaseIdentifyableSet<*>?

      return data == that!!.data

    }

    override fun hashCode(): Int {
      var result = 1
      result = 31 * result + data.hashCode()
      return result
    }
  }

  private object EmptyIdentifyableSet : MutableIdentifyableSet<Identifiable> {

    override fun clone() = this

    override fun isEmpty() = true

    override val keys: Set<String> get() = emptySet()

    override fun values() = this

    override fun get(index: Int) = throw IndexOutOfBoundsException()
    override fun set(index: Int, value: Identifiable) = throw IndexOutOfBoundsException()

    override val size: Int get() = 0

    override fun contains(element: Identifiable) = false

    override fun containsAll(elements: Collection<Identifiable>) = elements.isEmpty()

    override fun indexOf(element: Identifiable)= -1

    override fun lastIndexOf(element: Identifiable) = -1

    override fun spliterator(): Spliterator<Identifiable> = Spliterators.emptySpliterator()

    override fun iterator() = Collections.emptyIterator<Identifiable>()

    override fun listIterator(): ListIterator<Identifiable> = Collections.emptyListIterator()

    override fun listIterator(index: Int): ListIterator<Identifiable> {
      if (index!=0) throw IndexOutOfBoundsException(index.toString()) else return Collections.emptyListIterator()
    }

    fun subList(fromIndex: Int, toIndex: Int): EmptyIdentifyableSet {
      if (fromIndex!=0 || toIndex !=0) throw IndexOutOfBoundsException() else return this
    }

    override fun removeAt(index: Int) = throw IndexOutOfBoundsException()

    override fun add(element: Identifiable) = throw IllegalStateException("No elements can be added to this list")

    override fun addAll(elements: Collection<Identifiable>)
            = if (elements.isEmpty()) false else throw IllegalStateException("No elements can be added to this list")

    override fun clear() = Unit // No meaning

    override fun remove(element: Identifiable) = false

    override fun hashCode() = 1

    override fun equals(o: Any?): Boolean {
      if (this === o) {
        return true
      }
      if (o == null || javaClass != o.javaClass) {
        return false
      }
      return true
    }
  }

  private class SingletonIdentifyableSet<V : Identifiable> : AbstractSet<V>, MutableIdentifyableSet<V> {
    private var element: V? = null

    constructor() {}

    constructor(element: V?) {
      if (element == null) throw NullPointerException()
      this.element = element
    }

    override fun clone(): SingletonIdentifyableSet<V> {
      if (element == null) {
        return SingletonIdentifyableSet()
      } else {
        return SingletonIdentifyableSet(element)
      }
    }

    override fun add(e: V): Boolean {
      if (e == element) {
        return false
      } else if (element == null) {
        element = e
        return true
      } else {
        throw IllegalStateException("Singleton node set can only contain one element")
      }
    }

    override fun values(): IdentifyableSet<V> = this

    override fun get(index: Int): V {
      element.let {
        if (it == null || index != 0) {
          throw IndexOutOfBoundsException()
        }
        return it
      }
    }

    override fun set(index: Int, element: V): V {
      this.element.let {
        if (it == null || index != 0) throw IndexOutOfBoundsException()
        this.element = element
        return it
      }
    }

    override val size: Int get() = if (element == null) 0 else 1

    override fun isEmpty() = element == null

    override fun removeAt(index: Int): V {
      element.let { element ->
        if (element == null || index != 0) throw IndexOutOfBoundsException()
        this.element = null
        return element
      }
    }

    override fun iterator(): MutableIterator<V> {
      return object: MutableIterator<V> {
        var pos=0
        override fun hasNext() = pos==0 && element!=null

        override fun next(): V {
          if (! hasNext()) throw NoSuchElementException()
          pos++
          return element!!
        }

        override fun remove() {
          if (pos!=1 || element==null) throw NoSuchElementException()
          element = null
        }
      }
    }

    override fun listIterator(initialPos: Int): ListIterator<V> {
      return when (element) {
        null -> {
          if (initialPos!=0) throw IndexOutOfBoundsException()
          Collections.emptyListIterator<V>()
        }
        else -> ReadonlyIterator(listOf(element!!).listIterator(initialPos))
      }
    }

    override fun spliterator(): Spliterator<V> {
      return super.spliterator()
    }

    override fun indexOf(element: V) = if (this.element == element) 0 else -1

    override fun lastIndexOf(element: V) = indexOf(element)

    override fun clear() { element = null }

    override fun remove(element: V): Boolean {
      if (this.element == element) {
        this.element = null
        return true
      } else return false
    }

    override fun removeAll(elements: Collection<V>) = elements.map { remove(it) }.reduce(Boolean::or)

    override fun retainAll(elements: Collection<V>) = element?.let { element ->
      if (element in elements) {
        this.element = null
        true
      } else {
        false
      }
    } ?: false

    override fun containsAll(elements: Collection<V>) = element != null && elements.all { it == element }

    override fun equals(o: Any?): Boolean {
      if (this === o) {
        return true
      }
      if (o == null || javaClass != o.javaClass) {
        return false
      }

      val that = o as SingletonIdentifyableSet<*>?

      return if (element != null) element == that!!.element else that!!.element == null

    }

    override fun hashCode(): Int {
      var result = 1
      result = 31 * result + if (element != null) element!!.hashCode() else 0
      return result
    }
  }

  private class MyKeyIterator(private val mParent: Iterator<Identifiable>) : MutableIterator<String> {

    override fun hasNext() = mParent.hasNext()

    override fun next() = mParent.next().id !!

    override fun remove() {
      (mParent as? MutableIterator<Identifiable>)?.remove() ?:
          throw UnsupportedOperationException("The key set is not mutable")
    }
  }

  private class MyKeySet(private val delegate: IdentifyableSet<*>) : AbstractSet<String>() {

    override fun iterator(): MutableIterator<String> {
      return MyKeyIterator(delegate.iterator())
    }

    override val size = delegate.size

  }

  override fun containsAll(elements: Collection<@kotlin.UnsafeVariance T>) = elements.all { contains(it) }

  override fun containsKey(key: String): Boolean {
    return get(key) != null
  }

  override fun containsValue(value: @kotlin.UnsafeVariance T): Boolean {
    return contains(value)
  }

  override fun isEmpty() = size==0

  public abstract override fun clone(): IdentifyableSet<T>

  operator fun get(pos: Int): T

  operator fun get(key: Identifiable): T? {
    return key.id?.let{get(it)}
  }

  override fun get(key: String): T? {
    if (key == null) {
      for (elem in this) {
        if (elem.id == null) {
          return elem
        }
      }
    } else {
      for (elem in this) {
        if (key == elem.id) {
          return elem
        }
      }
    }
    return null
  }

  fun indexOf(element: @kotlin.UnsafeVariance T) : Int = indexOfFirst { it == element }

  fun lastIndexOf(element: @kotlin.UnsafeVariance T) : Int = indexOfLast { it == element }

  override val keys: Set<String> get() = MyKeySet(this)

  override fun iterator(): Iterator<T> = listIterator(0)

  fun listIterator(): ListIterator<T> = listIterator(0)

  fun listIterator(initialPos:Int): ListIterator<T>

  override fun spliterator(): Spliterator<@kotlin.UnsafeVariance T> {
    return Spliterators.spliterator(this, Spliterator.DISTINCT or Spliterator.NONNULL)
  }

  override val values get() = readOnly()
  override fun values() = readOnly()

  fun readOnly(): IdentifyableSet<T> {
    if (this is IdentifyableSet.ReadOnlyIdentifyableSet) {
      return this
    }
    return ReadOnlyIdentifyableSet(this)
  }

  companion object {

    fun <V : Identifiable> processNodeSet(): MutableIdentifyableSet<V> {
      return BaseIdentifyableSet()
    }

    fun <V : Identifiable> processNodeSet(initialCapacity: Int): MutableIdentifyableSet<V> {
      return BaseIdentifyableSet(initialCapacity)
    }

    fun <V : Identifiable> processNodeSet(collection: Sequence<V>): MutableIdentifyableSet<V> {
      return BaseIdentifyableSet(collection)
    }

    fun <V : Identifiable> processNodeSet(collection: Iterable<V>): MutableIdentifyableSet<V> {
      return BaseIdentifyableSet(collection)
    }

    fun <V : Identifiable> processNodeSet(maxSize: Int, elements: Collection<V>): MutableIdentifyableSet<V> {
      when (maxSize) {
        0 -> {
          if (elements.isNotEmpty()) {
            throw IllegalArgumentException("More elements than allowed")
          }
          return IdentifyableSet.empty<V>()
        }
        1 -> {
          run {
            if (elements.size > 1) {
              throw IllegalArgumentException("More elements than allowed")
            }
            val iterator = elements.iterator()
            if (iterator.hasNext())
              return singleton(iterator.next())
            else
              return singleton()
          }
          return processNodeSet(elements)
        }
        else -> return processNodeSet(elements)
      }
    }

    fun <V : Identifiable> processNodeSet(maxSize: Int, elements: Sequence<V>): IdentifyableSet<V> {
      val it = elements.iterator()
      when (maxSize) {
        0 -> {
          if (it.hasNext()) {
            throw IllegalArgumentException("More elements than allowed")
          }
          return IdentifyableSet.empty<V>()
        }
        1 -> {
          run {
            if (it.hasNext()) {
              try {
                return singleton(it.next())
              } finally {
                if (it.hasNext()) {
                  throw IllegalArgumentException("More elements than allowed")
                }
              }
            } else {
              return singleton()
            }
          }
          return processNodeSet(elements)
        }
        else -> return processNodeSet(elements)
      }
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Identifiable> empty(): MutableIdentifyableSet<V> {
      // This can be unsafe as it is actually empty
      return EmptyIdentifyableSet as MutableIdentifyableSet<V>
    }

    fun <V : Identifiable> singleton(): MutableIdentifyableSet<V> {
      return SingletonIdentifyableSet()
    }

    fun <V : Identifiable> singleton(element: V): MutableIdentifyableSet<V> {
      return SingletonIdentifyableSet(element)
    }
  }

}

