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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.LoanActivityContext
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import java.util.*

class TaskList constructor(
    authService: AuthService,
    private val engineService: EngineService,
    clientAuth: IdSecretAuthInfo,
    val principal: Principal
                          ) : ServiceImpl(authService, clientAuth) {
//    val nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>? get() = activityAccessToken?.nodeInstanceHandle

    override fun getServiceState(): String = principal.name

    private val tokens = mutableListOf<AuthToken>()

    fun postTask(
        authInfo: AuthToken,
        authorizationCode: AuthorizationCode,
        nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>
                ) {
        logMe(authInfo, authorizationCode, nodeInstanceHandle)
        validateAuthInfo(authInfo, LoanPermissions.POST_TASK)
        val token = authService.getAuthToken(serviceAuth, authorizationCode)
        assert(token.nodeInstanceHandle == nodeInstanceHandle)
        tokens.add(token)
    }

    fun unregisterTask(authToken: AuthInfo, nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>) {
        logMe(authToken, nodeInstanceHandle)
        validateAuthInfo(authToken, LoanPermissions.POST_TASK)
        tokens.removeIf { it.nodeInstanceHandle == nodeInstanceHandle }
    }

    fun contextImpl(browser: Browser): Context = ContextImpl(browser)

    fun acceptActivity(
        authToken: AuthToken,
        principal: Principal,
        pendingPermissions: ArrayDeque<LoanActivityContext.PendingPermission>,
        processNodeInstance: Handle<SecureObject<ProcessNodeInstance<*>>>
                      ): AuthorizationCode {
        logMe(processNodeInstance, principal)

        validateAuthInfo(authToken, LoanPermissions.ACCEPT_TASK)
        val activityAccessToken = tokens.single { it.nodeInstanceHandle == processNodeInstance }
        val userAuthorization =
            engineService.acceptActivity(activityAccessToken, processNodeInstance, principal, pendingPermissions)

        return userAuthorization

    }

    interface Context {
        fun loginToService(service: ServiceImpl): AuthToken
    }

    private inner class ContextImpl(val browser: Browser) : Context {
        override fun loginToService(service: ServiceImpl): AuthToken {
            logMe(service.serviceId)
            return browser.loginToService(service)
//            return authService.getAuthTokenDirect(browser.user, taskIdentityToken!!, service, ANYSCOPE)
        }

    }

}
