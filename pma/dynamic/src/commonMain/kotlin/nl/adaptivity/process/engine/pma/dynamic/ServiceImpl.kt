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

package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.models.*
import java.util.logging.Level
import kotlin.random.Random
import kotlin.random.nextULong

abstract class ServiceImpl(protected val authService: AuthService, protected val serviceAuth: IdSecretAuthInfo) {
    private val tokens = mutableListOf<AuthToken>()
//    open val serviceInstanceId: ServiceId<*> = getServiceId(serviceAuth)

    abstract fun getServiceState(): String

    constructor(authService: AuthService, name: String) : this(
        authService,
        authService.registerClient(
            name,
            Random.nextULong().toString(16)
        )
    )

    protected fun Service.validateAuthInfo(authInfo: AuthInfo, scope: UseAuthScope) {
        authService.validateAuthInfo(this, authInfo, scope)
    }

    fun globalAuthTokenForService(service: Service, scope: AuthScope = ANYSCOPE): AuthToken {
        logMe(service.serviceInstanceId, scope)

        tokens.removeAll { authService.isTokenInvalid(it) }

        tokens.lastOrNull { it.serviceId == service.serviceInstanceId }?.let { return it }

        return authService.getAuthTokenDirect(serviceAuth, service, ANYSCOPE).also { tokens.add(it) }
    }

    fun logMe(vararg params: Any?) {
        val args = params.joinToString { it.toString() }
        val service = this.javaClass.simpleName.substringAfterLast('.')
        val methodName = Throwable().stackTrace[1].methodName.substringBefore('-')
        val serviceState = getServiceState()
        val quotedServiceState = if (serviceState.isEmpty()) "" else "($serviceState)"
        authService.logger.log(Level.INFO, "$service$quotedServiceState.$methodName($args)")
    }

    companion object {
        private val counters = mutableMapOf<String, Int>()

        fun getServiceId(serviceAuth: IdSecretAuthInfo): String {
            val serviceName = serviceAuth.principal.name
            val id: Int = counters.merge(serviceName, 0) { oldValue, _ -> oldValue + 1 }!!
            return when {
                id > 0 -> "$serviceName#$id"
                else -> serviceName
            }
        }
    }
}

interface RunnableUIService: UIService {
    fun loginBrowser(browser: Browser): AuthToken
    override val serviceName: ServiceName<RunnableUIService>
    override val serviceInstanceId: ServiceId<RunnableUIService>
}

abstract class AbstractRunnableUIService : ServiceImpl, RunnableUIService {

    constructor(authService: AuthService, serviceAuth: IdSecretAuthInfo) : super(authService, serviceAuth)
    constructor(authService: AuthService, name: String) : super(authService, name)

    abstract override val serviceName: ServiceName<AbstractRunnableUIService>
    abstract override val serviceInstanceId: ServiceId<AbstractRunnableUIService>

    override fun loginBrowser(browser: Browser): AuthToken {
        val authorization = browser.loginToService(authService, this)
        return authService.getAuthToken(serviceAuth, authorization)
    }

}
