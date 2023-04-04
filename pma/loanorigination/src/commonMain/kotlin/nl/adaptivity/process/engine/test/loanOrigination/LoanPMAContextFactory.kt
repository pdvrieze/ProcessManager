package nl.adaptivity.process.engine.test.loanOrigination

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessContextFactory
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

class LoanPMAContextFactory(log: Logger, random: Random) :
    AbstractLoanContextFactory<LoanPMAActivityContext>(log, random),
    DynamicPmaProcessContextFactory<LoanPMAActivityContext> {

    private val processContexts = mutableMapOf<PIHandle, LoanPmaProcessContext>()

    private val taskList: TaskList by lazy {
        val clientAuth = authService.registerClient("TaskList(GLOBAL)", Random.nextString())
        TaskList("tasklist", authService, engineService, clientAuth, principals).also { t ->
            engineService.registerGlobalPermission(
                SimplePrincipal(engineService.serviceInstanceId.serviceId) as PrincipalCompat,
                t,
                CommonPMAPermissions.POST_TASK
            )
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

    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList> {
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

    override fun getOrCreateTaskListForUser(principal: PrincipalCompat): TaskList {
        log.log(Level.INFO, "Creating tasklist service for ${principal.name}")

        engineService.registerGlobalPermission(principal, taskList, CommonPMAPermissions.ACCEPT_TASK)

        // TODO, use an activity specific permission/token instead.
        engineService.registerGlobalPermission(
            SimplePrincipal(engineService.serviceInstanceId.serviceId) as Principal,
            taskList,
            CommonPMAPermissions.POST_TASK
        )
        return taskList
    }

}
