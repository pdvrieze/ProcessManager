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

package net.devrieze.util;

import org.jetbrains.annotations.Contract;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Collection class that allows accessing the entries in map.entry collection as a collection of values.
 * @author Paul de Vrieze
 *
 * @param <T>
 */
public class ValueCollection<T> extends AbstractCollection<T> {

  private final Collection<? extends Map.Entry<?, T>> mBackingCollection;

  public ValueCollection(final Collection<? extends Map.Entry<?, T>> pBackingCollection) {
    mBackingCollection = pBackingCollection;
  }

  @Contract(pure = true)
  @Override
  public boolean contains(final Object o) {
    if (o == null) {
      for (final Map.Entry<?, T> entry : mBackingCollection) {
        if (entry.getValue() == null) {
          return true;
        }
      }
    } else {
      for (final Map.Entry<?, T> entry : mBackingCollection) {
        if (o.equals(entry.getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  @Contract(pure = true)
  @Override
  public boolean containsAll(final Collection<?> pC) {
    for (final Object o : pC) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Contract(pure = true)
  @Override
  public boolean equals(final Object pO) {
    if (!(pO instanceof Iterable)) {
      return false;
    }
    final Iterator<T> myIterator = iterator();
    final Iterator<?> otherIterator = ((Iterable<?>) pO).iterator();
    while (myIterator.hasNext()) {
      if (!(otherIterator.hasNext() && myIterator.next().equals(otherIterator.next()))) {
        return false;
      }
    }
    return !otherIterator.hasNext();
  }

  @Contract(pure = true)
  @Override
  public int hashCode() {
    int result = 0;
    for (final T value : this) {
      result = (result * 31) + value.hashCode();
    }
    return result;
  }

  @Contract(pure = true)
  @Override
  public boolean isEmpty() {
    return mBackingCollection.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return new ValueIterator<>(mBackingCollection.iterator());
  }

  @Contract("_ -> fail")
  @Override
  public boolean remove(final Object o) {
    throw new UnsupportedOperationException();
  }

  @Contract("_ -> fail")
  @Override
  public boolean removeAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Contract("_ -> fail")
  @Override
  public boolean retainAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Contract(pure = true)
  @Override
  public int size() {
    return mBackingCollection.size();
  }


}