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

package net.devrieze.util

import java.util.*


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
 * @param <V> The type of object contained in the map.
 * @param pCapacity The initial capacity.
 *
 * @param pLoadFactor The load factor to use for the map.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
open class MemHandleMap<V:Any>
@JvmOverloads constructor(pCapacity: Int = MemHandleMap._DEFAULT_CAPACITY,
                          pLoadFactor: Float = MemHandleMap._DEFAULT_LOADFACTOR,
                          val handleAssigner: (V, Handle<V>)->V? = ::HANDLE_AWARE_ASSIGNER) : MutableHandleMap<V>, MutableIterable<V> {

  internal class MapCollection<T:Any>(private val handleMap: MemHandleMap<T>) : MutableCollection<T> {

    override val size:Int get() {
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
      synchronized(handleMap) {
        val result = arrayOfNulls<Any?>(size)
        @Suppress("UNCHECKED_CAST")
        return writeToArray(result) as Array<Any>
      }
    }

/*
    fun <U> toArray(pA: Array<U?>): Array<U?> {
      var array: Array<U?>
      synchronized(mHandleMap) {
        val size = size
        array = pA
        if (pA.size < size) {
          array = java.lang.reflect.Array.newInstance(array.javaClass.componentType, size) as Array<U>
        }
        writeToArray(array)
      }
      return array
    }
*/

    private fun writeToArray(result: Array<Any?>): Array<Any?> {
      var i = 0
      synchronized(handleMap) {
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
        return handleMap.remove(element.getHandle())
      }
      synchronized(handleMap) {
        val it = handleMap.iterator()
        while (it.hasNext()) {
          if (it.next() === element) {
            it.remove()
            return true
          }
        }
      }
      return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
      synchronized(handleMap) {
        return elements.all { contains(it) }
      }
    }

    override fun addAll(elements: Collection<T>): Boolean {
      synchronized(handleMap) {
        return elements.fold(false) { r, elem -> add(elem) or r }
      }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
      synchronized(handleMap) {
        return elements.fold(false) { r, elem -> remove(elem) or r }
      }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
      var result = false
      synchronized(handleMap) {
        val it = handleMap.iterator()
        while (it.hasNext()) {
          val elem = it.next()
          if (!elements.contains(elem)) {
            it.remove()
            result = result or true
          }
        }
      }
      return result
    }

    override fun clear() {
      handleMap.clear()
    }

  }

  private inner class MapIterator : MutableIterator<V> {

    private var mIterPos: Int = if (barrier >= values.size) 0 else barrier

    private val mIteratorMagic: Int = changeMagic

    private var mOldPos = -1

    private var mAtStart = true

    override fun hasNext(): Boolean {
      synchronized(this@MemHandleMap) {
        if (mIteratorMagic != changeMagic) {
          throw ConcurrentModificationException("Trying to iterate over a changed map.")
        }
        if (mIterPos == nextHandle && barrier == nextHandle) {
          return mAtStart
        }
        if (barrier < nextHandle) {
          return mIterPos < nextHandle
        }
        return mIterPos >= barrier || mIterPos < nextHandle
      }
    }


    override fun next(): V {
      mOldPos = mIterPos
      mAtStart = false

      synchronized(this@MemHandleMap) {
        if (mIteratorMagic != changeMagic) {
          throw ConcurrentModificationException("Trying to iterate over a changed map.")
        }

        do {
          mIterPos++
          if (mIterPos >= values.size) {
            mIterPos = 0
          }
        } while (values[mIterPos] == null && inRange(mIterPos))
        return values[mOldPos]!!
      }
    }

    override fun remove() {
      synchronized(this@MemHandleMap) {
        if (values[mOldPos] == null) {
          throw IllegalStateException("Calling remove twice can not work")
        }
        values[mOldPos] = null
        generations[mOldPos]++
        updateBarrier()
        if (mIterPos == barrier) {
          mAtStart = true
        }
      }
    }

    val handle: Long
      get() = handleFromIndex(mOldPos).toLong()

  }

  private var changeMagic = 0 // Counter that increases every change. This can detect concurrentmodification.

  /**
   * This array contains the actual values in the map.
   */
  @Suppress("UNCHECKED_CAST")
  private var values: Array<V?> = arrayOfNulls<Any>(pCapacity) as Array<V?>

  /**
   * This array records for each value what generation it is. This is used to
   * compare the generation of the value to the generation of the handle.
   */
  private var generations: IntArray = IntArray(pCapacity)

  private var nextHandle = FIRST_HANDLE

  private var barrier = pCapacity

  /**
   * The handle at the 0th element of the list. This allows for handle numbers
   * to increase over time without storage being needed.
   */
  private var offset = 0

  private var size = 0

  private var mLoadFactor = pLoadFactor

  /**
   * Create a new map with given load factor.

   * @param pLoadFactor The load factor to use.
   */
  constructor(pLoadFactor: Float) : this(_DEFAULT_CAPACITY, pLoadFactor) {
  }

  init {
    barrier = pCapacity
    mLoadFactor = pLoadFactor
  }

  /**
   * Completely reset the state of the map. This is mainly for testing.
   */
  @Synchronized fun reset() {
    Arrays.fill(values, null)
    Arrays.fill(generations, 0)
    size = 0
    barrier = values.size
    offset = 0
    nextHandle = FIRST_HANDLE
    ++changeMagic
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#clear()
   */
  @Synchronized override fun clear() {
    Arrays.fill(values, null)
    Arrays.fill(generations, 0)
    size = 0
    updateBarrier()
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#iterator()
   */
  override fun iterator(): MutableIterator<V> {
    return MapIterator()
  }

  @Deprecated("Don't use untyped handles")
  override fun contains(handle: Long): Boolean {
    synchronized(this) {
      val index = indexFromHandle(handle)
      if (index < 0 || index >= values.size) {
        return false
      }
      return values[index] != null
    }
  }


  operator fun contains(element: Any): Boolean {
    if (element is Handle<*>) {
      val candidateHandle = element.handleValue
      @Suppress("DEPRECATION")
      return contains(candidateHandle)
    } else {
      synchronized(this) {
        for (candidate in values) {
          if (candidate === element) {
            return true
          }
        }
      }
      return false
    }
  }

  /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#contains(java.lang.Object)
     */
  override fun containsElement(element: V): Boolean {
    return contains(element)
  }

  override fun contains(handle: Handle<V>): Boolean {
    @Suppress("DEPRECATION")
    return contains(handle.handleValue)
  }

  /**
   * Determine whether a handle might be valid. Used by the iterator.
   */
  private fun inRange(pIterPos: Int): Boolean {
    if (barrier <= nextHandle) {
      return pIterPos >= barrier && pIterPos < nextHandle
    }
    return pIterPos >= barrier || pIterPos < nextHandle
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#put(V)
   */
  override fun <W : V> put(value: W): ComparableHandle<W> {
    assert(if (value is ReadableHandleAware<*>) !value.getHandle().valid else true) { "Storing a value that already has a handle is invalid" }

    var index: Int // The space in the mValues array where to store the value
    var generation: Int // To allow reuse of spaces without reuse of handles
    // every reuse increases it's generation.
    val handle = synchronized(this) {
      ++changeMagic
      // If we can just add a handle to the ringbuffer.
      if (nextHandle != barrier) {
        index = nextHandle
        nextHandle++
        if (nextHandle == values.size) {
          if (barrier == values.size) {
            barrier = 0
          }
          nextHandle = 0
          offset += values.size
        }
        generation = mStartGeneration
      } else {
        // Ring buffer too full
        if (size == values.size || size >= mLoadFactor * values.size) {
          expand()
          return put(value)
          // expand
        } else {
          // Reuse a handle.
          index = findNextFreeIndex()
          generation = Math.max(generations[index], mStartGeneration)
        }
      }
      val h = (generation.toLong() shl 32) + handleFromIndex(index)
      val updatedValue = handleAssigner(value, Handles.handle(h)) ?: value

      values[index] = updatedValue
      generations[index] = generation
      size++

      h
    }
    return Handles.handle<W>(handle)
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#get(long)
   */
  operator fun get(pHandle: Long): V? {
    if (pHandle==-1L) return null
    // Split the handle up into generation and index.
    val generation = (pHandle shr 32).toInt()
    synchronized(this) {
      val index = indexFromHandle(pHandle.toInt().toLong())
      if (index < 0) {
        return null
//        throw ArrayIndexOutOfBoundsException(pHandle.toInt())
      }

      // If the generation doesn't map we have a wrong handle.
      if (generations[index] != generation) {
        return null
//        throw ArrayIndexOutOfBoundsException("Generation mismatch" + generation)
      }

      // Just get the element out of the map.
      return values[index]
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
    synchronized(this) {
      val index = indexFromHandle(handle.toInt().toLong())
      if (index < 0) {
        throw ArrayIndexOutOfBoundsException(handle.toInt())
      }

      // If the generation doesn't map we have a wrong handle.
      if (generations[index] != generation) {
        throw ArrayIndexOutOfBoundsException("Generation mismatch" + generation)
      }

      val updatedValue = handleAssigner(value, Handles.handle(handle)) ?: value

      // Just get the element out of the map.
      values[index] = updatedValue
      return values[index]
    }
  }

  override fun set(handle: Handle<V>, value: V): V? {
    @Suppress("DEPRECATION")
    return set(handle.handleValue, value)
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#size()
   */
  @Suppress("OverridingDeprecatedMember")
  override fun getSize() = size

  fun size() = size

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
    synchronized(this) {
      val index = indexFromHandle(handle.toInt().toLong())
      if (index < 0) {
        throw ArrayIndexOutOfBoundsException(handle.toInt())
      }

      if (generations[index] != generation) {
        return false
      }
      if (values[index] != null) {
        ++changeMagic
        values[index] = null
        generations[index]++ // Update the generation for safety checking
        size--
        updateBarrier()
        return true
      } else {
        return false
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
      barrier = values.size
      nextHandle = 0
    } else {
      if (barrier == values.size) {
        barrier = 0
      }
      while (values[barrier] == null) {
        barrier++
        if (barrier == values.size) {
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
      result = result + values.size
      if (result < barrier) {
        return -1
      }
    } else if (result >= nextHandle) {
      return -1
    }
    if (result >= values.size) {
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
      return pIndex + offset - values.size
    }
  }

  private fun findNextFreeIndex(): Int {
    var i = barrier
    while (values[i] != null) {
      i++
      if (i == values.size) {
        i = 0
      }
    }
    return i
  }

  private fun expand() {
    if (barrier == values.size) {
      System.err.println("Unexpected code visit")
      barrier = 0
    }

    if (barrier != nextHandle) {
      System.err.println("Expanding while not full")
      return
    }

    val newLen = values.size * 2

    @Suppress("UNCHECKED_CAST")
    val newValues = arrayOfNulls<Any>(newLen) as Array<V?>

    val newGenerations = IntArray(newLen)


    System.arraycopy(values, barrier, newValues, 0, values.size - barrier)
    System.arraycopy(generations, barrier, newGenerations, 0, generations.size - barrier)
    if (barrier > 0) {
      System.arraycopy(values, 0, newValues, values.size - barrier, barrier)
      System.arraycopy(generations, 0, newGenerations, generations.size - barrier, barrier)
    }

    offset = handleFromIndex(barrier)
    nextHandle = values.size
    values = newValues
    generations = newGenerations
    barrier = 0
  }

  @Suppress("OverridingDeprecatedMember")
  override fun isEmpty(): Boolean {
    return size !=0
  }

  @Suppress("UNCHECKED_CAST")
  fun toArray(): Array<Any> {
    synchronized(this) {
      val result = arrayOfNulls<Any>(size)
      return writeToArray(result) as Array<Any>
    }
  }

  fun <U> toArray(pA: Array<U?>): Array<U?> {
    synchronized(this) {
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
    synchronized(this) {
      for (elem in values) {
        @Suppress("UNCHECKED_CAST")
        result[i] = elem as T
        ++i
      }
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
    synchronized(this) {
      val it = iterator()
      while (it.hasNext()) {
        if (it.next() === element) {
          it.remove()
          return true
        }
      }
    }
    return false
  }

  fun containsAll(elements: Collection<*>): Boolean {
    synchronized(this) {
      return elements.all { contains(it) }
    }
  }

  fun addAll(elements: Collection<V>): Boolean {
    synchronized(this) {
      return elements.fold(false) { r, elem -> add(elem) or r }
    }
  }

  fun removeAll(elements: Collection<*>): Boolean {
    synchronized(this) {
      return elements.fold(false) { r, elem -> remove(elem as Any) or r }
    }
  }

  fun retainAll(pC: Collection<*>): Boolean {
    var result = false
    synchronized(this) {
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
    val builder = StringBuilder()
    builder.append("MemHandleMap [")

    if (size > 0) {
      val it = MapIterator()
      while (it.hasNext()) {
        if (builder.length > 14) {
          builder.append(", ")
        }
        val `val` = it.next()
        builder.append(it.handle).append(": ").append(`val`)
      }

    }
    builder.append("]")
    return builder.toString()
  }

  companion object {

    private val _DEFAULT_LOADFACTOR = 0.9f

    private val _DEFAULT_CAPACITY = 1024
    val FIRST_HANDLE = 0

    private val mStartGeneration = 0

  }

}
