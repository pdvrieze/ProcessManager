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

import net.devrieze.util.Transaction;
import net.devrieze.util.TransactionFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.DataSource;


public class DBTransaction implements Transaction {

  private Connection mConnection;
  private Savepoint mSafePoint;
  private DataSource mDataSource;
  private boolean mCommitted = true;

  public DBTransaction(DataSource pDataSource) throws SQLException {
    mDataSource = pDataSource;
    mConnection = mDataSource.getConnection();
    mConnection.setAutoCommit(false);
    mSafePoint = mConnection.setSavepoint();
  }



  private DBTransaction(DataSource pDataSource, Connection pConnection) throws SQLException {
    mDataSource = pDataSource;
    mConnection = pConnection;
    mConnection.setAutoCommit(false);
    mSafePoint = mConnection.setSavepoint();
  }



  public <T> T unwrap(Class<T> pIface) throws SQLException {
    return mConnection.unwrap(pIface);
  }

  public boolean isWrapperFor(Class<?> pIface) throws SQLException {
    return mConnection.isWrapperFor(pIface);
  }



  public Statement createStatement() throws SQLException {
    mCommitted = false;
    return mConnection.createStatement();
  }



  public PreparedStatement prepareStatement(String pSql) throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql);
  }

  public CallableStatement prepareCall(String pSql) throws SQLException {
    mCommitted = false;
    return mConnection.prepareCall(pSql);
  }

  public String nativeSQL(String pSql) throws SQLException {
    return mConnection.nativeSQL(pSql);
  }

  public boolean isClosed() throws SQLException {
    return mConnection==null || mConnection.isClosed();
  }

  public DatabaseMetaData getMetaData() throws SQLException {
    return mConnection.getMetaData();
  }

  public void setReadOnly(boolean pReadOnly) throws SQLException {
    mConnection.setReadOnly(pReadOnly);
  }

  public boolean isReadOnly() throws SQLException {
    return mConnection.isReadOnly();
  }

  public void setCatalog(String pCatalog) throws SQLException {
    mConnection.setCatalog(pCatalog);
  }

  public String getCatalog() throws SQLException {
    return mConnection.getCatalog();
  }

  public SQLWarning getWarnings() throws SQLException {
    return mConnection.getWarnings();
  }

  public void clearWarnings() throws SQLException {
    mConnection.clearWarnings();
  }

  public Statement createStatement(int pResultSetType, int pResultSetConcurrency) throws SQLException {
    mCommitted = false;
    return mConnection.createStatement(pResultSetType, pResultSetConcurrency);
  }

  public PreparedStatement prepareStatement(String pSql, int pResultSetType, int pResultSetConcurrency) throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql, pResultSetType, pResultSetConcurrency);
  }

  public CallableStatement prepareCall(String pSql, int pResultSetType, int pResultSetConcurrency) throws SQLException {
    mCommitted = false;
    return mConnection.prepareCall(pSql, pResultSetType, pResultSetConcurrency);
  }

  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return mConnection.getTypeMap();
  }

  public void setTypeMap(Map<String, Class<?>> pMap) throws SQLException {
    mConnection.setTypeMap(pMap);
  }

  public void setHoldability(int pHoldability) throws SQLException {
    mConnection.setHoldability(pHoldability);
  }

  public int getHoldability() throws SQLException {
    return mConnection.getHoldability();
  }

  public Savepoint setSavepoint() throws SQLException {
    return mConnection.setSavepoint();
  }

  public Savepoint setSavepoint(String pName) throws SQLException {
    return mConnection.setSavepoint(pName);
  }

  public void rollback(Savepoint pSavepoint) throws SQLException {
    mCommitted = false;
    mConnection.rollback(pSavepoint);
  }

  public void releaseSavepoint(Savepoint pSavepoint) throws SQLException {
    mConnection.releaseSavepoint(pSavepoint);
  }

  public Statement createStatement(int pResultSetType, int pResultSetConcurrency, int pResultSetHoldability) throws SQLException {
    mCommitted = false;
    return mConnection.createStatement(pResultSetType, pResultSetConcurrency, pResultSetHoldability);
  }

  public PreparedStatement prepareStatement(String pSql, int pResultSetType, int pResultSetConcurrency, int pResultSetHoldability)
      throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql, pResultSetType, pResultSetConcurrency, pResultSetHoldability);
  }

  public CallableStatement prepareCall(String pSql, int pResultSetType, int pResultSetConcurrency, int pResultSetHoldability)
      throws SQLException {
    mCommitted = false;
    return mConnection.prepareCall(pSql, pResultSetType, pResultSetConcurrency, pResultSetHoldability);
  }



  public PreparedStatement prepareStatement(String pSql, int pAutoGeneratedKeys) throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql, pAutoGeneratedKeys);
  }



  public PreparedStatement prepareStatement(String pSql, int[] pColumnIndexes) throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql, pColumnIndexes);
  }



  public PreparedStatement prepareStatement(String pSql, String[] pColumnNames) throws SQLException {
    mCommitted = false;
    return mConnection.prepareStatement(pSql, pColumnNames);
  }



  public Clob createClob() throws SQLException {
    return mConnection.createClob();
  }



  public Blob createBlob() throws SQLException {
    return mConnection.createBlob();
  }



  public NClob createNClob() throws SQLException {
    return mConnection.createNClob();
  }



  public SQLXML createSQLXML() throws SQLException {
    return mConnection.createSQLXML();
  }



  public boolean isValid(int pTimeout) throws SQLException {
    return mConnection.isValid(pTimeout);
  }



  public void setClientInfo(String pName, String pValue) throws SQLClientInfoException {
    mConnection.setClientInfo(pName, pValue);
  }



  public void setClientInfo(Properties pProperties) throws SQLClientInfoException {
    mConnection.setClientInfo(pProperties);
  }



  public String getClientInfo(String pName) throws SQLException {
    return mConnection.getClientInfo(pName);
  }



  public Properties getClientInfo() throws SQLException {
    return mConnection.getClientInfo();
  }



  public Array createArrayOf(String pTypeName, Object[] pElements) throws SQLException {
    return mConnection.createArrayOf(pTypeName, pElements);
  }



  public Struct createStruct(String pTypeName, Object[] pAttributes) throws SQLException {
    return mConnection.createStruct(pTypeName, pAttributes);
  }



  public void setSchema(String pSchema) throws SQLException {
    mConnection.setSchema(pSchema);
  }



  public String getSchema() throws SQLException {
    return mConnection.getSchema();
  }



  public void abort(Executor pExecutor) throws SQLException {
    mConnection.abort(pExecutor);
  }



  public void setNetworkTimeout(Executor pExecutor, int pMilliseconds) throws SQLException {
    mConnection.setNetworkTimeout(pExecutor, pMilliseconds);
  }



  public int getNetworkTimeout() throws SQLException {
    return mConnection.getNetworkTimeout();
  }



  public void rollback() throws SQLException {
    mConnection.rollback(mSafePoint);
  }

  public void commit() throws SQLException {
    mConnection.releaseSavepoint(mSafePoint);
    mConnection.commit();
    mSafePoint = mConnection.setSavepoint();
    mCommitted = true;
  }

  @Override
  public void close() {
    if (mConnection!=null) {
      // If commit has been called just before, this should not loose data.
      try {
        if (! mCommitted) {
          // In the case of mCommitted we know rollback is not needed.
          mConnection.rollback(mSafePoint);
        }
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      } finally {
        try {
          mConnection.close();
        } catch (SQLException ex) {
          throw new RuntimeException(ex);
        } finally {
          mConnection = null;
        }
      }
    }
  }



  public boolean providerEquals(DataSource pDataSource) {
    return mDataSource.equals(pDataSource);
  }



  public static DBTransaction take(DataSource pDataSource, Connection pConnection) throws SQLException {
    return new DBTransaction(pDataSource, pConnection);
  }



  /**
   * Helper method that will invoke commit and return the passed variable. This is just to
   * allow for prettier code.
   * @param pValue The value to return.
   * @return The passed in value.
   * @throws SQLException When commit fails.
   */
  @Override
  public <T> T commit(T pValue) throws SQLException {
    commit();
    return pValue;
  }

}
