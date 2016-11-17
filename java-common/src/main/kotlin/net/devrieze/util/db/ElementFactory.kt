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

import net.devrieze.util.Handle
import net.devrieze.util.Transaction
import uk.ac.bournemouth.kotlinsql.*
import uk.ac.bournemouth.util.kotlin.sql.DBConnection

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException


interface ElementFactory<T:Any> {

  val table: Table

  fun filter(select: Database._Where): Database.WhereClause?

  /**
   * Get the columns that need to be selected to create an element.

   * @return The wanted columns. Note that this could be "*" for any.
   */
  val createColumns: List<Column<*, *, *>>

  /**
   * Create a new element.

   * @param transaction The connection provider to use for additional
   * *          data. This can be used to instantiate embedded collections.
   * *
   * @param pResultSet The resultset (moved to the relevant row) to use.  @return A new element.
   * *
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun create(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>): T

  /**
   * Hook to allow for subsequent queries
   * @param transaction The connection to use
   * *
   * @param element The element that has been created.
   * *
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun postCreate(transaction: DBTransaction, element: T)

  /**
   * Get an SQL condition that would select the given object.
   * @param instance
   */
  fun getPrimaryKeyCondition(where: Database._Where, instance: T): Database.WhereClause?

  /**
   * Cast the parameter to the type of elements created by the factory.

   * @param obj The object to cast.
   * *
   * @return The object as a T. If the parameter is `null` or not an
   * *         instance this function must return `null` and not throw.
   */
  fun asInstance(obj: Any): T?

  /**
   * This method is called before an element is removed. This allows for foreign key
   * constraints to be satisfied by removing elements first.
   * @param transaction The connection to use in the background
   * *
   * @param element The element that is going to be removed.
   * *
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun preRemove(transaction: DBTransaction, element: T)

  /**
   * This method is called before an element is removed. This method
   * is particularly designed to support removeAll without creating temporary
   * elements.

   * @param transaction The connection to use in the background
   *
   * @param columns The columns in the values. This should be the same as the value of [createColumns]
   * @param values The values for the columns
   *
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun preRemove(transaction: DBTransaction, columns:List<Column<*,*,*>>, values: List<Any?>)

  /**
   * This method is clear before the collection is cleared out. This allows
   * for foreign key constraints satisfaction.
   * @param transaction The connection to use.
   * *
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun preClear(transaction: DBTransaction)

  fun insertStatement(value:T): Database.Insert

  fun store(update: Database._UpdateBuilder, value: T)

  /**
   * Execute related statements after the element itself has been stored. This
   * allows for storage of subservent lists in the same transaction.
   * @param connection The connection to use to store the related elements.
   * *
   * @param handle The new handle of the element. Note that the element itself
   * *          (if it implements [Handle]) is not yet updated with the new
   * *
   * @param oldValue
   * *
   * @param newValue
   */
  @Throws(SQLException::class)
  fun postStore(connection: DBConnection, handle: Handle<out T>, oldValue: T?, newValue: T)

  val keyColumn: Column<Long, ColumnType.NumericColumnType.BIGINT_T, *>
}
