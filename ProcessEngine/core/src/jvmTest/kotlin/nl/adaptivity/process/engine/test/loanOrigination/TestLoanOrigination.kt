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

class TestLoanOrigination : ProcessEngineTestSupport() {

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
            val modelResult = instance.outputs.single { it.name == "creditReport" }
            assertEquals(
                "<CreditReport maxLoan=\"20000\">John Doe is approved for loans up to 20000</CreditReport>",
                modelResult.content.contentString
                        )
        }
    }

}

private val clerk1 = SimplePrincipal("preprocessing clerk 1")

@UseExperimental(ImplicitReflectionSerializer::class)
private class Model1(owner: Principal) : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    owner, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")
                                                                                        ) {

    val customerFile = CustomerInformationFile()

    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer>(start) {
        val newData = CustomerData("cust123456", "taxId234", "passport345", "John Doe", "10 Downing Street")
        customerFile.enterCustomerData(AuthInfo(), newData)
        LoanCustomer(newData.customerId)
    }
    val createLoanRequest by configureRunnableActivity<LoanCustomer, LoanApplication>(
        inputCustomerMasterData,
        LoanApplication.serializer()
                                                                                     ) {
        defineInput(inputCustomerMasterData)
        action = {
            LoanApplication(
                it.customerId,
                10000,
                listOf(CustomerCollateral("house", "100000", "residential"))
                           )
        }
    }
    val evaluateCredit by object : ConfigurableCompositeActivity(createLoanRequest) {
        val creditBureau = CreditBureau()
        val customerFile get() = this@Model1.customerFile
        val creditApplication = CreditApplication(customerFile)

        val startCreditEvaluate by startNode
        val getCustomerApproval by runnableActivity(
            startCreditEvaluate,
            Approval.serializer(),
            LoanCustomer.serializer(),
            null,
            "customerId"
                                                   ) { customer ->
            Approval(true) // full approval for now
        }
        val getCreditReport by runnableActivity(
            getCustomerApproval,
            CreditReport.serializer(),
            LoanCustomer.serializer(),
            null, "customerId"
                                               ) { customer ->
            val customerData: CustomerData = customerFile.getCustomerData(AuthInfo(), customer.customerId)
                ?: throw NullPointerException("Missing customer data")
            creditBureau.getCreditReport(AuthInfo(), customerData)
        }
        val getLoanEvaluation by configureRunnableActivity<Pair<LoanApplication, CreditReport>, LoanEvaluation>(
            getCreditReport,
            LoanEvaluation.serializer()
                                                                                                               ) {
            inputCombiner = InputCombiner {
                Pair(it["application"] as LoanApplication, it["creditReport"] as CreditReport)
            }
            defineInput("application", null, "loanApplication", LoanApplication.serializer())
            defineInput("creditReport", getCreditReport, CreditReport.serializer())

            action = { (application, creditReport) ->
                creditApplication.evaluateLoan(AuthInfo(), application, creditReport)
            }
        }
        val end by endNode(getLoanEvaluation)

        init {
            input("customerId", this@Model1.inputCustomerMasterData)
            input("loanApplication", this@Model1.createLoanRequest)
            output("loanEvaluation", getLoanEvaluation)
        }
    }
    val end by endNode(evaluateCredit) {
        defines.add(XmlDefineType("input", evaluateCredit))
        results.add(XmlResultType("result", "input"))
    }

    init {
        output("loanEvaluation", evaluateCredit)
    }
}
