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

/**
 *
 */
package net.devrieze.util;

import java.util.Iterator;
import java.util.Map;


public class ValueIterator<T> implements Iterator<T> {

  private final Iterator<? extends Map.Entry<?, T>> mBackingIterator;

  public ValueIterator(final Iterator<? extends Map.Entry<?, T>> pBackingIterator) {
    mBackingIterator = pBackingIterator;
  }

  @Override
  public boolean hasNext() {
    return mBackingIterator.hasNext();
  }

  @Override
  public T next() {
    return mBackingIterator.next().getValue();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Read only collection");
  }

}