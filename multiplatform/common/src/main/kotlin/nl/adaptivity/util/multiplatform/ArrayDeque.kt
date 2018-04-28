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

expect interface Queue<E> : MutableCollection<E> {
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
    override fun add(element: E): Boolean

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
    fun offer(e: E): Boolean

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from [poll][.poll] only in that it throws an exception if this
     * queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    fun remove(): E

    /**
     * Retrieves and removes the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    fun poll(): E?

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from [peek][.peek] only in that it throws an exception
     * if this queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    fun element(): E

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    fun peek(): E?
}

expect interface Deque<E>: Queue<E> {
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
    fun addFirst(e: E)

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
    fun addLast(e: E)

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
    fun offerFirst(e: E): Boolean

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
    fun offerLast(e: E): Boolean

    /**
     * Retrieves and removes the first element of this deque.  This method
     * differs from [pollFirst][.pollFirst] only in that it throws an
     * exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    fun removeFirst(): E

    /**
     * Retrieves and removes the last element of this deque.  This method
     * differs from [pollLast][.pollLast] only in that it throws an
     * exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    fun removeLast(): E

    /**
     * Retrieves and removes the first element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the head of this deque, or `null` if this deque is empty
     */
    fun pollFirst(): E

    /**
     * Retrieves and removes the last element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the tail of this deque, or `null` if this deque is empty
     */
    fun pollLast(): E

    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * This method differs from [peekFirst][.peekFirst] only in that it
     * throws an exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    fun getFirst(): E

    /**
     * Retrieves, but does not remove, the last element of this deque.
     * This method differs from [peekLast][.peekLast] only in that it
     * throws an exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    fun getLast(): E

    /**
     * Retrieves, but does not remove, the first element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the head of this deque, or `null` if this deque is empty
     */
    fun peekFirst(): E

    /**
     * Retrieves, but does not remove, the last element of this deque,
     * or returns `null` if this deque is empty.
     *
     * @return the tail of this deque, or `null` if this deque is empty
     */
    fun peekLast(): E

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
    fun removeFirstOccurrence(o: Any?): Boolean

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
    fun removeLastOccurrence(o: Any?): Boolean

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
    override fun add(element: E): Boolean

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
    override fun offer(e: E): Boolean

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
    override fun remove(): E

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
    override fun poll(): E

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
    override fun element(): E

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
    override fun peek(): E?


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
    fun push(e: E)

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
    fun pop(): E


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
    override fun remove(element: E): Boolean

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
    override operator fun contains(element: E): Boolean

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    override val size: Int

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    override fun iterator(): MutableIterator<E>

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * @return an iterator over the elements in this deque in reverse
     * sequence
     */
    fun descendingIterator(): Iterator<E>


}

expect class ArrayDeque<E>: Deque<E> {
    constructor()
    constructor(numElements:Int)
    constructor(c: Collection<E>)
}