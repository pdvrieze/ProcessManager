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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by pdvrieze on 01/04/16.
 */

package uk.ac.bournemouth.ac.db.darwin.usertasks

import io.github.pdvrieze.kotlinsql.ddl.ForeignKey
import io.github.pdvrieze.kotlinsql.ddl.TableRef
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.invoke
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource

class UserTasksTest {


    object myDataSource : DataSource {
        const val JDBCURL = "jdbc:mysql://localhost/test"
        const val USERNAME = "test"
        const val PASSWORD = "DAGHYbH6Wb"

        private var _logWriter: PrintWriter? = PrintWriter(OutputStreamWriter(System.err))

        private var _loginTimeout: Int = 0

        override fun setLogWriter(out: PrintWriter?) {
            _logWriter = out
        }

        override fun setLoginTimeout(seconds: Int) {
            _loginTimeout = seconds
        }

        override fun getParentLogger() = Logger.getAnonymousLogger()

        override fun getLogWriter() = _logWriter

        override fun getLoginTimeout() = _loginTimeout

        override fun isWrapperFor(iface: Class<*>) = iface.isInstance(this)

        override fun <T : Any> unwrap(iface: Class<T>): T {
            if (!iface.isInstance(this)) throw SQLException("Interface not implemented")
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
        assertEquals(2, UserTaskDB._tables.size)
    }

    @Test//(dependsOnMethods = arrayOf("verifyTableCount"))
    fun verifyTablesRecorded() {
        assertTrue(UserTaskDB._tables.contains(UserTaskDB.usertasks))
        assertTrue(UserTaskDB._tables.contains(UserTaskDB.nodedata))
    }

    @Test//(dependsOnMethods = arrayOf("verifyTableCount"))
    fun verifyTableNames() {
        assertEquals(UserTaskDB.usertasks, UserTaskDB["usertasks"])
        assertEquals(UserTaskDB.nodedata, UserTaskDB["nodedata"])
    }

    @Test//(dependsOnMethods = arrayOf("verifyTablesRecorded"))
    fun verifyUserTaskRows() {
        assertEquals(listOf("taskhandle", "remotehandle", "version"),
                     UserTaskDB.usertasks._cols.map { it.name })
    }


    @Test//(dependsOnMethods = arrayOf("verifyTablesRecorded"))
    fun verifyUserTaskPrimaryKey() {
        assertEquals(listOf("taskhandle"), UserTaskDB.usertasks._primaryKey?.map { it.name })
    }

    @Test//(dependsOnMethods = arrayOf("verifyTablesRecorded"))
    fun verifyUserTaskForeignKeys() {
        assertEquals(emptyList<ForeignKey>(), UserTaskDB.usertasks._foreignKeys)
    }

    @Test//(dependsOnMethods = arrayOf("verifyUserTaskRows"))
    fun verifyUserTaskTaskHandle() {
        val taskHandle = UserTaskDB.usertasks.taskhandle
        assertEquals("taskhandle", taskHandle.name)
        assertEquals(true, taskHandle.notnull)
        assertEquals(true, taskHandle.autoincrement)
        assertNull(taskHandle.columnFormat)
        assertNull(taskHandle.comment)
        assertNull(taskHandle.default)
        assertNull(taskHandle.references)
        assertNull(taskHandle.storageFormat)
        assertFalse(taskHandle.unique)
        assertEquals(UserTaskDB.usertasks, taskHandle.table)
        val actual: TableRef = taskHandle.table
        val expected = UserTaskDB["usertasks"]
        assertEquals(expected, actual)
        assertEquals(X_TASKHANDLE, taskHandle.type)
    }

    @Test//(dependsOnMethods = arrayOf("verifyUserTaskRows"))
    fun verifyUserTaskColNames() {
        assertEquals(UserTaskDB.usertasks.remotehandle, UserTaskDB.usertasks._cols.first { it.name == "remotehandle" })
        assertEquals(UserTaskDB.usertasks.taskhandle, UserTaskDB.usertasks._cols.first { it.name == "taskhandle" })
    }

    @Test//(dependsOnMethods = arrayOf("verifyUserTaskTaskHandle"))
    fun testUserTasksDDL() {
        val expected = """
      CREATE TABLE `usertasks` (
        `taskhandle` BIGINT NOT NULL AUTO_INCREMENT,
        `remotehandle` BIGINT NOT NULL,
        `version` INT NOT NULL,
        PRIMARY KEY (`taskhandle`)
      ) ENGINE=InnoDB CHARSET=utf8;
    """.trimIndent()

        assertEquals(expected, StringBuilder().apply { UserTaskDB.usertasks.appendDDL(this) }.toString())
    }

    @Test
    fun testUpdateDb() {
        UserTaskDB1(myDataSource) {
            ensureTables(true).commit()
//            value(db._tables).flatMap<Any?> {
//                db._tables.flatMap { it.createTransitive(true) }
//            }.commit()
        }
        try {
            UserTaskDB(myDataSource) {
                connectionMetadata.getColumns(
                    tableNamePattern = UserTaskDB.usertasks._name,
                    columnNamePattern = UserTaskDB.usertasks.version.name
                ).map { rs ->
                    assertFalse(rs.next())
                }.evaluateNow()

                ensureTables().commit()
            }
            UserTaskDB(myDataSource) {
                connectionMetadata.getColumns(
                    tableNamePattern = UserTaskDB.usertasks._name,
                    columnNamePattern = UserTaskDB.usertasks.version.name
                ).map { rs ->
                    assertTrue(rs.next())
                }.evaluateNow()
            }
        } finally {
            UserTaskDB1(myDataSource) {

                value(UserTaskDB1._tables)
                    .flatMap { tables -> tables.map { table -> table.dropTransitive(true) } }
                    .commit()
            }
        }
    }

    @Test
    fun testEnsureWillCreateTables() {
        UserTaskDB(myDataSource) {
            try {
                ensureTables().commit()

                assertTrue(connectionMetadata.hasTable(UserTaskDB.usertasks).evaluateNow())
                assertTrue(connectionMetadata.hasTable(UserTaskDB.nodedata).evaluateNow())
            } finally {
                UserTaskDB._tables.forEach { it.dropTransitive(true).commit() }
            }
            assertFalse(connectionMetadata.hasTable(UserTaskDB.usertasks).evaluateNow())
        }
    }

}

