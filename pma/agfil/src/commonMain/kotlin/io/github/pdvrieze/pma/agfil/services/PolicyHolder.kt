package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.parties.policyHolderProcess
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class PolicyHolder(
    serviceAuth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<PolicyHolder>,
    authService: AuthService,
    processEngine: ProcessEngine<StubProcessTransaction>,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<PolicyHolder>(
    serviceAuth = serviceAuth,
    serviceName = serviceName,
    authService = authService,
    processEngine = processEngine,
    random = random,
    logger = logger,
    policyHolderProcess(serviceAuth.principal, ServiceId<PolicyHolder>(serviceAuth.id))
), AutoService, AutomatedService {

    constructor(
        serviceName: ServiceName<PolicyHolder>,
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


    val internal: Internal = Internal()

    /** From Lai's thesis */
    fun assignGarage(): Unit = TODO()

    /** From Lai's thesis */
    fun estimateRepairCost(): Unit = TODO()

    /** From Lai's thesis */
    fun sendClaimForm(): Unit = TODO()

    /** From Lai's thesis */
    fun repairCar(): Unit = TODO()

    inner class Internal {

        fun pickupCar(authToken: PmaAuthInfo, claimId: ClaimId) {
            TODO("not implemented")
        }

        fun sendCar(authToken: PmaAuthToken, carRegistration: CarRegistration, claimId: ClaimId, garage: GarageInfo) {
            withService(garage.service, authToken) {
                service.sendCar(authToken, carRegistration, claimId)
            }
        }

    }
}
