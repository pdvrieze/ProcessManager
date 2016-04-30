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

package uk.ac.bournemouth.darwin.accounts

import org.testng.Assert
import org.testng.annotations.*
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import uk.ac.bournemouth.util.kotlin.sql.useTransacted
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.logging.Logger
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.spi.InitialContextFactory
import javax.sql.DataSource

class MyContextFactory: InitialContextFactory {
  companion object {
    val context=SimpleNamingContext()
  }

  override fun getInitialContext(environment: Hashtable<*, *>?) = context
}

private class MyDataSource: DataSource {
  companion object {
    const val JDBCURL = "jdbc:mysql://localhost/test"
    const val USERNAME="test"
    const val PASSWORD="DAGHYbH6Wb"
  }

  private var _logWriter:PrintWriter? = PrintWriter(OutputStreamWriter(System.err))

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

/**
 * A test suite for the account manager.
 * Created by pdvrieze on 29/04/16.
 */
class TestAccountControllerDirect {

  companion object {
    const val testUser = "testUser"
    const val testPassword1 = "garbage"
    const val testPassword2 = "secret"
    const val RESOURCE = "jdbc/webauth"

    init {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MyContextFactory::class.java.canonicalName)
    }
  }

  @BeforeTest
  fun registerDatabase() {
    val ic = InitialContext()
    val ds:DataSource = MyDataSource()
    ic.createSubcontext("comp").createSubcontext("env").createSubcontext("jdbc").bind("webauthadm", ds)
    ic.createSubcontext("jdbc").bind("webauth", ds)
  }

  @BeforeMethod
  fun setupDatabase() {
    WebAuthDB.connect(MyDataSource()) {
      val conn = this
      WebAuthDB._tables.forEach { table ->
        table.createTransitive(conn, true)
      }
    }
  }

  @AfterMethod(alwaysRun = true)
  fun emptyDatabase() {
    WebAuthDB.connect(MyDataSource()) {
      WebAuthDB._tables.forEach { table ->
        table.dropTransitive(this, true)
      }
    }
  }

  @Test
  fun createUser() {
    accountDb(RESOURCE) {
      doCreateUser()
    }
    val users = WebAuthDB.connect(MyDataSource()) {
      WebAuthDB.SELECT(WebAuthDB.users.user).WHERE { WebAuthDB.users.user .IS_NOT(NULL)}.getList(this)
    }
    Assert.assertEquals(users, listOf(testUser))
  }

  private fun AccountDb.doCreateUser() {
    createUser(testUser)
    setPassword(testUser, testPassword1)
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticate() {
    accountDb {
      doCreateUser()
      Assert.assertTrue(verifyCredentials(testUser, testPassword1))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateEmpty() {
    accountDb {
      doCreateUser()
      Assert.assertFalse(verifyCredentials(testUser, ""))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateEmptyUser() {
    accountDb {
      doCreateUser()
      Assert.assertFalse(verifyCredentials("", testPassword1))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateInvalidPassword() {
    accountDb {
      doCreateUser()
      Assert.assertFalse(verifyCredentials(testUser, testPassword2))
    }
  }
}