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

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.pma.dynamic.UIServiceImpl
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class TaskList constructor(
    authService: AuthService,
    private val engineService: EngineService,
    clientAuth: IdSecretAuthInfo,
    val principals: List<PrincipalCompat>
) : UIServiceImpl(authService, clientAuth), TaskListService {
//    val nodeInstanceHandle: PNIHandle? get() = activityAccessToken?.nodeInstanceHandle

    constructor(
        authService: AuthService,
        engineService: EngineService,
        clientAuth: IdSecretAuthInfo,
        principal: PrincipalCompat
    ) : this(authService, engineService, clientAuth, listOf(principal))

    private val engineTokens = mutableMapOf<Long, AuthToken>()

    override fun acceptActivity(aic: PMAActivityContext<*>, user: PrincipalCompat) {
        val token = requireNotNull( engineTokens[aic.nodeInstanceHandle.handleValue]) { "No reply token for task(${aic.nodeInstanceHandle})" }
        engineService.acceptActivity(token, aic.nodeInstanceHandle, user, emptyList())
    }

    override fun getServiceState(): String = principals.joinToString(prefix = "[", postfix = "]")

    override fun servesFor(principal: PrincipalCompat): Boolean {
        return principal in principals
    }

    fun postTask(
        authInfo: AuthToken,
        authorizationCode: AuthorizationCode,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(authInfo, authorizationCode, nodeInstanceHandle)
        validateAuthInfo(authInfo, CommonPMAPermissions.POST_TASK)
        val token = authService.getAuthToken(serviceAuth, authorizationCode)
        assert(token.nodeInstanceHandle == nodeInstanceHandle)
        engineTokens[nodeInstanceHandle.handleValue] = token
    }

    fun unregisterTask(
        authToken: AuthInfo,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(authToken, nodeInstanceHandle)
        validateAuthInfo(authToken, CommonPMAPermissions.POST_TASK)
        engineTokens.remove(nodeInstanceHandle.handleValue)
    }

    fun contextImpl(browser: Browser): Context = ContextImpl(browser)

    fun acceptActivity(
        authToken: AuthToken,
        principal: PrincipalCompat,
        pendingPermissions: ArrayDeque<DynamicPMAActivityContext.PendingPermission>,
        processNodeInstance: Handle<SecureProcessNodeInstance>
    ): AuthorizationCode {
        logMe(processNodeInstance, principal)

        validateAuthInfo(authToken, CommonPMAPermissions.ACCEPT_TASK)
        val activityAccessToken = engineTokens[processNodeInstance.handleValue] ?: throw AuthorizationException("Task list has no access to activity $processNodeInstance")
        val userAuthorization =
            engineService.acceptActivity(activityAccessToken, processNodeInstance, principal, pendingPermissions)

        return userAuthorization

    }

    interface Context {
        fun uiServiceLogin(service: UIServiceImpl): AuthToken
    }

    private inner class ContextImpl(val browser: Browser) : Context {
        override fun uiServiceLogin(service: UIServiceImpl): AuthToken {
            logMe(service.serviceId)
            return browser.loginToService(service)
//            return authService.getAuthTokenDirect(browser.user, taskIdentityToken!!, service, ANYSCOPE)
        }

    }

}
