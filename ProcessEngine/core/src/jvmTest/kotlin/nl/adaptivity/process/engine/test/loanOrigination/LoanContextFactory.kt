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
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditApplication
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import java.security.Principal
import java.util.logging.Logger
import kotlin.random.Random

class LoanContextFactory(authLogger: Logger): ProcessContextFactory<LoanActivityContext> {
    val engineClientAuth: IdSecretAuthInfo =
        IdSecretAuthInfo(SimplePrincipal("ProcessEngine:${Random.nextString()}"))
    val engineClientId get() = engineClientAuth.principal.name

    private val processContexts = mutableMapOf<Handle<SecureObject<ProcessInstance>>, LoanProcessContext>()
    val authService = AuthService(authLogger)
    val customerFile = CustomerInformationFile(authService)
    val outputManagementSystem = OutputManagementSystem(authService)
    val accountManagementSystem = AccountManagementSystem(authService)
    val creditBureau = CreditBureau(authService)
    val creditApplication = CreditApplication(authService, customerFile)
    val pricingEngine = PricingEngine(authService)
    private val taskLists = mutableMapOf<Principal, TaskList>()
    private val taskListClientAuth =
        IdSecretAuthInfo(SimplePrincipal("TaskList:${Random.nextString()}"))
    val taskListClientId get() = taskListClientAuth.principal.name


    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
                                           ): LoanActivityContext {

        val instanceHandle = processNodeInstance.processContext.handle
        val processContext = processContexts.getOrPut(instanceHandle) { LoanProcessContext(this, instanceHandle) }
        return LoanActivityContext(processContext, processNodeInstance)
    }

    override fun onProcessFinished(
        engineDataAccess: ProcessEngineDataAccess,
        processInstance: Handle<SecureObject<ProcessInstance>>
                                  ) {
        processContexts.remove(processInstance)
    }

    fun taskList(principal: Principal): TaskList {
        return taskLists.getOrPut(principal) { TaskList(authService, taskListClientAuth, principal) }
    }
}
