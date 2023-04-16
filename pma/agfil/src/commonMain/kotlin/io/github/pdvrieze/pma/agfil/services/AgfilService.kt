package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.agfilProcess
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import kotlin.random.Random

class AgfilService(
    serviceName: ServiceName<AgfilService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    processEngine: ProcessEngine<StubProcessTransaction>,
    random: Random
) : RunnableProcessBackedService<AgfilService>(serviceName, authService, adminAuthInfo, processEngine, random, agfilProcess), RunnableAutomatedService, RunnableUiService {

    private val claims = mutableListOf<ClaimData>()

    /** From Lai's thesis */
    fun notifyClaim(authToken: PmaAuthInfo, claimId: ClaimId, accidentInfo: AccidentInfo, garage: GarageInfo): Unit =
        TODO()

    /** From Lai's thesis */
    fun returnClaimForm(completedClaimForm: CompletedClaimForm): Unit = TODO()

    /** From Lai's thesis */
    fun forwardInvoice(authToken: PmaAuthToken, invoice: Invoice): Unit = TODO()

    fun recordClaimInDatabase(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): ClaimId {
        val claimId = ClaimId(claims.size.toLong())
        claims.add(ClaimData(claimId, accidentInfo, Claim.Outcome.Undecided))
        return claimId
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

    fun getContractedGarages(authToken: PmaAuthInfo): List<GarageInfo> {
        validateAuthInfo(authToken, AgfilPermissions.LIST_GARAGES)
        TODO()
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
        override val id: ClaimId,
        override val accidentInfo: AccidentInfo,
        override var outcome: Claim.Outcome,
        override var assignedGarageInfo: GarageInfo? = null
    ) : Claim {
    }
}
