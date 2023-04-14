package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class LeeCsService(
    override val serviceName: ServiceName<LeeCsService>,
    authService: AuthService,
    override val garageServices: List<GarageService>
) : AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService , GarageAccessService {

    override val serviceInstanceId: ServiceId<GarageService> = ServiceId(getServiceId(serviceAuth))

    /** From Lai's thesis: sendRepairCosts */
    fun sendGarageEstimate(authToken: PmaAuthInfo, estimate: Estimate) {
        TODO()
    }

    /** From Lai's thesis */
    fun inspectCar(): Unit = TODO()

    /** From Lai's thesis: sendNewRepairCosts */
    fun sendAssessedCosts(authToken: PmaAuthToken, agreedCosts: AgreedCosts): Unit = TODO()

    /** From Lai's thesis */
    fun sendInvoice(): Unit = TODO()

    /**
     * forwardClaim from Lai's thesis
     */
    fun startClaimProcessing(authToken: PmaAuthInfo, claimId: ClaimId) {
        TODO("not implemented")
    }

    override val internal: Internal = Internal()

    inner class Internal internal constructor() : GarageAccessService.Internal {
        override val outer: GarageAccessService get() = this@LeeCsService

        fun contactGarage(authToken: PmaAuthToken, claim: Claim) {
            withGarage(authToken, claim.assignedGarageInfo) {
                this.service.informGarageOfIncomingCar(this.authToken, claim.id, claim.accidentInfo)
            }
        }

        fun assignAssessor(authToken: PmaAuthToken, accidentInfo: Claim, estimate: Estimate) {
            TODO("not implemented")
        }

        fun agreeClaim(authToken: PmaAuthToken, claim: Claim) {
            withGarage(authToken, claim.assignedGarageInfo) {
                service.agreeRepair(authToken, claim.id, claim.accidentInfo.carRegistration)
            }
        }

        fun verifyInvoice(authToken: PmaAuthInfo, invoice: Invoice) {
            TODO("not implemented")
        }

    }
}

class ServiceInvocationContext<S: AutomatedService>(val service: S, val authToken: PmaAuthToken)
