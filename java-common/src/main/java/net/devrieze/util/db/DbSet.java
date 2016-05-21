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
import net.devrieze.util.HandleMap.ComparableHandle;
import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Handles;
import net.devrieze.util.TransactionFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.lang.reflect.Array;
import java.sql.*;
import java.util.*;


public class DbSet<T> implements AutoCloseable {


  /**
   * Iterable that automatically closes
   * @author pdvrieze
   *
   */
  public final class ClosingIterable implements Iterable<T>, AutoCloseable {

    @Override
    public void close() {
      DbSet.this.close();
    }

    @Override
    public Iterator<T> iterator() {
      return DbSet.this.unsafeIterator();
    }

  }

  private class ResultSetIterator implements AutoCloseableIterator<T> {

    private final ResultSet mResultSet;
    private final PreparedStatement mStatement;
    private T mNextElem=null;
    private boolean mFinished = false;
    private DBTransaction mTransaction;
    private final boolean mCloseOnFinish;

    public ResultSetIterator(DBTransaction pTransaction, PreparedStatement pStatement, ResultSet pResultSet) throws SQLException {
      mTransaction = pTransaction;
      mStatement = pStatement;
      mResultSet = pResultSet;
      mElementFactory.initResultSet(pResultSet.getMetaData());
      mCloseOnFinish = false;
    }

    public int size() throws SQLException {
      int pos = mResultSet.getRow();
      try {
        mResultSet.last();
        return mResultSet.getRow();
      } finally {
        mResultSet.absolute(pos);
      }
    }

    @Override
    public boolean hasNext() {
      if (mFinished) { return false; }
      if (mNextElem!=null) { return true; }

      try {
        boolean success = mResultSet.next();
        if (success) {
          mNextElem = mElementFactory.create(mTransaction, mResultSet);
          while (success && mNextElem==null) {
            mElementFactory.preRemove(mTransaction, mResultSet);
            mResultSet.deleteRow();
            success = mResultSet.next();
            if(success) {
              mNextElem = mElementFactory.create(mTransaction, mResultSet);
            }
          }

        }
        if (! success) {
          mFinished = true;
          mTransaction.commit();
          if (mCloseOnFinish) {
            closeResultSet(mTransaction, mStatement, mResultSet);
          } else {
            closeResultSet(null, mStatement, mResultSet);
          }
          return false;
        }
        // TODO hope that this works
        mElementFactory.postCreate(mTransaction, mNextElem);
        return true;
      } catch (SQLException ex) {
        closeResultSet(mTransaction, mStatement, mResultSet) ;
        throw new RuntimeException(ex);
      }
    }

    @Override
    public T next() {
      final T nextElem = mNextElem;
      mNextElem = null;
      if (nextElem !=null) {
        return nextElem;
      }
      if (!hasNext()) { // hasNext will actually update mNextElem;
        throw new IllegalStateException("Reading beyond iterator");
      }
      return mNextElem;
    }

    @Override
    public void remove() {
      try {
        mResultSet.deleteRow();
      } catch (SQLException ex) {
        closeResultSet(mTransaction, mStatement, mResultSet) ;
        throw new RuntimeException(ex);
      }

    }

    @Override
    public void close() {
      try {
        try {
          try{
            mResultSet.close();
          } finally {
            mStatement.close();
          }
        } finally {
          mIterators.remove(this);
          if (mIterators.isEmpty()) {
            DbSet.this.close();
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private final TransactionFactory<DBTransaction> mTransactionFactory;
  private final ElementFactory<T> mElementFactory;

  private Collection<ResultSetIterator> mIterators = new ArrayList<>();

  public DbSet(TransactionFactory pTransactionFactory, ElementFactory<T> pElementFactory) {
    mTransactionFactory = pTransactionFactory;
    mElementFactory = pElementFactory;
  }

  public final ClosingIterable closingIterable() {
    return new ClosingIterable();
  }


  @Deprecated
  public final AutoCloseableIterator<T> unsafeIterator() {
    return unsafeIterator(false);
  }

  @Deprecated
  @SuppressWarnings("resource")
  public AutoCloseableIterator<T> unsafeIterator(boolean pReadOnly) {
    DBTransaction transaction = null;
    PreparedStatement statement = null;
    try {
      transaction = mTransactionFactory.startTransaction();
//      connection = mTransactionFactory.getConnection();
//      connection.setAutoCommit(false);
      CharSequence columns = mElementFactory.getCreateColumns();

      String sql = addFilter("SELECT "+columns+" FROM `"+ mElementFactory.getTableName()+"`", " WHERE ");

      statement = transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, pReadOnly ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE);
      setFilterParams(statement,1);

      statement.execute();
      final ResultSetIterator it = new ResultSetIterator(transaction, statement, statement.getResultSet());
      mIterators.add(it);
      return it;
    } catch (Exception e) {
      try {
        if (statement!=null) { statement.close(); }
      } catch (SQLException ex) {
        final RuntimeException runtimeException = new RuntimeException(ex);
        runtimeException.addSuppressed(e);
        throw runtimeException;
      } finally {
        if (transaction!=null) {
          rollbackConnection(transaction, null, e);
        }
      }

      if (e instanceof RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }

  }

  @SuppressWarnings("resource")
  public AutoCloseableIterator<T> iterator(DBTransaction pTransaction, boolean pReadOnly) throws SQLException {
    try {
      CharSequence columns = mElementFactory.getCreateColumns();

      String sql = addFilter("SELECT "+columns+" FROM `"+ mElementFactory.getTableName()+"`", " WHERE ");

      PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, pReadOnly ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE);
      setFilterParams(statement,1);

      statement.execute();
      final ResultSetIterator it = new ResultSetIterator(pTransaction, statement, statement.getResultSet());
      mIterators.add(it);
      return it;
    } catch (RuntimeException e) {
      rollbackConnection(pTransaction, e);
      close();
      throw e;
    } catch (SQLException e) {
      close();
      throw e;
    }
  }

  public final int size() {
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      return size(transaction);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public int size(DBTransaction connection) throws SQLException {
    CharSequence columns = mElementFactory.getCreateColumns();

    String sql = addFilter("SELECT COUNT( "+columns+" ) FROM `"+ mElementFactory.getTableName()+"`", " WHERE ");

    try(PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      setFilterParams(statement, 1);

      boolean result = statement.execute();
      if (result) {
        try(ResultSet resultset = statement.getResultSet()) {
          if (resultset.next()) {
            return resultset.getInt(1);
          } else {
            throw new RuntimeException("Retrieving row count failed");
          }
        }
      } else {
        throw new RuntimeException("Retrieving row count failed");
      }
    }
  }

  public final boolean contains(Object object) {
    if(mElementFactory.asInstance(object) == null) { return false; }
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      boolean result = contains(transaction, object);
      transaction.commit();
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean contains(DBTransaction connection, Object pO) throws SQLException {
    T object = mElementFactory.asInstance(pO);
    if (object!=null) {
      String sql = addFilter("SELECT COUNT(*) FROM "+ mElementFactory.getTableName()+ " WHERE (" + mElementFactory.getPrimaryKeyCondition(object)+")", " AND ");

      try(PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        int cnt = mElementFactory.setPrimaryKeyParams(statement,object, 1);
        setFilterParams(statement,cnt);

        boolean result = statement.execute();
        if (!result) { return false; }
        try(ResultSet resultset = statement.getResultSet()) {
          return resultset.next();
        }
      }
    } else {
      return false;
    }
  }

  public final boolean add(T pE) {
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      return commitIfTrue(transaction, add(transaction, pE));
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean commitIfTrue(DBTransaction pTransaction, boolean pValue) throws SQLException {
    if (pValue) { pTransaction.commit(); }
    return pValue;
  }

  public boolean add(DBTransaction pTransaction, T pE) {
    assert mTransactionFactory.isValidTransaction(pTransaction);
    if (pE==null) { throw new NullPointerException(); }

    try {
      String sql = "INSERT INTO "+ mElementFactory.getTableName()+ " ( "+join(mElementFactory.getStoreColumns(),", ") +" ) VALUES ( " +join(mElementFactory
                                                                                                                                                    .getStoreParamHolders(), ", ")+" )";

      try(PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        mElementFactory.setStoreParams(statement, pE, 1);

        int changecount = statement.executeUpdate();
        if (changecount>0) {
          final long handle;
          try (ResultSet keys = statement.getGeneratedKeys()) {
            keys.next();
            handle = keys.getLong(1);
          }
          mElementFactory.postStore(pTransaction, Handles.<T>handle(handle), null, pE);
          pTransaction.commit();
          return true;
        } else {
          return false;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static CharSequence join(List<CharSequence> pList, CharSequence pSeparator) {
    if (pList==null) { return null; }
    if (pList.isEmpty()) { return ""; }
    StringBuilder result = new StringBuilder();
    Iterator<CharSequence> it = pList.iterator();
    result.append(it.next());
    while (it.hasNext()) {
      result.append(pSeparator).append(it.next());
    }
    return result;
  }

  protected static CharSequence join(List<CharSequence> pList1, List<CharSequence> pList2, CharSequence pOuterSeparator, CharSequence pInnerSeparator) {
    if (pList1.size()!=pList2.size()) { throw new IllegalArgumentException("List sizes must match"); }
    if (pList1.isEmpty()) { return ""; }
    StringBuilder result = new StringBuilder();
    Iterator<CharSequence> it1 = pList1.iterator();
    Iterator<CharSequence> it2 = pList2.iterator();

    result.append(it1.next()).append(pInnerSeparator).append(it2.next());
    while (it1.hasNext()) {
      result.append(pOuterSeparator).append(it1.next()).append(pInnerSeparator).append(it2.next());
    }
    return result;
  }

  public final boolean addAll(Collection<? extends T> pC) {
    try (DBTransaction transaction = mTransactionFactory.startTransaction()) {
      return commitIfTrue(transaction, addAll(transaction, pC));
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean addAll(DBTransaction pTransaction, Collection<? extends T> pC) throws SQLException {
    if (pC==null) { throw new NullPointerException(); }
    DBTransaction connection = pTransaction;

    String sql = "INSERT INTO "+ mElementFactory.getTableName()+ " ( "+join(mElementFactory.getStoreColumns(),", ") +" ) VALUES ( " +join(mElementFactory
                                                                                                                                                  .getStoreParamHolders(),", ")+" )";

    try(PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      for(T element: pC) {
        mElementFactory.setStoreParams(statement, element, 1);
        statement.addBatch();
      }
      int[] result = statement.executeBatch();

      for(int c:result) {
        if (c<1) {
          connection.rollback();
          return false; // Error, we just roll back and don't change a thing
        }
      }
      try (ResultSet keys = statement.getGeneratedKeys()) {
        for(T element: pC) {
          keys.next();
          Handle<T> handle = Handles.handle(keys.getLong(1));
          mElementFactory.postStore(connection, handle, null, element);
        }
      }
      connection.commit();
      return result.length>0;
    }
  }

  public final boolean remove(Object pO) {
    if(mElementFactory.asInstance(pO)==null) { return false; }
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      return commitIfTrue(transaction, remove(transaction, pO));
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean remove(DBTransaction pTransaction, Object pO) throws SQLException {
    T object = mElementFactory.asInstance(pO);
    if (object!=null) {
      mElementFactory.preRemove(pTransaction, object);
      String sql = addFilter("DELETE FROM "+ mElementFactory.getTableName()+ " WHERE (" + mElementFactory.getPrimaryKeyCondition(object)+")", " AND ");

      try (PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        int cnt = mElementFactory.setPrimaryKeyParams(statement, object, 1);
        setFilterParams(statement,cnt);

        int changecount = statement.executeUpdate();
        pTransaction.commit();
        return changecount>0;
      }

    } else {
      return false;
    }
  }

  public final void clear() {
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      clear(transaction);
      transaction.commit();
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
 }

  public void clear(DBTransaction transaction) throws SQLException {
    mElementFactory.preClear(transaction);
    String sql = addFilter("DELETE FROM "+ mElementFactory.getTableName(), " WHERE ");

    try (PreparedStatement statement = transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      setFilterParams(statement, 1);

      statement.executeUpdate();
      transaction.commit();
    }
  }

  @Override
  public void close() {
    List<RuntimeException> errors = null;
    for(DbSet<T>.ResultSetIterator iterator: mIterators) {
      try {
        iterator.close();
      } catch (RuntimeException e) {
        if (errors==null) { errors = new ArrayList<>(); }
        errors.add(e);
      }
    }
    if (errors!=null) {
      Iterator<RuntimeException> it = errors.iterator();
      RuntimeException ex = it.next();
      while (it.hasNext()) { ex.addSuppressed(it.next()); }
      throw ex;
    }
  }

  public boolean isEmpty() {
    try(ResultSetIterator it = (ResultSetIterator)unsafeIterator(true)) {
      return it.hasNext();
    }
  }

  public boolean isEmpty(DBTransaction pTransaction) throws SQLException {
    try(ResultSetIterator it = (ResultSetIterator)iterator(pTransaction, true)) {
      return it.hasNext();
    }
  }

  public Object[] toArray() {
    return toArray(new Object[0]);
  }

  public <U> U[] toArray(U[] pA) {
    try (@SuppressWarnings({ "rawtypes", "unchecked" })
        DbSet<U>.ResultSetIterator it = (DbSet.ResultSetIterator) unsafeIterator(true)) {
      int size = it.size();

      @SuppressWarnings("unchecked")
      final U[] result = size<=pA.length? pA : (U[]) Array.newInstance(pA.getClass(), size);
      for(int i=0; it.hasNext() && i<result.length; ++i) {
        result[i] = it.next();
      }
      if (size<result.length) {
        result[size] = null;
      }
      return result;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  // TODO do this smarter
  public boolean containsAll(Collection<?> pC) {
    Collection<?> cpy = new HashSet<>(pC);
    try(ClosingIterable col = closingIterable()) {
      for(T elem:col) {
        cpy.remove(elem);
      }
    }
    return cpy.isEmpty();
  }

  // TODO do this smarter
  public boolean retainAll(Collection<?> pC) {
    boolean changed = false;
    try(ClosingIterable col = closingIterable()) {
      for(Iterator<T> it = col.iterator(); it.hasNext();) {
        if (!pC.contains(it.next())) {
          it.remove();
          changed = true;
        }
      }
    }
    return changed;
  }

  // TODO do this smarter
  public boolean removeAll(Collection<?> pC) {
    boolean changed = false;
    try(ClosingIterable col = closingIterable()) {
      for(Iterator<T> it = col.iterator(); it.hasNext();) {
        if (pC.contains(it.next())) {
          it.remove();
          changed = true;
        }
      }
    }
    return changed;
  }

  public boolean removeAll(DBTransaction pTransaction, String pSelection, Object... pSelectionArgs) throws SQLException {
    {
      // First call pre-remove for all elements
      // TODO can this hook into a cache/not require creation
      String sql = addFilter("SELECT "+ getElementFactory().getCreateColumns()+" FROM "+ getElementFactory().getTableName()+ " WHERE (" +pSelection+")", " AND ");
      try(PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        int cnt =1;
        for(Object param: pSelectionArgs) {
          statement.setObject(cnt, param);
          ++cnt;
        }
        setFilterParams(statement,cnt);

        boolean result = statement.execute();
        if (!result) {
          // There are no nodes, so no change.
          return false;
        }

        try(ResultSet resultset = statement.getResultSet()) {
          final ElementFactory<T> elementFactory = getElementFactory();
          elementFactory.initResultSet(resultset.getMetaData());
          while (resultset.next()) {
            elementFactory.preRemove(pTransaction, resultset);
          }
        }
      }
    }

    {
      String sql = addFilter("DELETE FROM "+ mElementFactory.getTableName()+ " WHERE (" +pSelection+")", " AND ");

      try (PreparedStatement statement = pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        int cnt =1;
        for(Object param: pSelectionArgs) {
          statement.setObject(cnt, param);
          ++cnt;
        }
        setFilterParams(statement,cnt);

        int changecount = statement.executeUpdate();
        pTransaction.commit();
        return changecount>0;
      }
    }
  }

  protected final Handle<T> addWithKey(T pE) {
    try (final DBTransaction transaction = mTransactionFactory.startTransaction()) {
      Handle<T> handle = addWithKey(transaction, pE);
      if (handle!=null) {
        transaction.commit();
      }
      return handle;
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected <W extends T> ComparableHandle<W> addWithKey(DBTransaction connection, W pE) throws SQLException {
    if (pE==null) { throw new NullPointerException(); }

    String sql = "INSERT INTO "+ mElementFactory.getTableName()+ " ( "+join(mElementFactory.getStoreColumns(),", ") +" ) VALUES ( " +join(mElementFactory
                                                                                                                                                  .getStoreParamHolders(),", ")+" )";

    try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      mElementFactory.setStoreParams(statement, pE, 1);

      String fullSql = statement.toString();
      try {
        int changecount = statement.executeUpdate();
        if (changecount > 0) {
          final ComparableHandle<W> handle;
          try (ResultSet keys = statement.getGeneratedKeys()) {
            keys.next();
            handle = Handles.handle(keys.getLong(1));
          }
          mElementFactory.postStore(connection, handle, null, pE);

          connection.commit();
          return handle;
        } else {
          return null;
        }
      } catch (SQLException e) {
        throw new SQLException("Error executing the query: "+fullSql, e);
      }
    }

  }

  protected ElementFactory<T> getElementFactory() {
    return mElementFactory;
  }

  protected final TransactionFactory<DBTransaction> getTransactionFactory() {
    return mTransactionFactory;
  }

  protected final Connection getConnection() {
    try {
      @SuppressWarnings("resource")
      Connection connection = mTransactionFactory.getConnection();
      try {
        connection.setAutoCommit(false);
      } catch (SQLException ex) {
        connection.close();
        throw ex;
      }
      return connection;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void rollbackConnection(DBTransaction pConnection, Throwable pCause) {
    rollbackConnection(pConnection, null, pCause);
  }

  private static void rollbackConnection(DBTransaction pConnection, Savepoint pSavepoint, Throwable pCause) {
    try {
      if (pSavepoint==null) {
        pConnection.rollback();
      } else {
        pConnection.rollback(pSavepoint);
      }
    } catch (SQLException ex) {
      if (pCause!=null) {
        pCause.addSuppressed(ex);
      } else {
        throw new RuntimeException(ex);
      }
    }
    if (pCause instanceof RuntimeException) {
      throw (RuntimeException) pCause;
    }
    throw new RuntimeException(pCause);
  }

  protected static void rollbackConnection(Connection pConnection, Savepoint pSavepoint, Throwable pCause) {
    try {
      if (pSavepoint==null) {
        pConnection.rollback();
      } else {
        pConnection.rollback(pSavepoint);
      }
    } catch (SQLException ex) {
      if (pCause!=null) {
        pCause.addSuppressed(ex);
      } else {
        throw new RuntimeException(ex);
      }
    }
    if (pCause instanceof RuntimeException) {
      throw (RuntimeException) pCause;
    }
    throw new RuntimeException(pCause);
  }

  /**
   *
   * @deprecated Use try-with
   */
  @Deprecated
  protected static void closeConnection(Connection pConnection, Exception e) {
    try {
      if (pConnection!=null) {
        pConnection.close();
      }
    } catch (Exception ex) {
      e.addSuppressed(ex);
    }
  }

  protected static void closeResultSet(DBTransaction pConnection, PreparedStatement pStatement, ResultSet pResultSet) {
    try {
      try {
        try {
          try {
            pResultSet.close();
          } finally {
            pStatement.close();
          }
        } finally {
          if (pConnection!=null) {
            if (! pConnection.isClosed()) {
              pConnection.rollback();
            }
          }
        }
      } finally {
        if (pConnection!=null) {
          pConnection.close();
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  protected String addFilter(CharSequence pSQL, CharSequence pConnector) {
    CharSequence filterExpression = getElementFactory().getFilterExpression();
    if (filterExpression==null || filterExpression.length()==0) {
      return new StringBuilder(pSQL.length()+1).append(pSQL).append(';').toString();
    }
    StringBuilder result = new StringBuilder(pSQL.length()+pConnector.length()+filterExpression.length());
    return result.append(pSQL).append(pConnector).append(filterExpression).append(';').toString();
  }

  protected void setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException {
    final CharSequence filterExpression = getElementFactory().getFilterExpression();
    if (filterExpression!=null && filterExpression.length()>=0) {
      getElementFactory().setFilterParams(pStatement, pOffset);
    }
  }

  /**
   * @deprecated use {@link #resourceNameToDataSource(Context, String)}, that is more reliable as the context can be gained earlier
   */
  @Deprecated
  public static DataSource resourceNameToDataSource2(String pResourceName) {
    try {
      return (DataSource) new InitialContext().lookup(pResourceName);
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  public static DataSource resourceNameToDataSource(final Context pContext, final String pDbresourcename) {
    try {
      if (pContext == null) {
        try {
          return (DataSource) new InitialContext().lookup(pDbresourcename);
        } catch (NamingException e) {
          return (DataSource) new InitialContext().lookup("java:/comp/env/"+pDbresourcename);
        }
      } else {
        return (DataSource) pContext.lookup(pDbresourcename);
      }
    } catch (NamingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "DbSet <SELECT * FROM `"+ mElementFactory.getTableName()+"`>";
  }
}
