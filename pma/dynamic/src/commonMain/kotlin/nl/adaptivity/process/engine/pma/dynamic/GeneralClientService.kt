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

import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceName
import java.security.Principal

class GeneralClientService(
    serviceName: ServiceName<GeneralClientService>,
    authService: AuthService,
    adminAuth: PmaAuthInfo
) : ServiceBase<GeneralClientService>(authService, adminAuth, serviceName), AutomatedService {
//    override val serviceName: ServiceName<GeneralClientService> = ServiceName(serviceName)
//    override val serviceInstanceId: ServiceId<GeneralClientService> = ServiceId(getServiceId(serviceAuth))

    fun <R> runWithAuthorization(
        authorizationCode: AuthorizationCode,
        action: ClientServiceContext.(PmaAuthToken) -> R
    ): R {
        val authToken = authServiceClient.exchangeAuthCode(authorizationCode)
        return ContextImpl(authToken).action(authToken)
        // Perhaps explicitly release the token.
    }

    override fun getServiceState(): String = ""

    val auth get() = authServiceClient.originatingClientAuth

    private inner class ContextImpl(private val authToken: PmaAuthToken) : ClientServiceContext {
        override val automatedService: Principal
            get() = authServiceClient.principal

        /**
         * Get a token that provides access to the given service. It is expected that permission for this
         * has been granted.
         */
        override fun getServiceToken(service: Service, scope: AuthScope): PmaAuthToken {
            logMe(service.serviceName, scope)
            return authServiceClient.exchangeDelegateToken(authToken, service.serviceInstanceId, scope)

        }
    }

    interface ClientServiceContext {
        val automatedService: Principal
        fun getServiceToken(service: Service, scope: AuthScope): PmaAuthToken
    }

}
