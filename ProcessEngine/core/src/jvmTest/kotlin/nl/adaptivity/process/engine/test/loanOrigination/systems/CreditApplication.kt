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
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthorizationCode
import nl.adaptivity.process.engine.test.loanOrigination.auth.Service
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditReport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanApplication
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanEvaluation

class CreditApplication(
    authService: AuthService,
    val customerInformationFile: CustomerInformationFile
                       ): Service(authService, "Credit_Application") {
    fun evaluateLoan(authInfo: AuthInfo, delegateAuthorization:AuthorizationCode, application: LoanApplication, creditReport: CreditReport): LoanEvaluation {
        val cifServiceAuth = authService.getAuthToken(serviceAuth, delegateAuthorization)
        val customer = customerInformationFile.getCustomerData(cifServiceAuth, application.customerId)!!
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
