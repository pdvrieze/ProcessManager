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

package nl.adaptivity.util.multiplatform

actual interface Queue<E> : MutableCollection<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * `true` upon success and throwing an `IllegalStateException`
     * if no space is currently available.
     *
     * @param element the element to add
     * @return `true` (as specified by [Collection.add])
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     * this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     * prevents it from being added to this queue
     */
    actual override fun add(element: E): Boolean

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to [.add], which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return `true` if the element was added to this queue, else
     * `false`
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     * this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     * prevents it from being added to this queue
     */
    actual fun offer(e: E): Boolean

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from [poll][.poll] only in that it throws an exception if this
     * queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    actual fun remove(): E

    /**
     * Retrieves and removes the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    actual fun poll(): E?

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from [peek][.peek] only in that it throws an exception
     * if this queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    actual fun element(): E

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    actual fun peek(): E?
}

actual interface Deque<E>: Queue<E> {
    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an `IllegalStateException` if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method [.offerFirst].
     *
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual fun addFirst(e: E)

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an `IllegalStateException` if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method [.offerLast].
     *
     *
     * This method is equivalent to [.add].
     *
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual fun addLast(e: E)

    /**
     * Inserts the specified element at the front of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the [.addFirst] method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @return `true` if the element was added to this deque, else
     * `false`
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual fun offerFirst(e: E): Boolean

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the [.addLast] method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @return `true` if the element was added to this deque, else
     * `false`
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual fun offerLast(e: E): Boolean

    /**
     * Retrieves and removes the first element of this deque.  This method
     * differs from [pollFirst][.pollFirst] only in that it throws an
     * exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual fun removeFirst(): E

    /**
     * Retrieves and removes the last element of this deque.  This method
     * differs from [pollLast][.pollLast] only in that it throws an
     * exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual fun removeLast(): E

    /**
     * Retrieves and removes the first element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the head of this deque, or `null` if this deque is empty
     */
    actual fun pollFirst(): E

    /**
     * Retrieves and removes the last element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the tail of this deque, or `null` if this deque is empty
     */
    actual fun pollLast(): E

    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * This method differs from [peekFirst][.peekFirst] only in that it
     * throws an exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual fun getFirst(): E

    /**
     * Retrieves, but does not remove, the last element of this deque.
     * This method differs from [peekLast][.peekLast] only in that it
     * throws an exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual fun getLast(): E

    /**
     * Retrieves, but does not remove, the first element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the head of this deque, or `null` if this deque is empty
     */
    actual fun peekFirst(): E

    /**
     * Retrieves, but does not remove, the last element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the tail of this deque, or `null` if this deque is empty
     */
    actual fun peekLast(): E

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element `e` such that
     * `Objects.equals(o, e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return `true` if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     * is incompatible with this deque
     * ([optional](Collection.html#optional-restrictions))
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * ([optional](Collection.html#optional-restrictions))
     */
    actual fun removeFirstOccurrence(o: Any?): Boolean

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element `e` such that
     * `Objects.equals(o, e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return `true` if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     * is incompatible with this deque
     * ([optional](Collection.html#optional-restrictions))
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * ([optional](Collection.html#optional-restrictions))
     */
    actual fun removeLastOccurrence(o: Any?): Boolean

    // *** Queue methods ***

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * `true` upon success and throwing an
     * `IllegalStateException` if no space is currently available.
     * When using a capacity-restricted deque, it is generally preferable to
     * use [offer][.offer].
     *
     *
     * This method is equivalent to [.addLast].
     *
     * @param element the element to add
     * @return `true` (as specified by [Collection.add])
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual override fun add(element: E): Boolean

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * `true` upon success and `false` if no space is currently
     * available.  When using a capacity-restricted deque, this method is
     * generally preferable to the [.add] method, which can fail to
     * insert an element only by throwing an exception.
     *
     *
     * This method is equivalent to [.offerLast].
     *
     * @param e the element to add
     * @return `true` if the element was added to this deque, else
     * `false`
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual override fun offer(e: E): Boolean

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque).
     * This method differs from [poll][.poll] only in that it throws an
     * exception if this deque is empty.
     *
     *
     * This method is equivalent to [.removeFirst].
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual override fun remove(): E

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * `null` if this deque is empty.
     *
     *
     * This method is equivalent to [.pollFirst].
     *
     * @return the first element of this deque, or `null` if
     * this deque is empty
     */
    actual override fun poll(): E

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque).
     * This method differs from [peek][.peek] only in that it throws an
     * exception if this deque is empty.
     *
     *
     * This method is equivalent to [.getFirst].
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    actual override fun element(): E

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque), or
     * returns `null` if this deque is empty.
     *
     *
     * This method is equivalent to [.peekFirst].
     *
     * @return the head of the queue represented by this deque, or
     * `null` if this deque is empty
     */
    actual override fun peek(): E?


    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * `IllegalStateException` if no space is currently available.
     *
     *
     * This method is equivalent to [.addFirst].
     *
     * @param e the element to push
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     * element prevents it from being added to this deque
     */
    actual fun push(e: E)

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     *
     * This method is equivalent to [.removeFirst].
     *
     * @return the element at the front of this deque (which is the top
     * of the stack represented by this deque)
     * @throws NoSuchElementException if this deque is empty
     */
    actual fun pop(): E


    // *** Collection methods ***

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element `e` such that
     * `Objects.equals(o, e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     *
     * This method is equivalent to [.removeFirstOccurrence].
     *
     * @param element element to be removed from this deque, if present
     * @return `true` if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     * is incompatible with this deque
     * ([optional](Collection.html#optional-restrictions))
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * ([optional](Collection.html#optional-restrictions))
     */
    actual override fun remove(element: E): Boolean

    /**
     * Returns `true` if this deque contains the specified element.
     * More formally, returns `true` if and only if this deque contains
     * at least one element `e` such that `Objects.equals(o, e)`.
     *
     * @param element element whose presence in this deque is to be tested
     * @return `true` if this deque contains the specified element
     * @throws ClassCastException if the class of the specified element
     * is incompatible with this deque
     * ([optional](Collection.html#optional-restrictions))
     * @throws NullPointerException if the specified element is null and this
     * deque does not permit null elements
     * ([optional](Collection.html#optional-restrictions))
     */
    actual override operator fun contains(element: E): Boolean

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    actual override val size: Int

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    actual override fun iterator(): MutableIterator<E>

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * @return an iterator over the elements in this deque in reverse
     * sequence
     */
    actual fun descendingIterator(): Iterator<E>


}

/**
 * Resizable-array implementation of the [Deque] interface.  Array
 * deques have no capacity restrictions; they grow as necessary to support
 * usage.  They are not thread-safe; in the absence of external
 * synchronization, they do not support concurrent access by multiple threads.
 * Null elements are prohibited.  This class is likely to be faster than
 * [Stack] when used as a stack, and faster than [LinkedList]
 * when used as a queue.
 *
 *
 * Most `ArrayDeque` operations run in amortized constant time.
 * Exceptions include
 * [remove][.remove],
 * [removeFirstOccurrence][.removeFirstOccurrence],
 * [removeLastOccurrence][.removeLastOccurrence],
 * [contains][.contains],
 * [iterator.remove()][.iterator],
 * and the bulk operations, all of which run in linear time.
 *
 *
 * The iterators returned by this class's [iterator][.iterator]
 * method are *fail-fast*: If the deque is modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * `remove` method, the iterator will generally throw a [ ].  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 *
 * Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw `ConcurrentModificationException` on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: *the fail-fast behavior of iterators
 * should be used only to detect bugs.*
 *
 *
 * This class and its iterator implement all of the
 * *optional* methods of the [Collection] and [ ] interfaces.
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.6
 * @param <E> the type of elements held in this deque
</E> */
actual class ArrayDeque<E> : AbstractCollection<E>, Deque<E> {
    /**
     * The array in which the elements of the deque are stored.
     * The capacity of the deque is the length of this array, which is
     * always a power of two. The array is never allowed to become
     * full, except transiently within an addX method where it is
     * resized (see doubleCapacity) immediately upon becoming full,
     * thus avoiding head and tail wrapping around to equal each
     * other.  We also guarantee that all array cells not holding
     * deque elements are always null.
     */
     private lateinit var elements: Array<Any?> // non-private to simplify nested class access

    /**
     * The index of the element at the head of the deque (which is the
     * element that would be removed by remove() or pop()); or an
     * arbitrary number equal to tail if the deque is empty.
     */
     private var head: Int = 0

    /**
     * The index at which the next element would be added to the tail
     * of the deque (via addLast(E), add(E), or push(E)).
     */
     private var tail: Int = 0

    // ******  Array allocation and resizing utilities ******

    /**
     * Allocates empty array to hold the given number of elements.
     *
     * @param numElements  the number of elements to hold
     */
    private fun allocateElements(numElements: Int) {
        var initialCapacity = MIN_INITIAL_CAPACITY
        // Find the best power of two to hold elements.
        // Tests "<=" because arrays aren't kept full.
        if (numElements >= initialCapacity) {
            initialCapacity = numElements
            initialCapacity = initialCapacity or initialCapacity.ushr(1)
            initialCapacity = initialCapacity or initialCapacity.ushr(2)
            initialCapacity = initialCapacity or initialCapacity.ushr(4)
            initialCapacity = initialCapacity or initialCapacity.ushr(8)
            initialCapacity = initialCapacity or initialCapacity.ushr(16)
            initialCapacity++

            if (initialCapacity < 0)
            // Too many elements, must back off
                initialCapacity = initialCapacity ushr 1 // Good luck allocating 2^30 elements
        }
        elements = arrayOfNulls(initialCapacity)
    }

    /**
     * Doubles the capacity of this deque.  Call only when full, i.e.,
     * when head and tail have wrapped around to become equal.
     */
    private fun doubleCapacity() {
        assert(head == tail)
        val p = head
        val n = elements.size
        val r = n - p // number of elements to the right of p
        val newCapacity = n shl 1
        if (newCapacity < 0)
            throw IllegalStateException("Sorry, deque too big")
        val a = arrayOfNulls<Any>(newCapacity)
        arraycopy(elements, p, a, 0, r)
        arraycopy(elements, 0, a, r, p)
        elements = a
        head = 0
        tail = n
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold 16 elements.
     */
    actual constructor() {
        elements = arrayOfNulls(16)
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold the specified number of elements.
     *
     * @param numElements  lower bound on initial capacity of the deque
     */
    actual constructor(numElements: Int) {
        allocateElements(numElements)
    }

    /**
     * Constructs a deque containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.  (The first element returned by the collection's
     * iterator becomes the first element, or *front* of the
     * deque.)
     *
     * @param c the collection whose elements are to be placed into the deque
     * @throws NullPointerException if the specified collection is null
     */
    actual constructor(c: Collection<E>) {
        allocateElements(c.size)
        addAll(c)
    }

    // The main insertion and extraction methods are addFirst,
    // addLast, pollFirst, pollLast. The other methods are defined in
    // terms of these.

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    override fun addFirst(e: E) {
        if (e == null)
            throw NullPointerException()
        head = head - 1 and elements.size - 1
        elements[head] = e
        if (head == tail)
            doubleCapacity()
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     *
     * This method is equivalent to [.add].
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    override fun addLast(e: E) {
        if (e == null)
            throw NullPointerException()
        elements[tail] = e
        tail = tail + 1 and elements.size - 1
        if (tail == head)
            doubleCapacity()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return elements.fold(false) { acc, e->
            acc or add(e)
        }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return elements.fold(false) { acc, e->
            acc or remove(e)
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var changed: Boolean = false
        val it = iterator()
        while (it.hasNext()) {
            if (it.next() !in elements) {
                it.remove()
                changed = true
            }
        }
        return changed
    }

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @return `true` (as specified by [Deque.offerFirst])
     * @throws NullPointerException if the specified element is null
     */
    override fun offerFirst(e: E): Boolean {
        addFirst(e)
        return true
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * @param e the element to add
     * @return `true` (as specified by [Deque.offerLast])
     * @throws NullPointerException if the specified element is null
     */
    override fun offerLast(e: E): Boolean {
        addLast(e)
        return true
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun removeFirst(): E {
        val x = pollFirst() ?: throw NoSuchElementException()
        return x
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun removeLast(): E {
        val x = pollLast() ?: throw NoSuchElementException()
        return x
    }

    override fun pollFirst(): E {
        val elements = this.elements
        val h = head
        @Suppress("UNCHECKED_CAST")
        val result = elements[h] as E
        // Element is null if deque empty
        if (result != null) {
            elements[h] = null // Must null out slot
            head = h + 1 and elements.size - 1
        }
        return result
    }

    override fun pollLast(): E {
        val elements = this.elements
        val t = tail - 1 and elements.size - 1
        @Suppress("UNCHECKED_CAST")
        val result = elements[t] as E
        if (result != null) {
            elements[t] = null
            tail = t
        }
        return result
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun getFirst(): E {
        @Suppress("UNCHECKED_CAST")
        val result = elements[head] as E ?: throw NoSuchElementException()
        return result
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun getLast(): E {
        @Suppress("UNCHECKED_CAST")
        val result = elements[tail - 1 and elements.size - 1] as E ?: throw NoSuchElementException()
        return result
    }

    override fun peekFirst(): E {
        // elements[head] is null if deque empty
        @Suppress("UNCHECKED_CAST")
        return elements[head] as E
    }

    override fun peekLast(): E {
        @Suppress("UNCHECKED_CAST")
        return elements[tail - 1 and elements.size - 1] as E
    }

    /**
     * Removes the first occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element `e` such that
     * `o.equals(e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return `true` if the deque contained the specified element
     */
    override fun removeFirstOccurrence(o: Any?): Boolean {
        if (o != null) {
            val mask = elements.size - 1
            var i = head
            var x: Any? = null
            while ((run { x = elements[i]; x }) != null) {
                if (o == x) {
                    delete(i)
                    return true
                }
                i = i + 1 and mask
            }
        }
        return false
    }

    /**
     * Removes the last occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element `e` such that
     * `o.equals(e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return `true` if the deque contained the specified element
     */
    override fun removeLastOccurrence(o: Any?): Boolean {
        if (o != null) {
            val mask = elements.size - 1
            var i = tail - 1 and mask
            var x: Any? = null
            while ((run { x = elements[i]; x }) != null) {
                if (o == x) {
                    delete(i)
                    return true
                }
                i = i - 1 and mask
            }
        }
        return false
    }

    // *** Queue methods ***

    /**
     * Inserts the specified element at the end of this deque.
     *
     *
     * This method is equivalent to [.addLast].
     *
     * @param e the element to add
     * @return `true` (as specified by [Collection.add])
     * @throws NullPointerException if the specified element is null
     */
    override fun add(element: E): Boolean {
        addLast(element)
        return true
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     *
     * This method is equivalent to [.offerLast].
     *
     * @param e the element to add
     * @return `true` (as specified by [Queue.offer])
     * @throws NullPointerException if the specified element is null
     */
    override fun offer(e: E): Boolean {
        return offerLast(e)
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque.
     *
     * This method differs from [poll][.poll] only in that it throws an
     * exception if this deque is empty.
     *
     *
     * This method is equivalent to [.removeFirst].
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun remove(): E {
        return removeFirst()
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * `null` if this deque is empty.
     *
     *
     * This method is equivalent to [.pollFirst].
     *
     * @return the head of the queue represented by this deque, or
     * `null` if this deque is empty
     */
    override fun poll(): E {
        return pollFirst()
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque.  This method differs from [peek][.peek] only in
     * that it throws an exception if this deque is empty.
     *
     *
     * This method is equivalent to [.getFirst].
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun element(): E {
        return getFirst()
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque, or returns `null` if this deque is empty.
     *
     *
     * This method is equivalent to [.peekFirst].
     *
     * @return the head of the queue represented by this deque, or
     * `null` if this deque is empty
     */
    override fun peek(): E? {
        return peekFirst()
    }

    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque.  In other
     * words, inserts the element at the front of this deque.
     *
     *
     * This method is equivalent to [.addFirst].
     *
     * @param e the element to push
     * @throws NullPointerException if the specified element is null
     */
    override fun push(e: E) {
        addFirst(e)
    }

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     *
     * This method is equivalent to [.removeFirst].
     *
     * @return the element at the front of this deque (which is the top
     * of the stack represented by this deque)
     * @throws NoSuchElementException {@inheritDoc}
     */
    override fun pop(): E {
        return removeFirst()
    }

    private fun checkInvariants() {
        assert(elements[tail] == null)
        assert(if (head == tail)
                   elements[head] == null
               else
                   elements[head] != null && elements[tail - 1 and elements.size - 1] != null)
        assert(elements[head - 1 and elements.size - 1] == null)
    }

    /**
     * Removes the element at the specified position in the elements array,
     * adjusting head and tail as necessary.  This can result in motion of
     * elements backwards or forwards in the array.
     *
     *
     * This method is called delete rather than remove to emphasize
     * that its semantics differ from those of [List.remove].
     *
     * @return true if elements moved backwards
     */
    internal fun delete(i: Int): Boolean {
        checkInvariants()
        val elements = this.elements
        val mask = elements.size - 1
        val h = head
        val t = tail
        val front = i - h and mask
        val back = t - i and mask

        // Invariant: head <= i < tail mod circularity
        if (front >= t - h and mask)
            throw ConcurrentModificationException()

        // Optimize for least element motion
        if (front < back) {
            if (h <= i) {
                arraycopy(elements, h, elements, h + 1, front)
            } else { // Wrap around
                arraycopy(elements, 0, elements, 1, i)
                elements[0] = elements[mask]
                arraycopy(elements, h, elements, h + 1, mask - h)
            }
            elements[h] = null
            head = h + 1 and mask
            return false
        } else {
            if (i < t) { // Copy the null tail as well
                arraycopy(elements, i + 1, elements, i, back)
                tail = t - 1
            } else { // Wrap around
                arraycopy(elements, i + 1, elements, i, mask - i)
                elements[mask] = elements[0]
                arraycopy(elements, 1, elements, 0, t)
                tail = t - 1 and mask
            }
            return true
        }
    }

    // *** Collection Methods ***

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    override val size: Int
        get() = tail - head and elements.size - 1

    /**
     * Returns `true` if this deque contains no elements.
     *
     * @return `true` if this deque contains no elements
     */
    override fun isEmpty(): Boolean {
        return head == tail
    }

    /**
     * Returns an iterator over the elements in this deque.  The elements
     * will be ordered from first (head) to last (tail).  This is the same
     * order that elements would be dequeued (via successive calls to
     * [.remove] or popped (via successive calls to [.pop]).
     *
     * @return an iterator over the elements in this deque
     */
    override fun iterator(): MutableIterator<E> {
        return DeqIterator()
    }

    override fun descendingIterator(): MutableIterator<E> {
        return DescendingIterator()
    }

    private inner class DeqIterator : MutableIterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private var cursor = head

        /**
         * Tail recorded at construction (also in remove), to stop
         * iterator and also to check for comodification.
         */
        private var fence = tail

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if element is deleted by a call to remove.
         */
        private var lastRet = -1

        override fun hasNext(): Boolean {
            return cursor != fence
        }

        override fun next(): E {
            if (cursor == fence)
                throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            val result = elements[cursor] as E
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (tail != fence || result == null)
                throw ConcurrentModificationException()
            lastRet = cursor
            cursor = cursor + 1 and elements.size - 1
            return result
        }

        override fun remove() {
            if (lastRet < 0)
                throw IllegalStateException()
            if (delete(lastRet)) { // if left-shifted, undo increment in next()
                cursor = cursor - 1 and elements.size - 1
                fence = tail
            }
            lastRet = -1
        }
    }

    /**
     * This class is nearly a mirror-image of DeqIterator, using tail
     * instead of head for initial cursor, and head instead of tail
     * for fence.
     */
    private inner class DescendingIterator : MutableIterator<E> {
        private var cursor = tail
        private var fence = head
        private var lastRet = -1

        override fun hasNext(): Boolean {
            return cursor != fence
        }

        override fun next(): E {
            if (cursor == fence)
                throw NoSuchElementException()
            cursor = cursor - 1 and elements.size - 1
            @Suppress("UNCHECKED_CAST")
            val result = elements[cursor] as E
            if (head != fence || result == null)
                throw ConcurrentModificationException()
            lastRet = cursor
            return result
        }

        override fun remove() {
            if (lastRet < 0)
                throw IllegalStateException()
            if (!delete(lastRet)) {
                cursor = cursor + 1 and elements.size - 1
                fence = head
            }
            lastRet = -1
        }
    }

    /**
     * Returns `true` if this deque contains the specified element.
     * More formally, returns `true` if and only if this deque contains
     * at least one element `e` such that `o.equals(e)`.
     *
     * @param o object to be checked for containment in this deque
     * @return `true` if this deque contains the specified element
     */
    override operator fun contains(element: E): Boolean {
        if (element != null) {
            val mask = elements.size - 1
            var i = head
            var x: Any? = null
            while (run { x = elements[i]; x } != null) {
                if (element == x)
                    return true
                i = i + 1 and mask
            }
        }
        return false
    }

    /**
     * Removes a single instance of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element `e` such that
     * `o.equals(e)` (if such an element exists).
     * Returns `true` if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     *
     * This method is equivalent to [.removeFirstOccurrence].
     *
     * @param element element to be removed from this deque, if present
     * @return `true` if this deque contained the specified element
     */
    override fun remove(element: E): Boolean {
        return removeFirstOccurrence(element)
    }

    /**
     * Removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     */
    override fun clear() {
        val h = head
        val t = tail
        if (h != t) { // clear all cells
            tail = 0
            head = tail
            var i = h
            val mask = elements.size - 1
            do {
                elements[i] = null
                i = i + 1 and mask
            } while (i != t)
        }
    }

    /**
     * Returns an array containing all of the elements in this deque
     * in proper sequence (from first to last element).
     *
     *
     * The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     *
     * This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
    public override fun toArray(): Array<Any?> {
        val head = this.head
        val tail = this.tail
        val wrap = tail < head
        val end = if (wrap) tail + elements.size else tail
        val a = elements.copyOfRange(head, end)
        if (wrap)
            arraycopy(elements, 0, a, elements.size - head, tail)
        return a
    }

    /**
     * Returns an array containing all of the elements in this deque in
     * proper sequence (from first to last element); the runtime type of the
     * returned array is that of the specified array.  If the deque fits in
     * the specified array, it is returned therein.  Otherwise, a new array
     * is allocated with the runtime type of the specified array and the
     * size of this deque.
     *
     *
     * If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * `null`.
     *
     *
     * Like the [.toArray] method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     *
     * Suppose `x` is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of `String`:
     *
     * `String[] y = x.toArray(new String[0]);`
     *
     * Note that `toArray(new Object[0])` is identical in function to
     * `toArray()`.
     *
     * @param array the array into which the elements of the deque are to
     * be stored, if it is big enough; otherwise, a new array of the
     * same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     * is not a supertype of the runtime type of every element in
     * this deque
     * @throws NullPointerException if the specified array is null
     */
    @Suppress("UNCHECKED_CAST")
    public override fun <T> toArray(array: Array<T>): Array<T> {
        @Suppress("UnsafeCastFromDynamic")
        var a: Array<Any?> = array.asDynamic()
        val head = this.head
        val tail = this.tail
        val wrap = tail < head
        val size = tail - head + if (wrap) elements.size else 0
        val firstLeg = size - if (wrap) tail else 0
        val len = a.size
        if (size > len) {
            a = elements.copyOfRange(head, head + size)
        } else {
            arraycopy(elements, head, a, 0, firstLeg)
            if (size < len)
                a[size] = null
        }
        if (wrap)
            arraycopy(elements, 0, a, firstLeg, tail)
        return a as Array<T>
    }

    // *** Object methods ***

    companion object {

        /**
         * The minimum capacity that we'll use for a newly created deque.
         * Must be a power of 2.
         */
        private const val MIN_INITIAL_CAPACITY = 8
    }

}
