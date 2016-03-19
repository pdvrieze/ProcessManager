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
import uk.ac.bournemouth.util.kotlin.sql.ConnectionHelper
import uk.ac.bournemouth.util.kotlin.sql.appendWarnings
import uk.ac.bournemouth.util.kotlin.sql.connection
import java.security.MessageDigest
import java.security.SecureRandom
import javax.naming.InitialContext
import javax.sql.DataSource

internal const val DARWINCOOKIENAME = "DWNID"
internal const val MAXTOKENLIFETIME = 864000 /* Ten days */
internal const val MAXCHALLENGELIFETIME = 60 /* 60 seconds */
internal const val MAX_RESET_VALIDITY = 1800 /* 12 hours */

/*
 * This file contains the functionality needed for managing accounts on darwin. This includes the underlying database
 * interaction through [AccountDb] as well as web interaction through [AccountController].
 */

/** Create a SHA1 digest of the source */
internal fun sha1(src:ByteArray):ByteArray = MessageDigest.getInstance("SHA1").digest(src)

const val DBRESOURCE = "java:comp/env/jdbc/webauth"

private inline fun SecureRandom.nextBytes(len:Int): ByteArray = ByteArray(len).apply { nextBytes(this) }

/**
 * A class that abstracts the interaction with the account database.
 */
class AccountDb constructor(private val connection: ConnectionHelper) {

    val now:Long by lazy { System.currentTimeMillis() / 1000 } // Current time in seconds since epoch (1-1-1970 UTC)

    companion object {
        val random: SecureRandom by lazy { SecureRandom() }
    }

    private fun getSalt(username:String):String {
        return ""
    }

    private fun createPasswordHash(salt:String, password:String):String {
        return "{SHA}${Base64.encoder().encodeToString(sha1(password.toByteArray()))}";
    }

    fun updateCredentials(username:String, password: String):Boolean {
        connection.prepareStatement("UPDATE users SET `password` = ? WHERE `user` = ?") {
            params { +password + username }
            return execute()
        }
    }

    fun verifyCredentials(username:String, password:String): Boolean {

        val salt = getSalt(username)
        val passwordHash = createPasswordHash(salt, password)

        connection.prepareStatement("SELECT `user` FROM users WHERE `user`=? AND `password`=?") {
            params { + username + passwordHash }
            return executeHasRows() // If we can get a record, the combination exists.
        }
    }

    private fun generateAuthToken() = Base64.encoder().encodeToString(random.nextBytes(32))

    fun createAuthtoken(username: String, remoteAddr: String, keyid:Int=0):String {
        val token = generateAuthToken()
        connection.prepareStatement("INSERT INTO tokens (`user`, `ip`, `keyid`, `token`, `epoch`) VALUES (?,?,?,?,?)") {
            params { + username + remoteAddr + keyid + token + now }
            if (executeUpdate()==0 ) throw AuthException("Failure to record the authentication token".appendWarnings(warningsIt))
        }
        return token
    }

    fun cleanChallenges() {
        val cutoff = now - MAXCHALLENGELIFETIME
        connection.prepareStatement("DELETE FROM challenges WHERE `epoch` < ?") {
            params { +cutoff }
            executeUpdate()
        }
    }


    fun newChallenge(keyid: Long, requestIp: String): String {
        val challenge = Base64.encoder().encodeToString(random.nextBytes(32))
        connection.prepareStatement("INSERT INTO challenges ( `keyid`, `requestip`, `challenge`, `epoch` ) VALUES ( ?, ?, ?, ? )  ON DUPLICATE KEY UPDATE `challenge`=?, `epoch`=?") {
            params { +keyid + requestIp + challenge + now + challenge + now }
            if (executeHasRows()) {
                return challenge
            } else {
                throw AuthException("Could not store challenge".appendWarnings(warningsIt))
            }
        }
    }

    fun registerkey(user: String, pubkey:String, keyid: Long? = null) {
        if (keyid!=null) {
            connection.prepareStatement("UPDATE `pubkeys` SET privkey=? WHERE `keyid`=? AND `user`=?") {
                params { +pubkey + keyid + user}

            }
        } else {

        }
/*
    if ($stmt=$DB->prepare('UPDATE `pubkeys` SET privkey=? WHERE `keyid`=? AND `user`=?')) {
      $stmt->bind_param("sis", $PUBKEY, $REQUESTKEYID, $USERNAME);

 */

    }

    /**
     * Update the auth token for the given user. This may change the token, but normally doesn't.
     *
     * @return The relevant authToken.
     */
    fun updateAuthToken(username: String, token: String): String {
        connection.prepareStatement("UPDATE `tokens` SET `epoch`=UNIX_TIMESTAMP() WHERE user = ? and token=?") {
            params { +username + token }
            if (executeUpdate()==0) throw AuthException("Failure to update the authentication token".appendWarnings(warningsIt))
        }
        return token
    }

    fun isUserInRole(user: String, role: String): Boolean {
        connection.prepareStatement("SELECT user FROM user_roles where user=? and role=?") {
            params { + user + role }
            return executeHasRows() // If there is an item,
        }
    }

    fun getUserRoles(user:String): List<String> {
        connection.prepareStatement("SELECT role FROM user_roles WHERE user=?") {
            params { +user }
            return mutableListOf<String>().apply {
                execute {
                    while (it.next()) {
                        add(it.getString(1))
                    }
                }
            }

        }
    }

    fun cleanAuthTokens() {
        connection.prepareStatement("DELETE FROM tokens WHERE `epoch` < ?") {
            setLong(1, now - MAXTOKENLIFETIME)
            executeUpdate()
        }
    }

    fun verifyResetToken(user:String, resetToken: String): Boolean {
        connection.prepareStatement("SELECT UNIX_TIMESTAMP()-UNIX_TIMESTAMP(`resettime`) AS `age` FROM `users` WHERE `user`=? AND `resettoken`=?") {
            params { +user + resetToken }
            execute {
                if (it.next()) {
                    return it.getLong(1) < MAX_RESET_VALIDITY
                } else {
                    return false
                }
            }
        }
    }

}


class AuthException(msg: String, cause:Throwable? = null): RuntimeException(msg, cause) {}


/** Helper function to create, use and dismiss an [AccountDb] instance. It will open the database and execute the
 * lambda it. There is no direct access to the underlying database connection.
 *
 * @param block The code to execute in relation to the database.
 */
inline fun <R> accountDb(resourceName:String = DBRESOURCE, block:AccountDb.()->R): R {

    val ic = InitialContext()
//    val username = ic.lookup(AUTHDBADMINUSERNAME) as String
//    val password = ic.lookup(AUTHDBADMINPASSWORD) as String
    return accountDb(ic.lookup(resourceName) as DataSource, block)
}

inline fun <R> accountDb(dataSource:DataSource, block:AccountDb.()->R): R {
    dataSource.connection {
        val accountDb = AccountDb(it)
        accountDb.cleanAuthTokens()
        it.commit()
        return accountDb.block()

    }
}

