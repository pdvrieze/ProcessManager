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
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.ProcessInstanceContext

class LoanProcessContext(
    private val loanContextFactory: LoanContextFactory,
    override val handle: Handle<SecureObject<ProcessInstance>>
                        ): ProcessInstanceContext {

    val customerFile get() = loanContextFactory.customerFile
    val outputManagementSystem get() = loanContextFactory.outputManagementSystem
    val accountManagementSystem get() = loanContextFactory.accountManagementSystem
    val creditBureau get() = loanContextFactory.creditBureau
    val creditApplication get() = loanContextFactory.creditApplication
    val pricingEngine get() = loanContextFactory.pricingEngine
    val authService get() = loanContextFactory.authService

}
