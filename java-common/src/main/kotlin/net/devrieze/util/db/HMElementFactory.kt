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
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.sql.PreparedStatement
import java.sql.SQLException

interface HMElementFactory<T> : ElementFactory<T> {
  fun getHandleCondition(where: Database._Where,
                         handle: Handle<T>): Database.WhereClause?

  /**
   * Called before removing an element with the given handle
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun preRemove(transaction: DBTransaction, handle: Handle<out T>)


}