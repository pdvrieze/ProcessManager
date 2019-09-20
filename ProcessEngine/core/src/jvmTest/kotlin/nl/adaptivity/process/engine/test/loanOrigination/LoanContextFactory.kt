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

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PNIHandle
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger

class LoanContextFactory(val log: Logger) : ProcessContextFactory<LoanActivityContext> {
    private val nodes = mutableMapOf<PNIHandle, String>()

    val authService = AuthService(log, nodes)

    @UseExperimental(ExperimentalUnsignedTypes::class)
    val engineClientAuth: IdSecretAuthInfo = authService.registerClient("ProcessEngine", Random.nextString()).also {
        authService.registerGlobalPermission(null, it.principal, authService, LoanPermissions.UPDATE_ACTIVITY_STATE)
        authService.registerGlobalPermission(null, it.principal, authService, LoanPermissions.GRANT_PERMISSION)
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
        val processContext = getProcessContext(engineDataAccess, instanceHandle)
        return LoanActivityContext(processContext, processNodeInstance)
    }

    private fun getProcessContext(
        engineDataAccess: ProcessEngineDataAccess,
        instanceHandle: Handle<SecureObject<ProcessInstance>>
                                 ) =
        processContexts.getOrPut(instanceHandle) { LoanProcessContext(engineDataAccess, this, instanceHandle) }

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
        val loanProcessContext = getProcessContext(engineDataAccess, processNodeInstance.processContext.handle)
        for (taskList in taskLists.values) {
            val authToken = loanProcessContext.engineService.authTokenForService(taskList)
            taskList.unregisterTask(authToken, nodeInstanceHandle)
        }
        authService.invalidateActivityTokens(engineClientAuth, processNodeInstance.handle)
    }

    fun taskList(engineService: EngineService, principal: Principal): TaskList {
        return taskLists.getOrPut(principal) {
            log.log(Level.INFO, "Creating tasklist for ${principal.name}")
            val clientAuth = authService.registerClient("TaskList(${principal.name})", Random.nextString())
            val t = TaskList(authService, engineService, clientAuth, principal)
            val auth = engineClientAuth
            authService.registerGlobalPermission(auth, principal, t, LoanPermissions.ACCEPT_TASK)
            authService.registerGlobalPermission(auth, SimplePrincipal(engineService.serviceId), t, LoanPermissions.POST_TASK)
            t
        }
    }


}

