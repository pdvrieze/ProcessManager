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

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.CreditApplication
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.nextUInt

class LoanContextFactory(val log: Logger) : ProcessContextFactory<LoanActivityContext> {
    private val nodes = mutableMapOf<Handle<SecureObject<ProcessNodeInstance<*>>>, String>()

    val authService = AuthService(log, nodes)

    @UseExperimental(ExperimentalUnsignedTypes::class)
    val engineClientAuth: IdSecretAuthInfo = authService.registerClient("ProcessEngine", Random.nextString()).also {
        authService.registerGlobalPermission(it.principal, authService, LoanPermissions.UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(it.principal, authService, LoanPermissions.GRANT_PERMISSION)
    }

    val engineClientId get() = engineClientAuth.principal.name

    private val processContexts = mutableMapOf<Handle<SecureObject<ProcessInstance>>, LoanProcessContext>()
    val customerFile = CustomerInformationFile(authService)
    val outputManagementSystem = OutputManagementSystem(authService)
    val accountManagementSystem = AccountManagementSystem(authService)
    val creditBureau = CreditBureau(authService)
    val creditApplication = CreditApplication(authService, customerFile)
    val pricingEngine = PricingEngine(authService)
    val generalClientService = GeneralClientService(authService)
    val signingService = SigningService(authService)


    private val taskLists = mutableMapOf<Principal, TaskList>()
    private val taskListClientAuth = authService.registerClient("TaskList", Random.nextString())

    val customerData = CustomerData(
        "cust123456",
        "taxId234",
        "passport345",
        "John Doe",
        "10 Downing Street"
                                   )

    val clerk1 = Browser(authService, SimplePrincipal("preprocessing clerk 1"))
    val postProcClerk = Browser(authService, SimplePrincipal("postprocessing clerk 2"))
    val customer = Browser(authService, SimplePrincipal(customerData.name))

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
                                           ): LoanActivityContext {
        val instanceHandle = processNodeInstance.processContext.handle
        nodes[processNodeInstance.handle] = processNodeInstance.node.id
        val processContext = processContexts.getOrPut(instanceHandle) { LoanProcessContext(engineDataAccess, this, instanceHandle) }
        return LoanActivityContext(processContext, processNodeInstance)
    }

    override fun onProcessFinished(
        engineDataAccess: ProcessEngineDataAccess,
        processInstance: Handle<SecureObject<ProcessInstance>>
                                  ) {
        processContexts.remove(processInstance)
    }

    override fun onActivityTermination(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
                                      ) {
        val nodeInstanceHandle = processNodeInstance.handle
        for (taskList in taskLists.values) {
            taskList.finishTask(nodeInstanceHandle)
        }
        authService.invalidateActivityTokens(engineClientAuth, processNodeInstance.handle)
    }

    fun taskList(engineService: EngineService, principal: Principal): TaskList {
        return taskLists.getOrPut(principal) {
            log.log(Level.INFO, "Creating tasklist for ${principal.name}")
            val t = TaskList(authService, engineService, taskListClientAuth, principal)
            authService.registerGlobalPermission(principal, t, LoanPermissions.ACCEPT_TASK)
            authService.registerGlobalPermission(SimplePrincipal(engineService.serviceId), t, LoanPermissions.POST_TASK)
            t
        }
    }


}

