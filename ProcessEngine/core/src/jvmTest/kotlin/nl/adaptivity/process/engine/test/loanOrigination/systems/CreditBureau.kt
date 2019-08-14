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
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.auth.Service
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditReport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData

class CreditBureau(authService: AuthService): Service(authService, "CreditBureau") {

    fun getCreditReport(authInfo: AuthInfo, customerData: CustomerData): CreditReport {
        validateAuthInfo(authInfo, LoanPermissions.GET_CREDIT_REPORT.context(customerData.taxId))

        val creditRating = 400
        return CreditReport(
            "${customerData.name} (rating $creditRating) is approved for loans up to 20000",
            creditRating,
            20000
                                                                                      )
    }

}
