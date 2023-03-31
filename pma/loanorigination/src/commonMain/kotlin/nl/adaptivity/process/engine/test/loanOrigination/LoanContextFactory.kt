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

package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.SimpleRolePrincipal
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAProcessContextFactory
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.pma.runtime.PMAActivityInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.set
import kotlin.random.Random

class LoanContextFactory(log: Logger, random: Random): AbstractLoanContextFactory<LoanActivityContext>(log, random) {

    private val processContexts = mutableMapOf<PIHandle, LoanProcessContext>()
    private val taskLists = mutableMapOf<Principal, TaskList>()

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): LoanActivityContext {
        val instanceHandle = processNodeInstance.hProcessInstance
        nodes[processNodeInstance.handle] = processNodeInstance.node.id
        val processContext = getProcessContext(engineDataAccess, instanceHandle)
        return LoanActivityContext(processContext, processNodeInstance)
    }

    fun getProcessContext(
        engineDataAccess: ProcessEngineDataAccess,
        instanceHandle: PIHandle
    ): LoanProcessContext = processContexts.getOrPut(instanceHandle) { LoanProcessContextImpl(engineDataAccess, this, instanceHandle) }

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

        val context: LoanProcessContext = getProcessContext(engineDataAccess, processNodeInstance.hProcessInstance)

        with(engineService) { context.onActivityTermination(processNodeInstance) }
    }

    fun getOrCreateTaskListForUser(principal: PrincipalCompat): TaskList {
        return taskLists.getOrPut(principal) {
            log.log(Level.INFO, "Creating tasklist service for ${principal.name}")
            val clientAuth = authService.registerClient("TaskList(${principal.name})", Random.nextString())
            val t = TaskList(authService, engineService, clientAuth, listOf(principal))
            engineService.registerGlobalPermission(principal, t, CommonPMAPermissions.ACCEPT_TASK)

            // TODO, use an activity specific permission/token instead.
            engineService.registerGlobalPermission(
                SimplePrincipal(engineService.serviceId) as Principal,
                t,
                CommonPMAPermissions.POST_TASK
            )
            t
        }
    }

}


class LoanPMAContextFactory(log: Logger, random: Random) :
    AbstractLoanContextFactory<LoanPMAActivityContext>(log, random),
    DynamicPMAProcessContextFactory<LoanPMAActivityContext> {

    private val processContexts = mutableMapOf<PIHandle, LoanPmaProcessContext>()
    private val taskList: TaskList by lazy {
        val clientAuth = authService.registerClient("TaskList(GLOBAL)", Random.nextString())
        TaskList(authService, engineService, clientAuth, principals)
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

    override fun <S : Service> resolveService(serviceId: ServiceId<S>): S {
        val id = serviceId.serviceId
        @Suppress("UNCHECKED_CAST")
        return services.first { it.serviceId == id } as S
    }

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): LoanPMAActivityContext {
        val instanceHandle = processNodeInstance.hProcessInstance
        nodes[processNodeInstance.handle] = processNodeInstance.node.id
        val processContext = getProcessContext(engineDataAccess, instanceHandle)
        return LoanPMAActivityContext(processContext, processNodeInstance as PMAActivityInstance<*>)
    }

    fun getProcessContext(
        engineDataAccess: ProcessEngineDataAccess,
        instanceHandle: PIHandle
    ): LoanPmaProcessContext = processContexts.getOrPut(instanceHandle) { LoanPmaProcessContextImpl(engineDataAccess, this, instanceHandle) }

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
            SimplePrincipal(engineService.serviceId) as Principal,
            taskList,
            CommonPMAPermissions.POST_TASK
        )
        return taskList
    }

}

abstract class AbstractLoanContextFactory<AIC: ActivityInstanceContext>(val log: Logger, val random: Random) :
    ProcessContextFactory<AIC> {

    protected val nodes = mutableMapOf<PNIHandle, String>()

    val authService: AuthService = AuthService(log, nodes, random)
    val authServiceClient: AuthServiceClient
        get() = AuthServiceClientImpl(authService)

    val engineService : EngineService = EngineService(authService)

    val customerFile = CustomerInformationFile(authService)
    val outputManagementSystem = OutputManagementSystem(authService)
    val accountManagementSystem = AccountManagementSystem(authService)
    val creditBureau = CreditBureau(authService)
    val creditApplication = CreditApplication(authService, customerFile)
    val pricingEngine = PricingEngine(authService)
    val generalClientService = GeneralClientService(authService)
    val signingService = SigningService(authService)


    val customerData = CustomerData(
        "cust123456",
        "taxId234",
        "passport345",
        "John Doe",
        "10 Downing Street"
    )

    object principals: AbstractList<PrincipalCompat>() {
        fun withName(userName: String): PrincipalCompat? {
            return map[userName]
        }

        val clerk1 = SimpleRolePrincipal("preprocessing clerk 1", "clerk", "bankuser")
        val clerk2 = SimpleRolePrincipal("postprocessing clerk 2", "clerk", "bankuser")
        val customer = SimpleRolePrincipal("John Doe", "customer")

        private val all = listOf(clerk1, clerk2, customer)

        private val map = all.associateBy { it.name }

        override val size: Int get() = all.size

        override fun get(index: Int): PrincipalCompat = all[index]
    }

    val clerk1: Browser = Browser(authService, principals.clerk1)
    val postProcClerk: Browser = Browser(authService, principals.clerk2)
    val customer: Browser = Browser(authService, principals.customer)


    override fun getPrincipal(userName: String): PrincipalCompat {
        return principals.withName(userName) ?: SimplePrincipal(userName)
    }
}

