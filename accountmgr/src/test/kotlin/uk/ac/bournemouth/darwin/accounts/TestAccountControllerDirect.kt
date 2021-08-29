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

package uk.ac.bournemouth.darwin.accounts

import io.github.pdvrieze.kotlinsql.monadic.actions.mapSeq
import io.github.pdvrieze.kotlinsql.monadic.invoke
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
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

/**
 * A test suite for the account manager.
 * Created by pdvrieze on 29/04/16.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestAccountControllerDirect {

    @BeforeAll
    fun registerDatabase() {
        val ic = InitialContext()
        val ds: DataSource = TestDataSource()
        ic.createSubcontext("comp").createSubcontext("env").createSubcontext("jdbc").bind("webauthadm", ds)
        ic.createSubcontext("jdbc").bind("webauth", ds)
    }

    @BeforeEach
    fun setupDatabase() {
        accountDb {
            ensureTables()
        }

/*
        try {
            WebAuthDB.connect2(MyDataSource()) {
                val conn = this
                val tablesCreated = mutableSetOf<Table>()
                var t: DBTransactionBase<WebAuthDB, *> = conn as DBConnection2<WebAuthDB>

                WebAuthDB._tables.forEach { table ->
                    with (table) {
                        if (table !in tablesCreated) {
                            t = t.flatmap { db.createTransitive(true, tablesCreated) }
                        }
                    }
                }
                (t as DBTransaction).commit()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            throw e
        }
*/
    }

    @AfterEach
    fun emptyDatabase() {
        WebAuthDB(TestDataSource()) {
            val tables = arrayOf(
                WebAuthDB.tokens,
                WebAuthDB.challenges,
                WebAuthDB.pubkeys,
                WebAuthDB.app_perms,
                WebAuthDB.user_roles,
                WebAuthDB.roles,
                WebAuthDB.users,
            )
            transaction {
                tables.forEach { table ->
                    DELETE_FROM(table).evaluateNow()
//                table.dropTransitive(this, true)
                }
                commit()
            }
        }
    }

    @Test
    fun createUser() {
        accountDb(RESOURCE) {
            doCreateUser()
        }
        val users = WebAuthDB(TestDataSource()) {
            SELECT(WebAuthDB.users.user).mapSeq { it.toList() }.commit()
        }
        assertEquals(listOf(testUser), users)
    }

    private fun AccountDb.doCreateUser() {
        createUser(testUser)
        updateCredentials(testUser, testPassword1)
    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testAuthenticate() {
        accountDb {
            doCreateUser()
            assertTrue(verifyCredentials(testUser, testPassword1), "The password should be valid")
            assertFalse(verifyCredentials(testUser.toUpperCase(), testPassword1),
                        "The username should be case sensitive")
        }
    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testAuthenticateEmpty() {
        accountDb {
            doCreateUser()
            assertFalse(verifyCredentials(testUser, ""))
        }
    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testAuthenticateEmptyUser() {
        accountDb {
            doCreateUser()
            assertFalse(verifyCredentials("", testPassword1))
        }
    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testAuthenticateInvalidPassword() {
        accountDb {
            doCreateUser()
            assertFalse(verifyCredentials(testUser, testPassword2))
        }
    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testChangePassword() {
        accountDb {
            doCreateUser()
            assertNotEquals(testPassword2, testPassword1)
            assertFalse(verifyCredentials(testUser, testPassword2), "The new password should not work yet")

            updateCredentials(testUser, testPassword2)
        }
        accountDb {
            assertFalse(verifyCredentials(testUser, testPassword1), "The old password should be invalid")
            assertTrue(verifyCredentials(testUser, testPassword2), "The new password should be valid")
        }
    }

    private fun AccountDb.doNewAuthToken(user: String = testUser, keyId: Int? = null): String {
        doCreateUser()
        return createAuthtoken(user, "127.0.0.1", keyId)
    }

    @Test
    fun testPasswdHashBinary() {
        val u = WebAuthDB.users
        accountDb { createUser() }
        WebAuthDB(TestDataSource()) {
            val hash = SELECT(u.password).WHERE { u.user eq testUser }.mapSingleOrNull { it }
                .evaluateNow()
            assertNotNull(hash);
            hash!!

            val hashUpper = hash.uppercase()
            val hashLower = hash.uppercase()
            assertNotEquals(hashUpper, hash)
            assertNotEquals(hashLower, hash)

            assertNotNull(
                SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hash) }.mapSingleOrNull { it }.evaluateNow())

            assertNull(
                SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hashLower) }.mapSingleOrNull { it }.evaluateNow())

            assertNull(
                SELECT(u.user).WHERE { (u.user eq testUser) AND (u.password eq hashUpper) }.mapSeq { it.singleOrNull() }.evaluateNow())
        }

    }

    @Test//(dependsOnMethods = arrayOf("createUser"))
    fun testNewAuthToken() {
        val genToken = accountDb {
            doNewAuthToken()
        }
        WebAuthDB(TestDataSource()) {
            SELECT(WebAuthDB.tokens.token, WebAuthDB.tokens.ip)
                .WHERE { WebAuthDB.users.user eq testUser }
                .map { rs ->
                    assertTrue(rs.next())
                    val (token, ip) = rs.rowData
                    assertEquals(genToken, token)
                    assertEquals("127.0.0.1", ip)
                    assertFalse(rs.next())
                }
                .commit()
        }
    }

    @Test//(dependsOnMethods = arrayOf("testNewAuthToken"))
    fun testUserFromAuthToken() {
        accountDb {
            val token = doNewAuthToken()
            val user = userFromToken(token, "127.0.0.1")
            assertEquals(testUser, user)
        }
    }

    @Test//(dependsOnMethods = arrayOf("testNewAuthToken"))
    fun testUserFromAuthTokenInvalidIp() {
        accountDb {
            val token = doNewAuthToken()
            val user1 = userFromToken(token, "127.0.0.2")
            assertNull(user1)
            val user2 = userFromToken(token, "")
            assertNull(user2)
        }
    }

    @Test//(dependsOnMethods = arrayOf("testNewAuthToken"))
    fun testUserFromInvalidToken() {
        accountDb {
            val token = doNewAuthToken()

            assertNull(userFromToken(token.lowercase(), "127.0.0.1"))

            assertNull(userFromToken(token.lowercase(), "127.0.0.1"))

            assertNull(userFromToken("foobar", "127.0.0.1"))

            assertNull(userFromToken("", "127.0.0.1"))
        }
    }

    @Test
    fun testKeyPairs() {
        val testData = "Some very secret message"

        val encrypter = Cipher.getInstance("RSA").apply { init(Cipher.ENCRYPT_MODE, testPrivateKey) }
        val decrypter = Cipher.getInstance("RSA").apply { init(Cipher.DECRYPT_MODE, testPubKey) }

        val encryptedData = encrypter.doFinal(testData.toByteArray())
        assertNotEquals(testData, encryptedData)

        val decryptedData = decrypter.doFinal(encryptedData)
        assertEquals(testData, String(decryptedData))
    }

    @Test//(dependsOnMethods = arrayOf("testKeyPairs"))
    fun testRegisterKey() {
        val nowMillis = (System.currentTimeMillis() / 1000) * 1000 // The system only uses second accuracy
        val keyId = accountDb {
            doCreateUser()
            registerkey(testUser, "$testModulusEnc:$testPubExpEnc", "Test system")
        }
        val p = WebAuthDB.pubkeys
        WebAuthDB(TestDataSource()) {
            SELECT(p.pubkey, p.user)
                .WHERE { (p.keyid eq keyId) }
                .map { rs ->
                    assertTrue(rs.next())
                    val (pubkey, user) = rs.rowData
                    assertEquals(testUser, user)
                    assertEquals("$testModulusEnc:$testPubExpEnc", pubkey)
                    assertFalse(rs.next())
                }
                .commit()
        }

        val keyInfo = accountDb { keyInfo(testUser) }
        assertEquals(1, keyInfo.size)
        val key = keyInfo.get(0)
        assertEquals("Test system", key.appname)
        assertEquals(keyId, key.keyId)
        assertTrue((key.lastUse?.time ?: Long.MIN_VALUE) >= nowMillis,
                   "Last use should be set to a value after the initial value (${key.lastUse}>=${Date(nowMillis)})")
    }

    @Test
    fun testUpdateKeyUsageTime() {
        val (keyid, token) = accountDb {
            doCreateUser()
            val keyid = registerkey(testUser, "$testModulusEnc:$testPubExpEnc", "Test system")
            val token = createAuthtoken(testUser, "127.0.0.1", keyid)
            (keyid to token)
        }
        Thread.sleep(1000)
        accountDb {
            val origUse = keyInfo(testUser).single { it.keyId == keyid }.lastUse
            assertNotEquals(0, origUse)
            assertNotEquals(nowMillis, origUse)
            assertNull(userFromToken(token, "127.0.0.2"))
            val useAfterInvalid = keyInfo(testUser).single { it.keyId == keyid }.lastUse
            assertEquals(origUse, useAfterInvalid)
            assertEquals(testUser, userFromToken(token, "127.0.0.1"))
            val useAfterTokenUse = keyInfo(testUser).single { it.keyId == keyid }.lastUse
            assertNotEquals(origUse, useAfterTokenUse)
            assertEquals(Date((nowMillis / 1000) * 1000), useAfterTokenUse)
        }
    }

    @Test
    fun testBase64Random() {
        val myRandom = SecureRandom()

        val randomBytes = ByteArray(32).apply { myRandom.nextBytes(this) }
        val randomEncoded = Base64.getUrlEncoder().encodeToString(randomBytes);
        val randomUrlSafe = buildString {
            randomEncoded.forEach {
                val char = when (it) {
                    '+'  -> '-'
                    '/'  -> '_'
                    else -> it
                }
                this.append(char)
            }
        }
        val urlsafeDecoded = Base64.getUrlDecoder().decode(randomUrlSafe)
        assertArrayEquals(urlsafeDecoded, randomBytes)
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

            Base64.getUrlDecoder().decode(challengeStr)
        }
        val oldKeyInfo = accountDb {
            keyInfo(testUser).single { it.keyId == keyid }
        }

        val nowMillis = System.currentTimeMillis()
        assertTrue(oldKeyInfo.lastUse?.time ?: Long.MIN_VALUE <= nowMillis, "Key last used before now")
        Thread.sleep(2000) /* sleep a second to get a new timestamp*/

        accountDb {
            val rsaEnc = Cipher.getInstance("RSA").apply { init(Cipher.ENCRYPT_MODE, testPrivateKey) }
            val response = rsaEnc.doFinal(challenge)

            assertEquals(testUser, userFromChallengeResponse(keyid, "127.0.0.1", response))
            val newKeyInfo = keyInfo(testUser).single { it.keyId == keyid }
            assertTrue(newKeyInfo.lastUse?.time ?: Long.MIN_VALUE > nowMillis,
                       "LastUse should be later than now (${newKeyInfo.lastUse}>$nowMillis)")
            assertTrue((newKeyInfo.lastUse ?: Date(Long.MIN_VALUE)) > oldKeyInfo.lastUse ?: Date(Long.MAX_VALUE),
                       "LastUse should be updated (${newKeyInfo.lastUse}>$nowMillis)")

            rsaEnc.init(Cipher.ENCRYPT_MODE, testPrivateKey)
            val invalidResponse = rsaEnc.doFinal(testPassword1.toByteArray())

            assertNull(userFromChallengeResponse(keyid, "127.0.0.1", invalidResponse))

            assertNull(userFromChallengeResponse(keyid, "127.0.0.2", response))

            assertNull(userFromChallengeResponse(keyid + 1, "127.0.0.1", response))
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

    @Test
    fun testLogout() {
        val token = accountDb {
            doNewAuthToken()
        }
        accountDb {
            assertEquals(testUser, userFromToken(token, "127.0.0.1"))
            logout(token)
            assertNull(userFromToken(token, "127.0.0.1"), "After logout the token should be invalid.")
        }
        accountDb {
            assertNull(userFromToken(token, "127.0.0.1"), "A new database session should still have the token invalid.")
        }
    }

    //  @Test
    fun testSimulatedAndroid() {
        val challengeEnc = "Rk7D9PjVfD5WmV8BfJrZzdzUew2tyvpqhUf/zHLOuVE="
        val privateExpEnc = "WoMAqA/wagiivZF2cuuRv078EuGb8q0NOXd/dOtUITdk7SZcDm8rl8NFb09qIgnH" +
                            "gU6lO+A2/iRxzbp4TPl0bECKZ3UigJR1VXhIJXGAuDkw5VZzXMY7E1MJ95DNyVEh8KVucGmPCSjxu+" +
                            "TL4NM+7E4V4PDyfOC/WMIxeE99NtHunixHu4+a0ozWmf7zdKou42BcvXmrwZwmLNV9GsqDnJv5pog8" +
                            "QEbTHF+8T66zgj/+5FdGIf6pHP59glkO1wKCQiE1CQ2RITchYhV7/beKAshUATeOrPJD8pV4CMW+BK" +
                            "X4/qJFiVLXeEGyknmjF3MrYSS9D1g5QU/QWNbncNPvAQ=="
        val modulusEnc = "ALoCoFoK0zSvfUvcxmnBdE+TTSkAneOXIG+GcAEdNnrJSPmnCCV3c2mFimaAzmrMZRu2" +
                         "ZCwp1Kt/PKhrz5K12jALfBcuE/yGE4Gom8gDQrDpUrA/pf0GykaYeG9RGwm0Z/zZH3/ASeAtrycdXP" +
                         "Td4XWcw6w/KBKFhEpxuIKh+hw5kGN8JQ+L5CrIzoROiPfN3L6BThquXMLFOJInkQ3IzaksP3pOpgOU" +
                         "0Ekpnu5qKOSFw0E4eMra1epVrCdM2wY/4XP2ZYQgkyAIRAa48o2ANRr1cHGEjFvTgm8Nh9JSclHi1X" +
                         "/FVLmmoCrEeUOiU6FqShwFh+EL/rd/TRLWuh6xwOk="

        val pubkeyExpEnc = "AQAB"

        val sentResponseEnc = "fecBS3QJ-EsZcVphtN1s8lQt0NcQUUDSCxrwG0s_ByZlm-0nC8uTwTupD_sYfw" +
                              "SWyqxhpJFV8kNH-wof1rYOPX2xplmzOgr_1oTqWPWDUsp2_2GepekHCXtDaEkB1v9RTwrp1eS2RYjX" +
                              "zu7c2XmnZ63wCHBV3kamT-soEV3Q3TLLezP3f0FqjkDENeWHmYP4T5Yrecn988GIB8dTY5HbZAQ1L7" +
                              "03VEmriZMeXKefaTi1hBGj2WqoOtXu24e2u2lLLfDd2zvgIgbNevRhXBAzez_wyDFurv9Dbq6BFJqT" +
                              "60h9pDgAfTxL8MwbR9I75Pz0csZCvPy-FkAjvjSPNYINSA=="

        val modulus = Base64.getUrlDecoder().decode(modulusEnc)

        val factory = KeyFactory.getInstance(KEY_ALGORITHM)
        val pubkey = factory.generatePublic(
            RSAPublicKeySpec(BigInteger(modulus), BigInteger(Base64.getUrlDecoder().decode(pubkeyExpEnc)))) as RSAPublicKey

        val privkey = factory.generatePrivate(RSAPrivateKeySpec(BigInteger(modulus), BigInteger(
            Base64.getUrlDecoder().decode(privateExpEnc)))) as RSAPrivateKey

        val challenge = Base64.getUrlDecoder().decode(challengeEnc)

        val cipher = Cipher.getInstance(CIPHERSUITE)
        cipher.init(Cipher.ENCRYPT_MODE, privkey)
        val sentResponse = Base64.getUrlDecoder().decode(sentResponseEnc)
        val calculatedResponse = cipher.doFinal(challenge)
        assertEquals(sentResponse, calculatedResponse)

        cipher.init(Cipher.DECRYPT_MODE, pubkey)
        val receivedResponse = Base64.getUrlDecoder().decode(sentResponseEnc)
        val decodedResponse = cipher.doFinal(receivedResponse)
        assertEquals(decodedResponse, challenge)


    }

    @Test
    fun testBase64dec() {
        val enc = "Y2hhbGxlbmdl"
        val dec = String(Base64.getUrlDecoder().decode(enc))
        assertEquals("challenge", dec)
    }


    //  @Test
    fun testSimulatedAndroid2() {
        val challengeEnc = "Y2hhbGxlbmdl"
        val privateExpEnc = "WoMAqA/wagiivZF2cuuRv078EuGb8q0NOXd/dOtUITdk7SZcDm8rl8NFb09qIgnH" +
                            "gU6lO+A2/iRxzbp4TPl0bECKZ3UigJR1VXhIJXGAuDkw5VZzXMY7E1MJ95DNyVEh8KVucGmPCSjxu+" +
                            "TL4NM+7E4V4PDyfOC/WMIxeE99NtHunixHu4+a0ozWmf7zdKou42BcvXmrwZwmLNV9GsqDnJv5pog8" +
                            "QEbTHF+8T66zgj/+5FdGIf6pHP59glkO1wKCQiE1CQ2RITchYhV7/beKAshUATeOrPJD8pV4CMW+BK" +
                            "X4/qJFiVLXeEGyknmjF3MrYSS9D1g5QU/QWNbncNPvAQ=="
        val modulusEnc = "ALoCoFoK0zSvfUvcxmnBdE+TTSkAneOXIG+GcAEdNnrJSPmnCCV3c2mFimaAzmrMZRu2" +
                         "ZCwp1Kt/PKhrz5K12jALfBcuE/yGE4Gom8gDQrDpUrA/pf0GykaYeG9RGwm0Z/zZH3/ASeAtrycdXP" +
                         "Td4XWcw6w/KBKFhEpxuIKh+hw5kGN8JQ+L5CrIzoROiPfN3L6BThquXMLFOJInkQ3IzaksP3pOpgOU" +
                         "0Ekpnu5qKOSFw0E4eMra1epVrCdM2wY/4XP2ZYQgkyAIRAa48o2ANRr1cHGEjFvTgm8Nh9JSclHi1X" +
                         "/FVLmmoCrEeUOiU6FqShwFh+EL/rd/TRLWuh6xwOk="

        val pubkeyExpEnc = "AQAB"

        val sentResponseEnc = "IdgRLSyfF_s-wmaTQiBfHWvGBDWd8p22JMUqz9E_lucroNRoBjH0CvSeOMCWVZ" +
                              "zzgVvd_ij1njWa6LNUKUGBMRlPXWeGX7OpseaMPW7OGnuySjn_8GGO6MKzhs-hLTGJq-FdpYRNpoj4" +
                              "8Kc1xEGfst3ZX-NCY-lIr2DAKyKSmvGXwLDWr6k3UR1NwUwsclzgKtATgKpJwlj_-Ohk-ZBAVyeRhe" +
                              "ieFgDImJlklCnms8zJjD70FHcbR20sSQyTUXML-eVb7_I7l-mPiBo_98p6ZPpzubPWwOf33wernu35" +
                              "d-v_qzTyvnQe8TbL_VvSo1i84rqtVA6tyvmqTnlcgQqdxw=="

        val modulus = Base64.getUrlDecoder().decode(modulusEnc)

        val factory = KeyFactory.getInstance(KEY_ALGORITHM)
        val pubkey = factory.generatePublic(
            RSAPublicKeySpec(BigInteger(modulus), BigInteger(Base64.getUrlDecoder().decode(pubkeyExpEnc)))) as RSAPublicKey

        val privkey = factory.generatePrivate(RSAPrivateKeySpec(BigInteger(modulus), BigInteger(
            Base64.getUrlDecoder().decode(privateExpEnc)))) as RSAPrivateKey

        val challenge = Base64.getUrlDecoder().decode(challengeEnc)
        val challengeStr = String(challenge)
        assertEquals("challenge", challengeStr)

        val cipher = Cipher.getInstance(CIPHERSUITE)
        cipher.init(Cipher.ENCRYPT_MODE, privkey)
        val calculatedResponse = cipher.doFinal(challenge)
        assertEquals(256, calculatedResponse.size)
        cipher.init(Cipher.DECRYPT_MODE, pubkey)
        val decryptedChallenge = String(cipher.doFinal(calculatedResponse))

        assertEquals("challenge", decryptedChallenge)

        val calculatedResponseEnc = Base64.getUrlEncoder().encodeToString(calculatedResponse)
        val webSafeCalculated = calculatedResponseEnc.replace('+', '-').replace('/', '_')
        assertEquals(344, calculatedResponseEnc.length)
        assertEquals(344, sentResponseEnc.length)
        assertEquals(webSafeCalculated, sentResponseEnc)

        val sentResponse = Base64.getUrlDecoder().decode(sentResponseEnc)
        assertEquals(sentResponse, calculatedResponse)

        cipher.init(Cipher.DECRYPT_MODE, pubkey)
        val decodedResponse = cipher.doFinal(sentResponse)
        assertEquals(decodedResponse, challenge)


    }

    companion object {
        //    const val CIPHERSUITE = "RSA/ECB/NoPadding"
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
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MyContextFactory::class.java.name)

            val modulusInt = BigInteger(Base64.getDecoder().decode(testModulusEnc))
            val publicExponentInt = BigInteger(Base64.getDecoder().decode(testPubExpEnc))
            val privKeyInt = BigInteger(Base64.getDecoder().decode(testPrivExpEnc))

            val factory = KeyFactory.getInstance("RSA")
            testPubKey = factory.generatePublic(RSAPublicKeySpec(modulusInt, publicExponentInt)) as RSAPublicKey
            testPrivateKey = factory.generatePrivate(RSAPrivateKeySpec(modulusInt, privKeyInt)) as RSAPrivateKey

        }
    }

    private class TestDataSource : DataSource {
        companion object {
            const val JDBCURL = "jdbc:mysql://localhost/test"
            const val USERNAME = "test"
            const val PASSWORD = "DAGHYbH6Wb"
        }

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

    internal class MyContextFactory : InitialContextFactory {
        companion object {
            val context = SimpleNamingContext()
        }

        override fun getInitialContext(environment: Hashtable<*, *>?) = context
    }

}

