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
import net.devrieze.util.db.DBHandleMap.HMElementFactory
import net.devrieze.util.db.DbSet.Companion.join
import uk.ac.bournemouth.util.kotlin.sql.use

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

@Deprecated("Use DBHandleMap")
open class OldDBHandleMap<V:Any>(pTransactionFactory: TransactionFactory<OldDBTransaction>, pElementFactory: HMElementFactory<V, OldDBTransaction>) :
      OldDbSet<V>(pTransactionFactory, pElementFactory), OldTransactionedHandleMap<V, OldDBTransaction> {


  private inner class TransactionIterable(private val mTransaction: OldDBTransaction) : MutableIterable<V> {

    override fun iterator(): MutableIterator<V> {
      return this@OldDBHandleMap.iterator(mTransaction, false)
    }

  }

  private val mPendingCreates = TreeMap<ComparableHandle<out V>, V>()

  protected fun isPending(handle: ComparableHandle<out V>): Boolean {
    return mPendingCreates.containsKey(handle)
  }

  override val elementFactory:HMElementFactory<V, OldDBTransaction> get() {
    return super.elementFactory as HMElementFactory<V, OldDBTransaction>
  }

  override fun newTransaction(): OldDBTransaction {
    return transactionFactory.startTransaction()
  }

  override fun <W : V> put(value: W): Handle<W> {
    try {
      transactionFactory.startTransaction().use { transaction ->
        val result = put(transaction, value)
        transaction.commit()
        return result
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  override fun <W : V> put(transaction: OldDBTransaction, value: W): ComparableHandle<W> {
    val result = addWithKey(transaction, value)
    if (value is HandleMap.HandleAware<*>) {
      value.setHandleValue(result.handleValue)
    }
    return result
  }

  override fun get(pHandle: Long): V? {
    try {
      transactionFactory.startTransaction().use { transaction ->
        return get(transaction, pHandle)
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  override fun get(transaction: OldDBTransaction, pHandle: Handle<out V>): V? {
    val handle = Handles.handle(pHandle)
    if (mPendingCreates.containsKey(handle)) {
      return mPendingCreates[handle]
    }

    val elementFactory = elementFactory
    val sql = addFilter("SELECT " + elementFactory.createColumns + " FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getHandleCondition(
          pHandle) + ")", " AND ")

    transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
      val cnt = this.elementFactory.setHandleParams(statement, pHandle, 1)
      setFilterParams(statement, cnt)

      val result = statement.execute()
      if (!result) {
        return null
      }
      statement.getResultSet().use({ resultset ->
                                     if (resultset.first()) {
                                       elementFactory.initResultSet(resultset.getMetaData())
                                       val `val` = elementFactory.create(transaction, resultset)
                                       if (`val` == null) {
                                         remove(transaction, handle)
                                         return null
                                       } else {
                                         mPendingCreates.put(handle, `val`)
                                         try {
                                           elementFactory.postCreate(transaction, `val`)
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

  override fun get(handle: Handle<out V>): V? {
    val element = elementFactory.asInstance(handle)
    if (element != null) {
      return element
    } // If the element is it's own handle, don't bother looking it up.
    return get(handle.handleValue)
  }


  @Throws(SQLException::class)
  override fun get(transaction: OldDBTransaction, handle: Long) =
        get(transaction, Handles.handle<V>(handle)!!)

  @Throws(SQLException::class)
  override fun castOrGet(transaction: OldDBTransaction, handle: Handle<out V>): V? {
    val element = elementFactory.asInstance(handle)
    if (element != null) {
      return element
    } // If the element is it's own handle, don't bother looking it up.
    return get(transaction, handle)
  }

  override fun set(handle: Long, value: V): V? {
    try {
      transactionFactory.startTransaction().use { transaction ->
        return transaction.commit(set(transaction, handle, value))
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  override fun set(transaction: OldDBTransaction, handle: Handle<out V>, value: V): V? {
    val oldValue = get(transaction, handle)

    return set(transaction, handle, oldValue, value)
  }

  @Throws(SQLException::class)
  protected operator fun set(pTransaction: OldDBTransaction,
                             pHandle: Handle<out V>,
                             oldValue: V?,
                             pValue: V): V? {
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
      pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
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

  override fun set(handle: Handle<out V>, value: V): V? {
    return set(handle.handleValue, value)
  }

  @Throws(SQLException::class)
  override fun set(transaction: OldDBTransaction, handle: Long, value: V): V? {
    return set(transaction, Handles.handle<V>(handle), value)
  }

  @Deprecated("This method maps to {@link #unsafeIterator()}. It does not automatically take care of closing the database connection if the iterator is not finished.")
  override fun iterator(): MutableIterator<V> {
    return unsafeIterator(false)
  }

  override fun iterator(pTransaction: OldDBTransaction, pReadOnly: Boolean): AutoCloseableIterator<V> {
    try {
      return super.iterator(pTransaction, pReadOnly)
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  override fun iterable(transaction: OldDBTransaction): MutableIterable<V> {
    return TransactionIterable(transaction)
  }


  override fun containsHandle(handle: Handle<out V>): Boolean {
    return contains(handle.handleValue)
  }

  @Throws(SQLException::class)
  override fun contains(transaction: OldDBTransaction, element: Any): Boolean {
    if (element is Handle<*>) {
      return contains(transaction, element.handleValue)
    }
    return super.contains(transaction, element)
  }

  operator fun contains(pHandle: Handle<out V>): Boolean {
    return contains(pHandle.handleValue)
  }

  @Throws(SQLException::class)
  override fun containsAll(transaction: OldDBTransaction, c: Collection<Any>): Boolean {
    for (o in c) {
      if (o==null || !contains(transaction, o)) {
        return false
      }
    }
    return true
  }

  @Throws(SQLException::class)
  override fun contains(transaction: OldDBTransaction, handle: Long): Boolean {
    return contains(transaction, Handles.handle<V>(handle)!!)
  }

  override fun contains(handle: Long): Boolean {
    try {
      transactionFactory.startTransaction().use { transaction ->
        val result = contains(transaction, handle)
        transaction.commit()
        return result
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  override fun contains(transaction: OldDBTransaction, handle: Handle<out V>): Boolean {
    val sql = addFilter("SELECT COUNT(*) FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getHandleCondition(
          handle) + ")", " AND ")

    transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
      val cnt = elementFactory.setHandleParams(statement, handle, 1)
      setFilterParams(statement, cnt)

      val result = statement.execute()
      if (!result) {
        return false
      }
      statement.getResultSet().use({ resultset -> return resultset.next() })
    }
  }

  override fun remove(handle: Handle<out V>): Boolean {
    transactionFactory.startTransaction().use { transaction ->
      return remove(transaction, handle)
    }
  }

  @Throws(SQLException::class)
  override fun remove(transaction: OldDBTransaction, handle: Long): Boolean {
    return remove(transaction, Handles.handle<V>(handle))
  }

  @Throws(SQLException::class)
  override fun remove(transaction: OldDBTransaction, handle: Handle<out V>): Boolean {
    val elementFactory = elementFactory
    val connection = transaction
    this.elementFactory.preRemove(connection, handle)
    val sql = addFilter("DELETE FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getHandleCondition(
          handle) + ")", " AND ")

    connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
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
