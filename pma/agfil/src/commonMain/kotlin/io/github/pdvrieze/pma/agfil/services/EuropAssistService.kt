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
import kotlin.random.Random

class EuropAssistService(
    override val serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    private val random: Random,
    private val agfilService: AgfilService,
    override val garageServices: List<GarageService>
) : AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService, GarageAccessService {

    override val serviceInstanceId: ServiceId<EuropAssistService> = ServiceId(getServiceId(serviceAuth))

    val garages = listOf<GarageInfo>(
        GarageInfo("Fix'R'Us"),
        GarageInfo("")
    )
    override val internal: Internal = Internal()


    /** From Lai's thesis */
    fun phoneClaim(authToken: PmaAuthInfo, carRegistration: CarRegistration, claimInfo: String) : ClaimId {
        TODO()
    }

    /** From Lai's thesis */
    fun receiveInfo(authToken: PmaAuthInfo, info: String): ClaimId {
        TODO()
    }

    inner class Internal : GarageAccessService.Internal {
        override val outer: EuropAssistService get() = this@EuropAssistService

        fun pickGarage(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): GarageInfo {
            validateAuthInfo(authToken, AgfilPermissions.PICK_GARAGE)
            return GarageInfo(garageServices.random(random).serviceInstanceId.serviceId)
        }

        fun informGarage(authToken: PmaAuthToken, garage: GarageInfo, claimId: ClaimId, accidentInfo: AccidentInfo): GarageInfo {
            withGarage(authToken, garage) {
                service.informGarageOfIncomingCar(this.authToken, claimId, accidentInfo)
            }

            val agfilToken = authService.exchangeDelegateToken(serviceAuth, authToken, agfilService.serviceInstanceId, CommonPMAPermissions.IDENTIFY)
            agfilService.recordAssignedGarage(agfilToken, claimId, garage)

            return garage // Smarter way to do something here
        }

    }
}
