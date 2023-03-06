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
import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.ServiceImpl
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData

class CustomerInformationFile(authService: AuthService): ServiceImpl(authService, "Customer_Information_File") {

    private val customerData = mutableMapOf<String, CustomerData>()

    override fun getServiceState(): String = ""

    fun enterCustomerData(authToken: AuthToken, data: CustomerData) {
        logMe()
        validateAuthInfo(authToken, LoanPermissions.CREATE_CUSTOMER)
        customerData[data.customerId] = data
    }

    fun getCustomerData(authToken: AuthToken, customerId: String): CustomerData? {
        logMe(customerId)
        // TODO make this customer specific and add an identifyCustomer function that only requires less data
        validateAuthInfo(authToken, LoanPermissions.QUERY_CUSTOMER_DATA(customerId))
        return customerData[customerId]
    }
}
