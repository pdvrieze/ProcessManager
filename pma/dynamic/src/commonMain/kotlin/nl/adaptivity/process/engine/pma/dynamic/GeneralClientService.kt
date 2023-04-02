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

import nl.adaptivity.process.engine.pma.dynamic.ServiceImpl
import nl.adaptivity.process.engine.pma.models.*
import java.security.Principal

class GeneralClientService(
    serviceName: String,
    authService: AuthService
) : ServiceImpl(authService, "<automated>"), AutomatedService {
    override val serviceName: ServiceName<GeneralClientService> = ServiceName(serviceName)
    override val serviceInstanceId: ServiceId<GeneralClientService> = ServiceId(getServiceId(serviceAuth))

    fun <R> runWithAuthorization(
        authorizationCode: AuthorizationCode,
        action: ClientServiceContext.(AuthToken) -> R
                                ): R {
        val authToken = authService.getAuthToken(serviceAuth, authorizationCode)
        return ContextImpl(authToken).action(authToken)
        // Perhaps explicitly release the token.
    }

    override fun getServiceState(): String = ""

    val auth get() = serviceAuth

    private inner class ContextImpl(private val authToken: AuthToken) : ClientServiceContext {
        override val automatedService: Principal
            get() = auth.principal

        /**
         * Get a token that provides access to the given service. It is expected that permission for this
         * has been granted.
         */
        override fun getServiceToken(service: Service, scope: AuthScope): AuthToken {
            logMe(service.serviceName, scope)
            return authService.getAuthTokenDirect(authToken, service, scope)

        }
    }
}

interface ClientServiceContext {
    val automatedService: Principal
    fun getServiceToken(service: Service, scope: AuthScope): AuthToken
}
