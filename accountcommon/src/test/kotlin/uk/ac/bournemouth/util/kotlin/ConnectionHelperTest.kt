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

package uk.ac.bournemouth.util.kotlin

import io.github.pdvrieze.kotlinsql.ddl.Database
import io.github.pdvrieze.kotlinsql.monadic.ConnectionSource
import io.github.pdvrieze.kotlinsql.monadic.DBReceiver
import io.github.pdvrieze.kotlinsql.monadic.DBTransactionContext
import io.github.pdvrieze.kotlinsql.monadic.invoke
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource


/**
 * Created by pdvrieze on 24/03/16.
 */
class DBConnectionTest {

    object db : Database(1) {}

    companion object {
        const val JDBCURL = "jdbc:mysql://localhost/test"
        const val USERNAME = "test"
        const val PASSWORD = "DAGHYbH6Wb"
        const val TABLENAME = "testTable"
    }

    var conn: Connection? = null

    val dataSource = object : DataSource {
        private var logWriter: PrintWriter = PrintWriter(System.err)
        private var loginTimeout: Int = 5

        override fun getLogWriter(): PrintWriter = logWriter

        override fun setLogWriter(out: PrintWriter?) {
            logWriter = out ?: PrintWriter(System.err)
        }

        override fun setLoginTimeout(seconds: Int) {
            loginTimeout = seconds
        }

        override fun getLoginTimeout(): Int = loginTimeout

        override fun getParentLogger(): Logger {
            TODO("not implemented")
        }

        override fun <T : Any?> unwrap(iface: Class<T>?): T {
            TODO("not implemented")
        }

        override fun isWrapperFor(iface: Class<*>?): Boolean {
            TODO("not implemented")
        }

        override fun getConnection(): Connection {
            if (conn!!.isClosed) {
                conn = makeConnection()
            }
            return conn!!
        }

        override fun getConnection(username: String?, password: String?): Connection {
            return connection
        }
    }

    @BeforeEach
    fun makeConnectionAndTable() {
        makeConnection().apply {
            conn = this

            System.err.println("Creating temporary table")
            prepareStatement("DROP TABLE IF EXISTS $TABLENAME").use { it.execute() }
            prepareStatement("CREATE TABLE $TABLENAME ( col1 INT AUTO_INCREMENT PRIMARY KEY, col2 VARCHAR(10), col3 VARCHAR(10), col4 BOOLEAN ) ENGINE = InnoDB").use {
                it.execute()
            }
        }
    }

    private fun makeConnection() = DriverManager.getConnection(JDBCURL, USERNAME, PASSWORD)

//    @AfterEach
    fun removeTempTable() {
        val c = conn
        if (c != null && !c.isClosed) {
            System.err.println("Removing temporary table")
            if (!c.autoCommit) c.rollback()
            c.prepareStatement("DROP TABLE ${TABLENAME}").use {
                it.execute()
            }
            if (!c.autoCommit) {
                c.commit()
            }
            c.close()
            conn = null
        }
    }

    @Test
    fun testUse() {
        db.invoke(dataSource) {
            transaction {
                simpleInsert()
                commit()
            }
            verifyRows()
        }
        assertTrue(conn!!.isClosed)
    }

    @Test
    fun testUseThrow() {
        db(dataSource) {
            try {
                transaction {
                    simpleInsert()
                    throw UnsupportedOperationException("test")
                }
            } catch (e: UnsupportedOperationException) {
                assertEquals("test", e.message)
            }
            verifyNoRows()
        }
        assertTrue(conn!!.isClosed)
    }

    @Test
    fun testOuterUse() {
        db(dataSource) {
            transaction {
                simpleInsert()
                commit()
            }
            genericAction { it.prepareStatement("DROP TABLE $TABLENAME") { statement.execute() } }
                .evaluateNow()
        }
        assertTrue(conn!!.isClosed)
    }

    @Test
    fun testCommit() {
        db(dataSource) {
            transaction {
                simpleInsert()
                commit()
            }
        }
        db(dataSource) {
            verifyRows()
        }
    }

    @Test
    fun testRollback() {
        db(dataSource) {
            transaction {
                simpleInsert()
                rollback()
            }
        }
        db(dataSource) {
            verifyNoRows()
        }
    }


    @Test
    fun testAutoRollback() {
        try {
            db(dataSource) {
                transaction {
                    simpleInsert()
                    throw UnsupportedOperationException("Test")
                }
            }
        } catch (e: UnsupportedOperationException) {
            assertEquals("Test", e.message)
        }
        db(dataSource) {
            verifyNoRows()
        }
    }


    @Test
    fun testAutoRollbackTransaction() {
        db(dataSource) {
            try {
                transaction {
                    simpleInsert()
                    throw UnsupportedOperationException("Test")
                }
            } catch (e: UnsupportedOperationException) {
                assertEquals("Test", e.message)
            }
            verifyNoRows()
        }
    }

    private fun DBReceiver<db>.verifyNoRows() {
        genericAction { conn ->
            conn.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
                statement.executeQuery().use { rs ->
                    assertFalse(rs.next())
                }
            }
        }
    }

    private fun DBReceiver<db>.verifyRows() {
        genericAction { conn ->
            conn.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
                statement.executeQuery().use { rs ->
                    assertTrue(rs.next(), "There should be 2 rows, not 0")
                    assertEquals(1, rs.getInt(1))
                    assertEquals("r1c2", rs.getString(2))
                    assertEquals("r1c3", rs.getString(3))
                    assertEquals(true, rs.getBoolean(4))

                    assertTrue(rs.next(), "There should be 2 rows, not 1")
                    assertEquals(2, rs.getInt(1))
                    assertEquals("r2c2", rs.getString(2))
                    assertEquals("r2c3", rs.getString(3))
                    assertEquals(false, rs.getBoolean(4))

                    assertFalse(rs.next(), "There should be 2 rows, not more")
                }
            }

        }.evaluateNow()
    }

    private fun DBTransactionContext<db>.simpleInsert() {
        val x = genericAction { conn ->
            conn.prepareStatement("INSERT INTO $TABLENAME (col1, col2, col3, col4) VALUES ( ?, ?, ?, ? ), (?, ?, ? ,?)") {
                @Suppress("USELESS_CAST")
                params(intValue = 1) + "r1c2" + "r1c3" + true + 2.i + "r2c2" + "r2c3" + false
                assertEquals(2, statement.executeUpdate())
                3
            }
        }.evaluateNow()
        assertEquals(3, x)
    }
    /*

      @Test
      fun testPrepareStatement() {

      }

      @Test
      fun testGetConnection() {

      }
      */
}
