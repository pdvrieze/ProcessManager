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

import net.devrieze.util.Transaction
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import java.sql.Connection
import java.sql.Savepoint
import javax.sql.DataSource

/**
 * Created by pdvrieze on 21/05/16.
 */

class DBTransaction(connection: DBConnection): Transaction {
  val connection: DBConnection
    get() { return _connection?: throw IllegalStateException("Using closed connection") }
  private var _connection: DBConnection? = connection

  constructor(dataSource: DataSource, db:Database): this(DBConnection(dataSource.connection, db))

  override fun close() {
    _connection?.rawConnection?.close()
    _connection=null
  }

  override fun commit() {
    _connection?.commit() ?: throw IllegalStateException("Committing closed connection")
  }

  override fun rollback() {
    _connection?.rollback() ?: throw IllegalStateException("Rolling back closed connection")
  }

  override fun <T : Any?> commit(pValue: T): T {
    commit()
    return pValue
  }

  fun rollback(savePoint: Savepoint) {
    connection.rollback(savePoint)
  }

  val isClosed: Boolean get() = _connection.let{ it ==null || it.isClosed() }
}