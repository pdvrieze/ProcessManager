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

import net.devrieze.util.*
import uk.ac.bournemouth.util.kotlin.sql.use

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

open class DBHandleMap<V:Any>(pTransactionFactory: TransactionFactory<out DBTransaction>, pElementFactory: DBHandleMap.HMElementFactory<V, DBTransaction>) :
      DbSet<V>(pTransactionFactory, pElementFactory), TransactionedHandleMap<V, DBTransaction> {


  private inner class TransactionIterable(private val mTransaction: DBTransaction) : Iterable<V> {

    override fun iterator(): Iterator<V> {
      return this@DBHandleMap.iterator(mTransaction, false)
    }

  }

  interface HMElementFactory<T, TR : Transaction> : ElementFactory<T, TR> {
    fun getHandleCondition(pElement: Handle<out T>): CharSequence

    @Throws(SQLException::class)
    fun setHandleParams(pStatement: PreparedStatement, pHandle: Handle<out T>, pOffset: Int): Int

    /**
     * Called before removing an element with the given handle
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preRemove(pConnection: TR, pHandle: Handle<out T>)
  }

  private val mPendingCreates = TreeMap<ComparableHandle<out V>, V>()

  protected fun isPending(handle: ComparableHandle<out V>): Boolean {
    return mPendingCreates.containsKey(handle)
  }

  override val elementFactory: HMElementFactory<V, DBTransaction>
    get() = super.elementFactory as HMElementFactory<V, DBTransaction>

  override fun newTransaction(): DBTransaction {
    return transactionFactory.startTransaction()
  }

  @Throws(SQLException::class)
  override fun <W : V> put(transaction: DBTransaction, value: W): ComparableHandle<W> {
    val result = addWithKey(transaction, value) ?: throw RuntimeException("Adding element $value failed")
    if (value is HandleMap.HandleAware<*>) {
      value.setHandleValue(result.handleValue)
    }
    return result
  }

  @Throws(SQLException::class)
  override fun get(transaction: DBTransaction, pHandle: Handle<out V>): V? {
    val handle = Handles.handle(pHandle)
    if (mPendingCreates.containsKey(handle)) {
      return mPendingCreates[handle]
    }

    val factory = elementFactory
    val sql = addFilter("SELECT " + factory.createColumns + " FROM " + factory.tableName + " WHERE (" + factory.getHandleCondition(
          pHandle) + ")", " AND ")

    transaction.connection.rawConnection.prepareStatement(sql,
                                                          ResultSet.TYPE_FORWARD_ONLY,
                                                          ResultSet.CONCUR_READ_ONLY).use { statement ->
      val cnt = factory.setHandleParams(statement, pHandle, 1)
      setFilterParams(statement, cnt)

      val result = statement.execute()
      if (!result) {
        return null
      }
      statement.getResultSet().use({ resultset ->
                                     if (resultset.first()) {
                                       factory.initResultSet(resultset.getMetaData())
                                       val `val` = factory.create(transaction, resultset)
                                       if (`val` == null) {
                                         remove(transaction, handle)
                                         return null
                                       } else {
                                         mPendingCreates.put(handle, `val`)
                                         try {
                                           factory.postCreate(transaction, `val`)
                                         } finally {
                                           mPendingCreates.remove(handle)
                                         }
                                         return `val`
                                       }
                                     } else {
                                       return null
                                     }
                                   })
    }
  }

  @Throws(SQLException::class)
  override fun castOrGet(transaction: DBTransaction, handle: Handle<out V>): V? {
    val element = elementFactory.asInstance(handle)
    if (element != null) {
      return element
    } // If the element is it's own handle, don't bother looking it up.
    return get(transaction, handle)
  }

  @Throws(SQLException::class)
  override fun set(transaction: DBTransaction, handle: Handle<out V>, value: V): V? {
    val oldValue = get(transaction, handle)

    return set(transaction, handle, oldValue, value)
  }

  @Throws(SQLException::class)
  protected operator fun set(pTransaction: DBTransaction, pHandle: Handle<out V>, oldValue: V?, pValue: V): V? {
    if (oldValue == pValue) {
      return oldValue
    }
    val sql = addFilter("UPDATE " + elementFactory.tableName + " SET " + DbSet.join(elementFactory.storeColumns,
                                                                                         elementFactory.storeParamHolders,
                                                                                         ", ",
                                                                                         " = ") + " WHERE (" + elementFactory.getHandleCondition(
          pHandle) + ")", " AND ")
    if (pValue is HandleMap.HandleAware<*>) {
      pValue.setHandleValue(pHandle.handleValue)
    }
    try {
      pTransaction.connection.rawConnection.prepareStatement(sql,
                                                             ResultSet.TYPE_FORWARD_ONLY,
                                                             ResultSet.CONCUR_READ_ONLY).use { statement ->
        var cnt = elementFactory.setStoreParams(statement, pValue, 1)
        cnt += elementFactory.setHandleParams(statement, pHandle, cnt + 1)
        setFilterParams(statement, cnt + 1)

        statement.execute()
        elementFactory.postStore(pTransaction, pHandle, oldValue, pValue)
        return oldValue
      }
    } catch (e: SQLException) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "Error executing query: " + sql, e)
      throw e
    }

  }

  override fun iterator(pTransaction: DBTransaction, pReadOnly: Boolean): AutoCloseableIterator<V> {
    try {
      return super.iterator(pTransaction, pReadOnly)
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  override fun iterable(transaction: DBTransaction): Iterable<V> {
    return TransactionIterable(transaction)
  }

  @Throws(SQLException::class)
  override fun contains(transaction: DBTransaction, element: Any): Boolean {
    if (element is Handle<*>) {
      return contains(transaction, element.handleValue)
    }
    return super.contains(transaction, element)
  }

  @Throws(SQLException::class)
  override fun containsAll(transaction: DBTransaction, c: Collection<*>): Boolean {
    for (o in c) {
      if (o==null || !contains(transaction, o)) {
        return false
      }
    }
    return true
  }

  @Throws(SQLException::class)
  override fun contains(transaction: DBTransaction, handle: Handle<out V>): Boolean {
    val sql = addFilter("SELECT COUNT(*) FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getHandleCondition(
          handle) + ")", " AND ")

    transaction.connection.rawConnection.prepareStatement(sql,
                                                          ResultSet.TYPE_FORWARD_ONLY,
                                                          ResultSet.CONCUR_READ_ONLY).use { statement ->
      val cnt = elementFactory.setHandleParams(statement, handle, 1)
      setFilterParams(statement, cnt)

      val result = statement.execute()
      if (!result) {
        return false
      }
      statement.getResultSet().use({ resultset -> return resultset.next() })
    }
  }

  @Throws(SQLException::class)
  override fun remove(transaction: DBTransaction, handle: Handle<out V>): Boolean {
    val elementFactory = elementFactory
    val connection = transaction
    elementFactory.preRemove(connection, handle)
    val sql = addFilter("DELETE FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getHandleCondition(
          handle) + ")", " AND ")

    connection.connection.rawConnection.prepareStatement(sql,
                                                         ResultSet.TYPE_FORWARD_ONLY,
                                                         ResultSet.CONCUR_READ_ONLY).use { statement ->
      val cnt = elementFactory.setHandleParams(statement, handle, 1)
      setFilterParams(statement, cnt)

      val changecount = statement.executeUpdate()
      connection.commit()
      return changecount > 0
    }

  }

  override fun invalidateCache(handle: Handle<out V>) {
    // No-op, there is no cache
  }

  override fun invalidateCache() { /* No-op, no cache */
  }

}
