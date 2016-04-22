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

import net.devrieze.util.MemHandleMap;
import net.devrieze.util.Transaction;

import java.sql.SQLException;


/**
 * Created by pdvrieze on 09/12/15.
 */
public class MemTransactionedHandleMap<T> extends MemHandleMap<T> implements net.devrieze.util.TransactionedHandleMap<T, Transaction> {

  @Override
  public long put(final Transaction transaction, final T value) throws SQLException {
    return put(value);
  }

  @Override
  public T get(final Transaction transaction, final long handle) throws SQLException {
    return get(handle);
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
  public T set(final Transaction transaction, final long handle, final T value) throws SQLException {
    return set(handle, value);
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
  public boolean contains(final Transaction transaction, final Object o) throws SQLException {
    return contains(o);
  }

  @Override
  public boolean contains(final Transaction transaction, final Handle<? extends T> handle) throws SQLException {
    return contains(handle);
  }

  @Override
  public boolean contains(final Transaction transaction, final long handle) throws SQLException {
    return contains(handle);
  }

  @Override
  public boolean remove(final Transaction transaction, final Handle<? extends T> object) throws SQLException {
    return remove(object);
  }

  @Override
  public boolean remove(final Transaction transaction, final long handle) throws SQLException {
    return remove(handle);
  }

  @Override
  public void invalidateCache(final Handle<? extends T> handle) { /* No-op */ }

  @Override
  public void clear(final Transaction transaction) throws SQLException {
    clear();
  }
}
