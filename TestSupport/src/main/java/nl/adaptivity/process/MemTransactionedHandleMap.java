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

package nl.adaptivity.process;

import net.devrieze.util.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class MemTransactionedHandleMap<T> extends MemHandleMap<T> implements net.devrieze.util.TransactionedHandleMap<T, Transaction> {

  private static class IteratorWrapper<T> implements AutoCloseableIterator<T> {

    private final boolean     readOnly;
    private final Iterator<T> delegate;

    public IteratorWrapper(Iterator<T> delegate, final boolean readOnly) {
      this.readOnly = readOnly;
      this.delegate = delegate;
    }

    @Override
    public void remove() {
      if (readOnly) throw new UnsupportedOperationException("The iterator is read-only");
    }

    @Override
    public T next() {
      return delegate.next();
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public void close() {
      // Do nothing
    }
  }

  @Override
  public <W extends T> ComparableHandle<W> put(final Transaction transaction, final W value) throws SQLException {
    final Handle<W> put = put(value);
    return Handles.<W>handle(put);
  }

  @Override
  public T get(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return get(handle);
  }

  @Override
  public T castOrGet(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return get(handle);
  }

  @Override
  public T set(final Transaction transaction, final Handle<? extends T> handle, final T value) throws SQLException {
    return set(handle, value);
  }

  @Override
  public Iterable<T> iterable(final Transaction transaction) {
    return this;
  }

  @Override
  public AutoCloseableIterator<T> iterator(final Transaction transaction, final boolean readOnly) {
    return new IteratorWrapper(iterator(), readOnly);
  }

  @Override
  public boolean containsAll(final Transaction transaction, final Collection<?> c) throws SQLException {
    for(Object o: c) {
      if (! contains(o)) { return false; }
    }
    return true;
  }

  @Override
  public Transaction newTransaction() {
    return new StubTransaction();
  }

  @Override
  public boolean contains(final Transaction transaction, final Object element) throws SQLException {
    return contains(element);
  }

  @Override
  public boolean contains(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return contains(handle);
  }

  @Override
  public boolean remove(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return remove(handle);
  }

  @Override
  public void invalidateCache(final Handle<? extends T> handle) { /* No-op */ }

  @Override
  public void invalidateCache() { /* No-op */ }

  @Override
  public void clear(final Transaction transaction) throws SQLException {
    clear();
  }
}
