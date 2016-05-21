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

package net.devrieze.util.db;

import net.devrieze.util.AutoCloseableIterator;
import net.devrieze.util.Handles;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.TransactionedHandleMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DBHandleMap<V> extends DbSet<V> implements TransactionedHandleMap<V, DBTransaction> {



  private final class TransactionIterable implements Iterable<V> {

    private DBTransaction mTransaction;

    public TransactionIterable(DBTransaction pTransaction) {
      mTransaction = pTransaction;
    }

    @Override
    public Iterator<V> iterator() {
      return DBHandleMap.this.iterator(mTransaction, false);
    }

  }

  public interface HMElementFactory<T> extends ElementFactory<T>{
    CharSequence getHandleCondition(Handle<? extends T> pElement);

    int setHandleParams(PreparedStatement pStatement, Handle<? extends T> pHandle, int pOffset) throws SQLException;

    /**
     * Called before removing an element with the given handle
     * @throws SQLException When something goes wrong.
     */
    void preRemove(DBTransaction pConnection, Handle<? extends T> pHandle) throws SQLException;
  }

  private Map<ComparableHandle<? extends V>, V> mPendingCreates = new TreeMap<>();

  protected boolean isPending(ComparableHandle<? extends V> handle) {
    return mPendingCreates.containsKey(handle);
  }

  public DBHandleMap(TransactionFactory pTransactionFactory, HMElementFactory<V> pElementFactory) {
    super(pTransactionFactory, pElementFactory);
  }

  @Override
  protected HMElementFactory<V> getElementFactory() {
    return (HMElementFactory<V>) super.getElementFactory();
  }

  @Override
  public DBTransaction newTransaction() {
    return getTransactionFactory().startTransaction();
  }

  @Override
  public final <W extends V> Handle<W> put(W value) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      Handle<W> result = put(transaction, value);
      transaction.commit();
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public <W extends V> ComparableHandle<W> put(DBTransaction transaction, W value) throws SQLException {
    ComparableHandle<W> result = addWithKey(transaction, value);
    if (result==null) { throw new RuntimeException("Adding element " + value + " failed"); }
    if (value instanceof HandleAware) {
      ((HandleAware<?>) value).setHandleValue(result.getHandleValue());
    }
    return result;
  }

  @Override
  public V get(long pHandle) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      V result = get(transaction, pHandle);
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  @Nullable
  public V get(@NotNull DBTransaction transaction, Handle<? extends V> pHandle) throws SQLException {
    final ComparableHandle<? extends V> handle = Handles.handle(pHandle);
    if (mPendingCreates.containsKey(handle)) { return mPendingCreates.get(handle); }

    final HMElementFactory<V> elementFactory = getElementFactory();
    String sql = addFilter("SELECT "+elementFactory.getCreateColumns()+" FROM "+elementFactory.getTableName()+ " WHERE (" +elementFactory.getHandleCondition(pHandle)+")", " AND ");

    try(PreparedStatement statement = transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = getElementFactory().setHandleParams(statement, pHandle, 1);
      setFilterParams(statement,cnt);

      boolean result = statement.execute();
      if (!result) { return null; }
      try(ResultSet resultset = statement.getResultSet()) {
        if(resultset.first()) {
          elementFactory.initResultSet(resultset.getMetaData());
          V val = elementFactory.create(transaction, resultset);
          if (val==null) {
            remove(transaction, handle);
            return null;
          } else {
            mPendingCreates.put(handle,val);
            try {
              elementFactory.postCreate(transaction, val);
            } finally {
              mPendingCreates.remove(handle);
            }
            return val;
          }
        } else {
          return null;
        }
      }
    }
  }

  @Override
  public final V get(net.devrieze.util.HandleMap.Handle<? extends V> handle) {
    V element = getElementFactory().asInstance(handle);
    if (element!=null) { return element; } // If the element is it's own handle, don't bother looking it up.
    return get(handle.getHandleValue());
  }


  @Override
  public final V get(DBTransaction transaction, long handle) throws SQLException {
    return get(transaction, Handles.<V>handle(handle));
  }

  @Override
  public final V castOrGet(DBTransaction transaction, Handle<? extends V> handle) throws SQLException {
    V element = getElementFactory().asInstance(handle);
    if (element!=null) { return element; } // If the element is it's own handle, don't bother looking it up.
    return get(transaction, handle);
  }

  @Override
  public final V set(long handle, V value) {
    try (DBTransaction transaction= getTransactionFactory().startTransaction()){
      return transaction.commit(set(transaction, handle, value));
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public V set(DBTransaction transaction, Handle<? extends V> handle, V value) throws SQLException {
    V oldValue = get(transaction, handle);

    return set(transaction, handle, oldValue, value);
  }

  protected V set(DBTransaction pTransaction, Handle<? extends V> pHandle, V oldValue, V pValue) throws SQLException {
    if (Objects.equals(oldValue, pValue)) { return oldValue; }
    String sql = addFilter("UPDATE "+ getElementFactory().getTableName()+ " SET "+join(getElementFactory().getStoreColumns(), getElementFactory().getStoreParamHolders(),", "," = ")+" WHERE (" + getElementFactory().getHandleCondition(pHandle)+")", " AND ");
    if (pValue instanceof HandleAware) {
      ((HandleAware<?>)pValue).setHandleValue(pHandle.getHandleValue());
    }
    try(PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = getElementFactory().setStoreParams(statement, pValue, 1);
      cnt += getElementFactory().setHandleParams(statement, pHandle, cnt+1);
      setFilterParams(statement,cnt+1);

      statement.execute();
      getElementFactory().postStore(pTransaction, pHandle, oldValue, pValue);
      return oldValue;
    } catch (SQLException e) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "Error executing query: "+sql,e);
      throw e;
    }
  }

  @Override
  public final V set(Handle<? extends V> handle, V value) {
    return set(handle.getHandleValue(), value);
  }

  @Override
  public final V set(DBTransaction transaction, long handle, V value) throws SQLException {
    return set(transaction, Handles.<V>handle(handle), value);
  }

  /**
   * @deprecated This method maps to {@link #unsafeIterator()}. It does not
   *             automatically take care of closing the database connection if
   *             the iterator is not finished.
   */
  @Override
  @Deprecated
  public Iterator<V> iterator() {
    return unsafeIterator();
  }

  @Override
  public AutoCloseableIterator<V> iterator(final DBTransaction pTransaction, final boolean pReadOnly) {
    try {
      return super.iterator(pTransaction, pReadOnly);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Iterable<V> iterable(DBTransaction transaction) {
    return new TransactionIterable(transaction);
  }


  @Override
  public final boolean containsHandle(Handle<? extends V> handle) {
    return contains(handle.getHandleValue());
  }

  @Override
  public final boolean contains(DBTransaction transaction, Object obj) throws SQLException {
    if (obj instanceof Handle) {
      return contains(transaction, ((Handle<?>) obj).getHandleValue());
    }
    return super.contains(transaction, obj);
  }

  public final boolean contains(Handle<? extends V> pHandle) {
    return contains(pHandle.getHandleValue());
  }

  @Override
  public boolean containsAll(final DBTransaction transaction, final Collection<?> c) throws SQLException {
    for(Object o: c) {
      if (! contains(transaction, o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public final boolean contains(DBTransaction transaction, long handle) throws SQLException {
    return contains(transaction, Handles.<V>handle(handle));
  }

  @Override
  public boolean contains(long handle) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      boolean result = contains(transaction, handle);
      transaction.commit();
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public boolean contains(DBTransaction transaction, Handle<? extends V> handle) throws SQLException {
    String sql = addFilter("SELECT COUNT(*) FROM " + getElementFactory().getTableName() + " WHERE (" + getElementFactory().getHandleCondition(handle) + ")", " AND ");

    try(PreparedStatement statement = transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = getElementFactory().setHandleParams(statement, handle, 1);
      setFilterParams(statement,cnt);

      boolean result = statement.execute();
      if (!result) { return false; }
      try (ResultSet resultset = statement.getResultSet()) {
        return resultset.next();
      }
    }
  }

  @Override
  public final boolean remove(Handle<? extends V> handle) {
    return remove(handle.getHandleValue());
  }

  @Override
  public final boolean remove(long handle) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      boolean result = remove(transaction, handle);
      if(result) { transaction.commit(); }
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public final boolean remove(DBTransaction transaction, long handle) throws SQLException {
    return remove(transaction, Handles.<V>handle(handle));
  }

  @Override
  public boolean remove(DBTransaction transaction, Handle<? extends V> handle) throws SQLException {
    HMElementFactory<V> elementFactory = getElementFactory();
    DBTransaction connection = transaction;
    getElementFactory().preRemove(connection, handle);
    String sql = addFilter("DELETE FROM " + elementFactory.getTableName() + " WHERE (" + elementFactory.getHandleCondition(handle) + ")", " AND ");

    try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = elementFactory.setHandleParams(statement, handle, 1);
      setFilterParams(statement,cnt);

      int changecount = statement.executeUpdate();
      connection.commit();
      return changecount>0;
    }

  }

  @Override
  public void invalidateCache(final Handle<? extends V> handle) {
    // No-op, there is no cache
  }

  @Override
  public void invalidateCache() { /* No-op, no cache */ }

  /**
   * <p>
   * {@inheritDoc}
   * </p>
   * <p>
   * Under water uses the same projection as MemHandleMap. Note that this may
   * not be fast or correct when the underlying database changes in flight!.
   * </p>
   * @deprecated This is a collection now, converting it to a collection is pointless.
   */
  @Override
  @Deprecated
  public Collection<V> toCollection() {
    return this;
  }

}
