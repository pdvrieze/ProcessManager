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

import net.devrieze.util.Handle
import net.devrieze.util.Transaction

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException


@Deprecated("")
interface OldElementFactory<T, TR : Transaction> {

    val tableName: CharSequence

    /**
     * Get a condition expression (in SQL) that is used to filter the elements.
     *
     * @return The expression as SQL. This must not include the WHERE clause.
     */
    val filterExpression: CharSequence

    /**
     * Get the columns that need to be selected to create an element.
     *
     * @return The wanted columns. Note that this could be "*" for any.
     */
    val createColumns: CharSequence

    /**
     * Get the columns for storing an element
     * @return The element to store.
     */
    val storeColumns: List<CharSequence>

    /**
     * Get the holders for the create parameters. This in general is a list of
     * comma separated question marks, but could contain constants.
     *
     * @return The text to use inside VALUES ( ... ) for the prepared statements.
     */
    val storeParamHolders: List<CharSequence>

    /**
     * The filter will be created as prepared statement. This function is called
     * to set the parameters.
     *
     * @param statement The statement to set the parameters on.
     * @param offset The offset to use.
     * @return the amount of parameters set.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun setFilterParams(statement: PreparedStatement, offset: Int): Int

    /**
     * Called before processing a resultset. This gives the factory the chance to
     * cache column numbers.
     *
     * @param metaData The metadata to use. `null` to force
     * forgetting.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun initResultSet(metaData: ResultSetMetaData)

    /**
     * Create a new element.
     *
     * @param transaction The connection provider to use for additional
     * data. This can be used to instantiate embedded collections.
     * @param resultSet The resultset (moved to the relevant row) to use.  @return A new element.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun create(transaction: TR, resultSet: ResultSet): T

    /**
     * Hook to allow for subsequent queries
     * @param connection The connection to use
     * @param element The element that has been created.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun postCreate(connection: TR, element: T)

    /**
     * Get an SQL condition that would select the given object.
     * @param obj
     */
    fun getPrimaryKeyCondition(obj: T): CharSequence

    /**
     * Set the primary key parameters related to [.getPrimaryKeyCondition]
     * @param statement The statement to set the parameters for.
     * @param obj The object that resolves the condition
     * @param offset the index of the first parameter. This is used to chain placeholders.  @return The amount of parameters set.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun setPrimaryKeyParams(statement: PreparedStatement, obj: T, offset: Int): Int

    /**
     * Cast the parameter to the type of elements created by the factory.
     *
     * @param obj The object to cast.
     * @return The object as a T. If the parameter is `null` or not an
     * instance this function must return `null` and not throw.
     */
    fun asInstance(obj: Any): T

    /**
     * Set the parameter values for the placeholders returned by
     * [.getStoreParamHolders]. Note that for repeated addition this may
     * be called repeatedly for one call to the paramholders query.
     *
     * @param pStatement The statement for which to set the parameters.
     * @param pElement The element to store.
     * @param pOffset The offset of the first parameter.
     * @return The amount of parameters set.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun setStoreParams(pStatement: PreparedStatement, pElement: T, pOffset: Int): Int

    /**
     * This method is called before an element is removed. This allows for foreign key
     * constraints to be satisfied by removing elements first.
     * @param pConnection The connection to use in the background
     * @param pElement The element that is going to be removed.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preRemove(pConnection: TR, pElement: T)

    /**
     * This method is called before an element is removed. This method
     * is particularly designed to support removeAll without creating temporary
     * elements.
     *
     * @param pConnection The connection to use in the background
     * @param pElementSource The resultset that would be passed to [.create]
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preRemove(pConnection: TR, pElementSource: ResultSet)

    /**
     * This method is clear before the collection is cleared out. This allows
     * for foreign key constraints satisfaction.
     * @param pConnection The connection to use.
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preClear(pConnection: TR)

    /**
     * Execute related statements after the element itself has been stored. This
     * allows for storage of subservent lists in the same transaction.
     * @param pConnection The connection to use to store the related elements.
     * @param handle The new handle of the element. Note that the element itself
     * (if it implements [Handle]) is not yet updated with the new
     * @param oldValue
     * @param newValue
     */
    @Throws(SQLException::class)
    fun postStore(pConnection: TR, handle: Handle<T>, oldValue: T, newValue: T)
}
