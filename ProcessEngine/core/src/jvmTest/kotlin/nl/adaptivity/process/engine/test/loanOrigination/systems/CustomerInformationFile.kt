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

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthToken
import nl.adaptivity.process.engine.test.loanOrigination.auth.IdSecretAuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import java.util.*
import kotlin.random.Random

class CustomerInformationFile(val authService: AuthService) {
    val clientAuth = IdSecretAuthInfo(SimplePrincipal("CustomerInformationFile:${Random.nextString()}"))
    val clientId get() = clientAuth.principal.name

    private val customerData = mutableMapOf<String, CustomerData>()

    fun enterCustomerData(authInfo: AuthInfo, data: CustomerData) {
        authService.validateAuthInfo(clientId, authInfo, LoanPermissions.CREATE_CUSTOMER)
        customerData[data.customerId] = data
    }

    fun getCustomerData(authInfo: AuthInfo, customerId: String): CustomerData? {
        // TODO make this customer specific and add an identifyCustomer function that only requires less data
        authService.validateAuthInfo(clientId, authInfo, LoanPermissions.QUERY_CUSTOMER_DATA.context(customerId))
        return customerData[customerId]
    }
}
