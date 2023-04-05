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

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

abstract class TaskList constructor(
    serviceName: String,
    authService: AuthService,
    private val engineService: EngineService,
    clientAuth: PmaIdSecretAuthInfo
) : AbstractRunnableUiService(authService, clientAuth), TaskListService {
//    val nodeInstanceHandle: PNIHandle? get() = activityAccessToken?.nodeInstanceHandle

    override val serviceName: ServiceName<TaskList> = ServiceName(serviceName)

    override val serviceInstanceId: ServiceId<TaskList> = ServiceId(getServiceId(clientAuth))

    private val engineTokens = mutableMapOf<Long, PmaAuthToken>()

/*
    override fun acceptActivity(aic: PMAActivityContext<*>, user: PrincipalCompat) {
        val token = requireNotNull(engineTokens[aic.nodeInstanceHandle.handleValue]) { "No reply token for task(${aic.nodeInstanceHandle})" }
        engineService.acceptActivity(token, aic.nodeInstanceHandle, user, emptyList())
    }
*/

    override fun getServiceState(): String = engineTokens.entries.joinToString(prefix = "Active tasks: [", postfix = "]") { (handle, token) -> "$handle -> $token" }

    fun postTask(
        authInfo: PmaAuthToken,
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
        authToken: PmaAuthInfo,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(authToken, nodeInstanceHandle)
        validateAuthInfo(authToken, CommonPMAPermissions.POST_TASK)
        engineTokens.remove(nodeInstanceHandle.handleValue)
    }

    fun contextImpl(browser: Browser): Context = ContextImpl(browser)

    fun acceptActivity(
        authToken: PmaAuthToken,
        principal: PrincipalCompat,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>,
        processNodeInstance: Handle<SecureProcessNodeInstance>
    ): AuthorizationCode {
        logMe(processNodeInstance, principal)

        validateAuthInfo(authToken, CommonPMAPermissions.ACCEPT_TASK)
        val activityAccessToken = engineTokens[processNodeInstance.handleValue] ?: throw AuthorizationException("Task list has no access to activity $processNodeInstance")
        val userAuthorization =
            engineService.acceptActivity(activityAccessToken,  processNodeInstance, principal, pendingPermissions)

        return userAuthorization

    }

    interface Context {
        fun uiServiceLogin(service: AbstractRunnableUiService): PmaAuthToken
    }

    private inner class ContextImpl(val browser: Browser) : Context {
        override fun uiServiceLogin(service: AbstractRunnableUiService): PmaAuthToken {
            logMe(service.serviceName)
            return browser.loginToService(service)
//            return authService.getAuthTokenDirect(browser.user, taskIdentityToken!!, service, ANYSCOPE)
        }

    }

}
