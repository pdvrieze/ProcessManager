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
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.*
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
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.messaging.SOAPMethodDesc
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.PMHandle
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.kotlin.arrayMap
import nl.adaptivity.xmlutil.util.CompactFragment
import java.security.Principal
import java.util.*
import java.util.logging.Logger
import javax.xml.namespace.QName
import kotlin.collections.ArrayDeque
import kotlin.random.Random

class EngineService(
    authService: AuthService,
    auth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<EngineService>,
    logger: Logger = authService.logger,
) : ServiceBase<EngineService>(authService, auth, serviceName, logger), AutomatedService {

    lateinit var processEngine: ProcessEngine<*>
        private set

    private var nextProcessNo = 1

    constructor(
        serviceName: ServiceName<EngineService>,
        authService: AuthService,
        adminAuth: PmaAuthInfo,
        logger: Logger = authService.logger
    ) : this(authService, authService.registerClient(adminAuth, serviceName, Random.nextString()), serviceName, logger) {
        val principal = SimplePrincipal(serviceInstanceId.serviceId)
        authService.registerGlobalPermission(adminAuth, principal, authServiceClient.authService, UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(adminAuth, principal, authServiceClient.authService, GRANT_GLOBAL_PERMISSION)
    }

    private val taskLists: MutableMap<Handle<SecureProcessNodeInstance>, List<TaskList<*>>> = mutableMapOf()

    fun initEngine(processEngine: ProcessEngine<*>) {
        require (! ::processEngine.isInitialized) { "The process engine can only be initialised once." }
        this.processEngine = processEngine
    }

    override fun getServiceState(): String = ""

    /**
     * Accept the activity and return an authorization code for the user to identify itself with in relation to
     * the activity.
     */
    @JvmName("acceptActivityPending")
    fun acceptActivity(
        authToken: PmaAuthToken,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        user: Principal,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>,
    ): AuthorizationCode {
        logMe(authToken, nodeInstanceHandle, user)
        validateAuthInfo(
            authToken,
            CommonPMAPermissions.ACCEPT_TASK(nodeInstanceHandle)
        ) // TODO mark correct expected permission
        require(nodeInstanceHandle == authToken.nodeInstanceHandle) { "Mismatch with node instances" }

        // Should register owner.

        val permissions = CommonPMAPermissions.IDENTIFY.union(pendingPermissions.toPermission())

        return acceptActivityUnsecured(user, authToken.nodeInstanceHandle, permissions)
    }

    /**
     * Accept the activity and return an authorization code for the user to identify itself with in relation to
     * the activity.
     */
    fun acceptActivity(
        authToken: PmaAuthToken,
        nodeInstanceHandle: Handle<SecureProcessNodeInstance>,
        user: Principal,
        permissions: Collection<AuthScope>,
    ): AuthorizationCode {
        logMe(authToken, nodeInstanceHandle, user)
        validateAuthInfo(
            authToken,
            CommonPMAPermissions.ACCEPT_TASK(nodeInstanceHandle)
        ) // TODO mark correct expected permission
        require(nodeInstanceHandle == authToken.nodeInstanceHandle) { "Mismatch with node instances" }

        // Should register owner.

        val perm = permissions.fold(CommonPMAPermissions.IDENTIFY) { l: AuthScope, r -> l.union(r) }

        return acceptActivityUnsecured(user, authToken.nodeInstanceHandle, perm)
    }

    private fun acceptActivityUnsecured(
        user: Principal,
        nodeInstanceHandle: PNIHandle,
        permissions: AuthScope
    ): AuthorizationCode {
        val authCode = authServiceClient.requestPmaAuthCode(
            user,
            nodeInstanceHandle,
            ServiceId(user.name),
            authServiceClient.authService.serviceInstanceId,
            permissions
        ) // TODO Do this better
        return authCode
    }

    fun doPostTaskToTasklist(
        taskList: TaskList<*>,
        pniHandle: Handle<SecureProcessNodeInstance>
    ) {
        logMe(pniHandle)

        val permissions = UnionPermissionScope(
            UPDATE_ACTIVITY_STATE(pniHandle),
            CommonPMAPermissions.ACCEPT_TASK(pniHandle)
        )
        val taskListToEngineAuthCode = authServiceClient.requestPmaAuthCode(
            authorizedService = taskList.serviceInstanceId,
            nodeInstanceHandle = pniHandle,
            tokenTargetService = this.serviceInstanceId,
            requestedScope = permissions
        )


        /* TODO use an activity specific token to access the task list. Try to treat it like any
           other service. Let the task list use a delegate token. Also see whether ACCEPT_TASK can be given
            to the user (and delegated to the worklist).
         */

        val taskListAuth = globalAuthTokenForService(taskList)
        taskLists.merge(pniHandle, listOf(taskList)) { old, new -> old + new }

        taskList.postTask(taskListAuth, taskListToEngineAuthCode, pniHandle)
    }

    fun createAuthorizationCode(
        authorizedService: ServiceId<*>,
        handle: PNIHandle,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope,
        pendingPermissions: Collection<AbstractDynamicPmaActivityContext.PendingPermission>
    ): AuthorizationCode {
        val actualPermissions = pendingPermissions.toPermission().union(requestedScope)

        return authServiceClient.requestPmaAuthCode(
            authorizedService = authorizedService,
            nodeInstanceHandle = handle,
            tokenTargetService = tokenTargetService,
            requestedScope = actualPermissions
        )
    }

    fun Collection<AbstractDynamicPmaActivityContext.PendingPermission>.toPermission(): AuthScope {
        if (isEmpty()) return CommonPMAPermissions.IDENTIFY // effectively dummy permission
        return asSequence().map { pendingPermission ->
            CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(
                pendingPermission.service,
                pendingPermission.scope
            )
/*
            CommonPMAPermissions.GRANT_ACTIVITY_PERMISSION.restrictTo(
                handle,
                pendingPermission.clientId ?: clientId,
                pendingPermission.service,
                pendingPermission.scope
            )
*/
        }.reduce { left, right -> left.union(right) }
    }

    private fun registerPendingPermissions(
        serviceAuthCode: AuthorizationCode,
        handle: PNIHandle,
        pendingPermissions: ArrayDeque<AbstractDynamicPmaActivityContext.PendingPermission>
    ): AuthorizationCode {
        val pending = pendingPermissions.toPermission()
        authServiceClient.authService.grantPermission(authServiceClient.originatingClientAuth, serviceAuthCode, authServiceClient.authService, pending)
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
        authServiceClient.invalidateActivityTokens(processNodeInstance.handle)
    }

    fun <AIC : DynamicPmaActivityContext<AIC, *>, S : AutomatedService, I : Any, O : Any> invokeAction(
        activityContext: AIC,
        serviceName: ServiceName<S>,
        input: I,
        action: ServiceActivityContext<AIC, S>.(I) -> O
    ): O {
        val service: S = activityContext.processContext.contextFactory.serviceResolver.resolveService(serviceName)

        return invokeAction(activityContext, service, action, input)
    }

    fun <AIC : DynamicPmaActivityContext<AIC, *>, S : AutomatedService, I : Any, O : Any> invokeAction(
        activityContext: AIC,
        serviceId: ServiceId<S>,
        input: I,
        action: ServiceActivityContext<AIC, S>.(I) -> O
    ): O {
        val service: S = activityContext.processContext.contextFactory.serviceResolver.resolveService(serviceId)

        return invokeAction(activityContext, service, action, input)
    }

    fun <AIC : DynamicPmaActivityContext<AIC, *>, I : Any, O : Any, S : AutomatedService> invokeAction(
        activityContext: AIC,
        service: S,
        action: ServiceActivityContext<AIC, S>.(I) -> O,
        input: I
    ): O {
        @Suppress("UNCHECKED_CAST")
        val scope: AuthScope = (activityContext.node as IPMAMessageActivity<AIC>).authorizationTemplates
            .mapNotNull { it.instantiateScope(activityContext) }
            .fold(EMPTYSCOPE) { left: AuthScope, right -> left.union(right) }

        val authToken = authServiceClient.requestPmaAuthToken(
            activityContext.nodeInstanceHandle,
            service.serviceInstanceId,
            scope
        )

        val serviceContext = ServiceActivityContext(activityContext, service, authToken)
        return serviceContext.action(input)
    }

    /**
     * Function that starts a process (with a given payload)
     */
    fun startProcess(engineToken: PmaAuthToken, processHandle: PMHandle, payload: CompactFragment?): PIHandle {
        fun <TR : ContextProcessTransaction> impl(processEngine: ProcessEngine<TR>): PIHandle {
            return processEngine.inTransaction { tr ->
                val name = tr.readableEngineData.processModel(processHandle).withPermission().name
                processEngine.startProcess(tr, engineToken.principal, processHandle, "$name ${nextProcessNo++}", UUID.randomUUID(), payload)
            }
        }
        return impl(processEngine)
    }

    fun deliverEvent(handle: PNIHandle, payload: CompactFragment?) {
        fun <TR: ContextProcessTransaction> impl(processEngine: ProcessEngine<TR>) {
            processEngine.inTransaction { tr ->
                tr.writableEngineData.updateNodeInstance(handle) {
                    finishTask(tr.writableEngineData, payload)
                }
            }
        }

        impl(processEngine)
    }

    fun deliverEvent(piHandle: PIHandle, nodeId: Identified, payload: CompactFragment?) {
        fun <TR: ContextProcessTransaction> impl(processEngine: ProcessEngine<TR>) {
            processEngine.inTransaction { tr ->
                tr.writableEngineData.updateInstance(piHandle) {
                    val child = allChildNodeInstances { it.node.identifier==nodeId && it.state.isActive }.sortedBy { it.entryNo }.first()
                    updateChild(child) {
                        finishTask(tr.writableEngineData, payload)
                    }
                }
            }
        }

        impl(processEngine)
    }

    /**
     * Function that invokes the process engine to ensure that all process models are registered, it returns the handles.
     * to the models.
     */
    fun ensureProcessHandles(
        engineToken: PmaAuthToken,
        processModels: Array<out ExecutableProcessModel>
    ): Array<Handle<ExecutableProcessModel>> {
        fun <TR : ContextProcessTransaction> impl(processEngine: ProcessEngine<TR>) : Array<Handle<ExecutableProcessModel>> {
            return processEngine.inTransaction {tr ->
                val existingModelHandles = processEngine.getProcessModels(tr.readableEngineData, engineToken.principal).asSequence()
                    .map { it.withPermission() }
                    .filter { it.uuid!=null }
                    .associate { it.uuid!! to it.handle }

                processModels.arrayMap { model ->
                    val handle = model.uuid?.let { existingModelHandles[it] } ?: processEngine.addProcessModel(tr, model, engineToken.principal).handle
                    handle
                }
            }
        }
        return impl(processEngine)
    }

    val runMessageMethod: InvokableMethod = SOAPMethodDesc(QName("engineService"), "internal", "run")
}

private fun newEngineClientAuth(authServiceClient: DefaultAuthServiceClient, serviceName: ServiceName<EngineService>): PmaIdSecretAuthInfo {
    return authServiceClient.registerClient(serviceName, Random.nextString()).also {
        authServiceClient.registerGlobalPermission(it.principal, authServiceClient.authService, UPDATE_ACTIVITY_STATE)
        authServiceClient.registerGlobalPermission( it.principal, authServiceClient.authService, GRANT_GLOBAL_PERMISSION)
    }
}

