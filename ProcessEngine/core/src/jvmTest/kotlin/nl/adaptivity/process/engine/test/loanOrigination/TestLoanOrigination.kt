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
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.get
import nl.adaptivity.process.engine.test.ProcessEngineFactory
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.*
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.engine.test.loanOrigination.systems.Browser
import nl.adaptivity.process.engine.test.loanOrigination.systems.SignedDocument
import nl.adaptivity.process.engine.test.loanOrigination.systems.TaskList
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

class TestLoanOrigination : ProcessEngineTestSupport() {

    @Test
    fun testCreateModel() {
        assertEquals("inputCustomerMasterData", Model1(modelOwnerPrincipal).inputCustomerMasterData.id)
    }

    @Test
    fun testRunModel() {
        val model = ExecutableProcessModel(Model1(modelOwnerPrincipal).configurationBuilder)
        val logger = Logger.getLogger(TestLoanOrigination::class.java.name)
        val pef: ProcessEngineFactory<LoanActivityContext> = { messageService, transactionFactory -> defaultEngineFactory(messageService, transactionFactory, LoanContextFactory(logger))}
        testProcess(pef, model) { processEngine, tr, model, hinstance ->
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

@UseExperimental(ImplicitReflectionSerializer::class)
private class Model1(owner: Principal) : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    owner, UUID.fromString("fbb730ab-f1c4-4af5-979b-7e04a399d75a")
                                                                                        ) {


    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer>(start) {
        // TODO break this down into subactivities
        registerTaskPermission(customerFile, QUERY_CUSTOMER_DATA)
        registerTaskPermission(customerFile, CREATE_CUSTOMER)
        registerTaskPermission(customerFile, UPDATE_CUSTOMER_DATA)
        acceptBrowserActivity(clerk1) {

            val customerFileAuthToken = loginToService(customerFile)

            val newData = ctx.processContext.customerData

            customerFile.enterCustomerData(customerFileAuthToken, newData)
            LoanCustomer(newData.customerId)
        }
    }
    val createLoanRequest by configureRunnableActivity<LoanCustomer, LoanApplication>(
        inputCustomerMasterData,
        LoanApplication.serializer()
                                                                                     ) {
        defineInput(this@Model1.inputCustomerMasterData)
        action = { customer ->
            registerTaskPermission(customerFile, QUERY_CUSTOMER_DATA(customer.customerId))
            acceptBrowserActivity(clerk1) {
                // TODO this would normally require some application access
                LoanApplication(
                    customer.customerId,
                    10000.0,
                    listOf(CustomerCollateral("house", "100000", "residential"))
                               )
            }
        }
    }
    val evaluateCredit by object : ConfigurableCompositeActivity(createLoanRequest) {

        init {
            input("customerId", this@Model1.inputCustomerMasterData)
            input("loanApplication", this@Model1.createLoanRequest)
        }

        val startCreditEvaluate by startNode
        val getCustomerApproval by runnableActivity(
            startCreditEvaluate,
            SignedDocument.serializer(Approval.serializer()),
            LoanCustomer.serializer(),
            null,
            "customerId"
                                                   ) { customer ->
            registerTaskPermission(signingService, SIGN)
            acceptBrowserActivity(ctx.customer) {
                val signingToken = loginToService(signingService)
                signingService.signDocument(signingToken, Approval(true))
            }
        }
        val verifyCustomerApproval by configureRunnableActivity<VerifyCustomerApprovalInput, SignedDocument<SignedDocument<Approval>>>(
            getCustomerApproval,
            SignedDocument.serializer(SignedDocument.serializer(Approval.serializer()))
                                                                                                                                      ) {
            val custIn = defineInput("customer", null, "customerId", LoanCustomer.serializer())
            val approvalIn = defineInput("approval", getCustomerApproval, SignedDocument.serializer(Approval.serializer()))
            inputCombiner = InputCombiner { VerifyCustomerApprovalInput(custIn(), approvalIn()) }
            action = { (customer, approval) ->
                registerTaskPermission(customerFile, QUERY_CUSTOMER_DATA(customer.customerId))
                registerTaskPermission(signingService, SIGN)

                acceptBrowserActivity(postProcClerk) {
                    val customerData = customerFile.getCustomerData(loginToService(customerFile), customer.customerId)

                    if (customerData?.name != approval.signedBy) {
                        throw IllegalArgumentException("Customer and signature mismatch: ${customerData?.name} != ${approval.signedBy}")
                    }

                    val signAuth = loginToService(signingService)
                    signingService.signDocument(signAuth, approval)
                }
            }
        }
        val getCreditReport by runnableActivity(
            verifyCustomerApproval,
            CreditReport.serializer(),
            LoanCustomer.serializer(),
            null, "customerId"
                                               ) { customer ->

            registerTaskPermission(customerFile, QUERY_CUSTOMER_DATA(customer.customerId))
            registerTaskPermission(creditBureau, GET_CREDIT_REPORT("taxId234"))
            generalClientService.runWithAuthorization(ctx.serviceTask()) { tknTID ->

                assertForbidden {
                    authService.getAuthTokenDirect(clerk1.user, tknTID, customerFile, CREATE_CUSTOMER)
                }
                assertForbidden {
                    authService.getAuthTokenDirect(clerk1.user, tknTID, customerFile, QUERY_CUSTOMER_DATA)
                }

                val custInfoAuthToken = getServiceToken(customerFile, QUERY_CUSTOMER_DATA.invoke(customer.customerId))

                val customerData: CustomerData = customerFile.getCustomerData(custInfoAuthToken, customer.customerId)
                    ?: throw NullPointerException("Missing customer data")

                assertForbidden {
                    authService.getAuthTokenDirect(automatedService, tknTID, creditBureau, CREATE_CUSTOMER)
                }

                assertForbidden {
                    authService.getAuthTokenDirect(automatedService, tknTID, creditBureau, GET_CREDIT_REPORT)
                }
                assertForbidden {
                    authService.getAuthTokenDirect(automatedService, tknTID, creditBureau, GET_CREDIT_REPORT.invoke("taxId5"))
                }
                val creditAuthToken = getServiceToken(creditBureau, GET_CREDIT_REPORT.invoke(customerData.taxId))


                assertForbidden { creditBureau.getCreditReport(custInfoAuthToken, customerData) }
                assertForbidden { creditBureau.getCreditReport(tknTID, customerData) }

                creditBureau.getCreditReport(creditAuthToken, customerData)
            }
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
                registerTaskPermission(creditApplication, EVALUATE_LOAN(application.customerId))
                registerTaskPermission(authService, GRANT_PERMISSION(customerFile, QUERY_CUSTOMER_DATA(application.customerId)))

                generalClientService.runWithAuthorization(ctx.serviceTask()) { taskIdToken ->

                    val delegatePermissionToken = getServiceToken(authService, GRANT_PERMISSION(customerFile, QUERY_CUSTOMER_DATA.invoke(application.customerId)))
                    val delegateAuthorization = authService.createAuthorizationCode(
                        delegatePermissionToken,
                        creditApplication.serviceId,
                        customerFile,
                        QUERY_CUSTOMER_DATA(application.customerId)
                                                                                   )

                    val authToken = getServiceToken(
                        creditApplication,
                        EVALUATE_LOAN(application.customerId, application.amount)
                                                   )
                    creditApplication.evaluateLoan(authToken, delegateAuthorization, application, creditReport)
                }
            }
        }
        val end by endNode(getLoanEvaluation)

        init {
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

    val offerPricedLoan by object : ConfigurableCompositeActivity(chooseBundledProduct) {

        init {
            input("loanEval", this@Model1.evaluateCredit, "loanEvaluation")
            input("chosenProduct", this@Model1.chooseBundledProduct)
        }

        val start by startNode
        val priceBundledProduct by configureRunnableActivity<PricingInput, PricedLoanProductBundle>(
            start,
            PricedLoanProductBundle.serializer()
                                                                                                   ) {

            val loanEval = defineInput("loanEval", null, "loanEval", LoanEvaluation.serializer())
            val productInput = defineInput("prod", null, "chosenProduct", LoanProductBundle.serializer())

            inputCombiner = InputCombiner {
                PricingInput(loanEval(), productInput())
            }

            action = { (loanEval, chosenProduct) ->
                registerTaskPermission(pricingEngine, PRICE_LOAN.restrictTo(Double.NaN))

                acceptBrowserActivity(postProcClerk) {
                    val pricingEngineLoginToken = loginToService(pricingEngine)
                    pricingEngine.priceLoan(pricingEngineLoginToken, chosenProduct, loanEval)
                }
            }
        }
        val approveOffer by runnableActivity(
            priceBundledProduct,
            PricedLoanProductBundle.serializer(),
            PricedLoanProductBundle.serializer(),
            priceBundledProduct
                                            ) { draftOffer ->
            acceptBrowserActivity(postProcClerk) {
                draftOffer.approve()
            }
        }
        val end by endNode(approveOffer)

        init {
            output("approvedOffer", approveOffer)
        }
    }
    val printOffer by runnableActivity(
        offerPricedLoan,
        Offer.serializer(),
        PricedLoanProductBundle.serializer(),
        offerPricedLoan
                                      ) { approvedOffer ->
        registerTaskPermission(outputManagementSystem, PRINT_OFFER)
        acceptBrowserActivity(postProcClerk) {

            val printAuth = loginToService(outputManagementSystem)

            outputManagementSystem.registerAndPrintOffer(printAuth, approvedOffer)
        }
    }
    val customerSignsContract by runnableActivity(
        printOffer,
        Offer.serializer(),
        Offer.serializer(),
        printOffer
                                                 ) { offer ->
        acceptBrowserActivity(customer) {
            offer.signCustomer("Signed by 'John Doe'")
        }
    }
    val bankSignsContract by runnableActivity(
        customerSignsContract,
        Contract.serializer(),
        Offer.serializer(),
        customerSignsContract
                                             ) { offer ->
        registerTaskPermission(outputManagementSystem, SIGN_LOAN.restrictTo(offer.customerId, Double.NaN))

        acceptBrowserActivity(postProcClerk) {

            val omsToken = loginToService(outputManagementSystem)
            outputManagementSystem.signAndRegisterContract(omsToken, offer, "Signed by 'the bank manager'")
        }
    }
    val openAccount by runnableActivity(
        bankSignsContract,
        BankAccountNumber.serializer(),
        Contract.serializer(),
        bankSignsContract
                                       ) { contract ->
        registerTaskPermission(accountManagementSystem, OPEN_ACCOUNT.invoke(contract.customerId))

        acceptBrowserActivity(postProcClerk) {
            val amsToken = loginToService(accountManagementSystem)

            accountManagementSystem.openAccountFor(amsToken, contract)
        }
    }
    val end by endNode(openAccount)

    init {
        output("loanEvaluation", evaluateCredit, "loanEvaluation")
        output("creditReport", evaluateCredit, "creditReport")
        output("accountNumber", openAccount)
    }
}

private data class PricingInput(val loanEvaluation: LoanEvaluation, val chosenProduct: LoanProductBundle)
private data class VerifyCustomerApprovalInput(val customer: LoanCustomer, val approval: SignedDocument<Approval>)

private inline val ActivityInstanceContext.ctx: LoanActivityContext get() = this as LoanActivityContext
private inline val ActivityInstanceContext.customerFile get() = ctx.processContext.customerFile
private inline val ActivityInstanceContext.signingService get() = ctx.processContext.signingService
private inline val ActivityInstanceContext.outputManagementSystem get() = ctx.processContext.outputManagementSystem
private inline val ActivityInstanceContext.accountManagementSystem get() = ctx.processContext.accountManagementSystem
private inline val ActivityInstanceContext.creditBureau get() = ctx.processContext.creditBureau
private inline val ActivityInstanceContext.creditApplication get() = ctx.processContext.creditApplication
private inline val ActivityInstanceContext.pricingEngine get() = ctx.processContext.pricingEngine
private inline val ActivityInstanceContext.authService get() = ctx.processContext.authService
private inline val ActivityInstanceContext.generalClientService get() = ctx.processContext.generalClientService
private inline val ActivityInstanceContext.clerk1 get() = ctx.processContext.clerk1
private inline val ActivityInstanceContext.postProcClerk get() = ctx.processContext.postProcClerk
private inline val ActivityInstanceContext.customer get() = ctx.processContext.customer


private fun ActivityInstanceContext.registerTaskPermission(service: Service, scope: PermissionScope) =
    ctx.registerTaskPermission(service, scope)
private inline fun <R> ActivityInstanceContext.acceptBrowserActivity(browser: Browser, action: TaskList.Context.() -> R): R =
    ctx.acceptBrowserActivity(browser, action)

const val ASSERTFORBIDDENENABLED = false

inline fun assertForbidden(noinline action: () -> Unit) {
    if (ASSERTFORBIDDENENABLED) Assertions.assertThrows(AuthorizationException::class.java, action)
}
