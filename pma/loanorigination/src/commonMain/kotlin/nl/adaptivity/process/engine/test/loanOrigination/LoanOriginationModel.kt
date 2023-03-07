package nl.adaptivity.process.engine.test.loanOrigination

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import nl.adaptivity.process.engine.pma.ANYSCOPE
import nl.adaptivity.process.engine.pma.AuthorizationException
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.*
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.engine.test.loanOrigination.systems.SignedDocument
import nl.adaptivity.process.processModel.configurableModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.toUUID
import org.junit.jupiter.api.Assertions

class LoanOriginationModel(owner: PrincipalCompat) : ConfigurableProcessModel<ExecutableProcessNode>(
    "testLoanOrigination",
    owner, "fbb730ab-f1c4-4af5-979b-7e04a399d75a".toUUID()
) {


    val start by startNode
    val inputCustomerMasterData by runnableActivity<Unit, LoanCustomer, LoanActivityContext>(start) {
        // TODO break this down into subactivities
        registerTaskPermission(customerFile, QUERY_CUSTOMER_DATA)
        registerTaskPermission(customerFile, CREATE_CUSTOMER)
        registerTaskPermission(customerFile, UPDATE_CUSTOMER_DATA)
        acceptBrowserActivity(clerk1) {

            val customerFileAuthToken = loginToService(customerFile)

            val newData = customerData

            customerFile.enterCustomerData(customerFileAuthToken, newData)
            LoanCustomer(newData.customerId, newData.taxId)
        }
    }
    val createLoanRequest by configureRunnableActivity<LoanCustomer, LoanApplication, LoanActivityContext>(
        inputCustomerMasterData,
        LoanApplication.serializer()
    ) {
        defineInput(this@LoanOriginationModel.inputCustomerMasterData)
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
            input("customerId", this@LoanOriginationModel.inputCustomerMasterData)
            input("loanApplication", this@LoanOriginationModel.createLoanRequest)
        }

        val startCreditEvaluate by startNode
        val getCustomerApproval by loanActivity(
            startCreditEvaluate,
            SignedDocument.serializer(Approval.serializer()),
            LoanCustomer.serializer(),
            null,
            "customerId"
        ) { customer ->
            registerTaskPermission(signingService, LoanPermissions.SIGN)
            acceptBrowserActivity(processContext.customer) {
                val signingToken = loginToService(signingService)
                signingService.signDocument(signingToken, Approval(true))
            }
        }

        val verifyCustomerApproval by let { m ->
            configureLoanActivity<VerifyCustomerApprovalInput, SignedDocument<SignedDocument<Approval>>>(
                getCustomerApproval,
                SignedDocument.serializer(SignedDocument.serializer(Approval.serializer()))
            ) {
                val custIn = defineInput("customer", null, "customerId", LoanCustomer.serializer())
                val approvalIn =
                    defineInput("approval", m.getCustomerApproval, SignedDocument.serializer(Approval.serializer()))
                inputCombiner = InputCombiner { VerifyCustomerApprovalInput(custIn(), approvalIn()) }
                action = { (customer, approval) ->
                    registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customer.customerId))
                    registerTaskPermission(signingService, LoanPermissions.SIGN)

                    acceptBrowserActivity(postProcClerk) {
                        val customerData =
                            customerFile.getCustomerData(loginToService(customerFile), customer.customerId)

                        if (customerData?.name != approval.signedBy) {
                            throw IllegalArgumentException("Customer and signature mismatch: ${customerData?.name} != ${approval.signedBy}")
                        }

                        val signAuth = loginToService(signingService)
                        signingService.signDocument(signAuth, approval)
                    }
                }
            }
        }

        val getCreditReport by loanActivity(
            verifyCustomerApproval,
            CreditReport.serializer(),
            LoanCustomer.serializer(),
            null, "customerId"
        ) { customer ->

            registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customer.customerId))
            registerTaskPermission(creditBureau, LoanPermissions.GET_CREDIT_REPORT(customer.taxId))
            generalClientService.runWithAuthorization(serviceTask()) { tknTID ->

                assertForbidden {
                    authService.getAuthTokenDirect(tknTID, customerFile, LoanPermissions.CREATE_CUSTOMER)
                }
                assertForbidden {
                    authService.getAuthTokenDirect(tknTID, customerFile, LoanPermissions.QUERY_CUSTOMER_DATA)
                }

                val custInfoAuthToken = getServiceToken(customerFile,
                    LoanPermissions.QUERY_CUSTOMER_DATA.invoke(customer.customerId)
                )

                val customerData: CustomerData = customerFile.getCustomerData(custInfoAuthToken, customer.customerId)
                    ?: throw NullPointerException("Missing customer data")

                assertForbidden {
                    authService.getAuthTokenDirect(tknTID, creditBureau, LoanPermissions.CREATE_CUSTOMER)
                }

                assertForbidden {
                    authService.getAuthTokenDirect(tknTID, creditBureau, LoanPermissions.GET_CREDIT_REPORT)
                }
                assertForbidden {
                    authService.getAuthTokenDirect(
                        tknTID,
                        creditBureau,
                        LoanPermissions.GET_CREDIT_REPORT.invoke("taxId5")
                    )
                }
                val creditAuthToken = getServiceToken(creditBureau,
                    LoanPermissions.GET_CREDIT_REPORT.invoke(customerData.taxId)
                )


                assertForbidden { creditBureau.getCreditReport(custInfoAuthToken, customerData) }
                assertForbidden { creditBureau.getCreditReport(tknTID, customerData) }

                creditBureau.getCreditReport(creditAuthToken, customerData)
            }
        }

        val getLoanEvaluation by let { m ->
            configureLoanActivity<Pair<LoanApplication, CreditReport>, LoanEvaluation>(
                getCreditReport,
                LoanEvaluation.serializer()
            ) {
                val apIn = defineInput("application", null, "loanApplication", LoanApplication.serializer())
                val credIn = defineInput("creditReport", m.getCreditReport, CreditReport.serializer())
                inputCombiner = InputCombiner {
                    Pair(apIn(), credIn())
                }

                action = { (application, creditReport) ->
                    registerTaskPermission(creditApplication, LoanPermissions.EVALUATE_LOAN(application.customerId))
                    registerDelegatePermission(
                        creditApplication,
                        customerFile,
                        LoanPermissions.QUERY_CUSTOMER_DATA(application.customerId)
                    )

                    generalClientService.runWithAuthorization(serviceTask()) { taskIdToken ->
                        val authToken = getServiceToken(creditApplication, ANYSCOPE)
                        // ANYSCOPE is valid here as we just defined the needed permissions. Enumerating is possible,
                        // but tedious
                        creditApplication.evaluateLoan(authToken, application, creditReport)
                    }
                }
            }
        }
        val end by endNode(getLoanEvaluation)

        init {
            output("loanEvaluation", getLoanEvaluation)
            output("creditReport", getCreditReport)
        }
    }

    val chooseBundledProduct by loanActivity(
        evaluateCredit,
        LoanProductBundle.serializer(),
        LoanEvaluation.serializer(), evaluateCredit, "loanEvaluation"
    ) { loanEvaluation ->
        LoanProductBundle("simpleLoan", "simpleLoan2019.a")
    }

    val offerPricedLoan by object : ConfigurableCompositeActivity(chooseBundledProduct) {

        init {
            input("loanEval", this@LoanOriginationModel.evaluateCredit, "loanEvaluation")
            input("chosenProduct", this@LoanOriginationModel.chooseBundledProduct)
        }

        val start by startNode
        val priceBundledProduct by configureLoanActivity<PricingInput, PricedLoanProductBundle>(
            start,
            PricedLoanProductBundle.serializer()
        ) {

            val loanEval = defineInput("loanEval", null, "loanEval", LoanEvaluation.serializer())
            val productInput = defineInput("prod", null, "chosenProduct", LoanProductBundle.serializer())

            inputCombiner = InputCombiner {
                PricingInput(loanEval(), productInput())
            }

            action = { (loanEval, chosenProduct) ->
                registerTaskPermission(pricingEngine, LoanPermissions.PRICE_LOAN.restrictTo(Double.NaN))

                acceptBrowserActivity(postProcClerk) {
                    val pricingEngineLoginToken = loginToService(pricingEngine)
                    pricingEngine.priceLoan(pricingEngineLoginToken, chosenProduct, loanEval)
                }
            }
        }
        val approveOffer by loanActivity(
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
    val printOffer by loanActivity(
        offerPricedLoan,
        Offer.serializer(),
        PricedLoanProductBundle.serializer(),
        offerPricedLoan
    ) { approvedOffer ->
        registerTaskPermission(outputManagementSystem, LoanPermissions.PRINT_OFFER)
        acceptBrowserActivity(postProcClerk) {

            val printAuth = loginToService(outputManagementSystem)

            outputManagementSystem.registerAndPrintOffer(printAuth, approvedOffer)
        }
    }
    val customerSignsContract by loanActivity(
        printOffer,
        Offer.serializer(),
        Offer.serializer(),
        printOffer
    ) { offer ->
        acceptBrowserActivity(customer) {
            offer.signCustomer("Signed by 'John Doe'")
        }
    }
    val bankSignsContract by loanActivity(
        customerSignsContract,
        Contract.serializer(),
        Offer.serializer(),
        customerSignsContract
    ) { offer ->
        registerTaskPermission(outputManagementSystem,
            LoanPermissions.SIGN_LOAN.restrictTo(offer.customerId, Double.NaN)
        )

        acceptBrowserActivity(postProcClerk) {

            val omsToken = loginToService(outputManagementSystem)
            outputManagementSystem.signAndRegisterContract(omsToken, offer, "Signed by 'the bank manager'")
        }
    }
    val openAccount by loanActivity(
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


fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.loanActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified?,
    inputRefName: String = "",
    action: RunnableAction2<I, O, LoanActivityContext>
): RunnableActivity.Builder<I, O, LoanActivityContext> =
    RunnableActivity.Builder(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer, action)

fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.configureLoanActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>,
    inputSerializer: DeserializationStrategy<I>,
    inputRefNode: Identified,
    inputRefName: String = "",
    config: @ConfigurationDsl RunnableActivity.Builder<I, O, LoanActivityContext>.() -> Unit
): RunnableActivity.Builder<I, O, LoanActivityContext> =
    RunnableActivity.Builder<I, O, LoanActivityContext>(predecessor, inputRefNode, inputRefName, inputSerializer, outputSerializer).apply(config)

fun <I : Any, O : Any> ConfigurableNodeContainer<ExecutableProcessNode>.configureLoanActivity(
    predecessor: Identified,
    outputSerializer: SerializationStrategy<O>?,
    config: @ConfigurationDsl RunnableActivity.Builder<I, O, LoanActivityContext>.() -> Unit
): RunnableActivity.Builder<I, O, LoanActivityContext> =
    RunnableActivity.Builder<I, O, LoanActivityContext>(predecessor, outputSerializer = outputSerializer).apply(config)

private val LoanActivityContext.ctx: LoanProcessContext get() = processContext


data class PricingInput(val loanEvaluation: LoanEvaluation, val chosenProduct: LoanProductBundle)
data class VerifyCustomerApprovalInput(val customer: LoanCustomer, val approval: SignedDocument<Approval>)


@Deprecated("These are testing things")
@Suppress("NOTHING_TO_INLINE")
private inline fun assertForbidden(noinline action: () -> Unit) {
    Assertions.assertThrows(AuthorizationException::class.java, action)
}

