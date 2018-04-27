/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package net.devrieze.util.db

import net.devrieze.util.*
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.getSingleList
import uk.ac.bournemouth.kotlinsql.getSingleListOrNull
import java.sql.SQLException
import java.util.*

open class DBHandleMap<TMP, V:Any, TR:DBTransaction>(
      transactionFactory: TransactionFactory<out TR>,
      database: Database,
      elementFactory: HMElementFactory<TMP, V, TR>,
      handleAssigner: (V, Handle<V>)->V? = ::HANDLE_AWARE_ASSIGNER) :
      DbSet<TMP, V, TR>(transactionFactory, database, elementFactory, handleAssigner), MutableTransactionedHandleMap<V, TR> {


  private inner class TransactionIterable(private val mTransaction: TR) : MutableIterable<V> {

    override fun iterator(): MutableIterator<V> {
      return this@DBHandleMap.iterator(mTransaction, false)
    }

  }

  private val mPendingCreates = TreeMap<ComparableHandle<V>, TMP>()

  protected fun isPending(handle: ComparableHandle<V>): Boolean {
    return mPendingCreates.containsKey(handle)
  }


  fun  pendingValue(handle: ComparableHandle<V>): TMP? {
    return mPendingCreates[handle]
  }


  override val elementFactory: HMElementFactory<TMP, V, TR>
    get() = super.elementFactory as HMElementFactory<TMP, V, TR>

  @Throws(SQLException::class)
  override fun <W : V> put(transaction: TR, value: W): ComparableHandle<W> {
    return addWithKey(transaction, value) ?: throw RuntimeException("Adding element $value failed")
  }

  @Throws(SQLException::class)
  override fun get(transaction: TR, handle: Handle<V>): V? {
    val comparableHandle = Handles.handle(handle)
    if (mPendingCreates.containsKey(comparableHandle)) {
      throw IllegalArgumentException("Pending create") // XXX This is not the best way
//      return mPendingCreates[comparableHandle]
    }

    val factory = elementFactory
    val result = database
          .SELECT(factory.createColumns)
          .WHERE { factory.getHandleCondition(this, comparableHandle) AND factory.filter(this) }
          .getSingleListOrNull(transaction.connection) { columns, values ->
            elementFactory.create(transaction, columns, values)
          } ?: return null
    mPendingCreates.put(comparableHandle, result)
    try {
      return factory.postCreate(transaction, result)
    } finally {
      mPendingCreates.remove(comparableHandle)
    }
  }

  @Throws(SQLException::class)
  override fun castOrGet(transaction: TR, handle: Handle<V>): V? {
    val element = elementFactory.asInstance(handle)
    if (element != null) {
      return element
    } // If the element is it's own handle, don't bother looking it up.
    return get(transaction, handle)
  }

  @Throws(SQLException::class)
  override fun set(transaction: TR, handle: Handle<V>, value: V): V? {
    val oldValue = get(transaction, handle)

    return set(transaction, handle, oldValue, value)
  }

  @Throws(SQLException::class)
  protected operator fun set(transaction: TR, handle: Handle<V>, oldValue: V?, newValue: V): V? {
    if (elementFactory.isEqualForStorage(oldValue, newValue)) {
      return newValue
    }

    val newValueWithHandle = handleAssigner(newValue, handle) ?: newValue

    database
          .UPDATE { elementFactory.store(this, newValueWithHandle) }
          .WHERE { elementFactory.filter(this) AND elementFactory.getHandleCondition(this, handle) }
          .executeUpdate(transaction.connection)
    elementFactory.postStore(transaction.connection, handle, oldValue, newValueWithHandle)
    return oldValue
  }

  override fun iterator(transaction: TR, readOnly: Boolean): MutableAutoCloseableIterator<V> {
    try {
      return super.iterator(transaction, readOnly)
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  override fun iterable(transaction: TR): MutableIterable<V> {
    return TransactionIterable(transaction)
  }

  @Throws(SQLException::class)
  override fun containsElement(transaction: TR, element: Any): Boolean {
    if (element is Handle<*>) {
      return containsElement(transaction, element.handleValue)
    }
    return super.contains(transaction, element)
  }

  @Throws(SQLException::class)
  override fun containsAll(transaction: TR, c: Collection<*>): Boolean {
    for (o in c) {
      if (o==null || !containsElement(transaction, o)) {
        return false
      }
    }
    return true
  }

  @Throws(SQLException::class)
  override fun contains(transaction: TR, handle: Handle<V>): Boolean {
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
  override fun remove(transaction: TR, handle: Handle<V>): Boolean {
    elementFactory.preRemove(transaction, handle)
    return database
          .DELETE_FROM(elementFactory.table)
          .WHERE { elementFactory.getHandleCondition(this, handle) AND elementFactory.filter(this) }
          .executeUpdate(transaction.connection)>0
  }

  override fun invalidateCache(handle: Handle<V>) =// No-op, there is no cache
    Unit

  override fun invalidateCache() { /* No-op, no cache */
  }

}
