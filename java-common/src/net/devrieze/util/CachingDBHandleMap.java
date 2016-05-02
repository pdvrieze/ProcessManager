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
import net.devrieze.util.db.DBTransaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;


/**
 * A {@link DBHandleMap} that uses {@link WeakReference WeakReferences} to store
 * its results. Note that the {@link WrappingIterator#remove() remove} method on
 * the iterator is not efficient if the elements do not implement Handle as it
 * will loop through the cache to find the underlying value.
 *
 * @author Paul de Vrieze
 * @param <V> The type of the elements in the map.
 */
public class CachingDBHandleMap<V> extends DBHandleMap<V> {


  private class WrappingIterator implements AutoCloseableIterator<V> {

    private final AutoCloseableIterator<V> mIterator;
    private V mLast;

    public WrappingIterator(AutoCloseableIterator<V> pIterator) {
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
        synchronized(mCache) {
          if (mLast instanceof Handle<?>) {
            mCache.remove(Long.valueOf(((Handle<?>)mLast).getHandle()));
          } else {
            for (Entry<Long, V> entry: mCache.entrySet()) {
              if (entry.getValue()==mLast) {
                mCache.remove(entry.getKey());
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
      mIterator.close();
    }

  }

  final WeakHashMap<Long, V> mCache;

  public CachingDBHandleMap(TransactionFactory pTransactionFactory, net.devrieze.util.db.DBHandleMap.HMElementFactory<V> pElementFactory) {
    super(pTransactionFactory, pElementFactory);
    mCache = new WeakHashMap<>();
  }

  @Override
  public boolean contains(long pHandle) {
    synchronized(mCache) {
      if (mCache.containsKey(Long.valueOf(pHandle))) {
        return true;
      }
    }
    return super.contains(pHandle);
  }

  @Override
  public long put(DBTransaction pTransaction, V pValue) throws SQLException {
    long handle = super.put(pTransaction, pValue);
    putCache(handle, pValue);
    return handle;
  }

  private void putCache(V pValue) {
    if (pValue instanceof HandleAware) {
      putCache(((HandleAware<?>)pValue).getHandle(), pValue);
    }
  }

  private void putCache(long pHandle, V pValue) {
    synchronized (mCache) {
      mCache.put(Long.valueOf(pHandle), pValue);
    }
  }

  @Override
  public V get(long pHandle) {
    Long key = Long.valueOf(pHandle);
    final V val;
    synchronized(mCache) {
      val = mCache.get(key);
      if (val!=null) {
        return val;
      }
      return storeInCache(pHandle, super.get(pHandle));
    }
  }

  @Override
  @Nullable public V get(@NotNull DBTransaction pTransaction, long pHandle) throws SQLException {
    Long key = Long.valueOf(pHandle);
    final V val;
    synchronized(mCache) {
      val = mCache.get(key);
      if (val!=null) {
        return val;
      }
      return storeInCache(pHandle, super.get(pTransaction, pHandle));
    }
  }

  @Override
  public V set(DBTransaction pTransaction, long pHandle, V pValue) throws SQLException {
    V oldValue = super.get(pTransaction, pHandle); // Do not cache the old value or get it from the old cache
    if (! pValue.equals(oldValue)) {
      invalidateCache(pHandle);
      return set(pTransaction, pHandle, oldValue, pValue);
    }
    return oldValue;
  }

  @Override
  protected V set(final DBTransaction pTransaction, final long pHandle, final V oldValue, final V pValue) throws
          SQLException {
    try {
      V result = super.set(pTransaction, pHandle, oldValue, pValue);
      storeInCache(pHandle, pValue);
      return result;
    } catch (Exception e) {
      invalidateCache(pHandle);
      throw e;
    }
  }

  private V storeInCache(long pHandle, V pV) {
    final Long key = Long.valueOf(pHandle);
    synchronized(mCache) {
      mCache.remove(key); // remove whatever old value was there
      mCache.put(key, pV);
    }
    return pV;
  }

  @Override
  public void invalidateCache(final Handle<? extends V> pHandle) {
    long handle = pHandle.getHandle();
    invalidateCache(handle);
  }

  @Override
  public void invalidateCache() {
    synchronized (mCache) {
      mCache.clear();
    }
  }

  private void invalidateCache(final long pHandle) {
    synchronized (mCache) {
      mCache.remove(Long.valueOf(pHandle));
    }
  }

  @Deprecated
  public V getUncached(DBTransaction pTransaction, long pHandle) throws SQLException {
    return storeInCache(pHandle, super.get(pTransaction, pHandle));
  }

  @Override
  public boolean remove(DBTransaction pTransaction, long pHandle) throws SQLException {
    Long key = Long.valueOf(pHandle);
    synchronized (mCache) {
      mCache.remove(key);
    }
    return super.remove(pTransaction, pHandle);
  }

  @Override
  public boolean remove(DBTransaction pTransaction, Object pO) throws SQLException {
    if (pO instanceof Handle<?>) {
      long handle = ((Handle<?>) pO).getHandle();
      return remove(pTransaction, handle);
    }
    for(Iterator<V> it = mCache.values().iterator(); it.hasNext();) {
      V val = it.next();
      if (val!=null && val.equals(pO)) {
        it.remove();
        break;
      }
    }
    return super.remove(pTransaction, pO);
  }

  @Deprecated
  @Override
  public AutoCloseableIterator<V> unsafeIterator(boolean pReadOnly) {
    return new WrappingIterator(super.unsafeIterator(pReadOnly));
  }

  @Override
  public AutoCloseableIterator<V> iterator(DBTransaction pTransaction, boolean pReadOnly) throws SQLException {
    return new WrappingIterator(super.iterator(pTransaction, pReadOnly));
  }

  @Override
  public void close() {
    super.close();
    mCache.clear();
  }

}
