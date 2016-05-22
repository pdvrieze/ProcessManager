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

package net.devrieze.util.db

import net.devrieze.util.Transaction

import java.sql.*
import java.util.*
import java.util.concurrent.Executor

import javax.sql.DataSource


@Deprecated("")
class OldDBTransaction : Transaction {

  private var mConnection: Connection? = null
  private var mSafePoint: Savepoint? = null
  private var mDataSource: DataSource? = null
  private var mCommitted = true

  private val rollbackHandlers = ArrayDeque<Runnable>()

  @Throws(SQLException::class)
  constructor(pDataSource: DataSource) {
    mDataSource = pDataSource
    mConnection = mDataSource!!.connection
    mConnection!!.autoCommit = false
    mSafePoint = mConnection!!.setSavepoint()
  }


  @Throws(SQLException::class)
  private constructor(pDataSource: DataSource, pConnection: Connection) {
    mDataSource = pDataSource
    mConnection = pConnection
    mConnection!!.autoCommit = false
    mSafePoint = mConnection!!.setSavepoint()
  }


  @Throws(SQLException::class)
  fun <T> unwrap(pIface: Class<T>): T {
    return mConnection!!.unwrap(pIface)
  }

  @Throws(SQLException::class)
  fun isWrapperFor(pIface: Class<*>): Boolean {
    return mConnection!!.isWrapperFor(pIface)
  }


  @Throws(SQLException::class)
  fun createStatement(): Statement {
    mCommitted = false
    return mConnection!!.createStatement()
  }


  @Throws(SQLException::class)
  fun prepareStatement(pSql: String): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql)
  }

  @Throws(SQLException::class)
  fun prepareCall(pSql: String): CallableStatement {
    mCommitted = false
    return mConnection!!.prepareCall(pSql)
  }

  @Throws(SQLException::class)
  fun nativeSQL(pSql: String): String {
    return mConnection!!.nativeSQL(pSql)
  }

  val isClosed: Boolean
    @Throws(SQLException::class)
    get() = mConnection == null || mConnection!!.isClosed

  val metaData: DatabaseMetaData
    @Throws(SQLException::class)
    get() = mConnection!!.metaData

  var isReadOnly: Boolean
    @Throws(SQLException::class)
    get() = mConnection!!.isReadOnly
    @Throws(SQLException::class)
    set(pReadOnly) {
      mConnection!!.isReadOnly = pReadOnly
    }

  var catalog: String
    @Throws(SQLException::class)
    get() = mConnection!!.catalog
    @Throws(SQLException::class)
    set(pCatalog) {
      mConnection!!.catalog = pCatalog
    }

  val warnings: SQLWarning
    @Throws(SQLException::class)
    get() = mConnection!!.warnings

  @Throws(SQLException::class)
  fun clearWarnings() {
    mConnection!!.clearWarnings()
  }

  @Throws(SQLException::class)
  fun createStatement(pResultSetType: Int, pResultSetConcurrency: Int): Statement {
    mCommitted = false
    return mConnection!!.createStatement(pResultSetType, pResultSetConcurrency)
  }

  @Throws(SQLException::class)
  fun prepareStatement(pSql: String, pResultSetType: Int, pResultSetConcurrency: Int): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql, pResultSetType, pResultSetConcurrency)
  }

  @Throws(SQLException::class)
  fun prepareCall(pSql: String, pResultSetType: Int, pResultSetConcurrency: Int): CallableStatement {
    mCommitted = false
    return mConnection!!.prepareCall(pSql, pResultSetType, pResultSetConcurrency)
  }

  var typeMap: Map<String, Class<*>>
    @Throws(SQLException::class)
    get() = mConnection!!.typeMap
    @Throws(SQLException::class)
    set(pMap) {
      mConnection!!.typeMap = pMap
    }

  var holdability: Int
    @Throws(SQLException::class)
    get() = mConnection!!.holdability
    @Throws(SQLException::class)
    set(pHoldability) {
      mConnection!!.holdability = pHoldability
    }

  @Throws(SQLException::class)
  fun setSavepoint(): Savepoint {
    return mConnection!!.setSavepoint()
  }

  @Throws(SQLException::class)
  fun setSavepoint(pName: String): Savepoint {
    return mConnection!!.setSavepoint(pName)
  }

  @Throws(SQLException::class)
  fun rollback(pSavepoint: Savepoint) {
    mCommitted = false
    mConnection!!.rollback(pSavepoint)

    while (rollbackHandlers.isNotEmpty()) {
      rollbackHandlers.pop().run()
    }
  }

  override fun addRollbackHandler(runnable: Runnable) {
    rollbackHandlers.add(runnable)
  }

  @Throws(SQLException::class)
  fun releaseSavepoint(pSavepoint: Savepoint) {
    mConnection!!.releaseSavepoint(pSavepoint)
  }

  @Throws(SQLException::class)
  fun createStatement(pResultSetType: Int, pResultSetConcurrency: Int, pResultSetHoldability: Int): Statement {
    mCommitted = false
    return mConnection!!.createStatement(pResultSetType, pResultSetConcurrency, pResultSetHoldability)
  }

  @Throws(SQLException::class)
  fun prepareStatement(pSql: String,
                       pResultSetType: Int,
                       pResultSetConcurrency: Int,
                       pResultSetHoldability: Int): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql, pResultSetType, pResultSetConcurrency, pResultSetHoldability)
  }

  @Throws(SQLException::class)
  fun prepareCall(pSql: String,
                  pResultSetType: Int,
                  pResultSetConcurrency: Int,
                  pResultSetHoldability: Int): CallableStatement {
    mCommitted = false
    return mConnection!!.prepareCall(pSql, pResultSetType, pResultSetConcurrency, pResultSetHoldability)
  }


  @Throws(SQLException::class)
  fun prepareStatement(pSql: String, pAutoGeneratedKeys: Int): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql, pAutoGeneratedKeys)
  }


  @Throws(SQLException::class)
  fun prepareStatement(pSql: String, pColumnIndexes: IntArray): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql, pColumnIndexes)
  }


  @Throws(SQLException::class)
  fun prepareStatement(pSql: String, pColumnNames: Array<String>): PreparedStatement {
    mCommitted = false
    return mConnection!!.prepareStatement(pSql, pColumnNames)
  }


  @Throws(SQLException::class)
  fun createClob(): Clob {
    return mConnection!!.createClob()
  }


  @Throws(SQLException::class)
  fun createBlob(): Blob {
    return mConnection!!.createBlob()
  }


  @Throws(SQLException::class)
  fun createNClob(): NClob {
    return mConnection!!.createNClob()
  }


  @Throws(SQLException::class)
  fun createSQLXML(): SQLXML {
    return mConnection!!.createSQLXML()
  }


  @Throws(SQLException::class)
  fun isValid(pTimeout: Int): Boolean {
    return mConnection!!.isValid(pTimeout)
  }


  @Throws(SQLClientInfoException::class)
  fun setClientInfo(pName: String, pValue: String) {
    mConnection!!.setClientInfo(pName, pValue)
  }


  @Throws(SQLException::class)
  fun getClientInfo(pName: String): String {
    return mConnection!!.getClientInfo(pName)
  }


  var clientInfo: Properties
    @Throws(SQLException::class)
    get() = mConnection!!.clientInfo
    @Throws(SQLClientInfoException::class)
    set(pProperties) {
      mConnection!!.clientInfo = pProperties
    }


  @Throws(SQLException::class)
  fun createStruct(pTypeName: String, pAttributes: Array<Any>): Struct {
    return mConnection!!.createStruct(pTypeName, pAttributes)
  }


  var schema: String
    @Throws(SQLException::class)
    get() = mConnection!!.schema
    @Throws(SQLException::class)
    set(pSchema) {
      mConnection!!.schema = pSchema
    }


  @Throws(SQLException::class)
  fun abort(pExecutor: Executor) {
    mConnection!!.abort(pExecutor)
  }


  @Throws(SQLException::class)
  fun setNetworkTimeout(pExecutor: Executor, pMilliseconds: Int) {
    mConnection!!.setNetworkTimeout(pExecutor, pMilliseconds)
  }


  val networkTimeout: Int
    @Throws(SQLException::class)
    get() = mConnection!!.networkTimeout


  @Throws(SQLException::class)
  override fun rollback() {
    mConnection!!.rollback(mSafePoint)

    while (rollbackHandlers.isNotEmpty()) {
      rollbackHandlers.pop().run()
    }
  }

  @Throws(SQLException::class)
  override fun commit() {
    mConnection!!.releaseSavepoint(mSafePoint)
    mConnection!!.commit()
    mSafePoint = mConnection!!.setSavepoint()
    mCommitted = true
  }

  override fun close() {
    if (mConnection != null) {
      // If commit has been called just before, this should not loose data.
      try {
        if (!mCommitted) {
          // In the case of mCommitted we know rollback is not needed.
          mConnection!!.rollback(mSafePoint)
        }
      } catch (ex: SQLException) {
        throw RuntimeException(ex)
      } finally {
        try {
          mConnection!!.close()
        } catch (ex: SQLException) {
          throw RuntimeException(ex)
        } finally {
          mConnection = null
        }
      }
    }
  }


  fun providerEquals(pDataSource: DataSource): Boolean {
    return mDataSource == pDataSource
  }


  /**
   * Helper method that will invoke commit and return the passed variable. This is just to
   * allow for prettier code.
   * @param pValue The value to return.
   * *
   * @return The passed in value.
   * *
   * @throws SQLException When commit fails.
   */
  @Throws(SQLException::class)
  override fun <T> commit(pValue: T): T {
    commit()
    return pValue
  }

  companion object {


    @Throws(SQLException::class)
    fun take(pDataSource: DataSource, pConnection: Connection): OldDBTransaction {
      return OldDBTransaction(pDataSource, pConnection)
    }
  }

}
