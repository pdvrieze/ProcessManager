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

import net.devrieze.util.Handles;
import net.devrieze.util.TransactionFactory;
import net.devrieze.util.TransactionedHandleMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
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
      try {
        return DBHandleMap.this.iterator(mTransaction, false);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }

  }

  public interface HMElementFactory<T> extends ElementFactory<T>{
    CharSequence getHandleCondition(long pElement);

    int setHandleParams(PreparedStatement pStatement, long pHandle, int pOffset) throws SQLException;

    /**
     * Called before removing an element with the given handle
     * @throws SQLException When something goes wrong.
     */
    void preRemove(DBTransaction pConnection, long pHandle) throws SQLException;
  }

  private Map<ComparableHandle<V>, V> mPendingCreates = new TreeMap<>();

  public DBHandleMap(TransactionFactory pTransactionFactory, HMElementFactory<V> pElementFactory) {
    super(pTransactionFactory, pElementFactory);
  }

  @Override
  protected HMElementFactory<V> getElementFactory() {
    return (HMElementFactory<V>) super.getElementFactory();
  }

  @Override
  public final long put(V pValue) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      long result = put(transaction, pValue);
      transaction.commit();
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public long put(DBTransaction pTransaction, V pValue) throws SQLException {
    long result = addWithKey(pTransaction, pValue);
    if (result<0) { throw new RuntimeException("Adding element "+pValue+" failed"); }
    if (pValue instanceof HandleAware) {
      ((HandleAware<?>) pValue).setHandle(result);
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
  public V get(@NotNull DBTransaction pTransaction, long pHandle) throws SQLException {
    final net.devrieze.util.HandleMap.ComparableHandle<V> handle = Handles.handle(pHandle);
    if (mPendingCreates.containsKey(handle)) { return mPendingCreates.get(handle); }

    final HMElementFactory<V> elementFactory = getElementFactory();
    String sql = addFilter("SELECT "+elementFactory.getCreateColumns()+" FROM "+elementFactory.getTableName()+ " WHERE (" +elementFactory.getHandleCondition(pHandle)+")", " AND ");

    try(PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = getElementFactory().setHandleParams(statement, pHandle, 1);
      setFilterParams(statement,cnt);

      boolean result = statement.execute();
      if (!result) { return null; }
      try(ResultSet resultset = statement.getResultSet()) {
        if(resultset.first()) {
          elementFactory.initResultSet(resultset.getMetaData());
          V val = elementFactory.create(pTransaction, resultset);
          if (val==null) {
            remove(pTransaction, handle);
            return null;
          } else {
            mPendingCreates.put(handle,val);
            elementFactory.postCreate(pTransaction, val);
            mPendingCreates.remove(handle);
            return val;
          }
        } else {
          return null;
        }
      }
    }
  }

  @Override
  public final V get(net.devrieze.util.HandleMap.Handle<? extends V> pHandle) {
    V element = getElementFactory().asInstance(pHandle);
    if (element!=null) { return element; } // If the element is it's own handle, don't bother looking it up.
    return get(pHandle.getHandle());
  }


  @Override
  public final V get(DBTransaction pTransaction, Handle<? extends V> pHandle) throws SQLException {
    return get(pTransaction, pHandle.getHandle());
  }

  @Override
  public final V castOrGet(DBTransaction pTransaction, Handle<? extends V> pHandle) throws SQLException {
    V element = getElementFactory().asInstance(pHandle);
    if (element!=null) { return element; } // If the element is it's own handle, don't bother looking it up.
    return get(pTransaction, pHandle);
  }

  @Override
  public final V set(long pHandle, V pValue) {
    try (DBTransaction transaction= getTransactionFactory().startTransaction()){
      return transaction.commit(set(transaction, pHandle, pValue));
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public V set(DBTransaction pTransaction, long pHandle, V pValue) throws SQLException {
    V oldValue = get(pTransaction, pHandle);

    return set(pTransaction, pHandle, oldValue, pValue);
  }

  protected V set(DBTransaction pTransaction, long pHandle, V oldValue, V pValue) throws SQLException {
    String sql = addFilter("UPDATE "+ getElementFactory().getTableName()+ " SET "+join(getElementFactory().getStoreColumns(), getElementFactory().getStoreParamHolders(),", "," = ")+" WHERE (" + getElementFactory().getHandleCondition(pHandle)+")", " AND ");
    if (pValue instanceof HandleAware) {
      ((HandleAware<?>)pValue).setHandle(pHandle);
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
  public final V set(Handle<? extends V> pHandle, V pValue) {
    return set(pHandle.getHandle(), pValue);
  }

  @Override
  public final V set(DBTransaction pTransaction, Handle<? extends V> pHandle, V pValue) throws SQLException {
    return set(pTransaction, pHandle.getHandle(), pValue);
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
  public Iterable<V> iterable(DBTransaction pTransaction) {
    return new TransactionIterable(pTransaction);
  }


  @Override
  public final boolean containsHandle(Handle<? extends V> pHandle) {
    return contains(pHandle.getHandle());
  }

  @Override
  public final boolean contains(DBTransaction pTransaction, Object pO) throws SQLException {
    if (pO instanceof Handle) {
      return contains(pTransaction, ((Handle<?>)pO).getHandle());
    }
    return super.contains(pTransaction, pO);
  }

  public final boolean contains(Handle<? extends V> pHandle) {
    return contains(pHandle.getHandle());
  }

  @Override
  public final boolean contains(DBTransaction pTransaction, Handle<? extends V> pHandle) throws SQLException {
    return contains(pTransaction, pHandle.getHandle());
  }

  @Override
  public boolean contains(long pHandle) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      boolean result = contains(transaction, pHandle);
      transaction.commit();
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public boolean contains(DBTransaction pTransaction, long pHandle) throws SQLException {
    String sql = addFilter("SELECT COUNT(*) FROM "+ getElementFactory().getTableName()+ " WHERE (" + getElementFactory().getHandleCondition(pHandle)+")", " AND ");

    try(PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = getElementFactory().setHandleParams(statement, pHandle, 1);
      setFilterParams(statement,cnt);

      boolean result = statement.execute();
      if (!result) { return false; }
      try (ResultSet resultset = statement.getResultSet()) {
        return resultset.next();
      }
    }
  }

  @Override
  public final boolean remove(Handle<? extends V> pObject) {
    return remove(pObject.getHandle());
  }

  @Override
  public final boolean remove(long pHandle) {
    try (final DBTransaction transaction = getTransactionFactory().startTransaction()) {
      boolean result = remove(transaction, pHandle);
      if(result) { transaction.commit(); }
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public final boolean remove(DBTransaction pTransaction, Handle<? extends V> pObject) throws SQLException {
    return remove(pTransaction, pObject.getHandle());
  }

  @Override
  public boolean remove(DBTransaction pTransaction, long pHandle) throws SQLException {
    HMElementFactory<V> elementFactory = getElementFactory();
    DBTransaction connection = pTransaction;
    getElementFactory().preRemove(connection, pHandle);
    String sql = addFilter("DELETE FROM "+elementFactory.getTableName()+ " WHERE (" +elementFactory.getHandleCondition(pHandle)+")", " AND ");

    try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      int cnt = elementFactory.setHandleParams(statement, pHandle, 1);
      setFilterParams(statement,cnt);

      int changecount = statement.executeUpdate();
      connection.commit();
      return changecount>0;
    }

  }

  @Override
  public void invalidateCache(final Handle<? extends V> pHandle) {
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
