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

import java.util.*;


public class Iterators {


  
  private static class AutoCloseIterable<T extends Iterable<? extends V> & AutoCloseable, V> implements Iterable<V>, Iterator<V>{

    private final T mParent;
    private Iterator<? extends V> mIterator;
    
    public AutoCloseIterable(T pIterable) {
      mParent = pIterable;
    }

    @Override
    public Iterator<V> iterator() {
      mIterator = mParent.iterator();
      if (!mIterator.hasNext()) {
        mIterator = null;
        try {
          mParent.close();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
      return this;
    }

    @Override
    public boolean hasNext() {
      return mIterator !=null && mIterator.hasNext();
    }

    @Override
    public V next() {
      try {
        V n = mIterator.next();
        if (n==null) {
          mIterator = null;
          mParent.close();
        }
        return n;
      } catch (Exception e) {
        try {
          mParent.close();
        } catch (Exception ex) {
          e.addSuppressed(ex);
        }
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      
    }

  }

  private static class EnumIterator<T> implements Iterator<T> {

    private final Enumeration<T> mEnumeration;

    public EnumIterator(final Enumeration<T> pEnumeration) {
      mEnumeration = pEnumeration;
    }

    @Override
    public boolean hasNext() {
      return mEnumeration.hasMoreElements();
    }

    @Override
    public T next() {
      return mEnumeration.nextElement();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Enumerations don't support deletion");
    }

  }

  private static class MergedIterable<T> implements Iterable<T> {

    private class MergedIterator implements Iterator<T> {

      int mIndex = 0;

      Iterator<? extends T> mIterator = mIterables[0].iterator();

      @Override
      public boolean hasNext() {
        if (mIterator.hasNext()) {
          return true;
        }
        while ((mIterator != null) && (!mIterator.hasNext())) {
          ++mIndex;
          if (mIndex < mIterables.length) {
            mIterator = mIterables[mIndex].iterator();
          } else {
            mIterator = null;
          }
        }
        return (mIterator != null);
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException("Reading past iterator end");
        }
        return mIterator.next();
      }

      @Override
      public void remove() {
        mIterator.remove();
      }

    }

    private final Iterable<? extends T>[] mIterables;

    public MergedIterable(final Iterable<? extends T>[] pIterables) {
      mIterables = pIterables;
    }

    @Override
    public Iterator<T> iterator() {
      return new MergedIterator();
    }

  }

  private Iterators() {}

  @SafeVarargs
  public static <T> Iterable<T> merge(final Iterable<? extends T>... pIterables) {
    if (pIterables.length == 0) {
      return Collections.emptyList();
    } else if (pIterables.length == 1) {
      @SuppressWarnings("unchecked")
      final Iterable<T> result = (Iterable<T>) pIterables[0];
      return result;
    }
    return new MergedIterable<>(pIterables);
  }

  public static <T> Iterable<T> toIterable(final Enumeration<T> e) {
    return new Iterable<T>() {

      @Override
      public Iterator<T> iterator() {
        return new EnumIterator<>(e);
      }


    };
  }

  public static <T> List<T> toList(final Iterator<T> pIterator) {
    if (!pIterator.hasNext()) {
      return Collections.<T> emptyList();
    }
    final T value = pIterator.next();
    if (!pIterator.hasNext()) {
      return Collections.singletonList(value);
    }
    final List<T> result = new ArrayList<>();
    result.add(value);
    do {
      result.add(pIterator.next());
    } while (pIterator.hasNext());
    return result;
  }

  public static <T> List<T> toList(final Enumeration<T> pEnumeration) {
    return toList(new EnumIterator<>(pEnumeration));
  }

  public static <T> List<T> toList(final Iterable<T> pIterable) {
    return toList(pIterable.iterator());
  }
  
  public static <T extends Iterable<V> & AutoCloseable, V> Iterable<V> autoClose(T iterable) {
    return new AutoCloseIterable<>(iterable);
  }
}
