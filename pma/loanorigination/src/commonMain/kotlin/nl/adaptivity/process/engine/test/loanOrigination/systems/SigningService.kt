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

import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions

class SigningService(serviceName: String, authService: AuthService): AbstractRunnableUiService(authService, "SigningService"), AutomatedService {

    override val serviceName: ServiceName<SigningService> = ServiceName(serviceName)
    override val serviceInstanceId: ServiceId<SigningService> = ServiceId(getServiceId(serviceAuth))

    override fun getServiceState(): String = ""

    fun <V> signDocument(authInfo: AuthToken, document: V): SignedDocument<V> {
        logMe(document)
        validateAuthInfo(authInfo, LoanPermissions.SIGN)
        return SignedDocument(authInfo.principal.name, authInfo.nodeInstanceHandle.handleValue, document)
    }

}

@Serializable
data class SignedDocument<V>(val signedBy: String, val nodeInstanceHandle: Long, val document: V)
