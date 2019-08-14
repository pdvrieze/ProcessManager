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

import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthToken
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthorizationCode
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import java.security.Principal

class TaskList(private val authService:AuthService, private val clientAuth: IdSecretAuthInfo, val principal: Principal) {
    private val tokens = mutableListOf<AuthToken>()

    var taskIdentityToken: AuthToken? = null
        private set

    fun registerToken(authorizationCode: AuthorizationCode): AuthToken {
        val token = authService.getAuthToken(clientAuth, authorizationCode)
        tokens.add(token)


        return authService.createTaskIdentityToken(clientAuth, token.nodeInstanceHandle, principal).also {
            taskIdentityToken = it
        }
    }

    val clientId get() = clientAuth.principal.name
}
