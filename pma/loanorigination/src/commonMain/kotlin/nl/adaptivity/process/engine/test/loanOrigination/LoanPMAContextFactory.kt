package nl.adaptivity.process.engine.test.loanOrigination

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessContextFactory
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.VALIDATE_AUTH
import nl.adaptivity.process.engine.pma.dynamic.services.EnumeratedTaskList
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

class LoanPMAContextFactory(log: Logger, random: Random) :
    AbstractLoanContextFactory<LoanPMAActivityContext>(log, random),
    DynamicPmaProcessContextFactory<LoanPMAActivityContext>, PmaServiceResolver {

    override val serviceResolver: PmaServiceResolver get() = this

    private val processContexts = mutableMapOf<PIHandle, LoanPmaProcessContext>()

    private val taskList: EnumeratedTaskList by lazy {
        val serviceName = ServiceName<EnumeratedTaskList>("TaskList(GLOBAL)")
        val clientAuth = adminAuthServiceClient.registerClient(serviceName, Random.nextString())
        adminAuthServiceClient.registerGlobalPermission(clientAuth.principal, authService, VALIDATE_AUTH(ServiceId<EnumeratedTaskList>(clientAuth.id)))

        EnumeratedTaskList(serviceName, authService, engineService, clientAuth, principals).also { t ->
            adminAuthServiceClient.registerGlobalPermission(
                SimplePrincipal(engineService.serviceInstanceId.serviceId) as PrincipalCompat,
                t,
                CommonPMAPermissions.POST_TASK
            )
            adminAuthServiceClient.registerGlobalPermission(SimplePrincipal(t.serviceInstanceId.serviceId), authService, VALIDATE_AUTH(t.serviceInstanceId))
        }
    }

    private val services: List<Service> = listOf(
        authService,
        engineService,
        customerFile,
        outputManagementSystem,
        accountManagementSystem,
        taskList,
        creditBureau,
        creditApplication,
        pricingEngine,
        generalClientService,
        signingService
    )

    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList<*>> {
        return listOf(taskList)
    }

    override fun <S : Service> resolveService(serviceName: ServiceName<S>): S {
        @Suppress("UNCHECKED_CAST")
        return requireNotNull(services.firstOrNull { it.serviceName == serviceName } as S?) { "No service found for name $serviceName" }
    }

    override fun <S : Service> resolveService(serviceId: ServiceId<S>): S {
        @Suppress("UNCHECKED_CAST")
        return requireNotNull(services.firstOrNull { it.serviceInstanceId == serviceId } as S?) { "No service found for id $serviceId" }
    }

    override fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return null
/*
        val targetId = (targetService as? Dynamic
        return services.filterIsInstance<Service>()
            .firstOrNull { it.serviceInstanceId ==  }

        return services.firstOrNull { it. }
*/
    }

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): LoanPMAActivityContext {
        val instanceHandle = processNodeInstance.hProcessInstance
        nodes[processNodeInstance.handle] = processNodeInstance.node.id
        val processContext = getProcessContext(engineDataAccess, instanceHandle)
        return LoanPMAActivityContext(processContext, processNodeInstance)
    }

    fun getProcessContext(
        engineDataAccess: ProcessEngineDataAccess,
        instanceHandle: PIHandle
    ): LoanPmaProcessContext = processContexts.getOrPut(instanceHandle) {
        LoanPmaProcessContextImpl(
            engineDataAccess,
            this,
            instanceHandle
        )
    }

    override fun onProcessFinished(
        engineDataAccess: ProcessEngineDataAccess,
        processInstance: PIHandle
    ) {
        processContexts.remove(processInstance)
    }

    override fun onActivityTermination(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ) {

        val context: LoanPmaProcessContext = getProcessContext(engineDataAccess, processNodeInstance.hProcessInstance)

        with(engineService) { context.onActivityTermination(processNodeInstance) }
    }

    override fun getOrCreateTaskListForUser(principal: PrincipalCompat): TaskList<*> {
        log.log(Level.INFO, "Creating tasklist service for ${principal.name}")

        adminAuthServiceClient.registerGlobalPermission(principal, taskList, CommonPMAPermissions.ACCEPT_TASK)

        // TODO, use an activity specific permission/token instead.
        adminAuthServiceClient.registerGlobalPermission(
            SimplePrincipal(engineService.serviceInstanceId.serviceId) as Principal,
            taskList,
            CommonPMAPermissions.POST_TASK
        )
        return taskList
    }

}
