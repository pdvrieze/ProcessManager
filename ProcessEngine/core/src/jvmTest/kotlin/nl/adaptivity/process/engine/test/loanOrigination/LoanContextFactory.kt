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
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditApplication
import nl.adaptivity.process.engine.test.loanOrigination.systems.*

class LoanContextFactory: ProcessContextFactory<LoanActivityContext> {
    private val processContexts = mutableMapOf<Handle<SecureObject<ProcessInstance>>, LoanProcessContext>()
    val customerFile = CustomerInformationFile()
    val outputManagementSystem = OutputManagementSystem()
    val accountManagementSystem = AccountManagementSystem()
    val creditBureau = CreditBureau()
    val creditApplication = CreditApplication(customerFile)
    val pricingEngine = PricingEngine()
    val authService = AuthService()

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
                                           ): LoanActivityContext {

        val instanceHandle = processNodeInstance.processContext.handle
        val processContext = processContexts.getOrPut(instanceHandle) { LoanProcessContext(this, instanceHandle) }
        return LoanActivityContext(processContext, processNodeInstance)
    }
}
