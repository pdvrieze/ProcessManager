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

package nl.adaptivity.process.engine.pma.dynamic.services

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.models.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

interface PmaService : Service {
    val authServiceClient: DefaultAuthServiceClient
}

abstract class ServiceBase<S: ServiceBase<S>>(
    final override val authServiceClient: DefaultAuthServiceClient,
    final override val serviceName: ServiceName<S>,
    protected val logger: Logger
) : PmaService {

    final override val serviceInstanceId: ServiceId<S> =
        ServiceId(authServiceClient.originatingClientAuth.principal.name)

    constructor(authService: AuthService, serviceAuth: PmaIdSecretAuthInfo, serviceName: ServiceName<S>, logger: Logger = authService.logger)
        : this(DefaultAuthServiceClient(serviceAuth, authService), serviceName, logger)

    constructor(authService: AuthService, adminAuth: PmaAuthInfo , serviceName: ServiceName<S>, logger: Logger = authService.logger)
        : this(DefaultAuthServiceClient(authService.registerClient(adminAuth, serviceName, Random.nextString()), authService), serviceName, logger)

    private val tokens: MutableList<PmaAuthToken> = mutableListOf<PmaAuthToken>()
//    open val serviceInstanceId: ServiceId<*> = getServiceId(serviceAuth)

    open fun getServiceState(): String = "<No state>"

/*
    constructor(authService: AuthService, name: String, logger: Logger = authService.logger) : this(
        authService = authService,
        serviceName = ServiceName<S>(name),
        logger = logger
    )

    constructor(authService: AuthService, serviceName: ServiceName<S>, logger: Logger = authService.logger) : this(
        authService = authService,
        serviceAuth = authService.registerClient(serviceName, Random.nextULong().toString(16)),
        serviceName = serviceName,
        logger = logger
    )
*/

    protected fun Service.validateAuthInfo(authInfo: PmaAuthInfo, scope: UseAuthScope) {
        authServiceClient.validateAuthInfo(authInfo, serviceInstanceId, scope)
    }

    fun globalAuthTokenForService(service: Service, scope: AuthScope = ANYSCOPE): PmaAuthToken {
        logMe(service.serviceInstanceId, scope)

        tokens.removeAll { !authServiceClient.isTokenValid(it) } // TODO have bulk option

        tokens.lastOrNull { it.serviceId == service.serviceInstanceId }?.let { return it }

        return authServiceClient.getAuthTokenDirect(service.serviceInstanceId, ANYSCOPE).also { tokens.add(it) }
    }

    fun logMe(vararg params: Any?) {
        val args = params.joinToString { it.toString() }
        val service = this.javaClass.simpleName.substringAfterLast('.')
        val methodName = Throwable().stackTrace[1].methodName.substringBefore('-')
        val serviceState = getServiceState()
        val quotedServiceState = if (serviceState.isEmpty()) "" else "($serviceState)"
        logger.log(Level.INFO, "$service$quotedServiceState.$methodName($args)")
    }

    companion object {
        private val counters = mutableMapOf<String, Int>()

        fun getServiceId(serviceAuth: PmaIdSecretAuthInfo): String {
            val serviceName = serviceAuth.principal.name
            val id: Int = counters.merge(serviceName, 0) { oldValue, _ -> oldValue + 1 }!!
            return when {
                id > 0 -> "$serviceName#$id"
                else -> serviceName
            }
        }
    }
}

