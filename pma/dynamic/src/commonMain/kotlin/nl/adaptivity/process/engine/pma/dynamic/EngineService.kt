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
import nl.adaptivity.process.engine.pma.dynamic.ServiceActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.GRANT_GLOBAL_PERMISSION
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.UPDATE_ACTIVITY_STATE
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import kotlin.random.Random

class EngineService(
    serviceName: String,
    authService: AuthService,
    serviceAuth: PmaIdSecretAuthInfo = newEngineClientAuth(authService, ServiceName(serviceName)),
) : ServiceBase(authService, serviceAuth), AutomatedService {

    val authServiceClient: DefaultAuthServiceClient = DefaultAuthServiceClient(serviceAuth, authService)

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
        require(nodeInstanceHandle == authToken.nodeInstanceHandle) { "Mismatch with node instances" }

        // Should register owner.

        val authCode = authService.requestPmaAuthCode(
            serviceAuth,
            principal,
            authToken.nodeInstanceHandle,
            authService.serviceInstanceId,
            CommonPMAPermissions.IDENTIFY
        )
        return registerPendingPermissions(authCode, authToken.nodeInstanceHandle, ArrayDeque(pendingPermissions))
    }


    fun doPostTaskToTasklist(
        taskList: TaskList,
        pniHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(pniHandle)

        val permissions = UnionPermissionScope(
            UPDATE_ACTIVITY_STATE(pniHandle),
            CommonPMAPermissions.ACCEPT_TASK(pniHandle)
        )
        val taskListToEngineAuthToken = authService.requestPmaAuthCode(
            requestorAuth = serviceAuth,
            client = taskList.serviceInstanceId,
            nodeInstanceHandle = pniHandle,
            serviceId = this.serviceInstanceId,
            requestedScope = permissions
        )


        /* TODO use an activity specific token to access the task list. Try to treat it like any
           other service. Let the task list use a delegate token. Also see whether ACCEPT_TASK can be given
            to the user (and delegated to the worklist).
         */

        val taskListAuth = globalAuthTokenForService(taskList)
        taskLists.merge(pniHandle, listOf(taskList)) { old, new -> old + new }

        taskList.postTask(taskListAuth, taskListToEngineAuthToken, pniHandle)
    }

    fun createAuthorizationCode(
        clientId: ServiceId<*>,
        handle: PNIHandle,
        service: Service,
        requestedScope: AuthScope,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>
    ): AuthorizationCode {
        //        authService.requestPmaAuthCode(serviceAuth, client, handle, service, requestedScope)

        val serviceAuthCode =
            authService.requestPmaAuthCode(serviceAuth, clientId, handle, service.serviceInstanceId, requestedScope)

        return registerPendingPermissions(serviceAuthCode, handle, ArrayDeque(pendingPermissions))
    }

    private fun registerPendingPermissions(
        serviceAuthCode: AuthorizationCode,
        handle: PNIHandle,
        pendingPermissions: ArrayDeque<AbstractDynamicPmaActivityContext.PendingPermission>
    ): AuthorizationCode {
        val clientId = serviceAuthCode.principal.name
        while (pendingPermissions.isNotEmpty()) {
            val pendingPermission = pendingPermissions.removeFirst()
            authService.grantPermission(
                serviceAuth, serviceAuthCode, authService,
                CommonPMAPermissions.GRANT_ACTIVITY_PERMISSION.restrictTo(
                    handle,
                    pendingPermission.clientId ?: clientId,
                    pendingPermission.service,
                    pendingPermission.scope
                )
            )
        }
        return serviceAuthCode
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

    /**
     * This function performs the registration of global permissions with the engine's auth.
     * This is a hack for testing.
     */
    fun registerGlobalPermission(
        principal: PrincipalCompat,
        service: Service,
        scope: AuthScope
    ) {
        authService.registerGlobalPermission(serviceAuth, principal, service, scope)
    }

    fun <AIC : DynamicPmaActivityContext<AIC, *>, S : AutomatedService, I : Any, O : Any> invokeAction(
        activityContext: AIC,
        serviceId: ServiceName<S>,
        input: I,
        action: ServiceActivityContext<AIC, S>.(I) -> O
    ): O {
        val service: S = activityContext.processContext.contextFactory.serviceResolver.resolveService(serviceId)

        @Suppress("UNCHECKED_CAST")
        val scope: AuthScope = (activityContext.node as IPMAMessageActivity<AIC>).authorizationTemplates
            .mapNotNull { it.instantiateScope(activityContext) }
            .reduce { left, right -> left.union(right) }

        val authToken = authService.requestPmaAuthToken(
            serviceAuth,
            activityContext.nodeInstanceHandle,
            service.serviceInstanceId,
            scope
        )

        val serviceContext = ServiceActivityContext(activityContext, service, authToken)
        return serviceContext.action(input)
    }
}

private fun newEngineClientAuth(authService: AuthService, serviceName: ServiceName<EngineService>): PmaIdSecretAuthInfo {
    return authService.registerClient(serviceName, Random.nextString()).also {
        authService.registerGlobalPermission(null, it.principal, authService, UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(null, it.principal, authService, GRANT_GLOBAL_PERMISSION)
    }
}

