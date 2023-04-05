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

import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.GeneralClientService
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.util.logging.Logger

interface CommonLoanProcessContext : ProcessInstanceContext {

    val contextFactory: AbstractLoanContextFactory<*>

    val customerData: CustomerData
    val signingService: SigningService
    val customerFile: CustomerInformationFile
    val outputManagementSystem: OutputManagementSystem
    val accountManagementSystem: AccountManagementSystem
    val creditBureau: CreditBureau
    val creditApplication: CreditApplication
    val pricingEngine: PricingEngine
}

interface LoanProcessContext : CommonLoanProcessContext {
    override val contextFactory: LoanContextFactory
    val authService: AuthService
    val generalClientService: GeneralClientService
    val engineService: EngineService

    val clerk1: Browser
    val postProcClerk: Browser
    val customer: Browser
    val log: Logger
}

interface LoanPmaProcessContext : DynamicPmaProcessInstanceContext<LoanPMAActivityContext>, CommonLoanProcessContext {
    override val contextFactory: LoanPMAContextFactory
    override fun resolveBrowser(principal: PrincipalCompat): Browser

    val clerk1: Browser
    val postProcClerk: Browser
    val customer: Browser
    val log: Logger
}

abstract class AbstractLoanProcessContext(
    protected val engineData: ProcessEngineDataAccess,
    override val processInstanceHandle: PIHandle
) : CommonLoanProcessContext {
    val processInstance: IProcessInstance get() = engineData.instance(processInstanceHandle).withPermission()

    override val signingService: SigningService get() = contextFactory.signingService
    override val customerFile: CustomerInformationFile get() = contextFactory.customerFile
    override val outputManagementSystem: OutputManagementSystem get() = contextFactory.outputManagementSystem
    override val accountManagementSystem: AccountManagementSystem get() = contextFactory.accountManagementSystem
    override val creditBureau: CreditBureau get() = contextFactory.creditBureau
    override val creditApplication: CreditApplication get() = contextFactory.creditApplication
    override val pricingEngine: PricingEngine get() = contextFactory.pricingEngine

    override val customerData: CustomerData get() = contextFactory.customerData
}

class LoanProcessContextImpl(
    engineData: ProcessEngineDataAccess,
    override val contextFactory: LoanContextFactory,
    processInstanceHandle: PIHandle
) : AbstractLoanProcessContext(engineData, processInstanceHandle), LoanProcessContext {

    override val authService: AuthService get() = contextFactory.authService
    override val generalClientService: GeneralClientService get() = contextFactory.generalClientService
    override val engineService: EngineService get() = contextFactory.engineService

    override val clerk1: Browser get() = contextFactory.clerk1
    override val postProcClerk: Browser get() = contextFactory.postProcClerk
    override val customer: Browser get() = contextFactory.customer

    override val log: Logger get() = contextFactory.log

    override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
        return engineData.instance(processInstanceHandle).withPermission().allChildNodeInstances()
            .filter { it.node.id == name.id }
            .toList()
    }
}

class LoanPmaProcessContextImpl(
    engineData: ProcessEngineDataAccess,
    override val contextFactory: LoanPMAContextFactory,
    processInstanceHandle: PIHandle
) : AbstractLoanProcessContext(engineData,processInstanceHandle), LoanPmaProcessContext {
    //

    override val authService: AuthService get() = contextFactory.authService
    override val generalClientService: GeneralClientService get() = contextFactory.generalClientService
    override val engineService: EngineService get() = contextFactory.engineService

    override val clerk1: Browser get() = contextFactory.clerk1
    override val postProcClerk: Browser get() = contextFactory.postProcClerk
    override val customer: Browser get() = contextFactory.customer

    override fun resolveBrowser(principal: PrincipalCompat): Browser = when (principal) {
        clerk1.user -> clerk1
        postProcClerk.user -> postProcClerk
        customer.user -> customer
        else -> error("No browser defined for ${principal}")
    }

    override val log: Logger get() = contextFactory.log

    override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
        return engineData.instance(processInstanceHandle).withPermission().allChildNodeInstances()
            .filter { it.node.id == name.id }
            .toList()
    }

}
