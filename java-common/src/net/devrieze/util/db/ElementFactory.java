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

package net.devrieze.util.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import net.devrieze.util.HandleMap.Handle;


public interface ElementFactory<T> {

  CharSequence getTableName();

  /**
   * Get a condition expression (in SQL) that is used to filter the elements.
   *
   * @return The expression as SQL. This must not include the WHERE clause.
   */
  CharSequence getFilterExpression();

  /**
   * The filter will be created as prepared statement. This function is called
   * to set the parameters.
   *
   * @param pStatement The statement to set the parameters on.
   * @param pOffset The offset to use.
   * @return the amount of parameters set.
   * @throws SQLException When something goes wrong.
   */
  int setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException;

  /**
   * Called before processing a resultset. This gives the factory the chance to
   * cache column numbers.
   *
   * @param pMetaData The metadata to use. <code>null</code> to force
   *          forgetting.
   * @throws SQLException When something goes wrong.
   */
  void initResultSet(ResultSetMetaData pMetaData) throws SQLException;

  /**
   * Get the columns that need to be selected to create an element.
   *
   * @return The wanted columns. Note that this could be "*" for any.
   */
  CharSequence getCreateColumns();

  /**
   * Create a new element.
   *
   * @param transaction The connection provider to use for additional
   *          data. This can be used to instantiate embedded collections.
   * @param pResultSet The resultset (moved to the relevant row) to use.  @return A new element.
   * @throws SQLException When something goes wrong.
   */
  T create(DBTransaction transaction, ResultSet pResultSet) throws SQLException;

  /**
   * Hook to allow for subsequent queries
   * @param pConnection The connection to use
   * @param pElement The element that has been created.
   * @throws SQLException When something goes wrong.
   */
  void postCreate(DBTransaction pConnection, T pElement) throws SQLException;

  /**
   * Get an SQL condition that would select the given object.
   * @param pObject
   */
  CharSequence getPrimaryKeyCondition(T pObject);

  /**
   * Set the primary key parameters related to {@link #getPrimaryKeyCondition(Object)}
   * @param pStatement The statement to set the parameters for.
   * @param pObject The object that resolves the condition
   * @param pOffset the index of the first parameter. This is used to chain placeholders.  @return The amount of parameters set.
   * @throws SQLException When something goes wrong.
   */
  int setPrimaryKeyParams(PreparedStatement pStatement, T pObject, int pOffset) throws SQLException;

  /**
   * Cast the parameter to the type of elements created by the factory.
   *
   * @param pO The object to cast.
   * @return The object as a T. If the parameter is <code>null</code> or not an
   *         instance this function must return <code>null</code> and not throw.
   */
  T asInstance(Object pO);

  /**
   * Get the columns for storing an element
   * @return The element to store.
   */
  List<CharSequence> getStoreColumns();

  /**
   * Get the holders for the create parameters. This in general is a list of
   * comma separated question marks, but could contain constants.
   *
   * @return The text to use inside VALUES ( ... ) for the prepared statements.
   */
  List<CharSequence> getStoreParamHolders();

  /**
   * Set the parameter values for the placeholders returned by
   * {@link #getStoreParamHolders()}. Note that for repeated addition this may
   * be called repeatedly for one call to the paramholders query.
   *
   * @param pStatement The statement for which to set the parameters.
   * @param pElement The element to store.
   * @param pOffset The offset of the first parameter.
   * @return The amount of parameters set.
   * @throws SQLException When something goes wrong.
   */
  int setStoreParams(PreparedStatement pStatement, T pElement, int pOffset) throws SQLException;

  /**
   * This method is called before an element is removed. This allows for foreign key
   * constraints to be satisfied by removing elements first.
   * @param pConnection The connection to use in the background
   * @param pElement The element that is going to be removed.
   * @throws SQLException When something goes wrong.
   */
  void preRemove(DBTransaction pConnection, T pElement) throws SQLException;

  /**
   * This method is called before an element is removed. This method
   * is particularly designed to support removeAll without creating temporary
   * elements.
   *
   * @param pConnection The connection to use in the background
   * @param pElementSource The resultset that would be passed to {@link #create(DBTransaction, ResultSet)}
   * @throws SQLException When something goes wrong.
   */
  void preRemove(DBTransaction pConnection, ResultSet pElementSource) throws SQLException;

  /**
   * This method is clear before the collection is cleared out. This allows
   * for foreign key constraints satisfaction.
   * @param pConnection The connection to use.
   * @throws SQLException When something goes wrong.
   */
  void preClear(DBTransaction pConnection) throws SQLException;

  /**
   * Execute related statements after the element itself has been stored. This
   * allows for storage of subservent lists in the same transaction.
   *  @param pConnection The connection to use to store the related elements.
   * @param handle The new handle of the element. Note that the element itself
   *          (if it implements {@link Handle}) is not yet updated with the new
   * @param oldValue
   * @param newValue
   */
  void postStore(DBTransaction pConnection, Handle<? extends T> handle, T oldValue, T newValue) throws SQLException;
}
