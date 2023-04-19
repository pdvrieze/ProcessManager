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

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.runtime.TokenPrincipal
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat

/**
 * An authorization token for PMA. Note that this is a bearer token (it doesn't record the client itself)
 * @param principal the user that is identified with the token. This can be a service (process engine/execution service/process owner).
 * @property nodeInstanceHandle The handle of the nodeInstance with this token (or invalid when relevant)
 * @property token The actual token
 * @property serviceId The service for which the token is valid. All tokens are single-target tokens (but can be used
 *    for delegation if that is in the scope).
 * @property scope The scope that specifies what this token is authorized to do.
 */
class PmaAuthToken(
    principal: PrincipalCompat,
    val nodeInstanceHandle: PNIHandle,
    override val token: String,
    val serviceId: ServiceId<*>,
    val scope: AuthScope
): PmaAuthInfo(), AuthorizationInfo.Token {

    override val principal: PrincipalCompat = Principal(serviceId.serviceId)

    override fun toString(): String {
        return "AuthToken($token - $principal[act=${nodeInstanceHandle.handleValue}] -> $serviceId.${scope.description})"
    }

    inner class Principal(private val name: String) : PrincipalCompat, TokenPrincipal {
        override fun getName(): String = name
        override val serviceId: ServiceId<*> get() = this@PmaAuthToken.serviceId
        override val scope: AuthScope get() = this@PmaAuthToken.scope
    }
}
