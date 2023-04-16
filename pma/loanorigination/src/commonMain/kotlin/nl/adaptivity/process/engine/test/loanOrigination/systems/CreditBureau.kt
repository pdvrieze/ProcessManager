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
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessContextFactory
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.LoanPMAActivityContext
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditReport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData

class CreditBureau(serviceName: ServiceName<CreditBureau>, authService: AuthService, adminAuth: PmaAuthInfo):
    ServiceBase<CreditBureau>(authService, adminAuth, serviceName), AutomatedService {

    override fun getServiceState(): String = ""

    fun getCreditReport(authInfo: PmaAuthInfo, customerData: CustomerData): CreditReport {
        logMe()
        validateAuthInfo(authInfo, LoanPermissions.GET_CREDIT_REPORT(customerData.taxId))

        val creditRating = 400
        return CreditReport(
            "${customerData.name} (rating $creditRating) is approved for loans up to 20000",
            creditRating,
            20000
                                                                                      )
    }

    fun getCreditReport(context: DynamicPmaProcessContextFactory<LoanPMAActivityContext>, authInfo: PmaAuthToken, customerId: String, taxId: String): CreditReport {
        logMe()
        validateAuthInfo(authInfo, LoanPermissions.GET_CREDIT_REPORT(taxId))
        val customerFile = context.serviceResolver.resolveService(ServiceNames.customerFile)

        val customerFileToken = authServiceClient.exchangeDelegateToken(authInfo, customerFile.serviceInstanceId, LoanPermissions.QUERY_CUSTOMER_DATA(customerId))

        val customerData = customerFile.getCustomerData(customerFileToken, customerId) ?: throw IllegalStateException("Unknown customer")

        val creditRating = 400
        return CreditReport(
            "${customerData.name} (rating $creditRating) is approved for loans up to 20000",
            creditRating,
            20000
                                                                                      )
    }

}
