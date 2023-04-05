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
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.LoanCustomer

class CustomerInformationFile(serviceName: String, authService: AuthService): AbstractRunnableUiService(authService, "Customer_Information_File"),
    AutomatedService {

    override val serviceName: ServiceName<CustomerInformationFile> = ServiceName(serviceName)
    override val serviceInstanceId: ServiceId<CustomerInformationFile> = ServiceId(getServiceId(serviceAuth))

    private val customerData = mutableMapOf<String, CustomerData>()

    override fun getServiceState(): String = ""

    fun enterCustomerData(authToken: PmaAuthToken, data: CustomerData): LoanCustomer {
        logMe()
        validateAuthInfo(authToken, LoanPermissions.CREATE_CUSTOMER)
        customerData[data.customerId] = data
        return LoanCustomer(data.customerId, data.taxId)
    }

    fun getCustomerData(authToken: PmaAuthToken, customerId: String): CustomerData? {
        logMe(customerId)
        // TODO make this customer specific and add an identifyCustomer function that only requires less data
        validateAuthInfo(authToken, LoanPermissions.QUERY_CUSTOMER_DATA(customerId))
        return customerData[customerId]
    }
}
