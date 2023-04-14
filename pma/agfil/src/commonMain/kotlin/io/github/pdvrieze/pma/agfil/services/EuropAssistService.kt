package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver
import kotlin.random.Random

class EuropAssistService(
    override val serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    private val random: Random,
    private val agfilService: AgfilService,
    override val serviceResolver: ServiceResolver,
) : AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService, AutoService {

    override val serviceInstanceId: ServiceId<EuropAssistService> = ServiceId(getServiceId(serviceAuth))

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
            val garageServices = withService(service = agfilService, authToken, AgfilPermissions.LIST_GARAGES) {
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

            val agfilToken = authService.exchangeDelegateToken(
                serviceAuth,
                authToken,
                agfilService.serviceInstanceId,
                CommonPMAPermissions.IDENTIFY
            )
            agfilService.recordAssignedGarage(agfilToken, claimId, garage)

            return garage // Smarter way to do something here
        }

    }
}
