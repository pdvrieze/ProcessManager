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
import java.sql.PreparedStatement
import java.sql.SQLException

interface OldHMElementFactory<T, TR : Transaction> : OldElementFactory<T, TR> {
  fun getHandleCondition(pElement: Handle<out T>): CharSequence

  @Throws(SQLException::class)
  fun setHandleParams(pStatement: PreparedStatement, pHandle: Handle<out T>, pOffset: Int): Int

  /**
   * Called before removing an element with the given handle
   * @throws SQLException When something goes wrong.
   */
  @Throws(SQLException::class)
  fun preRemove(pConnection: TR, pHandle: Handle<out T>)
}