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
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.GRANT_GLOBAL_PERMISSION
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.UPDATE_ACTIVITY_STATE
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import kotlin.random.Random

class EngineService(
    serviceName: String,
    authService: AuthService,
    serviceAuth: PmaIdSecretAuthInfo = newEngineClientAuth(authService),
) : ServiceBase(authService, serviceAuth), AutomatedService {

    val authServiceClient: AuthServiceClient = DefaultAuthServiceClient(serviceAuth, authService)

    private val taskLists: MutableMap<Handle<SecureProcessNodeInstance>, List<TaskList>> = mutableMapOf()

    override val serviceName: ServiceName<EngineService> = ServiceName(serviceName)

    override val serviceInstanceId: ServiceId<EngineService> = ServiceId(getServiceId(serviceAuth))

    override fun getServiceState(): String = ""

    /**
     * Accept the activity and return an authorization code for the user to identify itself with in relation to
     * the activity.
     */
    fun acceptActivity(
        authToken: PmaAuthToken,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        principal: Principal,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>,
    ): AuthorizationCode {
        logMe(authToken, nodeInstanceHandle, principal)
        validateAuthInfo(
            authToken,
            CommonPMAPermissions.ACCEPT_TASK(nodeInstanceHandle)
        ) // TODO mark correct expected permission
        if (nodeInstanceHandle != authToken.nodeInstanceHandle) throw IllegalArgumentException("Mismatch with node instances")
        // Should register owner.
//        val taskAuthorizationCode = authService.createAuthorizationCode(serviceAuth, authToken.principal.name, authToken.nodeInstanceHandle, this)
        return authService.createAuthorizationCode(
            serviceAuth,
            principal.name,
            authToken.nodeInstanceHandle,
            authService,
            CommonPMAPermissions.IDENTIFY
        ).also { authorizationCode ->
            val permissionsToProvide = ArrayDeque(pendingPermissions)

            val clientId = principal.name
            // Also use the result to register permissions for it
            while (permissionsToProvide.isNotEmpty()) {
                val pendingPermission = permissionsToProvide.removeFirst()
                authService.grantPermission(
                    serviceAuth,
                    authorizationCode,
                    authService,
                    CommonPMAPermissions.GRANT_ACTIVITY_PERMISSION.restrictTo(
                        nodeInstanceHandle,
                        clientId,
                        pendingPermission.service,
                        pendingPermission.scope
                    )
                )
            }

        }
    }


    fun doPostTaskToTasklist(
        taskList: TaskList,
        pniHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(pniHandle)

        val permissions = listOf(
            UPDATE_ACTIVITY_STATE(pniHandle),
            CommonPMAPermissions.ACCEPT_TASK(pniHandle)
        )
        val taskListToEngineAuthToken = authService.createAuthorizationCode(
            serviceAuth,
            taskList.serviceInstanceId.serviceId,
            pniHandle,
            this,
            UnionPermissionScope(permissions)
        )


        /* TODO use an activity specific token to access the task list. Try to treat it like any
           other service. Let the task list use a delegate token. Also see whether ACCEPT_TASK can be given
            to the user (and delegated to the worklist).
         */

        val taskListAuth = globalAuthTokenForService(taskList)
        taskLists.merge(pniHandle, listOf(taskList)) { old, new -> old + new }

        taskList.postTask(taskListAuth, taskListToEngineAuthToken, pniHandle)
    }

    fun createServiceAuthToken(handle: PNIHandle, service: Service, scope: AuthScope): PmaAuthToken {
        return authService.requestPmaAuthToken(serviceAuth, handle, service.serviceInstanceId, scope)
    }

    fun createAuthorizationCode(
        clientId: String,
        handle: PNIHandle,
        service: Service,
        scope: AuthScope,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>
    ): AuthorizationCode {
        val pendingPermissions = ArrayDeque(pendingPermissions)
        return authService.createAuthorizationCode(serviceAuth, clientId, handle, service, scope)
            .also { serviceAuthorization ->
                while (pendingPermissions.isNotEmpty()) {
                    val pendingPermission = pendingPermissions.removeFirst()
                    authService.grantPermission(
                        serviceAuth, serviceAuthorization, authService,
                        CommonPMAPermissions.GRANT_ACTIVITY_PERMISSION.restrictTo(
                            handle,
                            pendingPermission.clientId ?: clientId,
                            pendingPermission.service,
                            pendingPermission.scope
                        )
                    )
                }
            }
    }

    fun createAuthToken(
        clientId: String,
        handle: Handle<SecureProcessNodeInstance>,
        service: Service,
        scope: AuthScope
    ): PmaAuthToken {
        val authorizationCode = authService.createAuthorizationCode(serviceAuth, clientId, handle, service, scope)
        return authService.getAuthToken(serviceAuth, authorizationCode)
    }

    fun ProcessInstanceContext.onActivityTermination(
        processNodeInstance: IProcessNodeInstance
    ) {
        val nodeInstanceHandle = processNodeInstance.handle
        val taskLists = taskLists[nodeInstanceHandle] ?: emptyList()

        for (taskList in taskLists) {
            val authToken = globalAuthTokenForService(taskList)
            taskList.unregisterTask(authToken, nodeInstanceHandle)
        }

        authService.invalidateActivityTokens(serviceAuth, processNodeInstance.handle)

    }

    fun registerGlobalPermission(
        principal: PrincipalCompat,
        service: Service,
        scope: AuthScope
    ) {
        authService.registerGlobalPermission(serviceAuth, principal, service, scope)
    }
}

private fun newEngineClientAuth(authService: AuthService): PmaIdSecretAuthInfo {
    return authService.registerClient("ProcessEngine", Random.nextString()).also {
        authService.registerGlobalPermission(null, it.principal, authService, UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(null, it.principal, authService, GRANT_GLOBAL_PERMISSION)
    }
}

