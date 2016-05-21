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

import java.sql.SQLException;
import java.util.Collection;


/**
 * Interface for handlemaps that support transactions. {@link net.devrieze.util.db.DBHandleMap} does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
public interface TransactionedHandleMap<V, T extends Transaction> extends HandleMap<V> {

  <W extends V> ComparableHandle<W> put(T transaction, W value) throws SQLException;

  /**
   * @deprecated use typed {@link #get(Transaction, Handle)}
   */
  @Deprecated
  V get(T transaction, long handle) throws SQLException;

  V castOrGet(T transaction, Handle<? extends V> handle) throws SQLException;

  V get(T transaction, Handle<? extends V> handle) throws SQLException;

  /**
   * @deprecated use typed {@link #get(Transaction, Handle)}
   */
  @Deprecated
  V set(T transaction, long handle, V value) throws SQLException;

  V set(T transaction, Handle<? extends V> handle, V value) throws SQLException;

  Iterable<V> iterable(T transaction);

  boolean contains(T transaction, Object obj) throws SQLException;

  boolean contains(T transaction, Handle<? extends V> handle) throws SQLException;

  boolean containsAll(T transaction, Collection<?> c) throws SQLException;

  /**
   * @deprecated use typed {@link #get(Transaction, Handle)}
   */
  @Deprecated
  boolean contains(T transaction, long handle) throws SQLException;

  boolean remove(T transaction, Handle<? extends V> handle) throws SQLException;

  /**
   * @deprecated use typed {@link #get(Transaction, Handle)}
   */
  @Deprecated
  boolean remove(T transaction, long handle) throws SQLException;

  void invalidateCache(Handle<? extends V> handle);

  void invalidateCache();

  void clear(T transaction) throws SQLException;

  AutoCloseableIterator<V> iterator(T transaction, boolean readOnly);

  T newTransaction();
}
