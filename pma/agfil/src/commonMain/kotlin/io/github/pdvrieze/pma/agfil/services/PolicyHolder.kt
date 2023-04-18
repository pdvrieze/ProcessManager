package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.parties.policyHolderProcess
import nl.adaptivity.process.engine.pma.*
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
    engineService: EngineService,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<PolicyHolder>(
    serviceAuth = serviceAuth,
    serviceName = serviceName,
    authService = authService,
    processEngineService = engineService,
    random = random,
    logger = logger,
    policyHolderProcess(serviceAuth.principal, ServiceId<PolicyHolder>(serviceAuth.id))
), AutoService, AutomatedService {

    constructor(
        serviceName: ServiceName<PolicyHolder>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        engineService: EngineService,
        serviceResolver: ServiceResolver,
        random: Random,
        logger: Logger = authService.logger
    ) : this(
        authService.registerClient(adminAuthInfo, serviceName, random.nextString()),
        serviceName,
        authService,
        engineService,
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

    fun initiateClaimProcess() {

/*
        processEngine.inTransaction { tr ->
            startProcess(tr, this@PolicyHolder.authServiceClient.principal, processHandles[0], "claim", UUID.randomUUID(), null)
        }
*/
    }

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
