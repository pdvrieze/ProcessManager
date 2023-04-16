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

package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.impl.Level
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ANYSCOPE
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.util.multiplatform.PrincipalCompat

class Browser constructor(private val authService: AuthService, val auth: PmaIdSecretAuthInfo) {
    val user: PrincipalCompat get() = auth.principal
    private val tokens = mutableListOf<PmaAuthToken>()
    val logger: LoggerCompat get() = authService.logger

    init {
        tokens.add(authService.loginDirect(auth))
    }

    fun addToken(authToken: PmaAuthToken) {
        logger.log(Level.INFO, "Browser(${user.name}).addToken($authToken)")
        // Remove previous token for the service
        tokens.removeIf { it.serviceId == authToken.serviceId && it.nodeInstanceHandle == authToken.nodeInstanceHandle }
        tokens.add(authToken)
    }

    fun addToken(authService: AuthService, authorizationCode: AuthorizationCode) {
        logger.log(Level.INFO, "Browser(${user.name}).addTokenFromAuth($authorizationCode)")
        val auth =
            tokens.lastOrNull { it.scope == CommonPMAPermissions.IDENTIFY && it.serviceId == authService.serviceInstanceId }
                ?: this.auth

        addToken(authService.exchangeAuthCode(auth, authorizationCode))
    }

    fun loginToService(service: RunnableUiService): PmaAuthToken {
        tokens.removeIf { ! authService.isTokenValid(auth, it) }
        tokens.lastOrNull { it.serviceId == service.serviceInstanceId }?.let {
            logger.log(
                Level.INFO,
                "Browser(${user.name}).loginToService(${service.serviceInstanceId}) = already logged in - $it"
            )
            return it
        }
        return service.loginBrowser(this).also {
            addToken(it)
        }
    }

    fun loginToService(authService: AuthService, service: Service): AuthorizationCode {
        return loginToService(service.serviceInstanceId, authService)
    }

    private fun loginToService(
        serviceId: ServiceId<Service>,
        authService: AuthService
    ): AuthorizationCode {
        logger.log(Level.INFO, "Browser(${user.name}).loginToService($serviceId)")
        tokens.removeIf { ! authService.isTokenValid(auth, it) }
        val token =
            tokens.lastOrNull { it.scope.includes(CommonPMAPermissions.IDENTIFY) && it.serviceId == authService.serviceInstanceId }
                ?: throw AuthorizationException("Not logged in to authorization service")
        return authService.getAuthorizationCode(token, serviceId, ANYSCOPE)
    }
}
