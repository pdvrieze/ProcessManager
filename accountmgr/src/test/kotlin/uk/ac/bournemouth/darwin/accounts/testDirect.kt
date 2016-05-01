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

import net.sourceforge.migbase64.Base64
import org.testng.Assert
import org.testng.Assert.*
import org.testng.annotations.*
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import uk.ac.bournemouth.util.kotlin.sql.useTransacted
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.logging.Logger
import javax.crypto.Cipher
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

    const val testPrivExpEnc = "WuEDw/Maqf6SmURmkaHIoR69L5zU8dFlAE5l5ygD+3GRVrJOAtt2SZrU3knNim38p6XNuIF34QOPMpzpM0peAQ=="
    const val testPubExpEnc = "AQAB"
    const val testModulusEnc = "AM2wa0R9dY9FP3oaYU3o9nEownzIM1Yq2clbeIjFAVbN0JcgWRPdvu/NB+G9ksSWsw9r+RaLbIclMy1ac/SbCHc="

    val testPubKey: RSAPublicKey
    val testPrivateKey: RSAPrivateKey

    init {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MyContextFactory::class.java.canonicalName)

      val modulusInt = BigInteger(Base64.decoder().decode(testModulusEnc))
      val publicExponentInt = BigInteger(Base64.decoder().decode(testPubExpEnc))
      val privKeyInt  = BigInteger(Base64.decoder().decode(testPrivExpEnc))

      val factory = KeyFactory.getInstance("RSA")
      testPubKey = factory.generatePublic(RSAPublicKeySpec(modulusInt, publicExponentInt)) as RSAPublicKey
      testPrivateKey = factory.generatePrivate(RSAPrivateKeySpec(modulusInt, privKeyInt)) as RSAPrivateKey

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
      WebAuthDB.SELECT(WebAuthDB.users.user).getList(this)
    }
    assertEquals(users, listOf(testUser))
  }

  private fun AccountDb.doCreateUser() {
    createUser(testUser)
    updateCredentials(testUser, testPassword1)
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticate() {
    accountDb {
      doCreateUser()
      assertTrue(verifyCredentials(testUser, testPassword1), "The password should be valid")
      assertFalse(verifyCredentials(testUser.toUpperCase(), testPassword1), "The username should be case sensitive")
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateEmpty() {
    accountDb {
      doCreateUser()
      assertFalse(verifyCredentials(testUser, ""))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateEmptyUser() {
    accountDb {
      doCreateUser()
      assertFalse(verifyCredentials("", testPassword1))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testAuthenticateInvalidPassword() {
    accountDb {
      doCreateUser()
      assertFalse(verifyCredentials(testUser, testPassword2))
    }
  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testChangePassword() {
    accountDb {
      doCreateUser()
      assertNotEquals(testPassword1, testPassword2)
      assertFalse(verifyCredentials(testUser, testPassword2), "The new password should not work yet")

      updateCredentials(testUser, testPassword2)
    }
    accountDb {
      assertFalse(verifyCredentials(testUser, testPassword1), "The old password should be invalid")
      assertTrue(verifyCredentials(testUser, testPassword2), "The new password should be valid")
    }
  }

  private fun AccountDb.doNewAuthToken(user:String = testUser, keyId:Int?=null): String {
    doCreateUser()
    return createAuthtoken(user, "127.0.0.1", keyId)
  }

  fun testPasswdHashBinary() {
    val u = WebAuthDB.users
    accountDb { createUser() }
    val hash = WebAuthDB.connect(MyDataSource()) {
      val hash = WebAuthDB.SELECT(u.password).WHERE { u.user eq testUser }.getSingle(this)
      assertNotNull(hash);
      hash!!

      val hashUpper = hash.toUpperCase()
      val hashLower = hash.toLowerCase()
      assertNotEquals(hash, hashUpper)
      assertNotEquals(hash, hashLower)

      assertNotNull(WebAuthDB.SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hash) }.getSingle(this))

      assertNull(WebAuthDB.SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hashLower) }.getSingle(this))

      assertNull(WebAuthDB.SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hashUpper) }.getSingle(this))
    }

  }

  @Test(dependsOnMethods = arrayOf("createUser"))
  fun testNewAuthToken() {
    val genToken = accountDb {
      doNewAuthToken()
    }
    WebAuthDB.connect(MyDataSource()) {
      WebAuthDB.SELECT(WebAuthDB.tokens.token, WebAuthDB.tokens.ip)
            .WHERE { WebAuthDB.users.user eq testUser}
            .getSingle(this) { token, ip ->
              assertEquals(token, genToken)
              assertEquals(ip, "127.0.0.1")
            } ?: AssertionError("Result expected")
    }
  }

  @Test(dependsOnMethods = arrayOf("testNewAuthToken"))
  fun testUserFromAuthToken() {
    accountDb {
      val token = doNewAuthToken()
      val user = userFromToken(token, "127.0.0.1")
      assertEquals(user, testUser)
    }
  }

  @Test(dependsOnMethods = arrayOf("testNewAuthToken"))
  fun testUserFromAuthTokenInvalidIp() {
    accountDb {
      val token = doNewAuthToken()
      val user1 = userFromToken(token, "127.0.0.2")
      assertNull(user1)
      val user2 = userFromToken(token, "")
      assertNull(user2)
    }
  }

  @Test(dependsOnMethods = arrayOf("testNewAuthToken"))
  fun testUserFromInvalidToken() {
    accountDb {
      val token = doNewAuthToken()

      assertNull(userFromToken(token.toLowerCase(), "127.0.0.1"))

      assertNull(userFromToken(token.toUpperCase(), "127.0.0.1"))

      assertNull(userFromToken("foobar", "127.0.0.1"))

      assertNull(userFromToken("", "127.0.0.1"))
    }
  }

  @Test
  fun testKeyPairs() {
    val testData = "Some very secret message"

    val encrypter = Cipher.getInstance("RSA").apply { init(Cipher.ENCRYPT_MODE, testPrivateKey)}
    val decrypter = Cipher.getInstance("RSA").apply { init(Cipher.DECRYPT_MODE, testPubKey)}

    val encryptedData = encrypter.doFinal(testData.toByteArray())
    assertNotEquals(encryptedData, testData)

    val decryptedData = decrypter.doFinal(encryptedData)
    assertEquals(String(decryptedData), testData)
  }

  @Test(dependsOnMethods = arrayOf("testKeyPairs"))
  fun testRegisterKey() {
    val now = System.currentTimeMillis()
    val keyId = accountDb {
      doCreateUser()
      registerkey(testUser, "$testModulusEnc:$testPubExpEnc", "Test system")
    }
    val p = WebAuthDB.pubkeys
    WebAuthDB.connect(MyDataSource()) {
      WebAuthDB.SELECT(p.pubkey, p.user)
            .WHERE { (p.keyid eq keyId) }
            .getSingle(this) { pubkey, user ->
              assertEquals(user, testUser)
              assertEquals(pubkey, "$testModulusEnc:$testPubExpEnc")
            } ?: AssertionError("Result expected")
    }

    val keyInfo = accountDb { keyInfo(testUser) }
    assertEquals(keyInfo.size, 1)
    val key = keyInfo.get(0)
    assertEquals(key.appname, "Test system")
    assertEquals(key.keyId, keyId)
    assertTrue(key.lastUse>=now, "Last use should be set to a value after the initial value")
  }

  @Test
  fun testChallenge() {
    val keyid = accountDb {
      doCreateUser()
      registerkey(testUser, "$testModulusEnc:$testPubExpEnc", "Test system")
    }
    val challenge = accountDb {
      val challengeStr = newChallenge(keyid, "127.0.0.1")
      assertNotNull(challengeStr)
      assertFalse(challengeStr.any { c -> c < '\u0020' || c > '\u007f' })

      Base64.decoder().decode(challengeStr)
    }
    accountDb {
      val rsaEnc = Cipher.getInstance("RSA").apply { init(Cipher.ENCRYPT_MODE, testPrivateKey) }
      val response = rsaEnc.doFinal(challenge)
      val oldKeyInfo = keyInfo(testUser).single { it.keyId==keyid }
      val now = System.currentTimeMillis()
      assertTrue(oldKeyInfo.lastUse<=now, "Key last used before now")
      if (oldKeyInfo.lastUse==now) { Thread.sleep(1) /* sleep a milisecond to get a new timestamp*/ }

      assertEquals(userFromChallengeResponse(keyid, "127.0.0.1", response), testUser)

      val newKeyInfo = keyInfo(testUser).single {it.keyId==keyid }
      assertTrue(newKeyInfo.lastUse>now)
      assertTrue(newKeyInfo.lastUse>oldKeyInfo.lastUse)

      rsaEnc.init(Cipher.ENCRYPT_MODE, testPrivateKey)
      val invalidResponse = rsaEnc.doFinal(testPassword1.toByteArray())

      assertNull(userFromChallengeResponse(keyid, "127.0.0.1", invalidResponse))

      assertNull(userFromChallengeResponse(keyid, "127.0.0.2", response))

      assertNull(userFromChallengeResponse(keyid+1, "127.0.0.1", response))
    }
  }

  /**
   * Test creating and using reset tokens. This also checks for invalid username token combinations.
   */
  @Test
  fun testResetToken() {
    val otherUser = "otherUser"
    val resetToken = accountDb {
      createUser()
      createUser(otherUser)
      generateResetToken(testUser)
    }
    accountDb {
      assertTrue(verifyResetToken(testUser, resetToken))
      assertFalse(verifyResetToken("noone", resetToken))
      assertFalse(verifyResetToken(otherUser, resetToken))
      assertFalse(verifyResetToken(testUser, testPassword1))
    }
  }
}