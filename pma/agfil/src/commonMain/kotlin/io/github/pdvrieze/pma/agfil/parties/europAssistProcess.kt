package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.services.AgfilService
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import io.github.pdvrieze.pma.agfil.services.ServiceNames.europAssistService
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RoleRestriction
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.uiServiceLogin
import java.util.*

val europAssistProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("EuropAssistHandleCall", uuid = UUID.randomUUID()) {
    val registration = input<CarRegistration>("carRegistration")
    val claimInfo = input<String>("claimInfo")

    val callInfo = input<CallerInfo>("callerInfo")
    val start by startNode // TODO add inputs


    val registerClaim: DataNodeHandle<AccidentInfo> by taskActivity(
        predecessor = start,
        input = combine(registration named "registration", claimInfo named "claimInfo", callInfo named "callInfo"),
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) {(registration, claimInfo, callInfo) ->
            val customerId = uiServiceLogin<AgfilService>(agfilService) {
                service.findCustomerId(authToken, callInfo)
            }
            AccidentInfo(customerId, registration, randomAccidentDetails())
        }
    }

    val validateInfo: DataNodeHandle<Boolean> by taskActivity(
        registerClaim,
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) { accidentInfo ->
            true // TODO allow for alternative results
        }
    }

/*
    val continueSplit by split(validateInfo) {
        min = 1
        max = 1
    }
*/



    val recordClaim: DataNodeHandle<ClaimId> by serviceActivity(
        predecessor = validateInfo,
        authorizationTemplates = listOf(),
        input = combine(validateInfo named "validated", registerClaim named "claim"),
        service = ServiceNames.agfilService
    ) {(validated, claim) ->
        if (!validated) throw IllegalArgumentException("Storing an invalid claim is invalid")

        service.recordClaimInDatabase(authToken, claim)
    }

    val pickGarage: DataNodeHandle<GarageInfo> by taskActivity(
        recordClaim,
        input = registerClaim
    ) {
        acceptTask( { randomEaCallHandler() }) { accidentInfo ->
            uiServiceLogin(europAssistService) {
                val garage: GarageInfo = service.internal.pickGarage(authToken, accidentInfo)
                garage
            }
        }
    }

    val assignGarage: DataNodeHandle<GarageInfo> by serviceActivity(
        predecessor = pickGarage,
        service = europAssistService,
        input = combine(pickGarage named "garage", recordClaim named "claimId", registerClaim named "accidentInfo")) { (garage, claimId, accidentInfo) ->
        service.internal.informGarage(authToken, garage, claimId, accidentInfo)
    }

    val notifyAgfil: DataNodeHandle<Unit> by serviceActivity(
        assignGarage,
        service = agfilService,
        input = combine(assignGarage named "assignedGarage", recordClaim named "claimId", registerClaim named "accidentInfo")
    ) { (garage, claimId, accidentInfo) ->
        service.notifyClaim(authToken, claimId, accidentInfo, garage)
    }

    val end by endNode(notifyAgfil)
}
