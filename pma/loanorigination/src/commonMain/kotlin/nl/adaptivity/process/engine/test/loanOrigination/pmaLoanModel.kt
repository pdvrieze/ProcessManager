package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.*
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.GeneralClientService
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.accountManagementSystem
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.creditApplication
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.customerFile
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.outputManagementSystem
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.pricingEngine
import nl.adaptivity.process.engine.test.loanOrigination.ServiceNames.signingService
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions.*
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.engine.test.loanOrigination.systems.*
import nl.adaptivity.util.multiplatform.PrincipalCompat

val pmaLoanModel =
    runnablePmaProcess<LoanPMAActivityContext, LoanBrowserContext>("pmaLoanModel", SimplePrincipal("modelOwner")) {
        val start by startNode

        val inputCustomerMasterData: ActivityHandle<LoanCustomer> by taskActivity(
            predecessor = start,
            permissions = listOf(
                delegatePermissions(customerFile, QUERY_CUSTOMER_DATA, CREATE_CUSTOMER, UPDATE_CUSTOMER_DATA)
            ),
            accessRestrictions = RoleRestriction("clerk"),
        ) {
            acceptTask(clerk1) {
                uiServiceLogin(customerFile) {
                    val newData = data.customerData
                    service.enterCustomerData(authToken, newData)
                }
            }
        }

        val createLoanRequest: ActivityHandle<LoanApplication> by taskActivity(
            predecessor = inputCustomerMasterData,
            permissions = listOf(
                delegatePermissions(
                    customerFile,
                    ContextScopeTemplate(QUERY_CUSTOMER_DATA) {
                        nodeData(inputCustomerMasterData)?.let { customerData ->
                            it(customerData.customerId)
                        }
                    })
            ),
            accessRestrictions = RoleRestriction("clerk")
        ) {
            acceptTask(clerk1) { customer: LoanCustomer ->
                // TODO this would normally require some application access
                LoanApplication(
                    customer.customerId,
                    10000.0,
                    listOf(CustomerCollateral("house", "100000", "residential"))
                )
            }
        }

        val loanEvaluationOut: OutputRef<LoanEvaluation>
        val creditReportOut: OutputRef<CreditReport>

        val evaluateCredit by compositeActivity(createLoanRequest) {
            val customerIdInput = input("customerId", inputCustomerMasterData)
            val loanApplicationInput = input("loanApplication", createLoanRequest)

            val startCreditEvaluate by startNode

            val getCustomerApproval: ActivityHandle<SignedDocument<Approval>> by taskActivity(
                predecessor = startCreditEvaluate,
                authorizationTemplates = listOf(delegatePermissions(signingService, SIGN)),
                accessRestrictions = RoleRestriction("customer"), // TODO support dynamic restrictions
                input = customerIdInput as InputRef<LoanCustomer>,
            ) {
                acceptTask(customer) { _: LoanCustomer ->
                    uiServiceLogin(signingService) {
                        service.signDocument(authToken, Approval(true))
                    }
                }
            }

            val verifyCustomerApproval: ActivityHandle<SignedDocument<SignedDocument<Approval>>> by taskActivity(
                predecessor = getCustomerApproval,
                authorizationTemplates = listOf(
                    delegatePermissions(
                        customerFile,
                        ContextScopeTemplate(QUERY_CUSTOMER_DATA) { s ->
                            nodeData(customerIdInput)?.customerId?.let {
                                s.invoke(it)
                            }
                        }),
                    delegatePermissions(signingService, SIGN)
                ),
                input = combine(customerIdInput named "customer", getCustomerApproval named "approval") { a, b ->
                    VerifyCustomerApprovalInput(a, b)
                }
            ) {
                acceptTask(postProcClerk) { (customer, approval) ->
                    val customerData = uiServiceLogin(customerFile) {
                        service.getCustomerData(authToken, customer.customerId)
                    }

                    if (customerData?.name != approval.signedBy) {
                        throw IllegalArgumentException("Customer and signature mismatch: ${customerData?.name} != ${approval.signedBy}")
                    }

                    uiServiceLogin(signingService) {
                        service.signDocument(authToken, approval)
                    }
                }
            }

            val getCreditReport: ActivityHandle<CreditReport> by serviceActivity<LoanCustomer, CreditReport, CreditBureau>(
                predecessor = verifyCustomerApproval,
                service = ServiceNames.creditBureau,
                input = customerIdInput,
                authorizationTemplates = listOf(
                    delegatePermissions(
                        customerFile,
                        ContextScopeTemplate(QUERY_CUSTOMER_DATA) { t ->
                            nodeData(customerIdInput)?.customerId?.let {
                                t(it)
                            }
                        }),
                    ContextScopeTemplate(GET_CREDIT_REPORT) { t ->
                        nodeData(customerIdInput)?.taxId?.let {
                            t(
                                it
                            )
                        }
                    }
                ),
            ) { customer: LoanCustomer ->
                // TODO maybe retrieve the customer data to pass it to the creditBureau
                service.getCreditReport(processContext.contextFactory, authToken, customer.customerId, customer.taxId)

                /*
                generalClientService.runWithAuthorization(serviceTask()) { tknTID ->

                    assertForbidden {
                        authService.getAuthTokenDirect(tknTID, customerFile, LoanPermissions.CREATE_CUSTOMER)
                    }
                    assertForbidden {
                        authService.getAuthTokenDirect(tknTID, customerFile, LoanPermissions.QUERY_CUSTOMER_DATA)
                    }

                    val custInfoAuthToken = getServiceToken(
                        customerFile,
                        LoanPermissions.QUERY_CUSTOMER_DATA.invoke(customer.customerId)
                    )

                    val customerData: CustomerData =
                        customerFile.getCustomerData(custInfoAuthToken, customer.customerId)
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
                    val creditAuthToken = getServiceToken(
                        creditBureau,
                        LoanPermissions.GET_CREDIT_REPORT.invoke(customerData.taxId)
                    )


                    assertForbidden { creditBureau.getCreditReport(custInfoAuthToken, customerData) }
                    assertForbidden { creditBureau.getCreditReport(tknTID, customerData) }

                    creditBureau.getCreditReport(creditAuthToken, customerData)
*/
            }


            val getLoanEvaluation: ActivityHandle<LoanEvaluation> by serviceActivity(

                predecessor = getCreditReport,
                input = combine(
                    loanApplicationInput named "application",
                    getCreditReport named "creditReport"
                ),
                authorizationTemplates = listOf(
                    ContextScopeTemplate(EVALUATE_LOAN) { t -> nodeData(loanApplicationInput)?.let { t(it.customerId) } },
                    delegatePermissions(
                        customerFile,
                        ContextScopeTemplate(QUERY_CUSTOMER_DATA) { it(nodeData(loanApplicationInput)!!.customerId) })
                ),
                service = creditApplication
            ) { (application, creditReport) ->
                service.evaluateLoan(authToken, application, creditReport)
            }


            val end by endNode(getLoanEvaluation)

            loanEvaluationOut = output("loanEvaluation", getLoanEvaluation)

            creditReportOut = output("creditReport", getCreditReport)
        }

        val chooseBundledProduct by taskActivity(
            predecessor = evaluateCredit,
            accessRestrictions = RoleRestriction("customer"),
            input = loanEvaluationOut
        ) {
            acceptTask(customer) { loanEvaluation ->
                LoanProductBundle("simpleLoan", "simpleLoan2019.a")
            }
        }

        val approvedOfferOut: OutputRef<PricedLoanProductBundle>

        val offerPriceLoan by compositeActivity(chooseBundledProduct) {
            val loanEvalInput = input("loanEval", loanEvaluationOut)
            val chosenProductInput = input("chosenProduct", chooseBundledProduct)

            val start by startNode

            val priceBundledProduct: ActivityHandle<PricedLoanProductBundle> by taskActivity(
                predecessor = start,
                input = combine(loanEvalInput named "loanEval", chosenProductInput named "prod") { e, p ->
                    PricingInput(e, p)
                },
                authorizationTemplates = listOf(
                    delegatePermissions(
                        pricingEngine,
                        PRICE_LOAN.restrictTo(Double.NaN)
                    )
                )
            ) {
                acceptTask(postProcClerk) { (loanEval, chosenProduct) ->
                    uiServiceLogin(pricingEngine) {
                        service.priceLoan(authToken, chosenProduct, loanEval)
                    }
                }
            }

            val approveOffer: ActivityHandle<PricedLoanProductBundle> by taskActivity(
                predecessor = priceBundledProduct,
                input = priceBundledProduct,
            ) {
                acceptTask(postProcClerk) { draftOffer ->
                    draftOffer.approve()
                }
            }

            val end by endNode(approveOffer)

            approvedOfferOut = output("approvedOffer", approveOffer)
        }
        val printOffer: ActivityHandle<Offer> by taskActivity(
            predecessor = offerPriceLoan,
            input = approvedOfferOut,
            authorizationTemplates = listOf(delegatePermissions(outputManagementSystem, PRINT_OFFER))
        ) {
            acceptTask(postProcClerk) { approvedOffer ->
                uiServiceLogin(outputManagementSystem) {
                    service.registerAndPrintOffer(authToken, approvedOffer)
                }
            }
        }
        val customerSignsContract by taskActivity(
            predecessor = printOffer,
            input = printOffer
        ) {
            acceptTask(customer) { offer ->
                offer.signCustomer("Signed by 'John Doe'")
            }
        }

        val bankSignsContract: ActivityHandle<Contract> by taskActivity(
            predecessor = customerSignsContract,
            input = customerSignsContract,
            authorizationTemplates = listOf(
                delegatePermissions(
                    outputManagementSystem,
                    ContextScopeTemplate(SIGN_LOAN) {
                        it.restrictTo(
                            nodeData(customerSignsContract)!!.customerId,
                            Double.NaN
                        )
                    })
            )
        ) {
            acceptTask(postProcClerk) { offer ->
                uiServiceLogin(outputManagementSystem) {
                    service.signAndRegisterContract(authToken, offer, "Signed by 'the bank manager'")
                }
            }
        }
        val openAccount: ActivityHandle<BankAccountNumber> by taskActivity(
            predecessor = bankSignsContract,
            permissions = listOf(
                delegatePermissions(
                    accountManagementSystem,
                    ContextScopeTemplate(OPEN_ACCOUNT) { it(nodeData(bankSignsContract)!!.customerId) })
            )
        ) {
            acceptTask(postProcClerk) { contract ->
                uiServiceLogin(accountManagementSystem) {
                    service.openAccountFor(authToken, contract)

                }

            }
        }
        val end by endNode(openAccount)

        processResult("loanEvaluation", loanEvaluationOut)
        processResult("creditReport", creditReportOut)
        processResult("accountNumber", openAccount)

    }

//private inline val LoanBrowserContext.accountManagementSystem get() = processContext.accountManagementSystem.serviceInstanceId
//private inline val LoanBrowserContext.authService get() = processContext.authService
//private inline val LoanBrowserContext.creditApplication get() = processContext.creditApplication
//private inline val LoanBrowserContext.creditBureau get() = processContext.creditBureau
//private inline val LoanBrowserContext.customerData get() = processContext.customerData
//private inline val LoanBrowserContext.customerFile get() = processContext.customerFile
//private inline val LoanBrowserContext.generalClientService get() = processContext.generalClientService
//private inline val LoanBrowserContext.outputManagementSystem get() = processContext.outputManagementSystem
//private inline val LoanBrowserContext.pricingEngine get() = processContext.pricingEngine
//private inline val LoanBrowserContext.signingService get() = processContext.signingService

private inline val TaskBuilderContext<LoanPMAActivityContext, *, *>.customer: PrincipalCompat get() = AbstractLoanContextFactory.principals.customer
private inline val TaskBuilderContext<LoanPMAActivityContext, *, *>.clerk1: PrincipalCompat get() = AbstractLoanContextFactory.principals.clerk1
private inline val TaskBuilderContext<LoanPMAActivityContext, *, *>.postProcClerk: PrincipalCompat get() = AbstractLoanContextFactory.principals.clerk2

object ServiceNames {
    val accountManagementSystem: ServiceName<AccountManagementSystem> = ServiceName("accountManagementSystem")
    val authService: ServiceName<AuthService> = ServiceName("authService")
    val engineService: ServiceName<EngineService> = ServiceName("engineService")

    //    val clerk1 : ServiceId = ServiceId("clerk1")
    val creditApplication: ServiceName<CreditApplication> = ServiceName("creditApplication")
    val creditBureau: ServiceName<CreditBureau> = ServiceName("creditBureau")

    //    val customer : ServiceId = ServiceId("customer")
//    val customerData : ServiceId<CustomerData> = ServiceId("customerData")
    val customerFile: ServiceName<CustomerInformationFile> = ServiceName("customerFile")
    val generalClientService: ServiceName<GeneralClientService> = ServiceName("generalClientService")
    val outputManagementSystem: ServiceName<OutputManagementSystem> = ServiceName("outputManagementSystem")

    //    val postProcClerk : ServiceId = ServiceId("postProcClerk")
    val pricingEngine: ServiceName<PricingEngine> = ServiceName("pricingEngine")
    val signingService: ServiceName<SigningService> = ServiceName("signingService")
}
