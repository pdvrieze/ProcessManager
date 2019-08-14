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

package nl.adaptivity.process.engine.test.loanOrigination.datatypes

import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.systems.AuthService
import nl.adaptivity.process.engine.test.loanOrigination.systems.CustomerInformationFile

class CreditApplication(
    val authService: AuthService,
    val customerInformationFile: CustomerInformationFile
                       ) {
    fun evaluateLoan(authInfo: AuthInfo, application: LoanApplication, creditReport: CreditReport): LoanEvaluation {
        val customer = customerInformationFile.getCustomerData(AuthInfo(), application.customerId)!!
        if (application.amount<creditReport.maxLoan) {
            return LoanEvaluation(
                application.customerId,
                application,
                true,
                "Loan for customer ${customer.name} in the amoount of ${application.amount} approved"
                                                                                             )
        } else {
            return LoanEvaluation(
                application.customerId,
                application,
                false,
                "Loan for customer ${customer.name} for ${application.amount} could not be automatically approved"
                                                                                             )
        }
    }
}
