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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


public interface HandleMap<V> extends Set<V> {

  public static final long NULL_HANDLE=0;

  public static interface HandleAware<T> extends Handle<T> {

    public void setHandle(long pHandle);
  }

  /**
   * @param <T> Type parameter that should help with compile time handle differentiation
   */
  public static interface Handle<T> {

    public long getHandle();
  }

  public static interface ComparableHandle<T> extends Handle<T>, Comparable<ComparableHandle<T>> {
    // no body needed
  }

  /**
   * Clear out all elements from the map, but unlike creating a new map, don't
   * reset the other metadata determining the next handle.
   */
  @Override
  public void clear();

  @Override
  public Iterator<V> iterator();

  /**
   * Determine whether the given object is contained in the map. If the object
   * implements {@link HandleAware} a shortcut is applied instead of looping
   * through all values.
   *
   * @param pObject The object to check.
   * @return <code>true</code> if it does.
   */
  @Override
  public boolean contains(Object pObject);

  public boolean containsHandle(HandleMap.Handle<? extends V> pHandle);

  /**
   * Determine whether the given handle is contained in the map.
   *
   * @param pHandle The handle to check.
   * @return <code>true</code> if it does.
   */
  public boolean contains(long pHandle);

  /**
   * Put a new walue into the map. This is thread safe.
   *
   * @param pValue The value to put into the map.
   * @return The handle for the value.
   */
  public Handle<V> put(V pValue);

  /**
   * Get the element with the given handle.
   *
   * @param pHandle The handle
   * @return The element corresponding to the given handle.
   */
  public V get(long pHandle);

  public V get(HandleMap.Handle<? extends V> pHandle);

  public V set(long pHandle, V pValue);

  public V set(HandleMap.Handle<? extends V> pHandle, V pValue);

  @Override
  public int size();

  public boolean remove(HandleMap.Handle<? extends V> pObject);

  public boolean remove(long pHandle);

  /**
   * Return a collection view over this map. Note that this collection is just a
   * view, and changes on the collection will change the underlying map.
   *
   * @return The collection view.
   */
  public Collection<V> toCollection();

}