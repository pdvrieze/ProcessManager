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
import net.devrieze.util.db.OldHMElementFactory
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.IColumnType
import uk.ac.bournemouth.util.kotlin.sql.DBConnection

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException


/**
 * Abstract baseclass for [OldElementFactory] and [OldHMElementFactory].
 * This class provides implementations that allow optional extensions for
 * complex datatypes. Simple types do not need factories that override these.

 * @author Paul de Vrieze
 * *
 * @param  Type of the element created / handled
 */
abstract class AbstractElementFactory<T> : HMElementFactory<T> {

  companion object {

    inline fun <T, S: IColumnType<T, S, C>, C:Column<T,S,C>> C.value(columns:List<Column<*,*,*>>, values:List<Any?>):T? {
      return values[columns.indexOf(this)]?.let{ type.cast(it) }
    }

  }

  override fun filter(select: Database._Where) = null

  @Throws(SQLException::class)
  override open fun postCreate(connection: DBConnection, element: T) {
    // Do nothing.
  }

  @Throws(SQLException::class)
  override open fun postStore(connection: DBConnection, handle: Handle<out T>, oldValue: T?, newValue: T) {
    // Simple case, do nothing
  }

  @Throws(SQLException::class)
  override open fun preRemove(connection: DBConnection, handle: Handle<out T>) {
    // Don't do anything
  }

  @Throws(SQLException::class)
  override fun preRemove(connection: DBConnection, element: T) {
    // Don't do anything
  }

  override fun preRemove(transaction: DBTransaction, columns: List<Column<*, *, *>>, values: List<Any?>) {
    preRemove(transaction.connection, create(transaction, columns, values))
  }

  @Throws(SQLException::class)
  override fun preClear(connection: DBConnection) {
    // Don't do anything
  }

}
