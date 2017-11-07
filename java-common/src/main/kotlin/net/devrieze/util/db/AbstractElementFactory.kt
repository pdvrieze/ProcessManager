/*
 * Copyright (c) 2017.
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

import net.devrieze.util.Handle
import uk.ac.bournemouth.kotlinsql.Column
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.kotlinsql.IColumnType
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.sql.SQLException


/**
 * Abstract baseclass for [ElementFactory] and [HMElementFactory].
 * This class provides implementations that allow optional extensions for
 * complex datatypes. Simple types do not need factories that override these.

 * @author Paul de Vrieze
 * *
 * @param  T of the element created / handled
 */
abstract class AbstractElementFactory<BUILDER, T:Any, TR:DBTransaction> : HMElementFactory<BUILDER, T, TR> {

  companion object {

    fun <T, S: IColumnType<T, S, C>, C:Column<T,S,C>> C.nullableValue(columns:List<Column<*,*,*>>, values:List<Any?>):T? {
      return values[columns.checkedIndexOf(this)]?.let{ type.cast(it) }
    }

    fun <T, S: IColumnType<T, S, C>, C:Column<T,S,C>> C.value(columns:List<Column<*,*,*>>, values:List<Any?>):T {
      return type.cast(values[columns.checkedIndexOf(this)]!!)
    }

    fun List<Column<*,*,*>>.checkedIndexOf(column:Column<*,*,*>): Int {
      return indexOf(column).also {
        if (it<0) throw SQLException("Column $column not found in $this")
      }
    }

  }

  override fun filter(select: Database._Where) = null

  @Throws(SQLException::class)
  override fun postStore(connection: DBConnection, handle: Handle<T>, oldValue: T?, newValue: T) {
    // Simple case, do nothing
  }

  @Throws(SQLException::class)
  override fun preRemove(transaction: TR, handle: Handle<T>) {
    // Don't do anything
  }

  @Throws(SQLException::class)
  override fun preRemove(transaction: TR, element: T) {
    // Don't do anything
  }

  override fun preRemove(transaction: TR, columns: List<Column<*, *, *>>, values: List<Any?>) {
    // Don't do anything
  }

  @Throws(SQLException::class)
  override fun preClear(transaction: TR) {
    // Don't do anything
  }

}
