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
import java.math.BigInteger
import java.net.HttpURLConnection
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.naming.InitialContext
import javax.sql.DataSource

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

private inline fun SecureRandom.nextBytes(len: Int): ByteArray = ByteArray(len).apply { nextBytes(this) }

/**
 * A class that abstracts the interaction with the account database.
 */
class AccountDb constructor(private val connection: ConnectionHelper) {

  val now: Long by lazy { System.currentTimeMillis() / 1000 } // Current time in seconds since epoch (1-1-1970 UTC)

  companion object {
    val random: SecureRandom by lazy { SecureRandom() }
  }

  private fun getSalt(username: String): String {
    return ""
  }

  private fun createPasswordHash(salt: String, password: String): String {
    return "{SHA}${Base64.encoder().encodeToString(sha1(password.toByteArray()))}";
  }

  fun updateCredentials(username: String, password: String): Boolean {
    connection.prepareStatement("UPDATE users SET `password` = ? WHERE `user` = ?") {
      params(password) + username
      return execute()
    }
  }

  fun verifyCredentials(username: String, password: String): Boolean {

    val salt = getSalt(username)
    val passwordHash = createPasswordHash(salt, password)

    connection.prepareStatement("SELECT `user` FROM users WHERE `user`=? AND `password`=?") {
      params(username) + passwordHash
      return executeHasRows() // If we can get a record, the combination exists.
    }
  }

  private fun generateAuthToken() = Base64.encoder().encodeToString(random.nextBytes(32))

  fun createAuthtoken(username: String, remoteAddr: String, keyid: Long? = null): String {
    val token = generateAuthToken()
    return connection.prepareStatement("INSERT INTO tokens (`user`, `ip`, `keyid`, `token`, `epoch`) VALUES (?,?,?,?,?)") {
      params(username) + remoteAddr + keyid + token + now
      if (executeUpdate() == 0 ) throw AuthException("Failure to record the authentication token.".appendWarnings(warningsIt))
      token
    }
  }

  fun cleanChallenges() {
    val cutoff = now - MAXCHALLENGELIFETIME
    connection.prepareStatement("DELETE FROM challenges WHERE `epoch` < ?") {
      params(cutoff)
      executeUpdate()
    }
  }


  fun newChallenge(keyid: Long, requestIp: String): String {
    val challenge = Base64.encoder().encodeToString(random.nextBytes(32))
    connection.prepareStatement("INSERT INTO challenges ( `keyid`, `requestip`, `challenge`, `epoch` ) VALUES ( ?, ?, ?, ? )  ON DUPLICATE KEY UPDATE `challenge`=?, `epoch`=?") {
      params(keyid) + requestIp + challenge + now + challenge + now
      if (executeHasRows()) {
        return challenge
      } else {
        throw AuthException("Could not store challenge".appendWarnings(warningsIt))
      }
    }
  }

  private fun toRSAPubKey(keyData:String):RSAPublicKey {
    val colPos = keyData.indexOf(':')
    val modulus = BigInteger(Base64.decoder().decode(keyData.substring(0, colPos)))
    val publicExponent = BigInteger(Base64.decoder().decode(keyData.substring(colPos+1)))

    val factory = KeyFactory.getInstance("RSA")
    return factory.generatePublic(RSAPublicKeySpec(modulus, publicExponent)) as RSAPublicKey
  }

  fun userFromChallengeResponse(keyId:Long, requestIp:String, response:ByteArray): String? {
    cleanChallenges()
    var challenge:ByteArray?=null
    connection.prepareStatement("SELECT `challenge`, `requestip` FROM challenges WHERE keyid=?") {
      params(keyId)
      execute { rs ->
        if (rs.next()) {
          challenge = rs.getString(1)?.let { Base64.decoder().decode(it) }
          val remoteAddr = rs.getString(2)
          if (requestIp != remoteAddr || challenge == null) throw AuthException("Invalid challenge", errorCode = HttpURLConnection.HTTP_UNAUTHORIZED)
        } else {
          return null
        }
      }
    }

    var user:String? = null
    val pubkey = connection.prepareStatement("SELECT user, pubkey FROM pubkeys WHERE keyid=?") {
      params(keyId)
      execute { rs ->
        if (rs.next()) { user = rs.getString(1) ;rs.getString(2)?.let { toRSAPubKey(it) }} else null
      }
    } ?: throw AuthException("No suitable public key found", errorCode = HttpURLConnection.HTTP_UNAUTHORIZED)

    val rsa = Cipher.getInstance("RSA")
    rsa.init(Cipher.DECRYPT_MODE, pubkey)
    val decryptedResponse = rsa.doFinal(response)
    if (decryptedResponse==challenge) return user!! else return null
  }

  fun registerkey(user: String, pubkey: String, appname: String?, keyid: Long? = null) {
    if (keyid != null) {
      connection.prepareStatement("UPDATE `pubkeys` SET pubkey=? , appname=? WHERE `keyid`=? AND `user`=?") {
        params(pubkey) + appname + keyid + user
        if (executeUpdate()==0) throw AuthException("Failure to update the authentication key")
      }
    } else {
      connection.prepareStatement("INSERT INTO `pubkeys` (user, appname, pubkey, lastUse) VALUES (?,?,?,?)") {
        params(user) + appname + pubkey + now
        if (executeUpdate()==null) throw AuthException("Failure to store the authentication key")
      }
    }
  }

  fun userFromToken(token: String, remoteAddr: String): String? {
    connection.prepareStatement("SELECT user FROM tokens WHERE token=? AND ip=?") {
      params(token) + remoteAddr
      execute {
        if (it.next() && it.isLast) {
          return it.getString(1).apply { updateTokenEpoch(token, remoteAddr) }
        }
        return null
      }
    }
  }

  private inline fun updateTokenEpoch(token: String, remoteAddr: String) {
    connection.prepareStatement("UPDATE tokens SET epoch=? WHERE token=? and ip=?") {
      params(now) + token + remoteAddr
      if (executeUpdate() == 0) throw AuthException("Failure to update the authentication token".appendWarnings(warningsIt))
    }

  }

  /**
   * Update the auth token for the given user. This may change the token, but normally doesn't.
   *
   * @return The relevant authToken.
   */
  fun updateAuthToken(username: String, token: String, remoteAddr: String): String {
    connection.prepareStatement("UPDATE `tokens` SET `epoch`= ? WHERE user = ? and token=? and ip=?") {
      params(now) + username + token + remoteAddr
      if (executeUpdate() == 0) throw AuthException("Failure to update the authentication token".appendWarnings(warningsIt))
    }
    return token
  }

  fun isUserInRole(user: String, role: String): Boolean {
    connection.prepareStatement("SELECT user FROM user_roles where user=? and role=?") {
      params(user) + role
      return executeHasRows() // If there is an item,
    }
  }

  fun getUserRoles(user: String): List<String> {
    connection.prepareStatement("SELECT role FROM user_roles WHERE user=?") {
      params(user)
      return mutableListOf<String>().apply {
        execute {
          while (it.next()) {
            add(it.getString(1))
          }
        }
      }

    }
  }

  fun keyInfo(user:String) : List<out KeyInfo>  {
    connection.prepareStatement("SELECT keyid, appname, lastUse FROM pubkeys where user=?") {
      params(user)
      return executeEach { rs -> KeyInfo(rs.getLong(1), rs.getString(2), rs.getLong(3)) }
    }
  }

  fun cleanAuthTokens() {
    connection.prepareStatement("DELETE FROM tokens WHERE `epoch` < ?") {
      setLong(1, now - MAXTOKENLIFETIME)
      executeUpdate()
    }
  }

  fun verifyResetToken(user: String, resetToken: String): Boolean {
    connection.prepareStatement("SELECT UNIX_TIMESTAMP()-UNIX_TIMESTAMP(`resettime`) AS `age` FROM `users` WHERE `user`=? AND `resettoken`=?") {
      params(user) + resetToken
      execute {
        if (it.next()) {
          return it.getLong(1) < MAX_RESET_VALIDITY
        } else {
          return false
        }
      }
    }
  }

  fun logout(authToken: String): Unit {
    connection.prepareStatement("DELETE FROM tokens WHERE token = ?") {
      params (authToken)
      execute()
    }
  }

  fun alias(user: String): String? {
    connection.prepareStatement("SELECT alias FROM users WHERE user=?") {
      params(user)
      return stringResult()
    }
  }

  fun fullname(user:String): String? {
    connection.prepareStatement("SELECT fullname FROM users WHERE user=?") {
      params(user)
      return stringResult()
    }
  }

  fun isLocalAccount(user:String): Boolean {
    connection.prepareStatement("SELECT password FROM users WHERE user=?") {
      params(user)
      return ! stringResult().isNullOrBlank()
    }
  }

}

class KeyInfo(val keyId:Long, val appname:String?, val lastUse: Long)

class AuthException(msg: String, cause: Throwable? = null, val errorCode:Int=HttpURLConnection.HTTP_INTERNAL_ERROR) : RuntimeException(msg, cause) {
  override val message: String
    get() = super.message!!
}


/** Helper function to create, use and dismiss an [AccountDb] instance. It will open the database and execute the
 * lambda it. There is no direct access to the underlying database connection.
 *
 * @param block The code to execute in relation to the database.
 */
inline fun <R> accountDb(resourceName: String = DBRESOURCE, block: AccountDb.() -> R): R {

  val ic = InitialContext()
  //    val username = ic.lookup(AUTHDBADMINUSERNAME) as String
  //    val password = ic.lookup(AUTHDBADMINPASSWORD) as String
  return accountDb(ic.lookup(resourceName) as DataSource, block)
}

inline fun <R> accountDb(dataSource: DataSource, block: AccountDb.() -> R): R {
  return dataSource.connection { AccountDb(it).block() }
}

