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
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat

class PmaAuthToken(
    principal: PrincipalCompat,
    val nodeInstanceHandle: PNIHandle,
    override val token: String,
    val serviceId: ServiceId<*>,
    val scope: AuthScope
): PmaAuthInfo(principal), AuthorizationInfo.Token {



    override fun toString(): String {
        return "AuthToken($token - $principal[act=${nodeInstanceHandle.handleValue}] -> $serviceId.${scope.description})"
    }
}
