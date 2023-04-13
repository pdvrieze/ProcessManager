package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class AgfilService(override val serviceName: ServiceName<AgfilService>, authService: AuthService) :
    AbstractRunnableUiService(authService, serviceName.serviceName), RunnableAutomatedService {

    override val serviceInstanceId: ServiceId<GarageService> = ServiceId(getServiceId(serviceAuth))

    /** From Lai's thesis */
    fun notifyClaim(authToken: PmaAuthToken, accidentInfo: AccidentInfo, garage: GarageInfo): Unit = TODO()

    /** From Lai's thesis */
    fun returnClaimForm(): Unit = TODO()

    /** From Lai's thesis */
    fun forwardInvoice(): Unit = TODO()
}
