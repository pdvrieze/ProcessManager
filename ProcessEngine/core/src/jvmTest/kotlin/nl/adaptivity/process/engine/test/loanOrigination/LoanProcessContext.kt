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
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.EngineService
import java.security.Principal

class LoanProcessContext(
    engineData: ProcessEngineDataAccess,
    internal val loanContextFactory: LoanContextFactory,
    override val handle: Handle<SecureObject<ProcessInstance>>
                        ) : ProcessInstanceContext {

    val customerData get()= loanContextFactory.customerData
    val signingService get()= loanContextFactory.signingService

    val customerFile get() = loanContextFactory.customerFile
    val outputManagementSystem get() = loanContextFactory.outputManagementSystem
    val accountManagementSystem get() = loanContextFactory.accountManagementSystem
    val creditBureau get() = loanContextFactory.creditBureau
    val creditApplication get() = loanContextFactory.creditApplication
    val pricingEngine get() = loanContextFactory.pricingEngine
    val authService get() = loanContextFactory.authService
    val generalClientService get() = loanContextFactory.generalClientService
    val engineService = EngineService(engineData, loanContextFactory.authService, loanContextFactory.engineClientAuth)

    val clerk1 get() = loanContextFactory.clerk1
    val postProcClerk get() = loanContextFactory.postProcClerk
    val customer get() = loanContextFactory.customer

    fun taskList(principal: Principal) = loanContextFactory.taskList(principal)
}
