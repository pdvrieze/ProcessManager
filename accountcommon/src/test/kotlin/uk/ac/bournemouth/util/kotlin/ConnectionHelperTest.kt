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

package uk.ac.bournemouth.util.kotlin

import org.testng.Assert.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import uk.ac.bournemouth.util.kotlin.sql.ConnectionHelper
import uk.ac.bournemouth.util.kotlin.sql.connection
import uk.ac.bournemouth.util.kotlin.sql.use
import java.sql.Connection
import java.sql.DriverManager


/**
 * Created by pdvrieze on 24/03/16.
 */
class ConnectionHelperTest {

  companion object {
    const val JDBCURL = "jdbc:mysql://localhost/test"
    const val USERNAME="test"
    const val PASSWORD="DAGHYbH6Wb"
    const val TABLENAME="testTable"
  }

  var conn: Connection? = null

  @BeforeMethod
  fun makeConnectionAndTable() {
    val c = makeConnection()
    conn = c
    System.err.println("Creating temporary table")
    c.prepareStatement("DROP TEMPORARY TABLE IF EXISTS ${TABLENAME}").use { it.execute() }
    conn!!.prepareStatement("CREATE TEMPORARY TABLE ${TABLENAME} ( col1 INT AUTO_INCREMENT PRIMARY KEY, col2 VARCHAR(10), col3 VARCHAR(10), col4 BOOLEAN ) ENGINE = InnoDB").use {
      it.execute()
    }
  }

  private fun makeConnection() = DriverManager.getConnection(JDBCURL, USERNAME, PASSWORD)

  @AfterMethod
  fun removeTempTable() {
    val c = conn
    if (c!=null && ! c!!.isClosed) {
      System.err.println("Removing temporary table")
      if (!c.autoCommit) c.rollback()
      c.prepareStatement("DROP TABLE ${TABLENAME}").use {
        it.execute()
      }
      if (!c.autoCommit) {
        c.commit()
      }
      c.close()
      conn == null
    }
  }

  @Test
  fun testUse() {
    ConnectionHelper(conn!!).use {
      simpleInsert(it)
      verifyRows(ConnectionHelper(conn!!))
    }
    assertTrue(conn!!.isClosed)
  }

  @Test
  fun testUseThrow() {
    try {
      ConnectionHelper(conn!!).transaction {
        simpleInsert(it)
        throw UnsupportedOperationException("test")
      }
    } catch(e:UnsupportedOperationException) {
      assertEquals(e.message,"test")
    }
    verifyNoRows(ConnectionHelper(conn!!))
    assertFalse(conn!!.isClosed)
  }

  @Test
  fun testOuterUse() {
    conn!!.use {
      simpleInsert(ConnectionHelper(it))
      it.prepareStatement("DROP TABLE ${TABLENAME}").use { it.execute() }
    }
    assertTrue(conn!!.isClosed)
  }

  @Test
  fun testCommit() {
    val c = conn!!
    connection(c) {
      simpleInsert(it)
      it.commit()
      verifyRows(it)
    }
  }

  @Test
  fun testRollback() {
    val c = conn!!
    connection(c) {
      simpleInsert(it)
      it.rollback()
      verifyNoRows(it)
    }

  }

  @Test
  fun testAutoRollback() {
    val c = conn!!
    try {
      connection(c) {
        simpleInsert(it)
        throw UnsupportedOperationException("Test")
        fail("unreachable")
      }
    } catch (e:UnsupportedOperationException) {
      assertEquals(e.message,"Test")
    }
    connection(c) {
      verifyNoRows(it)
    }

  }

  private fun verifyNoRows(connectionHelper: ConnectionHelper) {
    connectionHelper.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
      execute {
        assertFalse(it.next())
      }
    }
  }

  private fun verifyRows(connectionHelper: ConnectionHelper) {
    connectionHelper.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
      execute {
        assertTrue(it.next())
        assertEquals(it.getInt(1), 1)
        assertEquals(it.getString(2), "r1c2")
        assertEquals(it.getString(3), "r1c3")
        assertEquals(it.getBoolean(4), true)

        assertTrue(it.next())
        assertEquals(it.getInt(1), 2)
        assertEquals(it.getString(2), "r2c2")
        assertEquals(it.getString(3), "r2c3")
        assertEquals(it.getBoolean(4), false)

        assertFalse(it.next())
      }
    }
  }

  private fun simpleInsert(connectionHelper: ConnectionHelper) {
    connectionHelper.prepareStatement("INSERT INTO $TABLENAME (col1, col2, col3, col4) VALUES ( ?, ?, ?, ? ), (?, ?, ? ,?)") {
      params(1 as Int) + "r1c2" + "r1c3" + true + 2 as Int + "r2c2" + "r2c3" + false
      assertEquals(executeUpdate(), 2)
    }
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