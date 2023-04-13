package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.parties.repairProcess
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableAutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.serialization.XML
import java.util.*
import kotlin.random.Random

class GarageService(
    override val serviceName: ServiceName<GarageService>,
    authService: AuthService,
    processEngine: ProcessEngine<StubProcessTransaction>,
    random: Random,
) : RunnableProcessBackedService(
    serviceName.serviceName,
    authService,
    processEngine,
    random,
    repairProcess,
), RunnableAutomatedService {
    val xml = XML { this.recommended() }
    val hRepairProcess: Handle<ExecutableProcessModel> get() = processHandles[0]

    override val serviceInstanceId: ServiceId<GarageService> = ServiceId(getServiceId(serviceAuth))

    fun informGarageOfIncomingCar(accidentInfo: AccidentInfo) {
        val payload = CompactFragment { xml.encodeToWriter(it, AccidentInfo.serializer(), accidentInfo) }
         processEngine.inTransaction { tr ->
             startProcess(tr, serviceAuth.principal, hRepairProcess, "estimate repair", UUID.randomUUID(), payload)
         }
    }

    /** From Lai's thesis. Receive car. */
    fun sendCar(): Unit = TODO()

    /** From Lai's thesis */
    fun contactGarage(): Unit = TODO()

    /** From Lai's thesis */
    fun agreeRepair(): Unit = TODO()

    /** From Lai's thesis */
    fun payRepairCost(): Unit = TODO()
}
