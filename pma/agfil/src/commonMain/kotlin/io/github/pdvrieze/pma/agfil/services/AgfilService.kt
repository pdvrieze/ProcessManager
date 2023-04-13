package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class AgfilService(override val serviceName: ServiceName<AgfilService>, authService: AuthService) :
    AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService {

    override val serviceInstanceId: ServiceId<GarageService> = ServiceId(getServiceId(serviceAuth))

    private val claims = mutableListOf<ClaimData>()

    /** From Lai's thesis */
    fun notifyClaim(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo, garage: GarageInfo): Unit = TODO()

    /** From Lai's thesis */
    fun returnClaimForm(completedClaimForm: CompletedClaimForm): Unit = TODO()

    /** From Lai's thesis */
    fun forwardInvoice(): Unit = TODO()

    fun recordClaimInDatabase(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): ClaimId {
        val claim = ClaimData(claims.size.toLong(), accidentInfo, ClaimData.Outcome.Undecided)
        claims.add(claim)
        return ClaimId(claim.id)
    }

    fun getClaim(authToken: PmaAuthToken, claimId: ClaimId): AccidentInfo {
        return claims[claimId.id.toInt()].accidentInfo
    }

    fun recordAssignedGarage(authToken: PmaAuthInfo, claimId: ClaimId, garage: GarageInfo) {
        TODO("not implemented")
    }

    fun getPolicy(authToken: PmaAuthToken, customerId: CustomerId, carRegistration: CarRegistration): InsurancePolicy? {
        TODO("not implemented")
    }

    fun findCustomerId(authToken: PmaAuthInfo, callerInfo: CallerInfo): CustomerId {
        TODO("not implemented")
    }

    val internal: Internal = Inner()

    interface Internal {
        fun sendClaimFormToCustomer(authToken: PmaAuthToken, claimId: ClaimId)
        fun terminateClaim(authToken: PmaAuthToken, claimId: ClaimId)
        fun notifyInvalidClaim(authToken: PmaAuthToken, claimId: ClaimId)
        fun processCompletedClaimForm(authToken: PmaAuthToken, claimForm: CompletedClaimForm)
        fun payGarageInvoice(authToken: PmaAuthToken, invoice: Invoice)
    }

    private inner class Inner constructor() : Internal {
        override fun sendClaimFormToCustomer(authToken: PmaAuthToken, claimId: ClaimId) {
            val claim = claims[claimId.id.toInt()]
        }

        override fun terminateClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            claims[claimId.id.toInt()].outcome= ClaimData.Outcome.Terminated
        }

        override fun notifyInvalidClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            TODO("not implemented")
        }

        override fun processCompletedClaimForm(authToken: PmaAuthToken, claimForm: CompletedClaimForm) {
            TODO("not implemented")
        }

        override fun payGarageInvoice(authToken: PmaAuthToken, invoice: Invoice) {
            TODO("not implemented")
        }
    }

    private class ClaimData(val id: Long, val accidentInfo: AccidentInfo, var outcome: Outcome) {
        enum class Outcome {
            Terminated,
            Completed,
            Undecided,
            Pending
        }
    }
}
