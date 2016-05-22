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
import net.devrieze.util.db.DBHandleMap.HMElementFactory

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException


/**
 * Abstract baseclass for [ElementFactory] and [HMElementFactory].
 * This class provides implementations that allow optional extensions for
 * complex datatypes. Simple types do not need factories that override these.

 * @author Paul de Vrieze
 * *
 * @param  Type of the element created / handled
 */
abstract class AbstractElementFactory<T, TR : Transaction> : HMElementFactory<T, TR> {

  override fun getFilterExpression(): CharSequence? {
    return null
  }

  @Throws(SQLException::class)
  override fun setFilterParams(pStatement: PreparedStatement, pOffset: Int): Int {
    return 0
  }

  @Throws(SQLException::class)
  override fun initResultSet(pMetaData: ResultSetMetaData) {
    // Do nothing.
  }

  @Throws(SQLException::class)
  override fun postCreate(pConnection: TR, pElement: T) {
    // Do nothing.
  }

  @Throws(SQLException::class)
  override fun postStore(pConnection: TR, pHandle: Handle<out T>, pOldValue: T, pElement: T) {
    // Simple case, do nothing
  }

  @Throws(SQLException::class)
  override fun preRemove(pConnection: TR, pHandle: Handle<out T>) {
    // Don't do anything
  }

  @Throws(SQLException::class)
  override fun preRemove(pConnection: TR, pElement: T) {
    // Don't do anything
  }

  /**
   * Default implementation that just creates the element.
   */
  @Throws(SQLException::class)
  override fun preRemove(pConnection: TR, pElementSource: ResultSet) {
    preRemove(pConnection, create(pConnection, pElementSource))
  }

  @Throws(SQLException::class)
  override fun preClear(pConnection: TR) {
    // Don't do anything
  }

}
