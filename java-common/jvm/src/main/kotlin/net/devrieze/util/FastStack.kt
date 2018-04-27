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

/*
 * Created on Oct 27, 2004
 */

package net.devrieze.util

import net.devrieze.lang.Const
import java.lang.ref.SoftReference
import java.util.*
import kotlin.NoSuchElementException


/**
 * A class implementing a Stack that can easilly grow due to the fact that the
 * stack is actually immutable. It's append and shrink methods allow operations
 * without cloneing. The contains operation is sped up using a HashSet

 * @param E The type of the elements in the stack
 *
 * @author Paul de Vrieze
 *
 * @version 1.0 $Revision$
 */

interface FastStack<E> : List<E> {
  val previous: FastStack<E>?

  fun append(elem: E) : FastStack<E>

  fun reverseSequence(): Sequence<E>

}

@Suppress("UNCHECKED_CAST")
fun <E> FastStack() : FastStack<E> = EMPTY as FastStack<E>

/**
 * Create a new FastStack, based on the to-be appended element and a previous
 * stack.
 *
 * @param elem The element to be appended
 */

fun <E> FastStack(elem: E) : FastStack<E> = FastStackImpl(elem)

/**
 * Create a new [FastStack]based on the given Iterable.
 *
 * @param pIterable The initial contents.
 */
fun <E> FastStack(iterable: Iterable<E>) = FastStack(iterable.asSequence())

/**
 * Create a new [FastStack]based on the given iterator.
 *
 * @param pIterator The iterator to base on.
 */
fun <E> FastStack(iterator: Iterator<E>) = FastStack(iterator.asSequence())

/**
 * Create a new [FastStack]based on the given Sequence. The iterable may
 * not be empty.

 * @param pIterable The initial contents. May not be empty.
 */
fun <E> FastStack(sequence: Sequence<E>): FastStack<E> {
  return sequence.fold(FastStack()) { base, elem -> base.append(elem) }
}

private object EMPTY: FastStack<Any> {
  override val size: Int = 0
  override fun contains(element: Any) = false
  override fun containsAll(elements: Collection<Any>) = elements.isEmpty()
  override fun get(index: Int) = throw IndexOutOfBoundsException()
  override fun indexOf(element: Any) = -1
  override fun isEmpty() = true

  override fun iterator(): Iterator<Any> = Collections.emptyIterator<Any>()
  override fun lastIndexOf(element: Any) = -1
  override fun listIterator(): ListIterator<Any> = Collections.emptyListIterator<Any>()
  override fun listIterator(index: Int) = if (index==0) listIterator() else  throw IndexOutOfBoundsException()
  override fun subList(fromIndex: Int, toIndex: Int) = if(fromIndex==0 && toIndex==0) this else throw IndexOutOfBoundsException()

  override val previous: FastStack<Any>? get() = null

  override fun append(elem: Any) = FastStack(elem)

  override fun reverseSequence(): Sequence<Any> { return sequenceOf() }
}

/**
 * Create a new FastStack, based on the to-be appended element and a previous
 * stack.

 * @param elem The element to be appended
 *
 * @param previous The stack to base the stack on.
 */
class FastStackImpl<E> @JvmOverloads constructor(elem: E, override var previous: FastStack<E> = FastStack()) : FastStack<E> {

  /**
   * The last item in the stack.
   */
  val lastElem: E = elem

  fun last(): E = lastElem

  @Transient private var containsCache: SoftReference<HashSet<E>>? = null

  @Suppress("UNCHECKED_CAST")
  fun toArray(): Array<Any> {
    val data = kotlin.arrayOfNulls<Any>(size)
    val last = data.size-1
    reverseSequence().forEachIndexed { ridx, e ->
      data[last-ridx] = e
    }
    return data as Array<Any>
  }

  @Suppress("UNCHECKED_CAST")
  fun toList(): List<E> = Arrays.asList<E>(*toArray() as Array<E>)

  override fun reverseSequence(): Sequence<E> {
    return object: Sequence<E> {
      override fun iterator() = reverseIterator()
    }
  }

  fun reverseIterator(): Iterator<E> {
    return object : Iterator<E> {
      var current: FastStack<E> = this@FastStackImpl

      override fun hasNext() = !previous.isEmpty()

      override fun next(): E {
        current = current.previous ?: throw NoSuchElementException()
        return current.last()
      }
    }
  }

  /**
   * Get a new fastStack representing the appended stack.

   * @param elem The element to append
   *
   * @return A new FastStack element representing the new list.
   */
  override fun append(elem: E): FastStack<E> {
    return FastStackImpl(elem, this)
  }

  /**
   * Get the FastStack representing a list with one less element.

   * @return The shorter stack
   */
  fun shrink(): FastStack<E> {
    return previous
  }

  override val size: Int = previous.size + 1

  /**
   * Allways returns `false` because of FastStacks not being able to
   * be empty.

   * @return `false`
   *
   * @see Collection.isEmpty
   */
  override fun isEmpty(): Boolean {
    return false
  }

  override fun contains(element: E): Boolean {
    if (lastElem == element) {
      return true
    }
    if (_ENABLE_CONTAINS_CACHING) {
      val cache = containsCache?.get() ?: reverseSequence().toHashSet().apply { containsCache = SoftReference(this) }

      return cache.contains(element)
    }
    return reverseSequence().lastIndexOf(element)>=0
  }

  /**
   * {@inheritDoc}

   * @see java.lang.Iterable.iterator
   */
  override fun iterator(): Iterator<E> {
    return listIterator()
  }

  /**
   * This method does not yet use the cache if enabled and available.
   * {@inheritDoc}

   * @see java.util.Collection.containsAll
   */
  override fun containsAll(elements: Collection<E>): Boolean {
    val remaining = elements.toHashSet<E>()

    /**
     * Helper function for the containsAll function. This function is recursive.
     * It checks whether the current element is part of the HashSet. It it is, the
     * element is removed. When the hash is empty, the function returns true.
     *
     * @return true if all elements are contained, false if not
     */
    fun FastStack<E>.containsAll2(): Boolean {
      if (isEmpty()) return remaining.isEmpty()
      val element:E = last()
      remaining.remove(element)
      if(remaining.isEmpty()) return true
      return previous?.containsAll2() ?: false // previous is empty, but remaining items to check not
    }

    return containsAll2()
  }

  /**
   * {@inheritDoc}

   * @see java.util.List.get
   */
  override fun get(index: Int): E {
    val size = size
    if (index >= size || index < 0) {
      throw IndexOutOfBoundsException()
    }
    return reverseSequence().elementAt(size - index - 1)
  }

  /**
   * The current implementation is very slow. {@inheritDoc}

   * @see java.util.List.indexOf
   */
  override fun indexOf(element: E): Int {
    return size - reverseSequence().lastIndexOf(element) - 1
  }

  /**
   * The current implementation is very slow. {@inheritDoc}

   * @see java.util.List.lastIndexOf
   */
  override fun lastIndexOf(element: E): Int {
    return size - reverseSequence().indexOf(element) - 1
  }

  /**
   * {@inheritDoc}

   * @see java.util.List.listIterator
   */
  override fun listIterator(): ListIterator<E> {
    return toList().listIterator()
  }

  /**
   * {@inheritDoc}

   * @see java.util.List.listIterator
   */
  override fun listIterator(index: Int): ListIterator<E> {
    return toList().listIterator(index)
  }

  /**
   * For now unsupported, but could be implemented with a subclass.

   * @param fromIndex the starting index
   *
   * @param toIndex the end index
   *
   * @return The resulting list
   *
   * @see java.util.List.subList
   */
  override fun subList(fromIndex: Int, toIndex: Int): List<E> {
    throw UnsupportedOperationException()
  }

  override fun toString(): String {

    fun FastStack<E>.appendTo(stringBuilder: StringBuilder) {
      previous?.let { previous ->
        if (!previous.isEmpty()) {
          previous.appendTo(stringBuilder)
          stringBuilder.append(", ")
        }

      }
      stringBuilder.append(last())
    }

    return buildString {
      append('[')
      appendTo(this)
      append(']')
    }
  }

  /**
   * Get the last x element in the stack.
   *
   * @param reverseIndex The index from the end
   *
   * @return the element
   */
  fun getLast(reverseIndex: Int): E {
    return reverseSequence().elementAt(reverseIndex)
  }

  /**
   * Flush the cache for this stack's containscache.
   */
  fun flushCache() {
    if (_ENABLE_CONTAINS_CACHING) {
      containsCache = null
    }
  }

  /**
   * Check the cache status.

   * @return `true` if the contains function is cached,
   * *         `false` if not.
   */
  val isCached: Boolean
    get() {
      if (_ENABLE_CONTAINS_CACHING) {
        return containsCache != null
      }

      return false
    }

  /**
   * @see java.lang.Object.equals
   */
  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }
    if (other === this) {
      return true
    }
    if (other.javaClass == FastStackImpl::class.java) {
      val f = other as FastStackImpl<*>
      if (lastElem != f.lastElem) {
        return false
      }
      return previous == f.previous
    }
    return  false
  }

  override fun hashCode(): Int {
    return super.hashCode() * Const._HASHPRIME xor (lastElem?.hashCode() ?: 0)
  }

  companion object {

    // Apparently faster without caching
    private val _ENABLE_CONTAINS_CACHING = false
  }

  init {
    containsCache = null
  }
}
