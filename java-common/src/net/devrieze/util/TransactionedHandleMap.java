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
