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
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.Service
import nl.adaptivity.process.engine.test.loanOrigination.auth.ServiceImpl
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import java.util.*
import kotlin.random.Random

class AccountManagementSystem(authService: AuthService): ServiceImpl(authService, "Account_Management_System") {

    fun openAccountFor(authInfo: AuthInfo, contract: Contract): BankAccountNumber {
        // check auth
        // check contract
        return BankAccountNumber("123456")
    }

}
