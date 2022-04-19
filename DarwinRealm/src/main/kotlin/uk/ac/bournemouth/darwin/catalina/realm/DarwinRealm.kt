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

package uk.ac.bournemouth.darwin.catalina.realm


import org.apache.catalina.Lifecycle
import org.apache.catalina.Wrapper
import org.apache.catalina.connector.CoyotePrincipal
import org.apache.catalina.realm.GenericPrincipal
import org.apache.catalina.realm.RealmBase
import org.apache.naming.ContextBindings
import org.ietf.jgss.GSSContext
import uk.ac.bournemouth.darwin.accounts.AccountDb
import uk.ac.bournemouth.darwin.accounts.DBRESOURCE
import uk.ac.bournemouth.darwin.accounts.accountDb
import java.security.Principal
import java.util.logging.Logger
import javax.naming.Context
import javax.naming.NamingException
import javax.sql.DataSource


class DarwinRealm : RealmBase(), Lifecycle {

    var resourceName:String = DBRESOURCE
    var globalResource:Boolean = true

    private val log = Logger.getLogger(DarwinRealm::class.java.name)

    val dataSource by lazy {
        val context: Context = if (globalResource) server.globalNamingContext else ContextBindings.getClassLoader().lookup("java:comp/env/") as Context

        context.lookup(resourceName) as DataSource
    }

    override fun authenticate(username: String, credentials: String): Principal? {
        log.fine("Authentication requested for username $username")
        accountDb(dataSource) {
            if (verifyCredentials(username, credentials)) {
                return getDarwinPrincipal(this, username)
            } else {
                return null
            }

        }
    }

    override fun initInternal() {
        super.initInternal()
        log.info("Initialising Darwin Realm. Ensuring database tables")
        accountDb(dataSource) { ensureTables() }
        log.info("Account tables ensured")
    }

    override fun authenticate(username: String, digest: String, nonce: String, nc: String, cnonce: String, qop: String, realm: String, md5a2: String): Principal? {
        return null
        // Digest authentication using md5 is not supported.
    }

    override fun authenticate(gssContext: GSSContext, storeCreds: Boolean): Principal? {
        return null
    }

    override fun getPassword(username: String): String {
        throw UnsupportedOperationException("This implementation does not allow retrieving passwords.")
    }

    fun getName(): String {
        return NAME
    }

    fun getInfo(): String {
        return INFO
    }

    override fun hasRole(wrapper: Wrapper?, principal: Principal?, role: String): Boolean {
        val userPrincipal= when (principal) {
            is GenericPrincipal -> principal.userPrincipal.apply { if (this is GenericPrincipal) { return hasRole(role)} }
            else -> principal
        }

        return when (userPrincipal) {
            is CoyotePrincipal -> getDarwinPrincipal(userPrincipal.getName()).hasRole(role)
            else -> false
        }
    }

    private fun getDarwinPrincipal(name: String): DarwinUserPrincipalImpl {
        return DarwinUserPrincipalImpl(dataSource, name)
    }

    private fun getDarwinPrincipal(accountDb: AccountDb, user: String): DarwinUserPrincipalImpl {
        return DarwinUserPrincipalImpl(dataSource, user, accountDb.getUserRoles(user))
    }

    override fun getPrincipal(username: String): Principal {
        try {
            return getDarwinPrincipal(username)
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }

    }

    companion object {

        private val INFO = "uk.ac.bournemouth.darwin.catalina.realm.DarwinRealm/1.0"

        @SuppressWarnings("unused")
        private val NAME = "DarwinRealm"
    }


}
