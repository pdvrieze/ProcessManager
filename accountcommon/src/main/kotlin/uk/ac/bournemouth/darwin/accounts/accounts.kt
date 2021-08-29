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

import io.github.pdvrieze.kotlinsql.ddl.Column
import io.github.pdvrieze.kotlinsql.ddl.IColumnType
import io.github.pdvrieze.kotlinsql.dml.WhereClause
import io.github.pdvrieze.kotlinsql.dml.impl._Where
import io.github.pdvrieze.kotlinsql.monadic.*
import io.github.pdvrieze.kotlinsql.monadic.actions.DBAction
import io.github.pdvrieze.kotlinsql.monadic.actions.mapSeq
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.writeAttribute
import uk.ac.bournemouth.ac.db.darwin.webauth.WebAuthDB
import java.math.BigInteger
import java.net.HttpURLConnection
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.util.*
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.naming.InitialContext
import javax.sql.DataSource

public const val DARWINCOOKIENAME: String = "DWNID"
public const val MAXTOKENLIFETIME: Int = 864000 /* Ten days */
public const val MAXCHALLENGELIFETIME: Int = 60 /* 60 seconds */
public const val MAX_RESET_VALIDITY: Int = 1800 /* 12 hours */

/*
 * This file contains the functionality needed for managing accounts on darwin. This includes the underlying database
 * interaction through [AccountDb] as well as web interaction through [AccountController].
 */

/** Create a SHA1 digest of the source */
internal fun sha1(src: ByteArray): ByteArray = MessageDigest.getInstance("SHA1").digest(src)

public const val DBRESOURCE: String = "jdbc/webauth"
public const val CIPHERSUITE: String = "RSA/ECB/PKCS1Padding"
public const val KEY_ALGORITHM: String = "RSA"

@Suppress("NOTHING_TO_INLINE")
private inline fun SecureRandom.nextBytes(len: Int): ByteArray = ByteArray(len).also { buffer -> nextBytes(buffer) }


public open class AccountDb(private val dataSource: DataSource) {

    private val u: WebAuthDB.users get() = WebAuthDB.users
    private val c: WebAuthDB.challenges get() = WebAuthDB.challenges
    private val t: WebAuthDB.tokens get() = WebAuthDB.tokens
    private val r: WebAuthDB.user_roles get() = WebAuthDB.user_roles
    private val p: WebAuthDB.pubkeys get() = WebAuthDB.pubkeys

    public val nowSeconds: Long by lazy { System.currentTimeMillis() / 1000 }
    public val nowMillis: Long get() = nowSeconds * 1000 // Current time in milliseconds since epoch (1-1-1970 UTC)

    public fun updateCredentials(username: String, password: String): Boolean {
        return WebAuthDB.invoke(dataSource) {
            UPDATE { SET(u.password, createPasswordHash(getSalt(username), password)) }
                .WHERE { u.user eq username }
                .commit() != 0
        }
    }


    private fun generateAuthToken() = buildString {
        Base64.getUrlEncoder().encode(random.nextBytes(32)).asSequence()
            .map { it.toInt().toChar() }
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

    public fun createAuthtoken(username: String, remoteAddr: String, keyid: Int? = null): String {
        val token = generateAuthToken()
        WebAuthDB.invoke(dataSource) {
            val updateCount = INSERT(t.user, t.ip, t.keyid, t.token, t.epoch)
                .VALUES(username, remoteAddr, keyid, token, nowSeconds)
                .commit()[0]

            if (updateCount != Statement.SUCCESS_NO_INFO && updateCount <= 0) {
                throw AuthException("Failure to record the authentication token.")
            }
        }
        return token
    }


    public fun registerkey(user: String, pubkey: String, appname: String?, keyid: Int? = null): Int {

        // Helper function for the shared code that determines the application name to use. It avoids duplicates
        fun DBActionReceiver<WebAuthDB>.realAppname(whereClauseFactory: _Where.(String) -> WhereClause): DBAction<WebAuthDB, String?> {
            return when (appname) {
                null -> value(null)
                else -> {
                    val names = sequence {
                        yield(appname)
                        for (i in 1..MAX_APP_IDX) yield("$appname $i")
                    }

                    value(names).first { candidateName ->
                        SELECT(p.appname)
                            .WHERE { whereClauseFactory(candidateName) }
                            .isEmpty()
                    }
                }
            }
        }

        if (keyid != null) {
            WebAuthDB.invoke(dataSource) {
                transaction {
                    realAppname { candidateName -> (p.user eq user) AND (p.appname eq candidateName) AND (p.keyid ne keyid) }
                        .then { name ->
                            if (name != null) {
                                UPDATE { SET(p.pubkey, pubkey); SET(p.appname, name) }
                                    .WHERE { (p.keyid eq keyid) AND (p.user eq user) }
                                    .map { it > 0 }
                            } else {
                                value(false)
                            }
                        }.commit()
                }
            }
            return keyid
        } else {
            return WebAuthDB.invoke(dataSource) {
                realAppname { (p.user eq user) AND (p.appname eq it) }
                    .then { realappname ->
                        INSERT(p.user, p.appname, p.pubkey, p.lastUse)
                            .VALUES(user, realappname, pubkey, nowSeconds)
                            .keys(p.keyid)
                    }.map { it.single()!! }
                    .commit()
            }
        }
    }

    public fun forgetKey(user: String, keyId: Int) {
        return WebAuthDB.invoke(dataSource) {
            DELETE_FROM(p)
                .WHERE { (p.user eq user) AND (p.keyid eq keyId) }
                .commit()
        }
    }

    internal fun updateTokenEpoch(token: String, remoteAddr: String) {
        WebAuthDB.invoke(dataSource) {
            UPDATE { SET(t.epoch, nowSeconds) }
                .WHERE { (t.token eq token) AND (t.ip eq remoteAddr) }
                .map { updateCount ->
                    if (updateCount == 0) {
                        throw AuthException(appendWarnings("Failure to update the authentication token"))
                    }
                }.commit()
        }
    }

    /**
     * Update the auth token for the given user. This may change the token, but normally doesn't.
     *
     * @return The relevant authToken.
     */
    public fun updateAuthToken(username: String, token: String, remoteAddr: String): String {
        WebAuthDB.invoke(dataSource) {
            UPDATE { SET(t.epoch, nowSeconds) }
                .WHERE { (t.user eq username) AND (t.token eq token) AND (t.ip eq remoteAddr) }
                .map { updateCount ->
                    if (updateCount == 0) {
                        throw AuthException(appendWarnings("Failure to update the authentication token"))
                    }
                }.commit()
        }
        return token
    }

    private fun DBConnectionContext<*>.appendWarnings(message: String): String {
        return buildString {
            append(message)
            val warnings = getConnectionWarnings()
            if (warnings.isNotEmpty()) {
                append(" - \n    ")
                warnings.joinTo(this, ",\n    ") { "${it.errorCode}: ${it.message}" }
            }
        }
    }

    private fun <T : Any, S : IColumnType<T, S, C>, C : Column<T, S, C>> getSingle(col: C, user: String): T? {
        return WebAuthDB(dataSource) {
            SELECT(col)
                .WHERE { u.user eq user }
                .mapSeq { it.singleOrNull() }
                .commit()
        }
    }

    public fun isLocalAccount(user: String): Boolean = !getSingle(u.password, user).isNullOrBlank()

    public fun isUser(user: String): Boolean = getSingle(u.user, user) != null

    public fun isUserInRole(user: String, role: String): Boolean = WebAuthDB(dataSource) {
        SELECT(r.user)
            .WHERE { (r.user eq user) AND (r.role eq role) }
            .mapSeq { !it.iterator().hasNext() }
            .commit()
    }

    public fun fullname(user: String): String? = getSingle(u.fullname, user)

    public fun alias(user: String): String? = getSingle(u.alias, user)

    public fun lastReset(user: String): Timestamp? = getSingle(u.resettime, user)

    public fun keyInfo(user: String): List<KeyInfo> {
        logger.fine("Getting key information for $user")
        return WebAuthDB(dataSource) {
            SELECT(p.keyid, p.appname, p.lastUse)
                .WHERE { p.user eq user }
                .mapEach { keyId, appName, lastUse ->
                    logger.fine("Found key information ($keyId, $appName, $lastUse)")
                    KeyInfo(keyId ?: throw NullPointerException("Keyid should never be null"), appName,
                            lastUse?.let { if (it > 10000) Date(it * 1000) else null })
                }
                .commit()
        }
    }

    private fun getSalt(@Suppress("UNUSED_PARAMETER") username: String): String {
        return ""
    }

    private fun createPasswordHash(@Suppress("UNUSED_PARAMETER") salt: String, password: String): String {
        return "{SHA}${Base64.getEncoder().encodeToString(sha1(password.toByteArray()))}"
    }

    public fun verifyCredentials(username: String, password: String): Boolean {
        // TODO use proper salts
        val passwordHash = createPasswordHash(getSalt(username), password)

        return WebAuthDB(dataSource) {
            SELECT(u.user)
                .WHERE { (u.user eq username) AND (u.password eq passwordHash) }
                .mapSeq { it.iterator().hasNext() }
                .commit()
        }
    }

    public fun generateResetToken(user: String): String {
        val resetToken = Base64.getEncoder().encodeToString(random.nextBytes(15))

        val hasUpdated = WebAuthDB(dataSource) {
            UPDATE {
                SET(u.resettoken, resetToken)
                SET(u.resettime, Timestamp(nowSeconds))
            }
                .WHERE { u.user eq user }
                .map { it != 1 }
                .commit()
        }

        if (hasUpdated) {
            throw AuthException("Could not store the reset token")
        }
        return resetToken
    }

    public fun verifyResetToken(user: String, resetToken: String): Boolean {
        val resetTime: Timestamp = WebAuthDB(dataSource) {
            SELECT(u.resettime)
                .WHERE { (u.user eq user) AND (u.resettoken eq resetToken) }
                .mapSeq { it.singleOrNull() }
                .commit()
        } ?: return false

        return resetTime.time - nowSeconds < MAX_RESET_VALIDITY
    }

    public fun getUserRoles(user: String): List<String> {
        return WebAuthDB(dataSource) {
            SELECT(r.role)
                .WHERE { r.user eq user }
                .mapSeq { it.filterNotNull().toList() }
                .commit()
        }
    }

    public fun userFromToken(token: String, remoteAddr: String): String? {
        return WebAuthDB(dataSource) {
            SELECT(t.user, t.keyid)
                .WHERE { (t.token eq token) AND (t.ip eq remoteAddr) }
                .mapSingleOrNull { user, keyid ->
                    updateTokenEpoch(token, remoteAddr)
                    if (keyid != null) updateKeyLastUse(keyid)
                    user
                }
                .commit()
        }
    }

    public fun String.base64Decode(): ByteArray {
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

    public fun userFromChallengeResponse(keyId: Int, requestIp: String, response: ByteArray): String? {
        cleanChallenges()

        val challenge = WebAuthDB(dataSource) {
            SELECT(c.challenge)
                .WHERE { (c.keyid eq keyId) AND (c.requestip eq requestIp) }
                .mapSingleOrNull { it?.base64Decode() }
                .commit()
        } ?: return null

        val (user, encodedpubkey) = WebAuthDB(dataSource) {
            SELECT(p.user, p.pubkey)
                .WHERE { p.keyid eq keyId }
                .mapSingleOrNull { user, pubkey ->
                    Pair(user!!, pubkey)
                }.map {
                    it ?: throw AuthException(
                        "No suitable public key found",
                        errorCode = HttpURLConnection.HTTP_UNAUTHORIZED
                    )
                }.commit()
        }

        val pubkey = toRSAPubKey(
            encodedpubkey
                ?: throw AuthException("Invalid value for public key", errorCode = HttpURLConnection.HTTP_UNAUTHORIZED)
        )

        val rsa = Cipher.getInstance(CIPHERSUITE)
        rsa.init(Cipher.DECRYPT_MODE, pubkey)
        val decryptedResponse = rsa.doFinal(response)
        if (!Arrays.equals(decryptedResponse, challenge)) return null

        updateKeyLastUse(keyId)
        return user
    }

    private fun updateKeyLastUse(keyId: Int) {
        WebAuthDB(dataSource) {
            UPDATE { SET(p.lastUse, nowSeconds) }
                .WHERE { p.keyid eq keyId }
                .commit()
        }
    }


    public fun logout(authToken: String): Int {
        return WebAuthDB(dataSource) { DELETE_FROM(t).WHERE { t.token eq authToken }.commit() }
    }

    public fun cleanAuthTokens() {
        if (nowSeconds - lastTokenClean > 60000) {
            WebAuthDB(dataSource) { DELETE_FROM(t).WHERE { t.epoch lt (nowSeconds - MAXTOKENLIFETIME) }.commit() }
            lastTokenClean = nowSeconds
        }
    }

    public fun cleanChallenges() {
        val cutoff = nowSeconds - MAXCHALLENGELIFETIME
        WebAuthDB(dataSource) { DELETE_FROM(c).WHERE { c.epoch lt cutoff }.commit() }
    }

    public fun newChallenge(keyid: Int, requestIp: String): String {
        val challenge = Base64.getUrlEncoder().encodeToString(random.nextBytes(32))
        try {
            return WebAuthDB(dataSource) {
                INSERT_OR_UPDATE(c.keyid, c.requestip, c.challenge, c.epoch)
                    .VALUES(keyid, requestIp, challenge, nowSeconds)
                    .map {
                        if (it.isEmpty()) throw AuthException(appendWarnings("Could not store challenge")) else challenge
                    }.commit()
            }
        } catch (e: SQLException) {
            @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
            val foundKeyId = WebAuthDB(dataSource) {
                SELECT(p.keyid)
                    .WHERE { p.keyid eq keyid }
                    .mapSingleOrNull { it }
                    .commit()
            }
            throw when (foundKeyId) {
                null -> AuthException(
                    "Unknown or expired key id. Reauthentication required",
                    e,
                    HttpURLConnection.HTTP_NOT_FOUND
                )
                else -> AuthException("Could not store challenge")
            }

        }
    }

    public fun createUser(userName: String, fullName: String? = null) {
        WebAuthDB(dataSource) { INSERT(u.user, u.fullname).VALUES(userName, fullName).commit() }
    }

    @Deprecated(
        "updateCredentials is the correct way",
        ReplaceWith("updateCredentials(user, password)"),
        DeprecationLevel.ERROR
    )
    public fun setPassword(user: String, password: String) {
        val hash = createPasswordHash("", password)
        WebAuthDB(dataSource) { UPDATE { SET(u.password, hash) }.WHERE { u.user eq user }.commit() }
    }

    public fun ensureTables() {
        WebAuthDB(dataSource) { ensureTables() }
    }

    private companion object {
        @Volatile
        var lastTokenClean: Long = 0

        val random: SecureRandom by lazy { SecureRandom() }

        @JvmStatic
        val logger = Logger.getLogger(AccountDb::class.java.name)!!

        // The maximum app id to allow for searching for another one.
        const val MAX_APP_IDX = 10_000
    }

}

public class KeyInfo(public val keyId: Int, public val appname: String?, public val lastUse: Date?) {
    public fun toXmlWriter(writer: XmlWriter) {
        writer.smartStartTag(null, "key") {
            writeAttribute("id", keyId.toString())
            appname?.let { writeAttribute("appname", it) }
            lastUse?.let { writeAttribute("lastUse", it.time.toString()) }
        }
    }
}

public class AuthException(
    msg: String,
    cause: Throwable? = null,
    public val errorCode: Int = HttpURLConnection.HTTP_INTERNAL_ERROR
) : RuntimeException(msg, cause) {
    override val message: String get() = super.message!!
}


/** Helper function to create, use and dismiss an [AccountDb] instance. It will open the database and execute the
 * lambda it. There is no direct access to the underlying database connection.
 *
 * @param block The code to execute in relation to the database.
 */
public inline fun <R> accountDb(resourceName: String = DBRESOURCE, block: AccountDb.() -> R): R {
    return accountDb(InitialContext().lookup(resourceName) as DataSource, block)
}
//
//inline fun <R> accountDb(dataSource: DataSource, block: AccountDb.() -> R): R {
//  return dataSource.connection { AccountDb(it).block() }
//}

public inline fun <R> accountDb(dataSource: DataSource, block: AccountDb.() -> R): R {
    return AccountDb(dataSource).block()
}
