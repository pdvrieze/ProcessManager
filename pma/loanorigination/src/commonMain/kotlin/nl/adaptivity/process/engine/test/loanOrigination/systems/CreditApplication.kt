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

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditReport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanApplication
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanEvaluation

class CreditApplication(
    serviceName: ServiceName<CreditApplication>,
    authService: AuthService,
    adminAuth: PmaAuthInfo,
    val customerInformationFile: CustomerInformationFile,
) : ServiceBase<CreditApplication>(authService, adminAuth, serviceName), AutomatedService {

    override fun getServiceState(): String = ""

    fun evaluateLoan(
        authInfo: PmaAuthToken,
        application: LoanApplication,
        creditReport: CreditReport,
    ): LoanEvaluation {
        logMe(application)
        validateAuthInfo(authInfo, LoanPermissions.EVALUATE_LOAN.context(application.customerId, application.amount))

        val cifServiceAuth = authServiceClient.exchangeDelegateToken(authInfo, customerInformationFile.serviceInstanceId, LoanPermissions.QUERY_CUSTOMER_DATA(application.customerId))
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
