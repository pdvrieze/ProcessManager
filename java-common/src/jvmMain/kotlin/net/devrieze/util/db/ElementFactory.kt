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
import io.github.pdvrieze.kotlinsql.ddl.Table
import io.github.pdvrieze.kotlinsql.dml.Insert
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl.*
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertAction
import io.github.pdvrieze.kotlinsql.monadic.actions.InsertActionCommon
import io.github.pdvrieze.kotlinsql.monadic.actions.ValuelessInsertAction
import io.github.pdvrieze.kotlinsql.monadic.impl.SelectResultSetRow
import net.devrieze.util.Handle
import java.sql.SQLException


/**
 * Base interface for factories to help serialization/deserialization with a database
 *
 * @param BUILDER The type of the intermediate "Builder" that results from the [#create] step and must be transformed to T
 *                [#postCreate] step.
 * @param T       The actual type the factory works on.
 * @param TR      The transaction type to use for database interaction.
 */
interface ElementFactory<BUILDER, T : Any, in TR: MonadicDBTransaction<DB>, DB: Database> {

    val table: Table

    fun filter(select: _Where): WhereClause?

    /**
     * Get the columns that need to be selected to create an element.

     * @return The wanted columns. Note that this could be "*" for any.
     */
    val createColumns: List<Column<*, *, *>>

    /**
     * Create a new element.

     * @param transaction The connection provider to use for additional
     * *          data. This can be used to instantiate embedded collections.
     *
     * @param pResultSet The resultset (moved to the relevant row) to use.  @return A new element.
     *
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun createBuilder(transaction: TR, row: SelectResultSetRow<_ListSelect>): DBAction<DB, BUILDER>

    /**
     * Hook to allow for subsequent queries to update the intermediate.
     *
     *
     * @param transaction The connection to use
     *
     * @param builder The element that has been created.
     *
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun createFromBuilder(transaction: TR, setAccess: DbSet.DBSetAccess<BUILDER>, builder: BUILDER): DBAction<DB, T>

    /**
     * Get an SQL condition that would select the given object.
     * @param instance
     */
    fun getPrimaryKeyCondition(where: _Where, instance: T): WhereClause

    /**
     * Cast the parameter to the type of elements created by the factory.

     * @param obj The object to cast.
     *
     * @return The object as a T. If the parameter is `null` or not an
     * *         instance this function must return `null` and not throw.
     */
    fun asInstance(obj: Any): T?

    /**
     * This method is called before an element is removed. This allows for foreign key
     * constraints to be satisfied by removing elements first.
     * @param transaction The connection to use in the background
     *
     * @param element The element that is going to be removed.
     *
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preRemove(transaction: TR, element: T): DBAction<DB, Boolean>

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
    fun preRemove(transaction: TR, columns: List<Column<*, *, *>>, values: List<Any?>): DBAction<DB, Boolean>

    /**
     * This method is clear before the collection is cleared out. This allows
     * for foreign key constraints satisfaction.
     * @param transaction The connection to use.
     *
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preClear(transaction: TR): DBAction<DB, Any>

    fun insertStatement(transaction: TR): ValuelessInsertAction<DB,Insert>

    fun insertValues(
        transaction: TR,
        insert: InsertActionCommon<DB, Insert>,
        value: T
    ): InsertAction<DB, Insert>

    fun insertStatement(transaction: TR, value: T): InsertAction<DB,Insert> {
        return insertValues(transaction, insertStatement(transaction), value)
    }

    fun store(update: _UpdateBuilder, value: T)

    /**
     * Execute related statements after the element itself has been stored. This
     * allows for storage of subservent lists in the same transaction.
     * @param connection The connection to use to store the related elements.
     *
     * @param handle The new handle of the element. Note that the element itself
     *            (if it implements [Handle]) is not yet updated with the new
     *
     * @param oldValue
     *
     * @param newValue
     */
    @Throws(SQLException::class)
    fun postStore(transaction: TR, handle: Handle<T>, oldValue: T?, newValue: T): DBAction<DB, Boolean>

    val keyColumn: Column<Handle<T>, *, *>

    /**
     * Determine whether the two values are equal as far as storage is concerned. By default only on object identity
     */
    fun isEqualForStorage(oldValue: T?, newValue: T): Boolean = (oldValue === newValue)
}
