package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.parties.leeCsProcess
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class LeeCsService(
    serviceAuth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<LeeCsService>,
    authService: AuthService,
    processEngine: ProcessEngine<StubProcessTransaction>,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<LeeCsService>(
    serviceAuth = serviceAuth,
    serviceName = serviceName,
    authService = authService,
    processEngine = processEngine,
    random = random,
    logger = logger,
    leeCsProcess
), RunnableAutomatedService, AutoService {

    constructor(
        serviceName: ServiceName<LeeCsService>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngine: ProcessEngine<StubProcessTransaction>,
        serviceResolver: ServiceResolver,
        random: Random,
        logger: Logger = authService.logger
    ) : this(
        authService.registerClient(adminAuthInfo, serviceName, random.nextString()),
        serviceName,
        authService,
        processEngine,
        serviceResolver,
        random,
        logger
    )

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

    val internal: Internal = Internal()

    inner class Internal internal constructor(){

        fun contactGarage(authToken: PmaAuthToken, claim: Claim) {
            withGarage(authToken, claim.assignedGarageInfo) {
                this.service.informGarageOfIncomingCar(serviceAccessToken, claim.id, claim.accidentInfo)
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

class ServiceInvocationContext<S: Service>(val service: S, val serviceAccessToken: PmaAuthToken)
