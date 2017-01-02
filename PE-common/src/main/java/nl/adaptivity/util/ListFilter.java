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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ListFilter<T> extends AbstractList<T> {

  @NotNull private final List<T> mSource;
  private final Class<T> mClass;
  private final boolean mLax;

  public ListFilter(final Class<T> clazz, final boolean lax) {
    mSource = new ArrayList<>();
    mClass = clazz;
    mLax = lax;
  }

  @Override
  public T get(final int index) {
    return mSource.get(index);
  }

  @Override
  public int size() {
    return mSource.size();
  }

  @Override
  public void add(final int index, final Object element) {
    if (mClass.isInstance(element)) {
      mSource.add(index, mClass.cast(element));
    } else if(! mLax) {
      mClass.cast(element);
    }
  }

  @Override
  public boolean add(final Object elem) {
    if (mClass.isInstance(elem)) {
      return super.add(mClass.cast(elem));
    } else {
      return false;
    }
  }

  /**
   * Add all the objects in the collection to this one as they match the type
   * @param c The source collection
   * @return <code>true</code> if the souce list was changed, <code>false</code> if not.
   */
  @SuppressWarnings("UnusedReturnValue")
  public boolean addAllFiltered(@NotNull final Collection<?> c) {
    boolean result = false;

    if (mSource instanceof ArrayList) { ((ArrayList) mSource).ensureCapacity(mSource.size()+c.size()); }

    for(final Object elem: c) { result=add(elem) || result; }
    return result;
  }

  @Override
  public T set(final int index, final T element) {
    if (mClass.isInstance(element)) {
      return mSource.set(index, element);
    } else if(! mLax) {
      mClass.cast(element);
    }
    return mSource.get(index);
  }

}
