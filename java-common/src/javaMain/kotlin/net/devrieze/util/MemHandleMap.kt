/*
 * Copyright (c) 2021.
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

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.jvm.JvmOverloads
import kotlin.jvm.javaClass


/**
 *
 *
 * A map that helps with mapping items and their handles.
 *
 *
 *
 * The system will generate handles in a systematic way. A handle consists of
 * two parts: the first part (most significant 32 bits) is the generation. The
 * second part is the position in the array. The generation allows reuse of
 * storage space while still being able to have unique handles. The handles are
 * not guaranteed to have any relation between each other.
 *
 *
 *
 * While handles are not guaranteed, the generation of the handles is NOT secure
 * in the sense of being able to be predicted with reasonable certainty.
 *

 * @author Paul de Vrieze
 *
 * @param V The type of object contained in the map.
 * @param capacity The initial capacity.
 *
 * @param loadFactor The load factor to use for the map.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
actual open class MemHandleMap<V : Any>
@JvmOverloads constructor(capacity: Int = DEFAULT_CAPACITY,
                          private var loadFactor: Float = DEFAULT_LOADFACTOR,
                          val handleAssigner: (V, Handle<V>) -> V? = ::HANDLE_AWARE_ASSIGNER) : MutableHandleMap<V>, MutableIterable<V> {

    actual constructor(handleAssigner: (V, Handle<V>) -> V?) : this(
        DEFAULT_CAPACITY,
        DEFAULT_LOADFACTOR,
        handleAssigner
    )

    private var changeMagic = 0 // Counter that increases every change. This can detect concurrentmodification.

    /**
     * This array contains the actual values in the map.
     */
    @Suppress("UNCHECKED_CAST")
    private var _values: Array<V?> = arrayOfNulls<Any>(capacity) as Array<V?>

    /**
     * This array records for each value what generation it is. This is used to
     * compare the generation of the value to the generation of the handle.
     */
    private var generations: IntArray = IntArray(capacity)

    private var nextHandle = FIRST_HANDLE

    private var barrier = capacity

    /**
     * The handle at the 0th element of the list. This allows for handle numbers
     * to increase over time without storage being needed.
     */
    private var offset = 0

    private var size = 0

    val lock = ReentrantReadWriteLock()

    /**
     * Create a new map with given load factor.
     *
     * @param loadFactor The load factor to use.
     */
    constructor(loadFactor: Float) : this(DEFAULT_CAPACITY, loadFactor) {
    }

    /**
     * Completely reset the state of the map. This is mainly for testing.
     */
    fun reset() {
        lock.write {
            _values.fill(null)
            generations.fill(0)
            size = 0
            barrier = _values.size
            offset = 0
            nextHandle = FIRST_HANDLE
            ++changeMagic
        }
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#clear()
     */
    override fun clear() {
        lock.write {
            _values.fill(null)
            generations.fill(0)
            size = 0
            updateBarrier()
        }
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#iterator()
     */
    override fun iterator(): MutableIterator<V> {
        return MapIterator()
    }

    operator fun contains(element: Any): Boolean {
        if (element is Handle<*>) {
            return contains(element)
        } else {
            return lock.read {
                _values.any { it == element }
            }
        }
    }

    /* (non-Javadoc)
       * @see net.devrieze.util.HandleMap#contains(java.lang.Object)
       */
    override fun containsElement(element: V): Boolean {
        return contains(element)
    }

    override fun contains(handle: Handle<V>): Boolean {
        return lock.read {
            val index = indexFromHandle(handle.handleValue)
            if (index < 0 || index >= _values.size) {
                false
            } else _values[index] != null
        }
    }

    /**
     * Determine whether a handle might be valid. Used by the iterator.
     */
    private fun inRange(iterPos: Int): Boolean {
        if (barrier <= nextHandle) {
            return iterPos in barrier..(nextHandle - 1)
        }
        return iterPos >= barrier || iterPos < nextHandle
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#put(V)
     */
    override fun <W : V> put(value: W): Handle<W> {
        assert(
            if (value is ReadableHandleAware<*>) !value.handle.isValid else true
        ) { "Storing a value that already has a handle is invalid" }

        var index: Int // The space in the mValues array where to store the value
        var generation: Int // To allow reuse of spaces without reuse of handles
        // every reuse increases it's generation.
        val handle: Long = lock.write {
            ++changeMagic
            // If we can just add a handle to the ringbuffer.
            if (nextHandle != barrier) {
                index = nextHandle
                nextHandle++
                if (nextHandle == _values.size) {
                    if (barrier == _values.size) {
                        barrier = 0
                    }
                    nextHandle = 0
                    offset += _values.size
                }
                generation = START_GENERATION
            } else {
                // Ring buffer too full
                if (size == _values.size || size >= loadFactor * _values.size) {
                    expand()
                    return put(value)
                    // expand
                } else {
                    // Reuse a handle.
                    index = findNextFreeIndex()
                    generation = maxOf(generations[index], START_GENERATION)
                }
            }
            val h = (generation.toLong() shl 32) + handleFromIndex(index)
            val updatedValue = handleAssigner(value, if (h < 0) Handle.invalid() else Handle(h)) ?: value

            _values[index] = updatedValue
            generations[index] = generation
            size++

            h
        }
        return Handle(handle)
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#get(long)
     */
    operator fun get(handle: Long): V? {
        if (handle == -1L) return null
        // Split the handle up into generation and index.
        val generation = (handle shr 32).toInt()
        return lock.read {
            val index = indexFromHandle(handle.toInt().toLong())
            when {
                index < 0 ||
                    // If the generation doesn't map we have a wrong handle.
                    generations[index] != generation -> null

                else -> _values[index] // Just get the element out of the map.
            }
        }
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#get(net.devrieze.util.MemHandleMap.Handle)
     */
    override fun get(handle: Handle<V>): V? {
        return get(handle.handleValue)
    }


    @Deprecated("Don't use untyped handles")
    override fun set(handle: Long, value: V): V? {
        // Split the handle up into generation and index.
        val generation = (handle shr 32).toInt()
        return lock.write {
            val index = indexFromHandle(handle.toInt().toLong())
            if (index < 0) {
                throw ArrayIndexOutOfBoundsException(handle.toInt())
            }

            // If the generation doesn't map we have a wrong handle.
            if (generations[index] != generation) {
                throw ArrayIndexOutOfBoundsException("Generation mismatch ($generation)")
            }

            val updatedValue = handleAssigner(value, if (handle < 0) Handle.invalid() else Handle(handle)) ?: value

            // Just get the element out of the map.
            _values[index] = updatedValue
            updatedValue
        }
    }

    override fun set(handle: Handle<V>, value: V): V? {
        @Suppress("DEPRECATION")
        return set(handle.handleValue, value)
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#size()
     */
    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    @Suppress("OverridingDeprecatedMember")
    override fun getSize() = lock.read { size }

    fun size() = lock.read { size }

    /* (non-Javadoc)
         * @see net.devrieze.util.HandleMap#remove(net.devrieze.util.MemHandleMap.Handle)
         */
    override fun remove(handle: Handle<V>): Boolean {
        return remove(handle.handleValue)
    }

    /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#remove(long)
     */
    fun remove(handle: Long): Boolean {
        val generation = (handle shr 32).toInt()
        return lock.write {
            val index = indexFromHandle(handle.toInt().toLong())
            if (index < 0) {
                throw ArrayIndexOutOfBoundsException(handle.toInt())
            }

            if (generations[index] != generation) {
                return false
            }
            if (_values[index] != null) {
                ++changeMagic
                _values[index] = null
                generations[index]++ // Update the generation for safety checking
                size--
                updateBarrier()
                true
            } else {
                false
            }
        }
    }

    /**
     * Try to make the barrier move down to allow the map to stay small. This
     * method itself isn't synchronized as the calling method should be.
     */
    private fun updateBarrier() {
        if (size == 0) {
            offset += nextHandle
            barrier = _values.size
            nextHandle = 0
        } else {
            if (barrier == _values.size) {
                barrier = 0
            }
            while (_values[barrier] == null) {
                barrier++
                if (barrier == _values.size) {
                    barrier = 0
                }
            }
        }
    }

    /**
     * Get an index from the given handle. This method is not synchronized,
     * callers should be.

     * @param pHandle The handle to use.
     *
     * @return the index into the values array.
     *
     * @throws ArrayIndexOutOfBoundsException when the handle is not a valid
     * *           position.
     */
    private fun indexFromHandle(pHandle: Long): Int {
        val handle = pHandle.toInt()
        var result = handle - offset
        if (result < 0) {
            result += _values.size
            if (result < barrier) {
                return -1
            }
        } else if (result >= nextHandle) {
            return -1
        }
        if (result >= _values.size) {
            return -1
        }
        return result
    }

    private fun handleFromIndex(pIndex: Int): Int {
        if (nextHandle > barrier) {
            return pIndex + offset
        }

        if (pIndex < barrier) {
            // must be at same offset as mNextHandle
            return pIndex + offset
        } else {
            return pIndex + offset - _values.size
        }
    }

    private fun findNextFreeIndex(): Int {
        var i = barrier
        while (_values[i] != null) {
            i++
            if (i == _values.size) {
                i = 0
            }
        }
        return i
    }

    private fun expand() {
        if (barrier == _values.size) {
            System.err.println("Unexpected code visit")
            barrier = 0
        }

        if (barrier != nextHandle) {
            System.err.println("Expanding while not full")
            return
        }

        val newLen = _values.size * 2

        @Suppress("UNCHECKED_CAST")
        val newValues = arrayOfNulls<Any>(newLen) as Array<V?>

        val newGenerations = IntArray(newLen)


        System.arraycopy(_values, barrier, newValues, 0, _values.size - barrier)
        System.arraycopy(generations, barrier, newGenerations, 0, generations.size - barrier)
        if (barrier > 0) {
            System.arraycopy(_values, 0, newValues, _values.size - barrier, barrier)
            System.arraycopy(generations, 0, newGenerations, generations.size - barrier, barrier)
        }

        offset = handleFromIndex(barrier)
        nextHandle = _values.size
        _values = newValues
        generations = newGenerations
        barrier = 0
    }

    @Deprecated("Don't use, this may be expensive", level = DeprecationLevel.ERROR)
    @Suppress("OverridingDeprecatedMember")
    override fun isEmpty(): Boolean = lock.read { size == 0 }

    @Suppress("UNCHECKED_CAST")
    fun toArray(): Array<Any> {
        lock.read {
            val result = arrayOfNulls<Any>(size)
            return writeToArray(result) as Array<Any>
        }
    }

    fun <U> toArray(pA: Array<U?>): Array<U?> {
        lock.read {
            val size = size
            var array = pA
            if (pA.size < size) {
                @Suppress("UNCHECKED_CAST")
                array = java.lang.reflect.Array.newInstance(array.javaClass.componentType, size) as Array<U?>
            }
            writeToArray(array)
            return array
        }
    }

    private fun <T> writeToArray(result: Array<T?>): Array<T?> {
        var i = 0
        for (elem in _values) {
            @Suppress("UNCHECKED_CAST")
            result[i] = elem as T
            ++i
        }
        if (result.size > i) {
            result[i] = null // Mark the element afterwards as null as by {@link #toArray(T[])}
        }
        return result
    }

    fun add(element: V): Boolean {
        put(element)
        return true
    }

    fun remove(element: Any): Boolean {
        if (element is Handle<*>) {
            return remove(element.handleValue)
        }
        lock.write {
            val it = iterator()
            while (it.hasNext()) {
                if (it.next() == element) {
                    it.remove()
                    return true
                }
            }
        }
        return false
    }

    fun containsAll(elements: Collection<*>): Boolean {
        lock.read {
            return elements.all { contains(it) }
        }
    }

    fun addAll(elements: Collection<V>): Boolean {
        lock.write {
            return elements.fold(false) { r, elem -> add(elem) or r }
        }
    }

    fun removeAll(elements: Collection<*>): Boolean {
        lock.write {
            return elements.fold(false) { r, elem -> remove(elem as Any) or r }
        }
    }

    fun retainAll(pC: Collection<*>): Boolean {
        var result = false
        lock.write {
            val it = iterator()
            while (it.hasNext()) {
                val elem = it.next()
                if (!pC.contains(elem)) {
                    it.remove()
                    result = result or true
                }
            }
        }
        return result
    }

    override fun toString(): String {
        return lock.read {
            val it = MapIterator()
            generateSequence {
                if (it.hasNext()) {
                    val value = it.next()
                    "${it.handle}: $value"
                } else null
            }.joinToString(prefix = "MemHandleMap [", postfix = "]")
        }
    }

    companion object {

        private const val DEFAULT_LOADFACTOR = 0.9f

        private const val DEFAULT_CAPACITY = 1024
        const val FIRST_HANDLE = 0

        private const val START_GENERATION = 0

    }

    internal class MapCollection<T : Any>(private val handleMap: MemHandleMap<T>) : MutableCollection<T> {

        override val size: Int
            get() {
                return handleMap.size
            }

        override fun isEmpty(): Boolean {
            return handleMap.size == 0
        }

        override operator fun contains(element: T): Boolean {
            return handleMap.containsElement(element)
        }

        override fun iterator(): MutableIterator<T> {
            return handleMap.iterator()
        }

        fun toArray(): Array<Any> {
            handleMap.lock.read {
                val result = arrayOfNulls<Any?>(size)
                @Suppress("UNCHECKED_CAST")
                return writeToArray(result) as Array<Any>
            }
        }

        private fun writeToArray(result: Array<Any?>): Array<Any?> {
            var i = 0
            handleMap.lock.read {
                for (elem in handleMap) {
                    result[i] = elem
                    ++i
                }
            }
            if (result.size > i) {
                result[i] = null // Mark the element afterwards as null as by {@link #toArray(T[])}
            }
            return result
        }

        override fun add(element: T): Boolean {
            handleMap.put(element)
            return true
        }

        override fun remove(element: T): Boolean {
            if (element is ReadableHandleAware<*>) {
                return handleMap.remove(element.handle)
            }
            return handleMap.remove(element)
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return handleMap.containsAll(elements)
        }

        override fun addAll(elements: Collection<T>): Boolean {
            return handleMap.addAll(elements)
        }

        override fun removeAll(elements: Collection<T>): Boolean {
            return handleMap.removeAll(elements)
        }

        override fun retainAll(elements: Collection<T>): Boolean {
            return handleMap.retainAll(elements)
        }

        override fun clear() {
            handleMap.clear()
        }

    }

    private inner class MapIterator : MutableIterator<V> {

        private var iterPos: Int = -1

        private val iteratorMagic: Int

        private var oldPos = -1

        private var atStart = true

        init {
            iteratorMagic = lock.read {
                iterPos = if (barrier >= _values.size) 0 else barrier
                changeMagic
            }
        }

        override fun hasNext(): Boolean {
            lock.read {
                if (iteratorMagic != changeMagic) {
                    throw ConcurrentModificationException("Trying to iterate over a changed map.")
                }
                if (iterPos == nextHandle && barrier == nextHandle) {
                    return atStart
                }
                if (barrier < nextHandle) {
                    return iterPos < nextHandle
                }
                return iterPos >= barrier || iterPos < nextHandle
            }
        }


        override fun next(): V {
            oldPos = iterPos
            atStart = false

            lock.read {
                if (iteratorMagic != changeMagic) {
                    throw ConcurrentModificationException("Trying to iterate over a changed map.")
                }

                do {
                    iterPos++
                    if (iterPos >= _values.size) {
                        iterPos = 0
                    }
                } while (_values[iterPos] == null && inRange(iterPos))
                return _values[oldPos]!!
            }
        }

        override fun remove() {
            lock.write {
                if (iteratorMagic != changeMagic) throw ConcurrentModificationException("The underlying collection changed before remove")

                if (_values[oldPos] == null) {
                    throw IllegalStateException("Calling remove twice can not work")
                }
                _values[oldPos] = null
                generations[oldPos]++
                updateBarrier()
                if (iterPos == barrier) {
                    atStart = true
                }
            }
        }

        val handle: Long
            get() = lock.read { handleFromIndex(oldPos).toLong() }

    }

}
