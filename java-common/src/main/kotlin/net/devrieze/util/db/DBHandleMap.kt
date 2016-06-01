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
import uk.ac.bournemouth.kotlinsql.Database
import java.sql.SQLException
import java.util.*

open class DBHandleMap<V:Any>(transactionFactory: TransactionFactory<out DBTransaction>, database: Database, elementFactory: HMElementFactory<V>) :
      DbSet<V>(transactionFactory, database, elementFactory), TransactionedHandleMap<V, DBTransaction> {


  private inner class TransactionIterable(private val mTransaction: DBTransaction) : Iterable<V> {

    override fun iterator(): Iterator<V> {
      return this@DBHandleMap.iterator(mTransaction, false)
    }

  }

  private val mPendingCreates = TreeMap<ComparableHandle<out V>, V>()

  protected fun isPending(handle: ComparableHandle<out V>): Boolean {
    return mPendingCreates.containsKey(handle)
  }

  override val elementFactory: HMElementFactory<V>
    get() = super.elementFactory as HMElementFactory<V>

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
  override fun get(transaction: DBTransaction, handle: Handle<V>): V? {
    val comparableHandle = Handles.handle(handle)
    if (mPendingCreates.containsKey(comparableHandle)) {
      return mPendingCreates[comparableHandle]
    }

    val factory = elementFactory
    val result = database
          .SELECT(factory.createColumns)
          .WHERE { factory.getHandleCondition(this, comparableHandle) AND factory.filter(this) }
          .getSingleList(transaction.connection) { columns, values ->
            elementFactory.create(transaction, columns, values)
          }
    mPendingCreates.put(comparableHandle, result)
    try {
      factory.postCreate(transaction, result)
    } finally {
      mPendingCreates.remove(comparableHandle)
    }
    return result
  }

  @Throws(SQLException::class)
  override fun castOrGet(transaction: DBTransaction, handle: Handle<V>): V? {
    val element = elementFactory.asInstance(handle)
    if (element != null) {
      return element
    } // If the element is it's own handle, don't bother looking it up.
    return get(transaction, handle)
  }

  @Throws(SQLException::class)
  override fun set(transaction: DBTransaction, handle: Handle<V>, value: V): V? {
    val oldValue = get(transaction, handle)

    return set(transaction, handle, oldValue, value)
  }

  @Throws(SQLException::class)
  protected operator fun set(transaction: DBTransaction, handle: Handle<V>, oldValue: V?, newValue: V): V? {
    if (oldValue == newValue) {
      return oldValue
    }

    if (newValue is HandleMap.HandleAware<*>) newValue.setHandleValue(handle.handleValue)

    database
          .UPDATE { elementFactory.store(this, newValue) }
          .WHERE { elementFactory.filter(this) AND elementFactory.getHandleCondition(this, handle) }
          .executeUpdate(transaction.connection)
    elementFactory.postStore(transaction.connection, handle, oldValue, newValue)
    return oldValue
  }

  override fun iterator(transaction: DBTransaction, readOnly: Boolean): AutoCloseableIterator<V> {
    try {
      return super.iterator(transaction, readOnly)
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
  override fun contains(transaction: DBTransaction, handle: Handle<V>): Boolean {
    val query = database
          .SELECT(database.COUNT(elementFactory.createColumns[0]))
          .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }

    try {
      return query.getSingleList(transaction.connection) { cols, data ->
        data[0] as Int > 0
      }
    } catch (e:RuntimeException) {
      return false
    }
  }

  @Throws(SQLException::class)
  override fun remove(transaction: DBTransaction, handle: Handle<V>): Boolean {
    elementFactory.preRemove(transaction, handle)
    return database
          .DELETE_FROM(elementFactory.table)
          .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }
          .executeUpdate(transaction.connection)>0
  }

  override fun invalidateCache(handle: Handle<V>) {
    // No-op, there is no cache
  }

  override fun invalidateCache() { /* No-op, no cache */
  }

}
