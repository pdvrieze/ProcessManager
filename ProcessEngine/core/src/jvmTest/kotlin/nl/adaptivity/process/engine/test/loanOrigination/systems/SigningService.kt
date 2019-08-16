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
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthToken
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.auth.ServiceImpl
import java.security.Principal

class SigningService(authService: AuthService): ServiceImpl(authService, "SigningService") {

    override fun getServiceState(): String = ""

    fun <V> signDocument(authInfo: AuthToken, document: V): SignedDocument<V> {
        logMe(document)
        validateAuthInfo(authInfo, LoanPermissions.SIGN)
        return SignedDocument(authInfo.principal.name, authInfo.nodeInstanceHandle.handleValue, document)
    }

}

@Serializable
class SignedDocument<V>(val signedBy: String, val nodeInstanceHandle: Long, val document: V)
