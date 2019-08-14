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
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.get
import nl.adaptivity.process.engine.test.BaseProcessEngineTestSupport
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthInfo
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthorizationException
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.output
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.process.processModel.engine.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.Principal
import java.util.*
import java.util.logging.Logger

class TestLoanOrigination : BaseProcessEngineTestSupport<LoanActivityContext>(LoanContextFactory(Logger.getLogger(TestLoanOrigination::class.java.name))) {

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
            val creditReport = instance.outputs.singleOrNull { it.name == "creditReport" }
            assertEquals(
                "<CreditReport creditRating=\"400\" maxLoan=\"20000\">John Doe (rating 400) is approved for loans up to 20000</CreditReport>",
                creditReport?.content?.contentString
                        )
            val accountNumber = instance.outputs.singleOrNull { it.name == "accountNumber" }
            assertEquals("<BankAccountNumber>123456</BankAccountNumber>", accountNumber?.content?.contentString)
        }
    }

}

private val clerk1 = SimplePrincipal("preprocessing clerk 1")
private val automatedService = SimplePrincipal("<automated>")

@UseExperimental(ImplicitReflectionSerializer::class)
private class Model1(owner: Principal) : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    owner, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")
                                                                                        ) {


    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer>(start) {
        ctx.registerTaskPermission(customerFile, LoanPermissions.CREATE_CUSTOMER)
        ctx.acceptActivity(clerk1)

        val customerFileAuthToken = authService.getAuthTokenDirect(clerk1, ctx.taskList.taskIdentityToken!!, customerFile.serviceId, LoanPermissions.CREATE_CUSTOMER)

        val newData = CustomerData(
            "cust123456",
            "taxId234",
            "passport345",
            "John Doe",
            "10 Downing Street"
                                                                                             )
        customerFile.enterCustomerData(customerFileAuthToken, newData)
        LoanCustomer(newData.customerId)
    }
    val createLoanRequest by configureRunnableActivity<LoanCustomer, LoanApplication>(
        inputCustomerMasterData,
        LoanApplication.serializer()
                                                                                     ) {
        defineInput(this@Model1.inputCustomerMasterData)
        action = {
            // TODO this would normally require some application access
            LoanApplication(
                it.customerId,
                10000,
                listOf(CustomerCollateral("house", "100000", "residential"))
                                                                                       )
        }
    }
    val evaluateCredit by object : ConfigurableCompositeActivity(createLoanRequest) {

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

            ctx.registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA.context(customer.customerId))
            ctx.registerTaskPermission(creditBureau, LoanPermissions.GET_CREDIT_REPORT.context("taxId234"))
            ctx.acceptActivity(automatedService)

            val taskIdentityToken = ctx.taskList.taskIdentityToken!!
            assertForbidden {authService.getAuthTokenDirect(clerk1, taskIdentityToken, customerFile.serviceId, LoanPermissions.CREATE_CUSTOMER)}
            assertForbidden {authService.getAuthTokenDirect(clerk1, taskIdentityToken, customerFile.serviceId, LoanPermissions.QUERY_CUSTOMER_DATA)}

            val custInfoAuthToken = authService.getAuthTokenDirect(automatedService, taskIdentityToken, customerFile.serviceId, LoanPermissions.QUERY_CUSTOMER_DATA.context(customer.customerId))


            val customerData: CustomerData = customerFile.getCustomerData(custInfoAuthToken, customer.customerId)
                ?: throw NullPointerException("Missing customer data")

            assertForbidden { authService.getAuthTokenDirect(automatedService, taskIdentityToken, creditBureau.serviceId, LoanPermissions.CREATE_CUSTOMER) }
            assertForbidden { authService.getAuthTokenDirect(automatedService, taskIdentityToken, creditBureau.serviceId, LoanPermissions.GET_CREDIT_REPORT) }
            assertForbidden { authService.getAuthTokenDirect(automatedService, taskIdentityToken, creditBureau.serviceId, LoanPermissions.GET_CREDIT_REPORT.context("taxId5")) }
            val creditAuthToken = authService.getAuthTokenDirect(automatedService, taskIdentityToken, creditBureau.serviceId, LoanPermissions.GET_CREDIT_REPORT.context(customerData.taxId))


            assertForbidden { creditBureau.getCreditReport(custInfoAuthToken, customerData) }
            assertForbidden { creditBureau.getCreditReport(taskIdentityToken, customerData) }
            creditBureau.getCreditReport(creditAuthToken, customerData)
        }
        val getLoanEvaluation by configureRunnableActivity<Pair<LoanApplication, CreditReport>, LoanEvaluation>(
            getCreditReport,
            LoanEvaluation.serializer()
                                                                                                                                                                           ) {
            val apIn = defineInput("application", null, "loanApplication", LoanApplication.serializer())
            val credIn = defineInput("creditReport", getCreditReport, CreditReport.serializer())
            inputCombiner = InputCombiner {
                Pair(apIn(), credIn())
            }

            action = { (application, creditReport) ->
                ctx.registerTaskPermission(creditApplication, LoanPermissions.EVALUATE_LOAN.context(application.customerId))
                ctx.acceptActivity(automatedService)

                val delegateAuthorization = ctx.authService.createAuthorizationCode(ctx.processContext.engineService.serviceAuth, creditApplication.serviceId, handle, customerFile, LoanPermissions.QUERY_CUSTOMER_DATA.context(application.customerId))

                val authToken = authService.getAuthTokenDirect(automatedService, ctx.taskList.taskIdentityToken!!, creditApplication.serviceId, LoanPermissions.EVALUATE_LOAN.context(application.customerId))
                creditApplication.evaluateLoan(authToken, delegateAuthorization, application, creditReport)
            }
        }
        val end by endNode(getLoanEvaluation)

        init {
            input("customerId", this@Model1.inputCustomerMasterData)
            input("loanApplication", this@Model1.createLoanRequest)
            output("loanEvaluation", getLoanEvaluation)
            output("creditReport", getCreditReport)
        }
    }
    val chooseBundledProduct by runnableActivity(
        evaluateCredit,
        LoanProductBundle.serializer(),
        LoanEvaluation.serializer(), evaluateCredit, "loanEvaluation"
                                                ) { loanEvaluation ->
        LoanProductBundle("simpleLoan", "simpleLoan2019.a")
    }
    val offerPricedLoan by object: ConfigurableCompositeActivity(chooseBundledProduct) {

        init {
            input("loanEval", this@Model1.evaluateCredit, "loanEvaluation")
            input("chosenProduct", this@Model1.chooseBundledProduct)
        }
        val start by startNode
        val priceBundledProduct by configureRunnableActivity<PricingInput, PricedLoanProductBundle>(start, PricedLoanProductBundle.serializer()) {

            val loanEval = defineInput("loanEval", null, "loanEval", LoanEvaluation.serializer())
            val productInput = defineInput("prod", null, "chosenProduct", LoanProductBundle.serializer())

            inputCombiner = InputCombiner {
                PricingInput(loanEval(), productInput())
            }

            action = { (loanEval, chosenProduct) ->
                pricingEngine.priceLoan(chosenProduct, loanEval)
            }
        }
        val approveOffer by runnableActivity(priceBundledProduct, PricedLoanProductBundle.serializer(), PricedLoanProductBundle.serializer(), priceBundledProduct) { draftOffer ->
            draftOffer.approve()
        }
        val end by endNode(approveOffer)

        init {
            output("approvedOffer", approveOffer)
        }
    }
    val printOffer by runnableActivity(offerPricedLoan, Offer.serializer(), PricedLoanProductBundle.serializer(), offerPricedLoan) { approvedOffer ->
        outputManagementSystem.registerAndPrintOffer(approvedOffer)
    }
    val customerSignsContract by runnableActivity(printOffer, Offer.serializer(), Offer.serializer(), printOffer) { offer ->
        offer.signCustomer("Signed by 'John Doe'")
    }
    val bankSignsContract by runnableActivity(customerSignsContract, Contract.serializer(), Offer.serializer(), customerSignsContract) {
        outputManagementSystem.signAndRegisterContract(it, "Signed by 'the bank manager'")
    }
    val openAccount by runnableActivity(bankSignsContract, BankAccountNumber.serializer(), Contract.serializer(), bankSignsContract) { contract ->
        accountManagementSystem.openAccountFor(AuthInfo(), contract)
    }
    val end by endNode(openAccount)

    init {
        output("loanEvaluation", evaluateCredit, "loanEvaluation")
        output("creditReport", evaluateCredit, "creditReport")
        output("accountNumber", openAccount)
    }
}
private data class PricingInput(val loanEvaluation: LoanEvaluation, val chosenProduct: LoanProductBundle)

private inline val ActivityInstanceContext.ctx: LoanActivityContext get() = this as LoanActivityContext
private inline val ActivityInstanceContext.customerFile get() = ctx.processContext.customerFile
private inline val ActivityInstanceContext.outputManagementSystem get() = ctx.processContext.outputManagementSystem
private inline val ActivityInstanceContext.accountManagementSystem get() = ctx.processContext.accountManagementSystem
private inline val ActivityInstanceContext.creditBureau get() = ctx.processContext.creditBureau
private inline val ActivityInstanceContext.creditApplication get() = ctx.processContext.creditApplication
private inline val ActivityInstanceContext.pricingEngine get() = ctx.processContext.pricingEngine
private inline val ActivityInstanceContext.authService get() = ctx.processContext.authService

const val ASSERTFORBIDDENENABLED = true

inline fun assertForbidden(noinline action: ()-> Unit) {
    if(ASSERTFORBIDDENENABLED) Assertions.assertThrows(AuthorizationException::class.java, action)
}
