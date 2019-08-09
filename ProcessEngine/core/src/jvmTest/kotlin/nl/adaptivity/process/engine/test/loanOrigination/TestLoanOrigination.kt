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
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.get
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.output
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.engine.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.Principal
import java.util.*

class TestLoanOrigination: ProcessEngineTestSupport() {

    @Test
    fun testCreateModel() {
        assertEquals("inputCustomerMasterData", Model1(modelOwnerPrincipal).inputCustomerMasterData.id)
    }

    @Test
    fun testRunModel() {
        val model = ExecutableProcessModel(Model1(modelOwnerPrincipal).configurationBuilder)
        testProcess(model) { tr, model, hinstance ->
            val instance = tr[hinstance]
            assertEquals(ProcessInstance.State.FINISHED, instance.state)
            val modelResult = instance.outputs.single()
            assertEquals("<CreditReport maxLoan=\"20000\">John Doe is approved for loans up to 20000</CreditReport>", modelResult.content.contentString)
        }
    }

}

private val clerk1 = SimplePrincipal("preprocessing clerk 1")

@Serializable
@SerialName("loanCustomer")
data class LoanCustomer(val customerId: String)

@UseExperimental(ImplicitReflectionSerializer::class)
private class Model1(owner: Principal) : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    owner, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")
                                                                                        ) {

    val customerFile = CustomerInformationFile()
    val creditBureau = CreditBureau()

    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer>(start) {
        val newData = CustomerData("cust123456", "taxId234", "passport345", "John Doe", "10 Downing Street")
        customerFile.enterCustomerData(AuthInfo(), newData)
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
        val customerData: CustomerData = customerFile.getCustomerData(AuthInfo(), customer.customerId) ?: throw NullPointerException("Missing customer data")
        creditBureau.getCreditWorthiness(AuthInfo(), customerData)
    }
    val end by endNode(checkCreditWorthyness) {
        defines.add(XmlDefineType("input", checkCreditWorthyness))
        results.add(XmlResultType("result", "input"))
    }
    init {
        output("result", checkCreditWorthyness)
    }
}
