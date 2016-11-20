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

package net.devrieze.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;


/**
 * <p>
 * A map that helps with mapping items and their handles.
 * </p>
 * <p>
 * The system will generate handles in a systematic way. A handle consists of
 * two parts: the first part (most significant 32 bits) is the generation. The
 * second part is the position in the array. The generation allows reuse of
 * storage space while still being able to have unique handles. The handles are
 * not guaranteed to have any relation between each other.
 * </p>
 * <p>
 * While handles are not guaranteed, the generation of the handles is NOT secure
 * in the sense of being able to be predicted with reasonable certainty.
 * </p>
 *
 * @author Paul de Vrieze
 * @param <V> The type of object contained in the map.
 */
public class MemHandleMap<V> implements MutableHandleMap<V>, Collection<V> {

  static class MapCollection<T> implements Collection<T> {

    private final MemHandleMap<T> mHandleMap;

    MapCollection(MemHandleMap<T> pHandleMap) {
      mHandleMap = pHandleMap;
    }

    @Override
    public int size() {
      return mHandleMap.size();
    }

    @Override
    public boolean isEmpty() {
      return mHandleMap.size()==0;
    }

    @Override
    public boolean contains(final Object pObject) {
      return mHandleMap.containsElement((T)pObject);
    }

    @Override
    public Iterator<T> iterator() {
      return mHandleMap.iterator();
    }

    @Override
    public Object[] toArray() {
      synchronized (mHandleMap) {
        final Object[] result = new Object[size()];
        return writeToArray(result);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> U[] toArray(final U[] pA) {
      U[] array;
      synchronized (mHandleMap) {
        final int size = size();
        array = pA;
        if (pA.length < size) {
          array = (U[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
        }
        writeToArray(array);
      }
      return array;
    }

    private Object[] writeToArray(final Object[] result) {
      int i = 0;
      synchronized (mHandleMap) {
        for (final T elem : mHandleMap) {
          result[i] = elem;
          ++i;
        }
      }
      if (result.length > i) {
        result[i] = null; // Mark the element afterwards as null as by {@link #toArray(T[])}
      }
      return result;
    }

    @Override
    public boolean add(final T pElement) {
      mHandleMap.put(pElement);
      return true;
    }

    @Override
    public boolean remove(final Object pObject) {
      if (pObject instanceof HandleMap.HandleAware) {
        return mHandleMap.remove(((HandleMap.HandleAware<?>) pObject).getHandle());
      }
      synchronized (mHandleMap) {
        for (final Iterator<T> it = mHandleMap.iterator(); it.hasNext();) {
          if (it.next() == pObject) {
            it.remove();
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public boolean containsAll(final Collection<?> pC) {
      synchronized (mHandleMap) {
        for (final Object elem : pC) {
          if (!contains(elem)) {
            return false;
          }
        }
        return true;
      }
    }

    @Override
    public boolean addAll(final Collection<? extends T> pC) {
      synchronized (mHandleMap) {
        boolean result = false;
        for (final T elem : pC) {
          result |= add(elem);
        }
        return result;
      }
    }

    @Override
    public boolean removeAll(final Collection<?> pC) {
      synchronized (mHandleMap) {
        boolean result = false;
        synchronized (mHandleMap) {
          for (final Object elem : pC) {
            result |= remove(elem);
          }
        }
        return result;
      }
    }

    @Override
    public boolean retainAll(final Collection<?> pC) {
      boolean result = false;
      synchronized (mHandleMap) {
        for (final Iterator<T> it = mHandleMap.iterator(); it.hasNext();) {
          final T elem = it.next();
          if (!pC.contains(elem)) {
            it.remove();
            result |= true;
          }
        }
      }
      return result;
    }

    @Override
    public void clear() {
      mHandleMap.clear();
    }

  }

  private final class MapIterator implements Iterator<V> {

    private int mIterPos;

    private final int mIteratorMagic;

    private int mOldPos = -1;

    private boolean mAtStart = true;

    public MapIterator() {
      synchronized (MemHandleMap.this) {
        mIterPos = mBarrier >= mValues.length ? 0 : mBarrier;
        mIteratorMagic = mChangeMagic;
      }
    }

    @Override
    public boolean hasNext() {
      synchronized (MemHandleMap.this) {
        if (mIteratorMagic != mChangeMagic) {
          throw new ConcurrentModificationException("Trying to iterate over a changed map.");
        }
        if ((mIterPos == mNextHandle) && (mBarrier == mNextHandle)) {
          return mAtStart;
        }
        if (mBarrier < mNextHandle) {
          return mIterPos < mNextHandle;
        }
        return (mIterPos >= mBarrier) || (mIterPos < mNextHandle);
      }
    }


    @Override
    public V next() {
      mOldPos = mIterPos;
      mAtStart = false;

      synchronized (MemHandleMap.this) {
        if (mIteratorMagic != mChangeMagic) {
          throw new ConcurrentModificationException("Trying to iterate over a changed map.");
        }

        @SuppressWarnings("unchecked")
        final V result = (V) mValues[mOldPos];
        do {
          mIterPos++;
          if (mIterPos >= mValues.length) {
            mIterPos = 0;
          }
        } while ((mValues[mIterPos] == null) && inRange(mIterPos));
        return result;
      }
    }

    @Override
    public void remove() {
      synchronized (MemHandleMap.this) {
        if (mValues[mOldPos] == null) {
          throw new IllegalStateException("Calling remove twice can not work");
        }
        mValues[mOldPos] = null;
        mGenerations[mOldPos]++;
        updateBarrier();
        if (mIterPos == mBarrier) {
          mAtStart = true;
        }
      }
    }

    public long getHandle() {
      return handleFromIndex(mOldPos);
    }

  }

  private static final float _DEFAULT_LOADFACTOR = 0.9f;

  private static final int _DEFAULT_CAPACITY = 1024;
  public static final int FIRST_HANDLE = 0;

  private int mChangeMagic = 0; // Counter that increases every change. This can detect concurrentmodification.

  /**
   * This array contains the actual values in the map.
   */
  private V[] mValues;

  /**
   * This array records for each value what generation it is. This is used to
   * compare the generation of the value to the generation of the handle.
   */
  private int[] mGenerations;

  private final static int mStartGeneration = 0;

  private int mNextHandle = FIRST_HANDLE;

  private int mBarrier = -1;

  /**
   * The handle at the 0th element of the list. This allows for handle numbers
   * to increase over time without storage being needed.
   */
  private int mOffset = 0;

  private int mSize = 0;

  private float mLoadFactor = _DEFAULT_LOADFACTOR;

  /**
   * Create a new map.
   */
  public MemHandleMap() {
    this(_DEFAULT_CAPACITY, _DEFAULT_LOADFACTOR);
  }

  /**
   * Create a new map with given initial capacity.
   *
   * @param pCapacity The capacity of the map.
   */
  public MemHandleMap(final int pCapacity) {
    this(pCapacity, _DEFAULT_LOADFACTOR);
  }

  /**
   * Create a new map with given load factor.
   *
   * @param pLoadFactor The load factor to use.
   */
  public MemHandleMap(final float pLoadFactor) {
    this(_DEFAULT_CAPACITY, pLoadFactor);
  }

  /**
   * Create a new map.
   *
   * @param pCapacity The initial capacity.
   * @param pLoadFactor The load factor to use for the map.
   */
  public MemHandleMap(final int pCapacity, final float pLoadFactor) {
    mValues = (V[]) new Object[pCapacity];
    mGenerations = new int[pCapacity];
    mBarrier = pCapacity;
    mLoadFactor = pLoadFactor;
  }

  /**
   * Completely reset the state of the map. This is mainly for testing.
   */
  public synchronized void reset() {
    Arrays.fill(mValues, null);
    Arrays.fill(mGenerations, 0);
    mSize = 0;
    mBarrier=mValues.length;
    mOffset=0;
    mNextHandle=FIRST_HANDLE;
    ++mChangeMagic;
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#clear()
   */
  public synchronized void clear() {
    Arrays.fill(mValues, null);
    Arrays.fill(mGenerations, 0);
    mSize = 0;
    updateBarrier();
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#iterator()
   */
  @Override
  public Iterator<V> iterator() {
    return new MapIterator();
  }



  @Override
  public boolean contains(long handle) {
    synchronized (this) {
      final int index = indexFromHandle(handle);
      if ((index < 0) || (index >= mValues.length)) { return false; }
      return mValues[index]!=null;
    }
  }


  @Override
  public boolean contains(final Object o) {
    return containsElement(o);
  }

  /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#contains(java.lang.Object)
     */
  @Override
  public boolean containsElement(@NotNull final Object element) {
    if (element instanceof Handle) {
      final long candidateHandle = ((Handle<?>) element).getHandleValue();
      return contains(candidateHandle);
    } else {
      synchronized (this) {
        for (final Object candidate : mValues) {
          if (candidate == element) {
            return true;
          }
        }
      }
      return false;
    }
  }

  @Override
  public boolean contains(Handle<? extends V> handle) {
    return contains(handle.getHandleValue());
  }

  /**
   * Determine whether a handle might be valid. Used by the iterator.
   */
  private boolean inRange(final int pIterPos) {
    if (mBarrier <= mNextHandle) {
      return ((pIterPos >= mBarrier) && (pIterPos < mNextHandle));
    }
    return ((pIterPos >= mBarrier) || (pIterPos < mNextHandle));
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#put(V)
   */
  @Override
  public <W extends V> Handle<W> put(final W value) {
    if (value == null) {
      throw new NullPointerException("Handles can point to null objects");
    }
    int index; // The space in the mValues array where to store the value
    int generation; // To allow reuse of spaces without reuse of handles
    // every reuse increases it's generation.
    final long handle;
    synchronized (this) {
      ++mChangeMagic;
      // If we can just add a handle to the ringbuffer.
      if (mNextHandle != mBarrier) {
        index = mNextHandle;
        mNextHandle++;
        if (mNextHandle == mValues.length) {
          if (mBarrier == mValues.length) {
            mBarrier = 0;
          }
          mNextHandle = 0;
          mOffset += mValues.length;
        }
        generation = mStartGeneration;
      } else {
        // Ring buffer too full
        if ((mSize == mValues.length) || (mSize >= (mLoadFactor * mValues.length))) {
          expand();
          return put(value);
          // expand
        } else {
          // Reuse a handle.
          index = findNextFreeIndex();
          generation = Math.max(mGenerations[index], mStartGeneration);
        }
      }
      mValues[index] = value;
      mGenerations[index] = generation;
      mSize++;

      handle = ((long) generation << 32) + handleFromIndex(index);
    }
    if (value instanceof HandleMap.HandleAware<?>) {
      ((HandleMap.HandleAware<?>) value).setHandleValue(handle);
    }
    return Handles.handle(handle);
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#get(long)
   */
  public V get(final long pHandle) {
    // Split the handle up into generation and index.
    final int generation = (int) (pHandle >> 32);
    synchronized (this) {
      final int index = indexFromHandle((int) pHandle);
      if (index<0) { throw new ArrayIndexOutOfBoundsException((int)pHandle); }

      // If the generation doesn't map we have a wrong handle.
      if (mGenerations[index] != generation) {
        throw new ArrayIndexOutOfBoundsException("Generation mismatch" + generation);
      }

      // Just get the element out of the map.
      final @SuppressWarnings("unchecked")
      V result = (V) mValues[index];
      return result;
    }
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#get(net.devrieze.util.MemHandleMap.Handle)
   */
  @Override
  public V get(final Handle<? extends V> handle) {
    return get(handle.getHandleValue());
  }

  @Override
  public V set(long handle, V value) {
    // Split the handle up into generation and index.
    final int generation = (int) (handle >> 32);
    synchronized (this) {
      final int index = indexFromHandle((int) handle);
      if (index<0) { throw new ArrayIndexOutOfBoundsException((int) handle); }

      // If the generation doesn't map we have a wrong handle.
      if (mGenerations[index] != generation) {
        throw new ArrayIndexOutOfBoundsException("Generation mismatch" + generation);
      }

      // Just get the element out of the map.
      final @SuppressWarnings("unchecked")
      V oldValue = (V) mValues[index];
      mValues[index] = value;
      if (value instanceof HandleMap.HandleAware<?>) {
        ((HandleMap.HandleAware<?>) value).setHandleValue(handle);
      }
      return oldValue;
    }
  }

  @Override
  public V set(Handle<? extends V> handle, V value) {
    return set(handle.getHandleValue(), value);
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#size()
   */
  @Override
  public int getSize() {
    return mSize;
  }

  @Override
  public int size() {
    return getSize();
  }

  /* (non-Javadoc)
     * @see net.devrieze.util.HandleMap#remove(net.devrieze.util.MemHandleMap.Handle)
     */
  @Override
  public boolean remove(final Handle<? extends V> handle) {
    return remove(handle.getHandleValue());
  }

  /* (non-Javadoc)
   * @see net.devrieze.util.HandleMap#remove(long)
   */
  public boolean remove(final long handle) {
    final int generation = (int) (handle >> 32);
    synchronized (this) {
      final int index = indexFromHandle((int) handle);
      if (index<0) { throw new ArrayIndexOutOfBoundsException((int) handle); }

      if (mGenerations[index] != generation) {
        return false;
      }
      if (mValues[index] != null) {
        ++mChangeMagic;
        mValues[index] = null;
        mGenerations[index]++; // Update the generation for safety checking
        mSize--;
        updateBarrier();
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Try to make the barrier move down to allow the map to stay small. This
   * method itself isn't synchronized as the calling method should be.
   */
  private void updateBarrier() {
    if (mSize == 0) {
      mOffset += mNextHandle;
      mBarrier = mValues.length;
      mNextHandle = 0;
    } else {
      if (mBarrier == mValues.length) {
        mBarrier = 0;
      }
      while (mValues[mBarrier] == null) {
        mBarrier++;
        if (mBarrier == mValues.length) {
          mBarrier = 0;
        }
      }
    }
  }

  /**
   * Get an index from the given handle. This method is not synchronized,
   * callers should be.
   *
   * @param pHandle The handle to use.
   * @return the index into the values array.
   * @throws ArrayIndexOutOfBoundsException when the handle is not a valid
   *           position.
   */
  private int indexFromHandle(final long pHandle) {
    final int handle = (int) pHandle;
    int result = handle - mOffset;
    if (result < 0) {
      result = result + mValues.length;
      if (result < mBarrier) {
        return -1;
      }
    } else if (result >= mNextHandle) {
      return -1;
    }
    if (result >= mValues.length) {
      return -1;
    }
    return result;
  }

  private int handleFromIndex(final int pIndex) {
    if (mNextHandle > mBarrier) {
      return pIndex + mOffset;
    }

    if (pIndex < mBarrier) {
      // must be at same offset as mNextHandle
      return pIndex + mOffset;
    } else {
      return (pIndex + mOffset) - mValues.length;
    }
  }

  private int findNextFreeIndex() {
    int i = mBarrier;
    while (mValues[i] != null) {
      i++;
      if (i == mValues.length) {
        i = 0;
      }
    }
    return i;
  }

  private void expand() {
    if (mBarrier == mValues.length) {
      System.err.println("Unexpected code visit");
      mBarrier = 0;
    }

    if (mBarrier != mNextHandle) {
      System.err.println("Expanding while not full");
      return;
    }

    final int newLen = mValues.length * 2;
    final V[] newValues = (V[]) new Object[newLen];
    final int[] newGenerations = new int[newLen];


    System.arraycopy(mValues, mBarrier, newValues, 0, mValues.length - mBarrier);
    System.arraycopy(mGenerations, mBarrier, newGenerations, 0, mGenerations.length - mBarrier);
    if (mBarrier > 0) {
      System.arraycopy(mValues, 0, newValues, mValues.length - mBarrier, mBarrier);
      System.arraycopy(mGenerations, 0, newGenerations, mGenerations.length - mBarrier, mBarrier);
    }

    mOffset = handleFromIndex(mBarrier);
    mNextHandle = mValues.length;
    mValues = newValues;
    mGenerations = newGenerations;
    mBarrier = 0;
  }

  /**
   * Get a very simple Handle implementation.
   *
   * @param pHandle The handle
   * @return a Handle<T> object corresponding to the handle.
   * @deprecated Use {@link Handles#handle(long)} instead
   */
  @Deprecated
  public static <T> Handle<T> handle(final long pHandle) {
    return Handles.<T>handle(pHandle);
  }

  @Override
  public boolean isEmpty() {
    return size()==0;
  }

  public Object[] toArray() {
    synchronized (this) {
      final Object[] result = new Object[size()];
      return writeToArray(result);
    }
  }

  @SuppressWarnings("unchecked")
  public <U> U[] toArray(final U[] pA) {
    U[] array;
    synchronized (this) {
      final int size = size();
      array = pA;
      if (pA.length < size) {
        array = (U[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
      }
      writeToArray(array);
    }
    return array;
  }

  private Object[] writeToArray(final Object[] result) {
    int i = 0;
    synchronized (this) {
      for (final V elem : mValues) {
        result[i] = elem;
        ++i;
      }
    }
    if (result.length > i) {
      result[i] = null; // Mark the element afterwards as null as by {@link #toArray(T[])}
    }
    return result;
  }

  public boolean add(final V pElement) {
    put(pElement);
    return true;
  }

  public boolean remove(final Object pObject) {
    if (pObject instanceof Handle) {
      return remove(((Handle<?>) pObject).getHandleValue());
    }
    synchronized (this) {
      for (final Iterator<V> it = iterator(); it.hasNext();) {
        if (it.next() == pObject) {
          it.remove();
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsAll(final Collection<?> pC) {
    synchronized (this) {
      for (final Object elem : pC) {
        if (!containsElement(elem)) {
          return false;
        }
      }
      return true;
    }
  }

  public boolean addAll(final Collection<? extends V> pC) {
    synchronized (this) {
      boolean result = false;
      for (final V elem : pC) {
        result |= add(elem);
      }
      return result;
    }
  }

  public boolean removeAll(final Collection<?> pC) {
    synchronized (this) {
      boolean result = false;
      for (final Object elem : pC) {
        result |= remove(elem);
      }
      return result;
    }
  }

  public boolean retainAll(final Collection<?> pC) {
    boolean result = false;
    synchronized (this) {
      for (final Iterator<V> it = iterator(); it.hasNext();) {
        final V elem = it.next();
        if (!pC.contains(elem)) {
          it.remove();
          result |= true;
        }
      }
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MemHandleMap [");

    if (mSize>0) {
      MapIterator it = new MapIterator();
      while(it.hasNext()) {
        if (builder.length()>14) { builder.append(", "); }
        V val = it.next();
        builder.append(it.getHandle()).append(": ").append(val);
      }

    }
    builder.append("]");
    return builder.toString();
  }

}
