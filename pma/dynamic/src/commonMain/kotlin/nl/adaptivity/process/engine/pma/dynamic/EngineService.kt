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
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.pma.CommonPMAPermissions.GRANT_GLOBAL_PERMISSION
import nl.adaptivity.process.engine.pma.CommonPMAPermissions.UPDATE_ACTIVITY_STATE
import nl.adaptivity.process.engine.pma.dynamic.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.ServiceImpl
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.PermissionScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.engine.pma.runtime.PMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import kotlin.random.Random

class EngineService(
    authService: AuthService,
    serviceAuth: IdSecretAuthInfo = newEngineClientAuth(authService),
) : ServiceImpl(authService, serviceAuth), AutomatedService {

    private val taskLists: MutableMap<PNIHandle, List<TaskList>> = mutableMapOf()

    override fun getServiceState(): String = ""

    /**
     * Accept the activity and return an authorization code for the user to identify itself with in relation to
     * the activity.
     */
    fun acceptActivity(
        authToken: AuthToken,
        nodeInstanceHandle: PNIHandle,
        principal: Principal,
        pendingPermissions: ArrayDeque<DynamicPMAActivityContext.PendingPermission>,
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

            val clientId = principal.name
            // Also use the result to register permissions for it
            while (pendingPermissions.isNotEmpty()) {
                val pendingPermission = pendingPermissions.removeFirst()
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


    fun registerActivityToTaskList(taskList: TaskList, pniHandle: PNIHandle) {
        logMe(pniHandle)

        val permissions = listOf(
            UPDATE_ACTIVITY_STATE(pniHandle),
            CommonPMAPermissions.ACCEPT_TASK(pniHandle)
        )
        val taskListToEngineAuthToken = authService.createAuthorizationCode(
            serviceAuth,
            taskList.serviceId,
            pniHandle,
            this,
            UnionPermissionScope(permissions)
        )


        /* TODO use an activity specific token to access the task list. Try to treat it like any
           other service. Let the task list use a delegate token. Also see whether ACCEPT_TASK can be given
            to the user (and delegated to the worklist).
         */

        val taskListAuth = authTokenForService(taskList)
        taskLists.merge(pniHandle, listOf(taskList)) { old, new -> old + new }

        taskList.postTask(taskListAuth, taskListToEngineAuthToken, pniHandle)
    }

    fun createAuthorizationCode(
        clientServiceId: String,
        handle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        service: AuthService,
        scope: CommonPMAPermissions.IDENTIFY,
        pendingPermissions: ArrayDeque<DynamicPMAActivityContext.PendingPermission>
    ): AuthorizationCode {
        return authService.createAuthorizationCode(serviceAuth, clientServiceId, handle, service, scope)
            .also { serviceAuthorization ->
                while (pendingPermissions.isNotEmpty()) {
                    val pendingPermission = pendingPermissions.removeFirst()
                    authService.grantPermission(
                        serviceAuth, serviceAuthorization, authService,
                        CommonPMAPermissions.GRANT_ACTIVITY_PERMISSION.restrictTo(
                            handle,
                            pendingPermission.clientId ?: clientServiceId,
                            pendingPermission.service,
                            pendingPermission.scope
                        )
                    )
                }
            }
    }

    fun PMAProcessInstanceContext<*>.onActivityTermination(
        processNodeInstance: IProcessNodeInstance
    ) {
        val nodeInstanceHandle = processNodeInstance.handle
        val taskLists = taskLists[nodeInstanceHandle] ?: emptyList()

        for (taskList in taskLists) {
            val authToken = authTokenForService(taskList)
            taskList.unregisterTask(authToken, nodeInstanceHandle)
        }

        authService.invalidateActivityTokens(serviceAuth, processNodeInstance.handle)

    }

    fun registerGlobalPermission(
        principal: PrincipalCompat,
        service: Service,
        scope: PermissionScope
    ) {
        authService.registerGlobalPermission(serviceAuth, principal, service, scope)
    }
}

private fun newEngineClientAuth(authService: AuthService): IdSecretAuthInfo {
    return authService.registerClient("ProcessEngine", Random.nextString()).also {
        authService.registerGlobalPermission(null, it.principal, authService, UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(null, it.principal, authService, GRANT_GLOBAL_PERMISSION)
    }
}

