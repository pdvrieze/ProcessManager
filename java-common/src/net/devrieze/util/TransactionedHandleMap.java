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


/**
 * Interface for handlemaps that support transactions. {@link net.devrieze.util.db.DBHandleMap} does support this, but
 * the interface is needed for testing without hitting the database.
 * Created by pdvrieze on 18/08/15.
 */
public interface TransactionedHandleMap<V, T extends Transaction> extends HandleMap<V> {

  long put(T pTransaction, V pValue) throws SQLException;

  @Deprecated
  V get(T pTransaction, long pHandle) throws SQLException;

  V castOrGet(T pTransaction, Handle<? extends V> pHandle) throws SQLException;

  V get(T pTransaction, Handle<? extends V> pHandle) throws SQLException;

  @Deprecated
  V set(T pTransaction, long pHandle, V pValue) throws SQLException;

  V set(T pTransaction, Handle<? extends V> pHandle, V pValue) throws SQLException;

  Iterable<V> iterable(T pTransaction);

  boolean contains(T pTransaction, Object pO) throws SQLException;

  boolean contains(T pTransaction, Handle<? extends V> pHandle) throws SQLException;

  @Deprecated
  boolean contains(T pTransaction, long pHandle) throws SQLException;

  boolean remove(T pTransaction, Handle<? extends V> pObject) throws SQLException;

  @Deprecated
  boolean remove(T pTransaction, long pHandle) throws SQLException;

  void invalidateCache(Handle<? extends V> pHandle);

  void clear(T pTransaction) throws SQLException;
}
