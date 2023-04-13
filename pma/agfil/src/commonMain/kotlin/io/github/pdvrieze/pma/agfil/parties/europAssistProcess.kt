package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.AccidentInfo
import io.github.pdvrieze.pma.agfil.data.CallerInfo
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import io.github.pdvrieze.pma.agfil.services.ServiceNames.europAssistService
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RoleRestriction
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.uiServiceLogin
import java.util.*

val europAssistProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("EuropAssistHandleCall", uuid = UUID.randomUUID()) {
    val callInfo = input<CallerInfo>("callerInfo")
    val start by startNode


    val gatherInfo: DataNodeHandle<AccidentInfo> by taskActivity(
        predecessor = start,
        input = callInfo,
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) {callInfo ->
            AccidentInfo(callInfo, randomAccidentDetails())
        }
    }

    val validateInfo: DataNodeHandle<Boolean> by taskActivity(
        gatherInfo,
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) { accidentInfo ->
            true // TODO allow for alternative results
        }
    }

    val continueSplit by split(validateInfo) {
        min = 1
        max = 1
    }

    val pickGarage: DataNodeHandle<GarageInfo> by taskActivity(continueSplit, input = gatherInfo) {
        acceptTask( { randomEaCallHandler() }) { accidentInfo ->
            uiServiceLogin(europAssistService) {
                val garage: GarageInfo = service.pickGarage(authToken, accidentInfo)
                garage
            }
        }
    }

    val assignGarage: DataNodeHandle<GarageInfo> by serviceActivity(
        predecessor = pickGarage,
        service = europAssistService,
        input = combine(pickGarage named "garage", gatherInfo named "accidentInfo")) {(garage, accidentInfo) ->
        service.informGarage(garage, accidentInfo)
    }

    val informAgfil: DataNodeHandle<Unit> by serviceActivity(
        assignGarage,
        service = agfilService,
        input = combine(assignGarage named "assignedGarage", gatherInfo named "accidentInfo")
    ) { (garage, accidentInfo) ->
        service.notifyClaim(authToken, accidentInfo, garage)
    }
}
