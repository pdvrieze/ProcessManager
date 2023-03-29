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
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.process.util.Identified
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

interface LoanPmaProcessContext : DynamicPMAProcessInstanceContext<LoanPMAActivityContext>, CommonLoanProcessContext {
    override val contextFactory: LoanPMAContextFactory

    val clerk1: Browser
    val postProcClerk: Browser
    val customer: Browser
    val log: Logger
}

abstract class AbstractLoanProcessContext(
    protected val engineData: ProcessEngineDataAccess,
) : CommonLoanProcessContext {
    abstract val processInstance: IProcessInstance

    override val processInstanceHandle: PIHandle get() = processInstance.handle

    override val customerData: CustomerData get() = contextFactory.customerData
    override val signingService: SigningService get() = contextFactory.signingService
    override val customerFile: CustomerInformationFile get() = contextFactory.customerFile
    override val outputManagementSystem: OutputManagementSystem get() = contextFactory.outputManagementSystem
    override val accountManagementSystem: AccountManagementSystem get() = contextFactory.accountManagementSystem
    override val creditBureau: CreditBureau get() = contextFactory.creditBureau
    override val creditApplication: CreditApplication get() = contextFactory.creditApplication
    override val pricingEngine: PricingEngine get() = contextFactory.pricingEngine

}


class LoanProcessContextImpl(
    engineData: ProcessEngineDataAccess,
    override val contextFactory: LoanContextFactory,
    processInstanceHandle: PIHandle
) : AbstractLoanProcessContext(engineData), LoanProcessContext {
    override val processInstance: IProcessInstance = engineData.instance(processInstanceHandle).withPermission()


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
) : AbstractLoanProcessContext(engineData), LoanPmaProcessContext {
    override val processInstance: IProcessInstance = engineData.instance(processInstanceHandle).withPermission()


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
