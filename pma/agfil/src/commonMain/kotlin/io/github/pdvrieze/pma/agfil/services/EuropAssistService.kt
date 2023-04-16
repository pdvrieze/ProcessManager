package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.parties.europAssistProcess
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import java.util.logging.Logger
import kotlin.random.Random

class EuropAssistService(
    serviceAuth: PmaIdSecretAuthInfo,
    serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    processEngine: ProcessEngine<StubProcessTransaction>,
    override val serviceResolver: ServiceResolver,
    random: Random,
    logger: Logger,
) : RunnableProcessBackedService<EuropAssistService>(
    serviceAuth = serviceAuth,
    serviceName = serviceName,
    authService = authService,
    processEngine = processEngine,
    random = random,
    logger = logger,
    europAssistProcess
), RunnableAutomatedService, AutoService {
    constructor(
        serviceName: ServiceName<EuropAssistService>,
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
    fun phoneClaim(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimInfo: String): ClaimId {
        TODO()
    }

    /** From Lai's thesis */
    fun receiveInfo(authToken: PmaAuthInfo, info: String): ClaimId {
        TODO()
    }

    inner class Internal {
        fun pickGarage(authToken: PmaAuthToken, accidentInfo: AccidentInfo): GarageInfo {
            validateAuthInfo(authToken, AgfilPermissions.PICK_GARAGE)
            val garageServices = withService(serviceName = agfilService, authToken, AgfilPermissions.LIST_GARAGES) {
                service.getContractedGarages(serviceAccessToken)
            }

            return garageServices.random(random)
        }

        fun informGarage(
            authToken: PmaAuthToken,
            garage: GarageInfo,
            claimId: ClaimId,
            accidentInfo: AccidentInfo
        ): GarageInfo {
            withService(
                garage.service,
                authToken,
                AgfilPermissions.INFORM_GARAGE
            ) {
                service.informGarageOfIncomingCar(serviceAccessToken, claimId, accidentInfo)
            }

            withService(agfilService, authToken, CommonPMAPermissions.IDENTIFY) {
                service.recordAssignedGarage(serviceAccessToken, claimId, garage)
            }
/*

            val agfilService = serviceResolver.resolveService(agfilService)

            val agfilToken = authServiceClient.exchangeDelegateToken(
                authToken,
                agfilService.serviceInstanceId,
                CommonPMAPermissions.IDENTIFY
            )
            agfilService.recordAssignedGarage(agfilToken, claimId, garage)
*/

            return garage // Smarter way to do something here
        }

    }
}
