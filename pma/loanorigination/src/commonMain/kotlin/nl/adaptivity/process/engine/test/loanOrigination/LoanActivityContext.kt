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

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.PMAActivityContext
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import java.util.logging.Logger

class LoanActivityContext(override val processContext:LoanProcessContext, baseContext: ActivityInstanceContext):
    PMAActivityContext<LoanActivityContext>(baseContext), LoanProcessContext {
    override val contextFactory: LoanContextFactory
        get() = super.contextFactory as LoanContextFactory

    override val customerData: CustomerData
        get() = processContext.customerData
    override val signingService: SigningService
        get() = processContext.signingService
    override val customerFile: CustomerInformationFile
        get() = processContext.customerFile
    override val outputManagementSystem: OutputManagementSystem
        get() = processContext.outputManagementSystem
    override val accountManagementSystem: AccountManagementSystem
        get() = processContext.accountManagementSystem
    override val creditBureau: CreditBureau
        get() = processContext.creditBureau
    override val creditApplication: CreditApplication
        get() = processContext.creditApplication
    override val pricingEngine: PricingEngine
        get() = processContext.pricingEngine
    override val clerk1: Browser
        get() = processContext.clerk1
    override val postProcClerk: Browser
        get() = processContext.postProcClerk
    override val customer: Browser
        get() = processContext.customer
    override val log: Logger
        get() = processContext.log
}
