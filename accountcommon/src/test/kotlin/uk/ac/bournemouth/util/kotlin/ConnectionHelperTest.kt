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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import uk.ac.bournemouth.kotlinsql.Database
import uk.ac.bournemouth.util.kotlin.sql.DBConnection
import uk.ac.bournemouth.util.kotlin.sql.use
import java.sql.Connection
import java.sql.DriverManager


/**
 * Created by pdvrieze on 24/03/16.
 */
class DBConnectionTest {

  object db: Database(1) {}

  companion object {
    const val JDBCURL = "jdbc:mysql://localhost/test"
    const val USERNAME="test"
    const val PASSWORD="DAGHYbH6Wb"
    const val TABLENAME="testTable"
  }

  var conn: Connection? = null

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

  @AfterEach
  fun removeTempTable() {
    val c = conn
    if (c!=null && ! c.isClosed) {
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
    DBConnection(conn!!, db).use {
      simpleInsert(it)
      verifyRows(DBConnection(conn!!, db))
    }
    assertTrue(conn!!.isClosed)
  }

  @Test
  fun testUseThrow() {
    try {
      DBConnection(conn!!, db).transaction {
        simpleInsert(it)
        throw UnsupportedOperationException("test")
      }
    } catch(e:UnsupportedOperationException) {
        assertEquals("test", e.message)
    }
    verifyNoRows(DBConnection(conn!!, db))
    assertFalse(conn!!.isClosed)
  }

  @Test
  fun testOuterUse() {
    conn!!.use {
      simpleInsert(DBConnection(it, db))
      it.prepareStatement("DROP TABLE ${TABLENAME}").use { it.execute() }
    }
    assertTrue(conn!!.isClosed)
  }

  @Test
  fun testCommit() {
    val c = conn!!
    DBConnection(c, db).use {
      simpleInsert(it)
      it.commit()
      verifyRows(it)
    }
  }

  @Test
  fun testRollback() {
    val c = conn!!
    DBConnection(c, db).use {
      simpleInsert(it)
      it.rollback()
      verifyNoRows(it)
    }

  }


  @Test
  fun testAutoRollback() {
    val c = conn!!
    try {
      DBConnection(c, db).use { it ->
        simpleInsert(it)
        throw UnsupportedOperationException("Test")
      }
    } catch (e:UnsupportedOperationException) {
        assertEquals("Test", e.message)
    }
    DBConnection(makeConnection(), db).use {
      verifyNoRows(it)
    }
  }


  @Test
  fun testAutoRollbackTransaction() {
    val c = conn!!
    DBConnection(c, db).use { it ->
      try {
        it.transaction {
          simpleInsert(it)
          throw UnsupportedOperationException("Test")
        }
      } catch (e:UnsupportedOperationException) {
          assertEquals("Test", e.message)
      }
      verifyNoRows(it)

    }

  }

  private fun verifyNoRows(connectionHelper: DBConnection) {
    connectionHelper.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
      execute {
        assertFalse(it.next())
      }
    }
  }

  private fun verifyRows(connectionHelper: DBConnection) {
    connectionHelper.prepareStatement("SELECT col1, col2, col3, col4 FROM $TABLENAME") {
      execute {
        assertTrue(it.next())
          assertEquals(1, it.getInt(1))
          assertEquals("r1c2", it.getString(2))
          assertEquals("r1c3", it.getString(3))
          assertEquals(true, it.getBoolean(4))

        assertTrue(it.next())
          assertEquals(2, it.getInt(1))
          assertEquals("r2c2", it.getString(2))
          assertEquals("r2c3", it.getString(3))
          assertEquals(false, it.getBoolean(4))

        assertFalse(it.next())
      }
    }
  }

  private fun simpleInsert(connectionHelper: DBConnection) {
    connectionHelper.prepareStatement("INSERT INTO $TABLENAME (col1, col2, col3, col4) VALUES ( ?, ?, ?, ? ), (?, ?, ? ,?)") {
        params(intValue = 1) + "r1c2" + "r1c3" + true + 2 as Int + "r2c2" + "r2c3" + false
        assertEquals(2, executeUpdate())
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
