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
import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.ServiceImpl
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAProcessContextFactory
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.test.loanOrigination.LoanPMAActivityContext
import nl.adaptivity.process.engine.test.loanOrigination.ServiceIds
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CreditReport
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData

class CreditBureau(authService: AuthService): ServiceImpl(authService, "CreditBureau"), AutomatedService {

    override fun getServiceState(): String = ""

    fun getCreditReport(authInfo: AuthInfo, customerData: CustomerData): CreditReport {
        logMe()
        validateAuthInfo(authInfo, LoanPermissions.GET_CREDIT_REPORT(customerData.taxId))

        val creditRating = 400
        return CreditReport(
            "${customerData.name} (rating $creditRating) is approved for loans up to 20000",
            creditRating,
            20000
                                                                                      )
    }

    fun getCreditReport(context: DynamicPMAProcessContextFactory<LoanPMAActivityContext>, authInfo: AuthToken, customerId: String, taxId: String): CreditReport {
        logMe()
        validateAuthInfo(authInfo, LoanPermissions.GET_CREDIT_REPORT(taxId))
        val customerFile = context.resolveService(ServiceIds.customerFile)

        // TODO this shouldn't need a code
        val customerFileCode = authService.exchangeDelegateCode(authInfo, this@CreditBureau, customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customerId))
        val customerFileToken = authService.getAuthToken(serviceAuth, customerFileCode)

        val customerData = customerFile.getCustomerData(customerFileToken, customerId) ?: throw IllegalStateException("Unknown customer")

        val creditRating = 400
        return CreditReport(
            "${customerData.name} (rating $creditRating) is approved for loans up to 20000",
            creditRating,
            20000
                                                                                      )
    }

}
