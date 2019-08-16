/*
 * Copyright (c) 2019.
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

package nl.adaptivity.process.engine.test.loanOrigination.systems

import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import kotlin.random.Random

class Browser private constructor(val auth: IdSecretAuthInfo) {
    val user get() = auth.principal
    private val tokens=mutableListOf<AuthToken>()

    constructor(authService: AuthService, user: Principal): this(authService.registerClient(user, Random.nextString())) {
        tokens.add(authService.loginDirect(auth))
    }

    fun addToken(authToken: AuthToken) {
        // Remove previous token for the service
        tokens.removeIf { it.serviceId == authToken.serviceId && it.nodeInstanceHandle == authToken.nodeInstanceHandle }
        tokens.add(authToken)
    }

    fun addToken(authService: AuthService, authorizationCode: AuthorizationCode) {
        addToken(authService.getAuthToken(auth, authorizationCode))
    }

    fun loginToService(service: ServiceImpl): AuthToken {
        return service.loginBrowser(this)
    }

    fun loginToService(authService: AuthService, service: ServiceImpl): AuthToken {
        tokens.removeIf { authService.isTokenInvalid(it) }
        val token = tokens.lastOrNull { it.scope == LoanPermissions.IDENTIFY && it.serviceId == authService.serviceId }
            ?: throw AuthorizationException("Not logged in to authorization service")
        return authService.getAuthTokenDirect(token, service, ANYSCOPE)
    }
}
