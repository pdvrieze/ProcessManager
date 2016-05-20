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

import net.devrieze.util.db.DBHandleMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * A {@link DBHandleMap} that uses {@link WeakReference WeakReferences} to store
 * its results. Note that the {@link WrappingIterator#remove() remove} method on
 * the iterator is not efficient if the elements do not implement Handle as it
 * will loop through the cache to find the underlying value.
 *
 * @author Paul de Vrieze
 * @param <V> The type of the elements in the map.
 */
public final class CachingHandleMap<V, T extends Transaction> extends AbstractTransactionedHandleMap<V, T> implements Closeable, AutoCloseable {


  private class WrappingIterator implements AutoCloseableIterator<V> {

    private final Iterator<V> mIterator;
    private V mLast;

    public WrappingIterator(Iterator<V> pIterator) {
      mIterator = pIterator;
    }

    @Override
    public boolean hasNext() {
      return mIterator.hasNext();
    }

    @Override
    public V next() {
      V result = mIterator.next();
      putCache(result);
      mLast = result;
      return result;
    }

    @Override
    public void remove() {
      if (mLast!=null) {
        synchronized(mCacheHandles) {
          if (mLast instanceof Handle<?>) {
            invalidateCache(((Handle<? extends V>)mLast));
          } else {
            for (int i = 0; i < mCacheHandles.length; i++) {
              V value = mCacheValues[i];
              if(value!=null && value.equals(mLast)) {
                mCacheHandles[i]=-1;
                mCacheValues[i]=null;
                break;
              }
            }
          }
        }
      }
      mIterator.remove();
    }

    @Override
    public void close() throws Exception {
      if (mIterator instanceof AutoCloseable) {
        ((AutoCloseable) mIterator).close();
      }
    }

  }

  private class WrappingIterable implements Iterable<V> {

    private final Iterable<V> delegateIterable;

    @Override
    public Iterator<V> iterator() {
      return new WrappingIterator(delegateIterable.iterator());
    }

    public WrappingIterable(final Iterable<V> iterable) {
      delegateIterable = iterable;
    }
  }

  int mCacheHead = 0;
  final long[] mCacheHandles;
  final V[] mCacheValues;
  final CopyOnWriteArraySet<Handle<? extends V>> mPendingHandles = new CopyOnWriteArraySet<>();

  final TransactionedHandleMap<V,T> mDelegate;

  public CachingHandleMap(TransactionedHandleMap<V, T> delegate, final int cacheSize) {
    mDelegate = delegate;
    mCacheHandles = new long[cacheSize];
    mCacheValues = (V[]) new Object[cacheSize];
  }

  @Override
  public T newTransaction() {
    return mDelegate.newTransaction();
  }

  @Override
  public Handle<V> put(T transaction, V value) throws SQLException {
    Handle<V> handle = mDelegate.put(transaction, value);
    putCache(handle, value);
    return handle;
  }

  private void putCache(V pValue) {
    if (pValue instanceof HandleAware) {
      putCache(((HandleAware<V>)pValue), pValue);
    }
  }

  private void putCache(Handle<? extends V> pHandle, V pValue) {
    if (pValue!=null) { // never store null
      synchronized (mCacheHandles) {
        final int pos = mCacheHead;
        final long handle = pHandle.getHandle();
        if (mCacheHandles[pos] != handle) {
          removeFromCache(handle);
        }
        mCacheHandles[pos] = handle;
        mCacheValues[pos] = pValue;
        mCacheHead = (pos + 1) % mCacheValues.length;
      }
    }
  }

  public V getFromDelegate(T pTransaction, Handle<? extends V> pHandle) throws SQLException {
    mPendingHandles.add(pHandle); // internal locking so no locking needed here
    try {
      return mDelegate.get(pTransaction, pHandle);
    } finally {
      mPendingHandles.remove(pHandle);
    }
  }

  public V getFromDelegate(Handle<? extends V> pHandle) throws SQLException {
    mPendingHandles.add(pHandle); // internal locking so no locking needed here
    try {
      return mDelegate.get(pHandle);
    } finally {
      mPendingHandles.remove(pHandle);
    }
  }

  private V getFromCache(long handle) {
    synchronized (mCacheHandles) {
      for (int i = 0; i < mCacheHandles.length; i++) {
        if (mCacheHandles[i] == handle) {
          return mCacheValues[i];
        }
      }
      return null;
    }
  }

  @Override
  public V get(long pHandle) {
    ComparableHandle<V> handle = Handles.<V>handle(pHandle);
    if (isPending(handle)) { // don't cache pending values
      return mDelegate.get(handle);
    }
    long key = pHandle;
    final V val;
    synchronized(mCacheHandles) {
      val = getFromCache(pHandle);

      if (val!=null) {
        return val;
      }
      try {
        return storeInCache(handle, getFromDelegate(handle));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  @Nullable public V get(@NotNull T transaction, Handle<? extends V> handle) throws SQLException {
    Long key = Long.valueOf(handle.getHandle());
    final V val;
    synchronized(mCacheHandles) {
      val = getFromCache(handle.getHandle());
      if (val!=null) {
        return val;
      }
      return storeInCache(Handles.handle(handle), mDelegate.get(transaction, handle));
    }
  }

  /**
   * @deprecated Don't use this, It has no transaction support and doesn't work with large element counts.
   */
  @NotNull
  @Deprecated
  @Override
  public <T> T[] toArray(final T[] a) {
    return mDelegate.toArray(a);
  }

  @Override
  public boolean contains(final T transaction, final Handle<? extends V> handle) throws SQLException {
    if (getFromCache(handle.getHandle())!=null) {
      return true;
    }
    return mDelegate.contains(transaction, handle);
  }

  @Override
  public boolean contains(final T transaction, final Object obj) throws SQLException {
    if (obj instanceof Handle) {
      return contains(transaction, (Handle<? extends V>) obj);
    }
    return mDelegate.contains(transaction, obj);
  }

  @Override
  public int size() {
    return mDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return mDelegate.isEmpty();
  }

  @Override
  public V set(T transaction, Handle<? extends V> handle, V value) throws SQLException {
    invalidateCache(handle);
    return storeInCache(handle, mDelegate.set(transaction, handle, value));
  }

  private V storeInCache(Handle<? extends V> pHandle, V pV) {
    if (! isPending(pHandle)) {
      final long handle = pHandle.getHandle();
      synchronized (mCacheHandles) {
        removeFromCache(handle); // remove whatever old value was there
        putCache(pHandle, pV);
      }
    }
    return pV;
  }

  private void removeFromCache(final long handle) {
    boolean doAssert = false;
    assert doAssert=true;
    for (int i = 0; i < mCacheHandles.length; i++) {
      if (mCacheHandles[i]==handle) {
        mCacheHandles[i]=-1;
        mCacheValues[i]=null;
        if (!doAssert) {
          return;
        }
      }
    }
  }

  private boolean isPending(final Handle<? extends V> pHandle) {
    return mPendingHandles.contains(pHandle);
  }

  @Override
  public void invalidateCache(final Handle<? extends V> pHandle) {
    long handle = pHandle.getHandle();
    removeFromCache(handle);
  }

  @Override
  public void invalidateCache() {
    synchronized (mCacheHandles) {
      Arrays.fill(mCacheHandles,-1);
      Arrays.fill(mCacheValues, null);
      mCacheHead=0;
    }
  }

  @Deprecated
  public V getUncached(T pTransaction, ComparableHandle<V> pHandle) throws SQLException {
    return storeInCache(pHandle, mDelegate.get(pTransaction, pHandle));
  }

  @Override
  public boolean remove(T transaction, Handle<? extends V> handle) throws SQLException {
    removeFromCache(handle.getHandle());
    return mDelegate.remove(transaction, handle);
  }

  @Deprecated
  @Override
  public boolean remove(final Object o) {
    if (o instanceof Handle) {
      return remove((Handle<? extends V>)o);
    }
    invalidateCache();
    return mDelegate.remove(o);
  }

  @Deprecated
  @Override
  public boolean retainAll(final Collection<?> c) {
    invalidateCache();
    return mDelegate.retainAll(c);
  }

  @Override
  public void clear(final T transaction) throws SQLException {
    invalidateCache();
    mDelegate.clear(transaction);
  }

  @Override
  public AutoCloseableIterator<V> iterator(T transaction, boolean readOnly) {
    return new WrappingIterator(mDelegate.iterator(transaction, readOnly));
  }

  @Override
  public Iterable<V> iterable(final T transaction) {
    return new WrappingIterable(mDelegate.iterable(transaction));
  }

  @Override
  public void close() throws IOException {
    invalidateCache();
    if (mDelegate instanceof Closeable) {
      ((Closeable) mDelegate).close();
    } else if (mDelegate instanceof AutoCloseable) {
      try {
        ((AutoCloseable) mDelegate).close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
