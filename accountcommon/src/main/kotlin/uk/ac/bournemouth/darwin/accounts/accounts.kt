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

package uk.ac.bournemouth.darwin.accounts

import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import uk.ac.bournemouth.kotlinsql.*
import uk.ac.bournemouth.util.kotlin.sql.*
import java.math.BigInteger
import java.net.HttpURLConnection
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.xml.stream.XMLStreamWriter

const val DARWINCOOKIENAME = "DWNID"
const val MAXTOKENLIFETIME = 864000 /* Ten days */
const val MAXCHALLENGELIFETIME = 60 /* 60 seconds */
const val MAX_RESET_VALIDITY = 1800 /* 12 hours */

/*
 * This file contains the functionality needed for managing accounts on darwin. This includes the underlying database
 * interaction through [AccountDb] as well as web interaction through [AccountController].
 */

/** Create a SHA1 digest of the source */
internal fun sha1(src: ByteArray): ByteArray = MessageDigest.getInstance("SHA1").digest(src)

const val DBRESOURCE = "jdbc/webauth"
const val CIPHERSUITE = "RSA/ECB/PKCS1Padding"
const val KEY_ALGORITHM = "RSA"

@Suppress("NOTHING_TO_INLINE")
private inline fun SecureRandom.nextBytes(len: Int): ByteArray = ByteArray(len).also { buffer -> nextBytes(buffer) }


open class AccountDb(private val connection: DBConnection2<WebAuthDB>) {

    companion object {
        @Volatile
        private var lastTokenClean: Long = 0

        val random: SecureRandom by lazy { SecureRandom() }

        @JvmStatic
        val logger = Logger.getLogger(AccountDb::class.java.name)!!

    }

    private val u: WebAuthDB.users get() = WebAuthDB.users
    private val c: WebAuthDB.challenges get() = WebAuthDB.challenges
    private val t: WebAuthDB.tokens get() = WebAuthDB.tokens
    private val r: WebAuthDB.user_roles get() = WebAuthDB.user_roles
    private val p: WebAuthDB.pubkeys get() = WebAuthDB.pubkeys

    val nowSeconds: Long by lazy { System.currentTimeMillis() / 1000 }
    val nowMillis: Long get() = nowSeconds * 1000 // Current time in milliseconds since epoch (1-1-1970 UTC)

    fun updateCredentials(username: String, password: String): Boolean = connection.update {
        UPDATE { SET(u.password, createPasswordHash(getSalt(username), password)) }
            .WHERE { u.user eq username }
    }.commit() != 0

    private fun generateAuthToken() = buildString {
        Base64.getUrlEncoder().encode(random.nextBytes(32)).asSequence()
            .map(Byte::toChar)
            .forEach { c ->
                when (c) {
                    '+' -> append('-')
                    '/' -> append('_')
                    '=' -> {
                    }
                    else -> append(c)
                }
            }
    }

    fun createAuthtoken(username: String, remoteAddr: String, keyid: Int? = null): String {
        val token = generateAuthToken()
        connection
            .update {
                INSERT(t.user, t.ip, t.keyid, t.token, t.epoch)
                    .VALUES(username, remoteAddr, keyid, token, nowSeconds)
            }
            .commit()
            .let { if (it == 0) throw AuthException("Failure to record the authentication token.") }
        return token
    }


    fun registerkey(user: String, pubkey: String, appname: String?, keyid: Int? = null): Int {

        // Helper function for the shared code that determines the application name to use. It avoids duplicates
        fun DBTransactionBase<WebAuthDB, *>.realAppname(whereClauseFactory: Database._Where.(String) -> Database.WhereClause?): DBTransaction<WebAuthDB, String?> {
            return when (appname) {
                null -> map { null }
                else -> map {
                    sequence {
                        yield(appname)
                        for (i in 1..Int.MAX_VALUE) yield("appname $i")
                    }.first { candidateName ->
                        query { SELECT(p.appname).WHERE { whereClauseFactory(appname) } }
                            .map { it.iterator().hasNext() }
                            .commit()
                    }
                }
            }
        }

        if (keyid != null) {
            connection
                .realAppname { candidateName -> (p.user eq user) AND (p.appname eq candidateName) AND (p.keyid ne keyid) }
                .update { realAppname ->
                    UPDATE { SET(p.pubkey, pubkey); SET(p.appname, realAppname) }
                        .WHERE { (p.keyid eq keyid) AND (p.user eq user) }
                }
                .map { updateCount ->
                    if (updateCount == 0) throw AuthException("Failure to update the authentication key")
                }
                .commit()
            return keyid
        } else {
            return connection
                .realAppname { (p.user eq user) AND (p.appname eq it) }
                .insert(p.keyid) { realappname ->
                    INSERT(p.user, p.appname, p.pubkey, p.lastUse)
                        .VALUES(user, realappname, pubkey, nowSeconds)
                }
                .map { it.single() }
                .commit()
        }
    }

    fun forgetKey(user: String, keyId: Int) {
        connection
            .update { DELETE_FROM(p).WHERE { (p.user eq user) AND (p.keyid eq keyId) } }
            .requireNonZero { appendWarnings("The key is not owned by the given user") }
            .commit()
    }

    internal fun updateTokenEpoch(token: String, remoteAddr: String) {
        connection
            .update {
                UPDATE { SET(t.epoch, nowSeconds) }
                    .WHERE { (t.token eq token) AND (t.ip eq remoteAddr) }
            }
            .map { updateCount ->
                if (updateCount == 0) {
                    throw AuthException(appendWarnings("Failure to update the authentication token"))
                }
            }
            .commit()
    }

    /**
     * Update the auth token for the given user. This may change the token, but normally doesn't.
     *
     * @return The relevant authToken.
     */
    fun updateAuthToken(username: String, token: String, remoteAddr: String): String {
        connection
            .update {
                UPDATE { SET(t.epoch, nowSeconds) }
                    .WHERE { (t.user eq username) AND (t.token eq token) AND (t.ip eq remoteAddr) }
            }
            .map { updateCount ->
                if (updateCount == 0) {
                    throw AuthException(appendWarnings("Failure to update the authentication token"))
                }
            }
            .commit()
        return token
    }

    fun DBTransactionBase.ActionContext<*>.appendWarnings(message: String): String {
        val connection = connection
        return buildString {
            append(message)
            connection.apply {
                val warnings = warningsIt
                if (warnings.hasNext()) {
                    append(" - \n    ")
                    warnings.asSequence().map { "${it.errorCode}: ${it.message}" }.joinTo(this@buildString, ",\n    ")
                }
            }
        }
    }

    private fun <T : Any, S : IColumnType<T, S, C>, C : Column<T, S, C>> getSingle(col: C, user: String): T? {
        return connection
            .query {
                SELECT(col).WHERE { u.user eq user }
            }
            .commit().singleOrNull()
    }

    fun isLocalAccount(user: String) = !getSingle(u.password, user).isNullOrBlank()

    fun isUser(user: String) = getSingle(u.user, user) != null

    fun isUserInRole(user: String, role: String) = WebAuthDB.SELECT(r.user)
        .WHERE { (r.user eq user) AND (r.role eq role) }
        .getSingleOrNull(connection) != null

    fun fullname(user: String): String? = getSingle(u.fullname, user)

    fun alias(user: String): String? = getSingle(u.alias, user)

    fun lastReset(user: String): Timestamp? = getSingle(u.resettime, user)

    fun keyInfo(user: String): List<KeyInfo> {
        logger.fine("Getting key information for $user")
        return WebAuthDB
            .SELECT(p.keyid, p.appname, p.lastUse)
            .WHERE { p.user eq user }
            .getList(connection) { keyId, appName, lastUse ->
                logger.fine("Found key information ($keyId, $appName, $lastUse)")
                KeyInfo(keyId ?: throw NullPointerException("Keyid should never be null"), appName,
                        lastUse?.let { if (it > 10000) Date(it * 1000) else null })
            }
    }

    private fun getSalt(@Suppress("UNUSED_PARAMETER") username: String): String {
        return ""
    }

    private fun createPasswordHash(@Suppress("UNUSED_PARAMETER") salt: String, password: String): String {
        return "{SHA}${Base64.getEncoder().encodeToString(sha1(password.toByteArray()))}"
    }

    fun verifyCredentials(username: String, password: String): Boolean {
        // TODO use proper salts
        val passwordHash = createPasswordHash(getSalt(username), password)

        return WebAuthDB.SELECT(u.user).WHERE { (u.user eq username) AND (u.password eq passwordHash) }.getSingleOrNull(
            connection
                                                                                                                       ) != null

    }

    fun generateResetToken(user: String): String {
        val resetToken = Base64.getEncoder().encodeToString(random.nextBytes(15))
        if (WebAuthDB.UPDATE {
                SET(u.resettoken, resetToken); SET(
                u.resettime,
                Timestamp(nowSeconds)
                                                  )
            }.WHERE { u.user eq user }.executeUpdate(connection) != 1) {
            throw AuthException("Could not store the reset token")
        }
        return resetToken
    }

    fun verifyResetToken(user: String, resetToken: String): Boolean {
        val resetTime: Timestamp =
            WebAuthDB.SELECT(u.resettime).WHERE { (u.user eq user) AND (u.resettoken eq resetToken) }.getSingleOrNull(
                connection
                                                                                                                     )
                ?: return false
        return resetTime.time - nowSeconds < MAX_RESET_VALIDITY
    }

    fun getUserRoles(user: String): List<String> =
        WebAuthDB.SELECT(r.role).WHERE { r.user eq user }.getList(connection).filterNotNull()

    fun userFromToken(token: String, remoteAddr: String): String? {
        return WebAuthDB.SELECT(t.user, t.keyid)
            .WHERE { (t.token eq token) AND (t.ip eq remoteAddr) }
            .getSingle(connection) { user, keyid ->
                updateTokenEpoch(token, remoteAddr)
                if (keyid != null) updateKeyLastUse(keyid)
                user
            }
    }

    fun String.base64Decode(): ByteArray {
        try {
            return Base64.getUrlDecoder().decode(this)
        } catch (e: IllegalArgumentException) {
            return Base64.getDecoder().decode(this)
        }
    }

    private fun toRSAPubKey(keyData: String): RSAPublicKey {
        val colPos = keyData.indexOf(':')
        val modulusEnc = keyData.substring(0, colPos)
        val modulus = BigInteger(modulusEnc.base64Decode())
        val publicExponent = BigInteger(keyData.substring(colPos + 1).base64Decode())

        val factory = KeyFactory.getInstance(KEY_ALGORITHM)
        return factory.generatePublic(RSAPublicKeySpec(modulus, publicExponent)) as RSAPublicKey
    }

    fun userFromChallengeResponse(keyId: Int, requestIp: String, response: ByteArray): String? {
        cleanChallenges()
        val challenge = WebAuthDB.SELECT(c.challenge)
            .WHERE { (c.keyid eq keyId) AND (c.requestip eq requestIp) }
            .getSingleOrNull(connection)
            ?.base64Decode() ?: return null

        val (user, encodedpubkey) = WebAuthDB.SELECT(p.user, p.pubkey)
            .WHERE { p.keyid eq keyId }
            .getSingle(connection) ?: throw AuthException(
            "No suitable public key found",
            errorCode = HttpURLConnection.HTTP_UNAUTHORIZED
                                                         )

        val pubkey = toRSAPubKey(
            encodedpubkey ?: throw AuthException(
                "Invalid value for public key",
                errorCode = HttpURLConnection.HTTP_UNAUTHORIZED
                                                )
                                )

        val rsa = Cipher.getInstance(CIPHERSUITE)
        rsa.init(Cipher.DECRYPT_MODE, pubkey)
        val decryptedResponse = rsa.doFinal(response)
        if (!Arrays.equals(decryptedResponse, challenge)) return null

        updateKeyLastUse(keyId)
        return user
    }

    private fun updateKeyLastUse(keyId: Int) {
        WebAuthDB.UPDATE { SET(p.lastUse, nowSeconds) }.WHERE { p.keyid eq keyId }.executeUpdate(connection)
    }


    fun logout(authToken: String): Int {
        return WebAuthDB.DELETE_FROM(t).WHERE { t.token eq authToken }.executeUpdate(connection)
    }

    fun cleanAuthTokens() {
        if (nowSeconds - lastTokenClean > 60000) {
            WebAuthDB.DELETE_FROM(t).WHERE { t.epoch lt (nowSeconds - MAXTOKENLIFETIME) }.executeUpdate(connection)
            lastTokenClean = nowSeconds
        }
    }

    fun cleanChallenges() {
        val cutoff = nowSeconds - MAXCHALLENGELIFETIME
        WebAuthDB.DELETE_FROM(c).WHERE { c.epoch lt cutoff }.executeUpdate(connection)
    }

    fun newChallenge(keyid: Int, requestIp: String): String {
        val conn = connection
        val challenge = Base64.getUrlEncoder().encodeToString(random.nextBytes(32))
        try {
            return conn
                .update {
                    INSERT_OR_UPDATE(c.keyid, c.requestip, c.challenge, c.epoch)
                        .VALUES(keyid, requestIp, challenge, nowSeconds)
                }
                .map { if (it == 0) throw AuthException(appendWarnings("Could not store challenge")) else challenge }
                .commit()
        } catch (e: SQLException) {
            conn
                .query { SELECT(p.keyid).WHERE { p.keyid eq keyid } }
                .map { s ->
                    throw if (s.none()) {
                        AuthException(
                            "Unknown or expired key id. Reauthentication required",
                            e,
                            HttpURLConnection.HTTP_NOT_FOUND
                                     )
                    } else {
                        AuthException("Could not store challenge")
                    }
                }
                .commit()
        }
    }

    fun createUser(userName: String, fullName: String? = null) {
        WebAuthDB.INSERT(u.user, u.fullname).VALUES(userName, fullName).executeUpdate(connection)
    }

    @Deprecated(
        "updateCredentials is the correct way",
        ReplaceWith("updateCredentials(user, password)"),
        DeprecationLevel.ERROR
               )
    fun setPassword(user: String, password: String) {
        val hash = createPasswordHash("", password)
        WebAuthDB.UPDATE { SET(u.password, hash) }.WHERE { u.user eq user }.executeUpdate(connection)
    }

    fun ensureTables(): Unit {
        connection.apply {
            db.ensuretables()
        }
    }


}

class KeyInfo(val keyId: Int, val appname: String?, val lastUse: Date?) {
    fun toXmlWriter(writer: XMLStreamWriter) {
        writer.writeEmptyElement("key")
        writer.writeAttribute("id", keyId.toString())
        appname?.let { writer.writeAttribute("appname", it) }
        lastUse?.let { writer.writeAttribute("lastUse", it.time.toString()) }
    }
}

class AuthException(msg: String, cause: Throwable? = null, val errorCode: Int = HttpURLConnection.HTTP_INTERNAL_ERROR) :
    RuntimeException(msg, cause) {
    override val message: String
        get() = super.message!!
}


/** Helper function to create, use and dismiss an [AccountDb] instance. It will open the database and execute the
 * lambda it. There is no direct access to the underlying database connection.
 *
 * @param block The code to execute in relation to the database.
 */
inline fun <R> accountDb(resourceName: String = DBRESOURCE, block: AccountDb.() -> R): R {
    return accountDb(InitialContext().lookup(resourceName) as DataSource, block)
}
//
//inline fun <R> accountDb(dataSource: DataSource, block: AccountDb.() -> R): R {
//  return dataSource.connection { AccountDb(it).block() }
//}

inline fun <R> accountDb(dataSource: DataSource, block: AccountDb.() -> R): R {
    return dataSource.withConnection(WebAuthDB) { connection ->
        AccountDb(connection).block()
    }
}
