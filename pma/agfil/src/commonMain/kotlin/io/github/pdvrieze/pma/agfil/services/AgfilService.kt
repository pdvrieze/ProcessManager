package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.data.Claim.Outcome
import io.github.pdvrieze.pma.agfil.data.InsurancePolicy.Coverage
import io.github.pdvrieze.pma.agfil.parties.agfilProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.random.nextULong

class AgfilService(
    serviceName: ServiceName<AgfilService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: PmaServiceResolver,
    random: Random,
    logger: Logger
) : RunnableProcessBackedService<AgfilService>(serviceName, authService, adminAuthInfo, engineService, random, logger, agfilProcess), RunnableAutomatedService, RunnableUiService, AutoService {

    private val claims = mutableMapOf<ClaimId, ClaimData>()
    private operator fun List<ClaimData>.get(claimId: ClaimId): ClaimData {
        return requireNotNull(claims[claimId]) { "Claim with id $claimId does not exist" }
    }


    private val customers = mutableMapOf<CustomerId, CustomerData>()

    /** From Lai's thesis */
    fun notifyClaimAssigned(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo, garage: GarageInfo) {
        validateAuthInfo(authToken, CLAIM.NOTIFY_ASSIGNED(claimId))
        val payload = payload<ClaimId>(claimId/*, "accidentInfo", accidentInfo, "garage", garage*/)
        val instanceHandle = startProcess(processHandles[0], payload)
        val oldClaim = requireNotNull(claims[claimId])
        claims[claimId] =  oldClaim.copy(instanceHandle = instanceHandle)
    }

    /** From Lai's thesis */
    fun evReturnClaimForm(authToken: PmaAuthInfo, completedClaimForm: CompletedClaimForm) {
        val claimId = completedClaimForm.claimId
        validateAuthInfo(authToken, CLAIM.RETURN_FORM(claimId))
        claims[claimId]?.run { claimForm = completedClaimForm }
    }

    /** From Lai's thesis */
    fun forwardInvoice(authToken: PmaAuthToken, invoice: Invoice) {
        val claimId = invoice.claimId
        validateAuthInfo(authToken, CLAIM.REGISTER_INVOICE(claimId))
        val claim = claims[claimId]
        claim?.let { it.invoice = invoice }
    }

    fun recordClaimInDatabase(authToken: PmaAuthInfo, accidentInfo: AccidentInfo, claimId: ClaimId): ClaimId {
        validateAuthInfo(authToken, AGFIL.CLAIM.CREATE)
        claims[claimId]=ClaimData(claimId, accidentInfo, Outcome.Undecided)
        return claimId
    }

    fun getAccidentInfo(authToken: PmaAuthToken, claimId: ClaimId): AccidentInfo {
        validateAuthInfo(authToken, CLAIM.READ_ACCIDENTINFO(claimId))
        return requireNotNull(claims[claimId]).accidentInfo
    }

    fun getFullClaim(authToken: PmaAuthToken, claimId: ClaimId): Claim {
        validateAuthInfo(authToken, CLAIM.READ(claimId))
        return requireNotNull(claims[claimId]).toClaim()
    }

    fun recordAssignedGarage(authToken: PmaAuthInfo, claimId: ClaimId, garage: GarageInfo) {
        validateAuthInfo(authToken, AGFIL.CLAIM.RECORD_ASSIGNED_GARAGE(claimId))
        val claim = claims[claimId]
        claim?.let { it.assignedGarageInfo =garage }
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
            val newId = CustomerId(random.nextULong())
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
        validateAuthInfo(authToken, AGFIL.LIST_GARAGES)
        return ServiceNames.garageServices.map {
            GarageInfo(it.serviceName, serviceResolver.resolveService(it).serviceInstanceId)
        }
    }

    fun getCustomerInfo(authToken: PmaAuthInfo, customerId: CustomerId): CustomerData {
        validateAuthInfo(authToken, AGFIL.GET_CUSTOMER_INFO(customerId))
        return requireNotNull(customers[customerId])
    }

    val internal: Internal = Internal()

    inner class Internal internal constructor(){
        fun sendClaimFormToCustomer(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.SEND_CLAIM_FORM(claimId))
            val claim = requireNotNull(claims[claimId])
            val customer = requireNotNull(customers[claim.accidentInfo.customerId])
            val form = IncompleteClaimForm(claimId, claim.accidentInfo.carRegistration, customer.name)
            val customerService = serviceResolver.resolveService(customer.service)
            withService(customerService, authToken, POLICYHOLDER.SEND_CLAIM_FORM(claimId)) {
                service.evReceiveClaimForm(serviceAccessToken, form)
            }
        }

        fun terminateClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.TERMINATE_CLAIM(claimId))
            claims[claimId]?.let { it.outcome = Outcome.Terminated }
        }

        fun notifyInvalidClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.NOTIFIY_INVALID_CLAIM(claimId))
            val claim = claims[claimId]
            claim?.let { it.outcome = Outcome.Invalid }
        }

        fun processCompletedClaimForm(authToken: PmaAuthToken, claimForm: CompletedClaimForm) {
            val claimId = claimForm.claimId
            validateAuthInfo(authToken, AGFIL.INTERNAL.PROCESS_CLAIM_FORM(claimId))
            claims[claimId]?.let { it.claimForm = claimForm }
        }

        fun payGarageInvoice(authToken: PmaAuthToken, invoice: Invoice) {
            validateAuthInfo(authToken, AGFIL.INTERNAL.PAY_GARAGE_INVOICE(invoice.claimId))
            val claim = claims[invoice.claimId]
            require(claim?.invoice == invoice) { "Attempt to pay unregistered invoice" }

            // TODO actually pay?
            withService(invoice.garage.serviceId, authToken, GARAGE.NOTIFY_INVOICE_PAID(invoice.invoiceId)) {
                service.evNotifyInvoicePaid(serviceAccessToken, invoice.invoiceId, invoice.amount)
            }
        }

        fun processHandleFor(claimId: ClaimId): PIHandle {
            return requireNotNull(claims[claimId]).instanceHandle
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
        var claimForm: CompletedClaimForm? = null,
        var instanceHandle: PIHandle = Handle.invalid(),
    ) {
        fun toClaim() = Claim(id, accidentInfo, outcome, assignedGarageInfo)
    }

}
