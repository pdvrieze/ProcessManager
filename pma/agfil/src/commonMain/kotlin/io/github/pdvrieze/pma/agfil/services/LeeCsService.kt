package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.ClaimId
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class LeeCsService(override val serviceName: ServiceName<LeeCsService>, authService: AuthService) :
    AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService {

    override val serviceInstanceId: ServiceId<GarageService> = ServiceId(getServiceId(serviceAuth))

    /** From Lai's thesis */
    fun forwardClaim(): Unit = TODO()

    /** From Lai's thesis */
    fun sendRepairCost(): Unit = TODO()

    /** From Lai's thesis */
    fun inspectCar(): Unit = TODO()

    /** From Lai's thesis. May not be needed */
    fun sendNewRepairCosts(): Unit = TODO()

    /** From Lai's thesis */
    fun sendInvoice(): Unit = TODO()
    fun startClaimProcessing(authToken: PmaAuthInfo, claimId: ClaimId) {
        TODO("not implemented")
    }
}
