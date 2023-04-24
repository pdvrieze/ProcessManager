package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.data.Claim.Outcome
import io.github.pdvrieze.pma.agfil.data.InsurancePolicy.Coverage
import io.github.pdvrieze.pma.agfil.parties.agfilProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class AgfilService(
    serviceName: ServiceName<AgfilService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger
) : RunnableProcessBackedService<AgfilService>(serviceName, authService, adminAuthInfo, engineService, random, logger, agfilProcess), RunnableAutomatedService, RunnableUiService, AutoService {

    private val claims = mutableListOf<ClaimData>()
    private operator fun List<ClaimData>.get(claimId: ClaimId): ClaimData {
        return claims[claimId.id.toInt()]
    }


    private val customers = mutableMapOf<CustomerId, CustomerData>()

    /** From Lai's thesis */
    fun notifyClaimAssigned(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo, garage: GarageInfo) {
        validateAuthInfo(authToken, CLAIM.NOTIFY_ASSIGNED(claimId))
        val payload = payload("claimId", claimId/*, "accidentInfo", accidentInfo, "garage", garage*/)
        startProcess(processHandles[0], payload)
    }

    /** From Lai's thesis */
    fun evReturnClaimForm(authToken: PmaAuthInfo, completedClaimForm: CompletedClaimForm) {
        val claimId = completedClaimForm.claimId
        validateAuthInfo(authToken, CLAIM.RETURN_FORM(claimId))
        claims[claimId].claimForm = completedClaimForm
    }

    /** From Lai's thesis */
    fun forwardInvoice(authToken: PmaAuthToken, invoice: Invoice) {
        val claimId = invoice.claimId
        validateAuthInfo(authToken, CLAIM.REGISTER_INVOICE(claimId))
        val claim = claims[claimId]
        claim.invoice = invoice
    }

    fun recordClaimInDatabase(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): ClaimId {
        validateAuthInfo(authToken, AGFIL.CLAIM.CREATE)
        val claimId = ClaimId(claims.size.toLong())
        claims.add(ClaimData(claimId, accidentInfo, Outcome.Undecided))
        return claimId
    }

    fun getAccidentInfo(authToken: PmaAuthToken, claimId: ClaimId): AccidentInfo {
        validateAuthInfo(authToken, CLAIM.READ_ACCIDENTINFO(claimId))
        return claims[claimId].accidentInfo
    }

    fun getFullClaim(authToken: PmaAuthToken, claimId: ClaimId): Claim {
        validateAuthInfo(authToken, CLAIM.READ(claimId))
        return claims[claimId.id.toInt()].toClaim()
    }

    fun recordAssignedGarage(authToken: PmaAuthInfo, claimId: ClaimId, garage: GarageInfo) {
        validateAuthInfo(authToken, CLAIM.RECORD_ASSIGNED_GARAGE(claimId))
        val claim = claims[claimId]
        claim.assignedGarageInfo =garage
    }

    fun getPolicy(authToken: PmaAuthToken, customerId: CustomerId, carRegistration: CarRegistration): InsurancePolicy? {
        validateAuthInfo(authToken, GET_POLICY(customerId))
        val customerData = customers[customerId]
        val policy = customerData?.policy
        if (policy!=null && policy.carCoverage.none { it.car==carRegistration }) {
            val newPolicy = policy.copy(carCoverage = (policy.carCoverage + InsurancePolicy.CoverageInfo(carRegistration, Coverage.values().random(random))))
            val newCustomer = customerData.copy(policy = newPolicy)
            customers[customerId] = newCustomer
            return newPolicy
        } else {
            return policy
        }
    }

    fun findCustomerId(authToken: PmaAuthInfo, callerInfo: CallerInfo): CustomerId {
        validateAuthInfo(authToken, FIND_CUSTOMER_ID)
        val existingRecord = customers.values.firstOrNull { it.name==callerInfo.name }
        if (existingRecord == null) {
            val newId = CustomerId(random.nextLong())
            val nextPolicyNumber = (customers.values.maxOfOrNull { it.policy.policyNumber } ?: 0) + 1
            val policy = InsurancePolicy(nextPolicyNumber, newId, emptyList())
            customers[newId] = CustomerData(newId, callerInfo.serviceId, callerInfo.name, callerInfo.phoneNumber, policy)
            return newId
        }

        if (existingRecord.phoneNumber != callerInfo.phoneNumber) {
            customers[existingRecord.id] = existingRecord.copy(phoneNumber = callerInfo.phoneNumber)
        }

        return existingRecord.id
    }

    fun getContractedGarages(authToken: PmaAuthInfo): List<GarageInfo> {
        validateAuthInfo(authToken, LIST_GARAGES)
        return ServiceNames.garageServices.map { GarageInfo(it.serviceName, it.serviceName) }
    }

    fun getCustomerInfo(authToken: PmaAuthInfo, customerId: CustomerId): CustomerData {
        validateAuthInfo(authToken, AGFIL.GET_CUSTOMER_INFO(customerId))
        return requireNotNull(customers[customerId])
    }

    val internal: Internal = Internal()

    inner class Internal internal constructor(){
        fun sendClaimFormToCustomer(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.SEND_CLAIM_FORM(claimId))
            val claim = claims[claimId]
            val customer = requireNotNull(customers[claim.accidentInfo.customerId])
            val form = IncompleteClaimForm(claimId, claim.accidentInfo.carRegistration, customer.name)
            val customerService = serviceResolver.resolveService(customer.service)
            withService(customerService, authToken, POLICYHOLDER.SEND_CLAIM_FORM(claimId)) {
                service.evReceiveClaimForm(serviceAccessToken, form)
            }
            TODO("Get the customer's service, send form to that service")
        }

        fun terminateClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.TERMINATE_CLAIM(claimId))
            claims[claimId].outcome = Outcome.Terminated
        }

        fun notifyInvalidClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.NOTIFIY_INVALID_CLAIM(claimId))
            val claim = claims[claimId]
            claim.outcome = Outcome.Invalid
        }

        fun processCompletedClaimForm(authToken: PmaAuthToken, claimForm: CompletedClaimForm) {
            val claimId = claimForm.claimId
            validateAuthInfo(authToken, AGFIL.INTERNAL.PROCESS_CLAIM_FORM(claimId))
            claims[claimId].claimForm = claimForm
        }

        fun payGarageInvoice(authToken: PmaAuthToken, invoice: Invoice) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.PAY_GARAGE_INVOICE(invoice.claimId))
            val claim = claims[invoice.claimId]
            require(claim.invoice != invoice) { "Attempt to pay unregistered invoice" }

            // TODO actually pay?
            withService(invoice.garage.service, authToken, GARAGE.NOTIFY_INVOICE_PAID(invoice.invoiceId)) {
                service.evNotifyInvoicePaid(serviceAccessToken, invoice.invoiceId, invoice.amount)
            }
        }
    }

    data class CustomerData(
        val id: CustomerId,
        val service: ServiceId<PolicyHolderService>,
        val name: String,
        val phoneNumber: String,
        val policy: InsurancePolicy,
    )

    private data class ClaimData(
        val id: ClaimId,
        val accidentInfo: AccidentInfo,
        var outcome: Outcome,
        var assignedGarageInfo: GarageInfo? = null,
        var invoice: Invoice? = null,
        var claimForm: CompletedClaimForm? = null
    ) {
        fun toClaim() = Claim(id, accidentInfo, outcome, assignedGarageInfo)
    }

}
