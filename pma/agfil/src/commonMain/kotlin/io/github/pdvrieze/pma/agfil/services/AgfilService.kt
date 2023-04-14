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
    fun notifyClaim(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo, garage: GarageInfo): Unit =
        TODO()

    /** From Lai's thesis */
    fun returnClaimForm(completedClaimForm: CompletedClaimForm): Unit = TODO()

    /** From Lai's thesis */
    fun forwardInvoice(authToken: PmaAuthToken, invoice: Invoice): Unit = TODO()

    fun recordClaimInDatabase(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): ClaimId {
        val claim = ClaimData(claims.size.toLong(), accidentInfo, Claim.Outcome.Undecided)
        claims.add(claim)
        return ClaimId(claim.id)
    }

    fun getAccidentInfo(authToken: PmaAuthToken, claimId: ClaimId): AccidentInfo {
        return claims[claimId.id.toInt()].accidentInfo
    }

    fun getFullClaim(authToken: PmaAuthToken, claimId: ClaimId): Claim {
        return claims[claimId.id.toInt()]
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

    val internal: Internal = Internal()

    inner class Internal internal constructor(){
        fun sendClaimFormToCustomer(authToken: PmaAuthToken, claimId: ClaimId) {
            val claim = claims[claimId.id.toInt()]
        }

        fun terminateClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            claims[claimId.id.toInt()].outcome = Claim.Outcome.Terminated
        }

        fun notifyInvalidClaim(authToken: PmaAuthToken, claimId: ClaimId) {
            TODO("not implemented")
        }

        fun processCompletedClaimForm(authToken: PmaAuthToken, claimForm: CompletedClaimForm) {
            TODO("not implemented")
        }

        fun payGarageInvoice(authToken: PmaAuthToken, invoice: Invoice) {
            TODO("not implemented")
        }
    }

    private class ClaimData(
        override val id: Long,
        override val accidentInfo: AccidentInfo,
        override var outcome: Claim.Outcome,
        override var assignedGarageInfo: GarageInfo? = null
    ) : Claim {
    }
}
