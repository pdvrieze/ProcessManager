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

import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._MaybeWhere
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.DBActionReceiver
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import net.devrieze.util.Handle
import java.sql.SQLException

interface HMElementFactory<BUILDER, T : Any, in TR : MonadicDBTransaction<DB>, DB : Database> :
    ElementFactory<BUILDER, T, TR, DB> {

    fun getHandleCondition(where: _Where, handle: Handle<T>): WhereClause

    /**
     * Called before removing an element with the given handle
     * @throws SQLException When something goes wrong.
     */
    @Throws(SQLException::class)
    fun preRemove(transaction: TR, handle: Handle<T>): DBAction<DB, Boolean>

}
