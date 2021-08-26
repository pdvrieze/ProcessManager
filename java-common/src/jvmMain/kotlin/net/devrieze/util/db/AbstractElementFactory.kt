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

import io.github.pdvrieze.kotlinsql.ddl.Column
import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.ddl.IColumnType
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._ListSelect
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.Handle
import java.sql.SQLException


/**
 * Abstract baseclass for [ElementFactory] and [HMElementFactory].
 * This class provides implementations that allow optional extensions for
 * complex datatypes. Simple types do not need factories that override these.

 * @author Paul de Vrieze
 *
 * @param  T of the element created / handled
 */
abstract class AbstractElementFactory<BUILDER, T : Any, in TR: MonadicDBTransaction<DB> , DB : Database> : HMElementFactory<BUILDER, T, TR, DB> {

    companion object {

        fun <T: Any, S : IColumnType<T, S, C>, C : Column<T, S, C>> C.nullableValue(
            row: SelectResultSetRow<_ListSelect>
        ): T? {
            return row.value(this, row.metaData.columnIdx(this))
        }

        fun <T, S : IColumnType<T, S, C>, C : Column<T, S, C>> C.nullableValue(
            columns: List<Column<*, *, *>>,
            values: List<Any?>
        ): T? {
            return values[columns.checkedIndexOf(this)]?.let { type.cast(it) }
        }

        fun <T, S : IColumnType<T, S, C>, C : Column<T, S, C>> C.value(
            columns: List<Column<*, *, *>>,
            values: List<Any?>
        ): T {
            return type.cast(values[columns.checkedIndexOf(this)]!!)
        }

        fun <T: Any, S : IColumnType<T, S, C>, C : Column<T, S, C>> C.value(
            row: SelectResultSetRow<_ListSelect>
        ): T {
            return row.value(this, row.metaData.columnIdx(this))!!
        }

        fun List<Column<*, *, *>>.checkedIndexOf(column: Column<*, *, *>): Int {
            return indexOf(column).also {
                if (it < 0) throw SQLException("Column $column not found in $this")
            }
        }

    }

    override fun filter(select: _Where): WhereClause? = null

    override fun postStore(
        transaction: TR,
        handle: Handle<T>,
        oldValue: T?,
        newValue: T
    ): DBAction<DB, Boolean> {
        return transaction.value(true)
    }

    override fun preRemove(transaction: TR, handle: Handle<T>): DBAction<DB, Boolean> {
        // Don't do anything
        return transaction.value(true)
    }

    override fun preRemove(transaction: TR, element: T): DBAction<DB, Boolean> {
        // Don't do anything
        return transaction.value(false)
    }

    override fun preRemove(
        transaction: TR,
        columns: List<Column<*, *, *>>,
        values: List<Any?>
    ): DBAction<DB, Boolean> {
        // Don't do anything
        return transaction.value(false)
    }

    override fun preClear(transaction: TR): DBAction<DB, Any> {
        // Don't do anything
        return transaction.value(Unit)
    }

}
