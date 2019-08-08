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

package nl.adaptivity.process.engine.test.loanOrigination

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.list
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.defineInput
import nl.adaptivity.process.processModel.engine.runnableActivity
import nl.adaptivity.process.processModel.engine.configureRunnableActivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class TestLoanOrigination {

    @Test
    fun testCreateModel() {
        assertEquals("inputCustomerMasterData", Model1.inputCustomerMasterData.id)
    }
}

private val clerk1 = SimplePrincipal("preprocessing clerk 1")

@Serializable
@SerialName("loanCustomer")
data class LoanCustomer(val customerId: String)

@UseExperimental(ImplicitReflectionSerializer::class)
private object Model1 : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    clerk1, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")
                                                                       ) {

    val customerFile = CustomerInformationFile()
    val creditBureau = CreditBureau()

    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer>(start) {
        val newData = CustomerData("cust123456", "taxId234", "passport345", "John Doe", "10 Downing Street")
        LoanCustomer(newData.customerId)
    }
    val customerIdentification by configureRunnableActivity<LoanCustomer, List<CustomerCollateral>>(
        inputCustomerMasterData,
        CustomerCollateral.serializer().list
                                                                                                   ) {
        defineInput(inputCustomerMasterData)
        action = {
            listOf(CustomerCollateral("house", "100000", "residential"))
        }
    }
    val checkCreditWorthyness by runnableActivity(customerIdentification, CreditReport.serializer(),  LoanCustomer.serializer(), inputCustomerMasterData) { customer ->
        val customerData: CustomerData = customerFile.getCustomerData(AuthInfo(), customer.customerId)!!
        creditBureau.getCreditWorthiness(AuthInfo(), customerData)
    }
    val end by endNode(checkCreditWorthyness) {
        defines.add(XmlDefineType("result", checkCreditWorthyness))
    }
}
