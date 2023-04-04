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

package nl.adaptivity.process.engine.test.loanOrigination.systems
import nl.adaptivity.process.engine.pma.AuthInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.BankAccountNumber
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.Contract

class AccountManagementSystem(serviceName: String, authService: AuthService): AbstractRunnableUiService(authService, "Account_Management_System"),
    AutomatedService {

    override val serviceName: ServiceName<AccountManagementSystem> = ServiceName(serviceName)
    override val serviceInstanceId: ServiceId<AccountManagementSystem> = ServiceId(getServiceId(serviceAuth))

    override fun getServiceState(): String = ""

    fun openAccountFor(authInfo: AuthInfo, contract: Contract): BankAccountNumber {
        logMe(authInfo, contract)

        validateAuthInfo(authInfo, LoanPermissions.OPEN_ACCOUNT(contract.customerId))

        // check contract
        return BankAccountNumber("123456")
    }

}
