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

import net.devrieze.util.StringCache
import org.ietf.jgss.GSSCredential
import uk.ac.bournemouth.darwin.accounts.accountDb
import java.security.Principal
import java.util.*
import javax.security.auth.login.LoginContext
import javax.sql.DataSource

class DarwinUserPrincipalImpl(private val dataSource: DataSource, name: String, roles: List<out String> = Collections.emptyList(), userPrincipal: Principal? = null, loginContext: LoginContext? = null, gssCredential: GSSCredential? = null) : DarwinBasePrincipal(name, roles, userPrincipal, loginContext, gssCredential), DarwinUserPrincipal {

    constructor(dataSource: DataSource, name: String) : this(dataSource, name, Collections.emptyList<String>())

    /**
     * Get a set of all the roles in the principal. Note that this will create a
     * copy to allow concurrency and refreshes.
     */
    @Synchronized override fun getRolesSet(): Set<String> {
        synchronized(this) {
            refreshIfNeeded()
            return roles.toSet()
        }
    }

    override fun getRoles(): Array<String> {
        synchronized (this) {
            refreshIfNeeded()
            return roles
        }
    }

    @Synchronized private fun refreshIfNeeded() {
        if (needsRefresh()) {
            val newRoles = linkedSetOf<String>()
            accountDb(dataSource) {
                roles = getUserRoles(getName()).toTypedArray().apply { sort() }
            }
            notifyRefresh()
        }
    }

    override fun hasRole(role: String): Boolean {
        if ("*" == role) {
            return true
        }
        refreshIfNeeded()
        return roles.contains(role)
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append("DarwinUserPrincipal[").append(getName()).append('(')
        synchronized(this) {
            refreshIfNeeded()
            roles.joinTo(result)
        }
        result.append(")])")
        return result.toString()
    }

    override fun getEmail() = "$name@$DOMAIN"

    override fun isAdmin()= hasRole("admin")

    @Synchronized override fun cacheStrings(stringCache: StringCache): Principal {
        name = stringCache.lookup(this.name)

        val tmpRoles = roles
        roles = Array<String>(tmpRoles.size) {i -> stringCache.lookup(tmpRoles[i]) }
        return this
    }

    companion object {

        private val DOMAIN = "bournemouth.ac.uk"
    }

}
