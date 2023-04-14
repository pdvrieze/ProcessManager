package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.CarRegistration
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.SEND_CAR
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver

class PolicyHolder(
    override val serviceName: ServiceName<PolicyHolder>,
    authService: AuthService,
    override val serviceResolver: ServiceResolver,
) : AbstractRunnableAutomatedService(authService, serviceName.serviceName), AutoService {
    override val serviceInstanceId: ServiceId<AutomatedService>
        get() = ServiceId(serviceName.serviceName)

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
            val garageService = serviceResolver.resolve(garage.service)
            val garageToken = authService.exchangeDelegateToken(serviceAuth, authToken, garageService.serviceInstanceId, SEND_CAR.context(claimId))
            garageService.sendCar(garageToken, carRegistration, claimId)
        }

    }
}
