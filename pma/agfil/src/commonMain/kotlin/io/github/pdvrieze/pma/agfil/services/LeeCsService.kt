package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.leeCsProcess
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import nl.adaptivity.process.util.Identifier
import java.util.logging.Logger
import kotlin.random.Random

class LeeCsService(
    serviceName: ServiceName<LeeCsService>,
    authService: AuthService,
    adminAuthInfo: PmaAuthInfo,
    engineService: EngineService,
    override val serviceResolver: PmaServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<LeeCsService>(
    serviceName = serviceName,
    authService = authService,
    adminAuthInfo = adminAuthInfo,
    processEngineService = engineService,
    random = random,
    logger = logger,
    leeCsProcess
), RunnableAutomatedService, AutoService {

    private val processes: MutableMap<ClaimId, ClaimData> = mutableMapOf()

    /** From Lai's thesis: sendRepairCosts */
    fun sendGarageEstimate(authToken: PmaAuthInfo, estimate: Estimate) {
        validateAuthInfo(authToken, LEECS.SEND_GARAGE_ESTIMATE(estimate.claimId))
        val data = requireNotNull(processes[estimate.claimId])
        processEngineService.deliverEvent(data.piHandle, Identifier("receiveEstimate"), payload(estimate))

        // TODO validate auth
        data.estimate = estimate
    }

    /** From Lai's thesis */
    fun inspectCar(): Unit = TODO()

    /** From Lai's thesis: sendNewRepairCosts */
    fun sendAssessedCosts(authToken: PmaAuthToken, agreedCosts: AgreedCosts): Unit = TODO()

    /** From Lai's thesis */
    fun sendInvoice(invoice: Invoice): Unit = TODO()

    /**
     * forwardClaim from Lai's thesis
     */
    fun startClaimProcessing(authToken: PmaAuthInfo, claimId: ClaimId) {
        validateAuthInfo(authToken, LEECS.START_PROCESSING(claimId))
        val piHandle = startProcess(processHandles[0], payload(claimId))
        processes[claimId] = ClaimData(piHandle, claimId)
    }

    val internal: Internal = Internal()

    inner class Internal internal constructor(){

        /** Starts the garage service */
        fun contactGarage(authToken: PmaAuthToken, claim: Claim) {
            validateAuthInfo(authToken, LEECS.INTERNAL.CONTACT_GARAGE(claim.id))
            withGarage(authToken, claim.assignedGarageInfo) {
                service.informGarageOfIncomingCar(serviceAccessToken, claim.id, claim.accidentInfo)
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

        fun processHandleFor(claimId: ClaimId): PIHandle? {
            return processes[claimId]?.piHandle
        }

    }

    class ClaimData(
        val piHandle: PIHandle,
        val claimId: ClaimId,
        var estimate: Estimate? = null
    )
}

