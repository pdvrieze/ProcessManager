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
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.DynamicPMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.process.util.Identified
import java.util.logging.Logger

interface LoanProcessContext : DynamicPMAProcessInstanceContext<LoanActivityContext> {
    override val contextFactory: LoanContextFactory

    val customerData: CustomerData
    val signingService: SigningService
    val customerFile: CustomerInformationFile
    val outputManagementSystem: OutputManagementSystem
    val accountManagementSystem: AccountManagementSystem
    val creditBureau: CreditBureau
    val creditApplication: CreditApplication
    val pricingEngine: PricingEngine

    val clerk1: Browser
    val postProcClerk: Browser
    val customer: Browser
    val log: Logger
}


class LoanProcessContextImpl(
    protected val engineData: ProcessEngineDataAccess,
    override val contextFactory: LoanContextFactory,
    override val processInstanceHandle: Handle<SecureObject<ProcessInstance>>
) : LoanProcessContext {

    override val customerData: CustomerData get() = contextFactory.customerData
    override val signingService: SigningService get() = contextFactory.signingService
    override val customerFile: CustomerInformationFile get() = contextFactory.customerFile
    override val outputManagementSystem: OutputManagementSystem get() = contextFactory.outputManagementSystem
    override val accountManagementSystem: AccountManagementSystem get() = contextFactory.accountManagementSystem
    override val creditBureau: CreditBureau get() = contextFactory.creditBureau
    override val creditApplication: CreditApplication get() = contextFactory.creditApplication
    override val pricingEngine: PricingEngine get() = contextFactory.pricingEngine
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
