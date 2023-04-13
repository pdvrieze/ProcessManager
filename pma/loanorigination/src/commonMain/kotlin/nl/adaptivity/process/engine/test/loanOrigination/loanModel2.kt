package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.*
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.pma.models.ANYSCOPE
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.*
import nl.adaptivity.process.engine.test.loanOrigination.systems.SignedDocument

val loanModel2 = runnableProcess<LoanActivityContext>("foo", SimplePrincipal("modelOwner")) {
    val start by startNode

    val inputCustomerMasterData by activity(
        predecessor = start,
        accessRestrictions = RoleRestriction("clerk"),
        onActivityProvided = { da, inst ->
            da.updateNodeInstance(inst.handle) {
                val ctx = da.processContextFactory.newActivityInstanceContext(da, inst) as LoanActivityContext
                takeTask(da, ctx.processContext.clerk1.user)
            }
            false
        }
    ) {
        // TODO break this down into subactivities
        registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA)
        registerTaskPermission(customerFile, LoanPermissions.CREATE_CUSTOMER)
        registerTaskPermission(customerFile, LoanPermissions.UPDATE_CUSTOMER_DATA)
        acceptBrowserActivity(clerk1) {

            val customerFileAuthToken = uiServiceLogin(customerFile)

            val newData = customerData

            customerFile.enterCustomerData(customerFileAuthToken, newData)
            LoanCustomer(newData.customerId, newData.taxId)
        }
    }

    val createLoanRequest by activity(inputCustomerMasterData) { customer: LoanCustomer ->
        registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customer.customerId))
        acceptBrowserActivity(clerk1) {
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

        val getCustomerApproval by activity(
            startCreditEvaluate,
            customerIdInput,
        ) { customer: LoanCustomer ->
            registerTaskPermission(signingService, LoanPermissions.SIGN)
            acceptBrowserActivity(processContext.customer) {
                val signingToken = uiServiceLogin(signingService)
                signingService.signDocument(signingToken, Approval(true))
            }
        }

        val verifyCustomerApproval by activity<VerifyCustomerApprovalInput, SignedDocument<SignedDocument<Approval>>>(
            predecessor = getCustomerApproval,
            input = combine(customerIdInput named "customer", getCustomerApproval named "approval") { a, b ->
                VerifyCustomerApprovalInput(a, b)
            },
            action = { (customer, approval) ->
                registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customer.customerId))
                registerTaskPermission(signingService, LoanPermissions.SIGN)

                acceptBrowserActivity(postProcClerk) {
                    val customerData =
                        customerFile.getCustomerData(uiServiceLogin(customerFile), customer.customerId)

                    if (customerData?.name != approval.signedBy) {
                        throw IllegalArgumentException("Customer and signature mismatch: ${customerData?.name} != ${approval.signedBy}")
                    }

                    val signAuth = uiServiceLogin(signingService)
                    signingService.signDocument(signAuth, approval)
                }
            }
        )

        val getCreditReport by activity(
            verifyCustomerApproval,
            input = customerIdInput,
            outputSerializer = CreditReport.serializer(),
        ) { customer: LoanCustomer ->
            registerTaskPermission(customerFile, LoanPermissions.QUERY_CUSTOMER_DATA(customer.customerId))
            registerTaskPermission(creditBureau, LoanPermissions.GET_CREDIT_REPORT(customer.taxId))
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
                val creditAuthToken = getServiceToken(
                    creditBureau,
                    LoanPermissions.GET_CREDIT_REPORT.invoke(customerData.taxId)
                )


                assertForbidden { creditBureau.getCreditReport(custInfoAuthToken, customerData) }
                assertForbidden { creditBureau.getCreditReport(tknTID, customerData) }

                creditBureau.getCreditReport(creditAuthToken, customerData)
            }
        }

        val getLoanEvaluation by activity<Pair<LoanApplication, CreditReport>, LoanEvaluation>(
            predecessor = getCreditReport,
            input = combine(
                loanApplicationInput named "application",
                getCreditReport named "creditReport"
            ),
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
        )


        val end by endNode(getLoanEvaluation)

        loanEvaluationOut = output("loanEvaluation", getLoanEvaluation)

        creditReportOut = output("creditReport", getCreditReport)
    }

    val chooseBundledProduct by activity(evaluateCredit, loanEvaluationOut) { loanEvaluation ->
        LoanProductBundle("simpleLoan", "simpleLoan2019.a")
    }

    val approvedOfferOut: OutputRef<PricedLoanProductBundle>

    val offerPriceLoan by compositeActivity(chooseBundledProduct) {
        val cmbc: CompositeModelBuilderContext<LoanActivityContext> = this
        val loanEvalInput = input("loanEval", loanEvaluationOut)
        val chosenProductInput = input("chosenProduct", chooseBundledProduct)

        val start by startNode

        val priceBundledProduct by cmbc.activity<PricingInput, PricedLoanProductBundle>(
            start,
            combine(loanEvalInput named "loanEval", chosenProductInput named "prod") { e, p ->
                PricingInput(e, p)
            },
            action = { (loanEval, chosenProduct) ->
                registerTaskPermission(pricingEngine, LoanPermissions.PRICE_LOAN.restrictTo(Double.NaN))

                acceptBrowserActivity(postProcClerk) {
                    val pricingEngineLoginToken = uiServiceLogin(pricingEngine)
                    pricingEngine.priceLoan(pricingEngineLoginToken, chosenProduct, loanEval)
                }
            }
        )
        val approveOffer: DataNodeHandle<PricedLoanProductBundle> by cmbc.activity(
            predecessor = priceBundledProduct,
            input = priceBundledProduct
        ) { draftOffer ->
            acceptBrowserActivity(postProcClerk) {
                draftOffer.approve()
            }
        }
        val end by endNode(approveOffer)

        approvedOfferOut = output("approvedOffer", approveOffer)
    }
    val printOffer: DataNodeHandle<Offer> by activity(
        predecessor = offerPriceLoan,
        input = approvedOfferOut
    ) { approvedOffer ->
        registerTaskPermission(outputManagementSystem, LoanPermissions.PRINT_OFFER)
        acceptBrowserActivity(postProcClerk) {

            val printAuth = uiServiceLogin(outputManagementSystem)

            outputManagementSystem.registerAndPrintOffer(printAuth, approvedOffer)
        }
    }
    val customerSignsContract by activity(
        predecessor = printOffer,
        input = printOffer
    ) { offer ->
        acceptBrowserActivity(customer) {
            offer.signCustomer("Signed by 'John Doe'")
        }
    }

    val bankSignsContract: DataNodeHandle<Contract> by activity(
        predecessor = customerSignsContract,
        input = customerSignsContract
    ) { offer ->
        registerTaskPermission(outputManagementSystem,
            LoanPermissions.SIGN_LOAN.restrictTo(offer.customerId, Double.NaN)
        )

        acceptBrowserActivity(postProcClerk) {

            val omsToken = uiServiceLogin(outputManagementSystem)
            outputManagementSystem.signAndRegisterContract(omsToken, offer, "Signed by 'the bank manager'")
        }
    }
    val openAccount: DataNodeHandle<BankAccountNumber> by activity(
        predecessor = bankSignsContract,
        bankSignsContract
    ) { contract ->
        registerTaskPermission(accountManagementSystem, LoanPermissions.OPEN_ACCOUNT.invoke(contract.customerId))

        acceptBrowserActivity(postProcClerk) {
            val amsToken = uiServiceLogin(accountManagementSystem)

            accountManagementSystem.openAccountFor(amsToken, contract)
        }
    }
    val end by endNode(openAccount)

    processResult("loanEvaluation", loanEvaluationOut)
    processResult("creditReport", creditReportOut)
    processResult("accountNumber", openAccount)

}

private inline val LoanActivityContext.accountManagementSystem get() = processContext.accountManagementSystem
private inline val LoanActivityContext.authService get() = processContext.authService
private inline val LoanActivityContext.clerk1 get() = processContext.clerk1
private inline val LoanActivityContext.creditApplication get() = processContext.creditApplication
private inline val LoanActivityContext.creditBureau get() = processContext.creditBureau
private inline val LoanActivityContext.customer get() = processContext.customer
private inline val LoanActivityContext.customerData get() = processContext.customerData
private inline val LoanActivityContext.customerFile get() = processContext.customerFile
private inline val LoanActivityContext.generalClientService get() = processContext.generalClientService
private inline val LoanActivityContext.outputManagementSystem get() = processContext.outputManagementSystem
private inline val LoanActivityContext.postProcClerk get() = processContext.postProcClerk
private inline val LoanActivityContext.pricingEngine get() = processContext.pricingEngine
private inline val LoanActivityContext.signingService get() = processContext.signingService
