package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import kotlin.random.Random

class EuropAssistService(
    override val serviceName: ServiceName<EuropAssistService>,
    authService: AuthService,
    private val random: Random,
    private val garageServices: List<GarageService>
) : AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService {

    override val serviceInstanceId: ServiceId<EuropAssistService> = ServiceId(getServiceId(serviceAuth))

    val garages = listOf<GarageInfo>(
        GarageInfo("Fix'R'Us"),
        GarageInfo("")
    )

    fun pickGarage(authToken: PmaAuthInfo, accidentInfo: AccidentInfo): GarageInfo {
        validateAuthInfo(authToken, AgfilPermissions.PICK_GARAGE)
        return GarageInfo(garageServices.random(random).serviceInstanceId.serviceId)
    }

    fun informGarage(garage: GarageInfo, accidentInfo: AccidentInfo): GarageInfo {
        garageServices.first { it.serviceInstanceId.serviceId == garage.name }
            .also { garage ->
                garage.informGarageOfIncomingCar(accidentInfo)
            }
        return garage // Smarter way to do something here
    }

    /** From Lai's thesis */
    fun phoneClaim(claimInfo: String) : ClaimId {
        TODO()
    }

    /** From Lai's thesis */
    fun receiveInfo(info: String): ClaimId {
        TODO()
    }
}
