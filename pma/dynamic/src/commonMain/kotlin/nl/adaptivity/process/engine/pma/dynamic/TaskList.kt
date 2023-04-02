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
import nl.adaptivity.process.engine.pma.dynamic.AbstractRunnableUIService
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class TaskList constructor(
    serviceName: String,
    authService: AuthService,
    private val engineService: EngineService,
    clientAuth: IdSecretAuthInfo,
    val principals: List<PrincipalCompat>
) : AbstractRunnableUIService(authService, clientAuth), TaskListService {
//    val nodeInstanceHandle: PNIHandle? get() = activityAccessToken?.nodeInstanceHandle

    constructor(
        serviceName: String,
        authService: AuthService,
        engineService: EngineService,
        clientAuth: IdSecretAuthInfo,
        principal: PrincipalCompat
    ) : this(serviceName, authService, engineService, clientAuth, listOf(principal))

    override val serviceName: ServiceName<TaskList> = ServiceName(serviceName)

    override val serviceInstanceId: ServiceId<TaskList> = ServiceId(getServiceId(clientAuth))

    private val engineTokens = mutableMapOf<Long, AuthToken>()

/*
    override fun acceptActivity(aic: PMAActivityContext<*>, user: PrincipalCompat) {
        val token = requireNotNull(engineTokens[aic.nodeInstanceHandle.handleValue]) { "No reply token for task(${aic.nodeInstanceHandle})" }
        engineService.acceptActivity(token, aic.nodeInstanceHandle, user, emptyList())
    }
*/

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
        pendingPermissions: Collection<DynamicPMAActivityContext.PendingPermission>,
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
        fun uiServiceLogin(service: AbstractRunnableUIService): AuthToken
    }

    private inner class ContextImpl(val browser: Browser) : Context {
        override fun uiServiceLogin(service: AbstractRunnableUIService): AuthToken {
            logMe(service.serviceName)
            return browser.loginToService(service)
//            return authService.getAuthTokenDirect(browser.user, taskIdentityToken!!, service, ANYSCOPE)
        }

    }

}
