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

import nl.adaptivity.process.engine.test.loanOrigination.Random
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import java.util.logging.Level

class Browser private constructor(private val authService: AuthService, val auth: IdSecretAuthInfo) {
    val user get() = auth.principal
    private val tokens=mutableListOf<AuthToken>()
    val logger get() = authService.logger

    constructor(authService: AuthService, user: Principal): this(authService, authService.registerClient(user, Random.nextString())) {
        addToken(authService.loginDirect(auth))
    }

    fun addToken(authToken: AuthToken) {
        logger.log(Level.INFO, "Browser(${user.name}).addToken($authToken)")
        // Remove previous token for the service
        tokens.removeIf { it.serviceId == authToken.serviceId && it.nodeInstanceHandle == authToken.nodeInstanceHandle }
        tokens.add(authToken)
    }

    fun addToken(authService: AuthService, authorizationCode: AuthorizationCode) {
        logger.log(Level.INFO, "Browser(${user.name}).addTokenFromAuth($authorizationCode)")
        val auth = tokens.lastOrNull { it.scope == LoanPermissions.IDENTIFY && it.serviceId == authService.serviceId }
            ?: this.auth

        addToken(authService.getAuthToken(auth, authorizationCode))
    }

    fun loginToService(service: ServiceImpl): AuthToken {
        tokens.removeIf { authService.isTokenInvalid(it) }
        tokens.lastOrNull { it.serviceId == service.serviceId }?.let {
            logger.log(Level.INFO, "Browser(${user.name}).loginToService(${service.serviceId}) = already logged in - $it")
            return it
        }
        return service.loginBrowser(this).also {
            addToken(it)
        }
    }

    fun loginToService(authService: AuthService, service: ServiceImpl): AuthorizationCode {
        logger.log(Level.INFO, "Browser(${user.name}).loginToService(${service.serviceId})")
        tokens.removeIf { authService.isTokenInvalid(it) }
        val token = tokens.lastOrNull { it.scope == LoanPermissions.IDENTIFY && it.serviceId == authService.serviceId }
            ?: throw AuthorizationException("Not logged in to authorization service")
        return authService.getAuthorizationCode(token, service, ANYSCOPE)
    }
}
