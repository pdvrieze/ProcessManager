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

/**
 * Created by pdvrieze on 01/04/16.
 */

package uk.ac.bournemouth.ac.db.darwin.usertasks

import org.testng.Assert.*
import org.testng.annotations.Test
import uk.ac.bournemouth.kotlinsql.*
import uk.ac.bournemouth.kotlinsql.ColumnType.*
import uk.ac.bournemouth.kotlinsql.ColumnType.NumericColumnType.*
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource

class UserTasksTest {


  object myDataSource: DataSource {
    const val JDBCURL = "jdbc:mysql://localhost/test"
    const val USERNAME="test"
    const val PASSWORD="DAGHYbH6Wb"

    private var _logWriter: PrintWriter? = PrintWriter(OutputStreamWriter(System.err))

    private var _loginTimeout:Int = 0

    override fun setLogWriter(out: PrintWriter?) { _logWriter = out }

    override fun setLoginTimeout(seconds: Int) { _loginTimeout = seconds }

    override fun getParentLogger() = Logger.getAnonymousLogger()

    override fun getLogWriter() = _logWriter

    override fun getLoginTimeout() = _loginTimeout

    override fun isWrapperFor(iface: Class<*>) = iface.isInstance(this)

    override fun <T : Any> unwrap(iface: Class<T>): T {
      if (! iface.isInstance(this)) throw SQLException("Interface not implemented")
      return iface.cast(this)
    }

    override fun getConnection(): Connection {
      return DriverManager.getConnection(JDBCURL, USERNAME, PASSWORD)
    }

    override fun getConnection(username: String?, password: String?): Connection {
      return DriverManager.getConnection(JDBCURL, username, password)
    }

  }


  @Test
  fun verifyTableCount() {
    assertEquals(UserTaskDB._tables.size, 2)
  }

  @Test(dependsOnMethods = arrayOf("verifyTableCount"))
  fun verifyTablesRecorded() {
    assertTrue(UserTaskDB._tables.contains<Table>(UserTaskDB.usertasks))
    assertTrue(UserTaskDB._tables.contains<Table>(UserTaskDB.nodedata))
  }

  @Test(dependsOnMethods = arrayOf("verifyTableCount"))
  fun verifyTableNames() {
    assertEquals(UserTaskDB["usertasks"], UserTaskDB.usertasks)
    assertEquals(UserTaskDB["nodedata"], UserTaskDB.nodedata)
  }

  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
  fun verifyUserTaskRows() {
    assertEquals(UserTaskDB.usertasks._cols.map { it.name }, listOf("taskhandle", "remotehandle", "version"))
  }


  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
  fun verifyUserTaskPrimaryKey() {
    assertEquals(UserTaskDB.usertasks._primaryKey?.map { it.name }, listOf("taskhandle"))
  }

  @Test(dependsOnMethods = arrayOf("verifyTablesRecorded"))
  fun verifyUserTaskForeignKeys() {
    assertEquals(UserTaskDB.usertasks._foreignKeys, emptyList<ForeignKey>())
  }

  @Test(dependsOnMethods = arrayOf("verifyUserTaskRows"))
  fun verifyUserTaskTaskHandle() {
    val taskHandle = UserTaskDB.usertasks.taskhandle
    assertEquals(taskHandle.name, "taskhandle")
    assertEquals(taskHandle.notnull, true)
    assertEquals(taskHandle.autoincrement, true)
    assertNull(taskHandle.columnFormat)
    assertNull(taskHandle.comment)
    assertNull(taskHandle.default)
    assertNull(taskHandle.references)
    assertNull(taskHandle.storageFormat)
    assertFalse(taskHandle.unique)
    assertEquals(taskHandle.table, UserTaskDB.usertasks)
    val actual: TableRef = taskHandle.table
    val expected = UserTaskDB["usertasks"]
    assertEquals(actual, expected)
    assertEquals(taskHandle.type, BIGINT_T)
  }

  @Test(dependsOnMethods = arrayOf("verifyUserTaskRows"))
  fun verifyUserTaskColNames() {
    assertEquals(UserTaskDB.usertasks["remotehandle"], UserTaskDB.usertasks.remotehandle)
    assertEquals(UserTaskDB.usertasks["taskhandle"], UserTaskDB.usertasks.taskhandle)
  }

  @Test(dependsOnMethods = arrayOf("verifyUserTaskTaskHandle"))
  fun testUserTasksDDL() {
    val expected ="""
      CREATE TABLE `usertasks` (
        `taskhandle` BIGINT NOT NULL AUTO_INCREMENT,
        `remotehandle` BIGINT NOT NULL,
        PRIMARY KEY (`taskhandle`)
      ) ENGINE=InnoDB CHARSET=utf8;
    """.trimIndent()

    assertEquals(StringBuilder().apply{UserTaskDB.usertasks.appendDDL(this)}.toString(), expected)
  }

  @Test
  fun testUpdateDb() {
    UserTaskDB1.connect(myDataSource) {
      UserTaskDB1._tables.forEach { table ->
        table.createTransitive(this, true)
      }
    }
    try {
      UserTaskDB.connect(myDataSource) {
        getMetaData().getColumns(tableNamePattern = UserTaskDB.usertasks._name, columnNamePattern = UserTaskDB.usertasks.version.name).use { rs ->
          assertFalse(rs.next())
        }
        UserTaskDB.ensureTables(this)
        getMetaData().getColumns(tableNamePattern = UserTaskDB.usertasks._name, columnNamePattern = UserTaskDB.usertasks.version.name).use { rs ->
          assertTrue(rs.next())
        }
      }
    } finally {
      UserTaskDB1.connect(myDataSource) {
        UserTaskDB1._tables.forEach {
          it.dropTransitive(this, true)
        }
      }
    }
  }

  @Test
  fun testEnsureWillCreateTables() {
    UserTaskDB.connect(myDataSource) {
      autoCommit=false
      UserTaskDB.ensureTables(this)
      assertTrue(hasTable(UserTaskDB.usertasks))
      assertTrue(hasTable(UserTaskDB.nodedata))
      rollback()
    }
  }

}