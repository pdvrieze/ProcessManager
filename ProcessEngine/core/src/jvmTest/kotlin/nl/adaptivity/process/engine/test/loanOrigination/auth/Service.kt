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

package nl.adaptivity.process.engine.test.loanOrigination.auth

import nl.adaptivity.process.engine.test.loanOrigination.Random
import nl.adaptivity.process.engine.test.loanOrigination.systems.AuthService
import nl.adaptivity.process.engine.test.loanOrigination.systems.Browser
import java.util.logging.Level
import kotlin.random.nextULong

interface Service {
    val serviceId: String
}

abstract class ServiceImpl(protected val authService: AuthService, protected val serviceAuth: IdSecretAuthInfo) : Service {
    private val tokens=mutableListOf<AuthToken>()

    override val serviceId: String get() = serviceAuth.principal.name

    abstract fun getServiceState(): String

    @UseExperimental(ExperimentalUnsignedTypes::class)
    constructor(authService: AuthService, name: String): this(authService, authService.registerClient(name, Random.nextULong().toString(16)))

    protected fun validateAuthInfo(authInfo: AuthInfo, scope: UseAuthScope) {
        authService.validateAuthInfo(this, authInfo, scope)
    }

    fun loginBrowser(browser: Browser): AuthToken {
        val authorization = browser.loginToService(authService, this)
        return authService.getAuthToken(serviceAuth, authorization)
    }

    fun authTokenForService(service: Service, scope: PermissionScope = ANYSCOPE): AuthToken {
        logMe(service.serviceId, scope)

        tokens.removeIf { authService.isTokenInvalid(it) }

        tokens.lastOrNull { it.serviceId == service.serviceId }?.let { return it }

        return authService.getAuthTokenDirect(serviceAuth, service, ANYSCOPE).also { tokens.add(it) }
    }

    fun logMe(vararg params: Any?) {
        val args = params.joinToString { it.toString() }
        val service = this.javaClass.simpleName.substringAfterLast('.')
        val methodName = Throwable().stackTrace[1].methodName
        val serviceState = getServiceState()
        val quotedServiceState = if (serviceState.isEmpty()) "" else "($serviceState)"
        authService.logger.log(Level.INFO, "$service$quotedServiceState.$methodName($args)")
    }
}
