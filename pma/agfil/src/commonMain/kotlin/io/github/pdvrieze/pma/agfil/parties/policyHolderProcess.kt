package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.CompletedClaimForm
import io.github.pdvrieze.pma.agfil.data.IncompleteClaimForm
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.util.multiplatform.PrincipalCompat

fun policyHolderProcess(owner: PrincipalCompat) = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("get_car_fixed", owner) {

    val start by startNode

    val reportClaim by serviceActivity(start, listOf(), ServiceNames.europAssistService) {
        service.phoneClaim(, agfilProcessContext.carRegistration, "Random Accident info")
    }

    val sendCar by serviceActivity(reportClaim, listOf(), ServiceNames.garageServices.first()) {
        service.sendCar(, agfilProcessContext.carRegistration)
    }

    val receiveClaimForm by eventNode(sendCar, IncompleteClaimForm.serializer())

    val completedClaimForm: DataNodeHandle<CompletedClaimForm> by taskActivity(receiveClaimForm) {
        acceptTask({ owner }) {
            it.fill()
        }
    }

    val returnClaimForm by serviceActivity(completedClaimForm, listOf(), ServiceNames.agfilService) { claimForm ->
        service.returnClaimForm(claimForm)
    }

    // TODO be notified of car being fixed

    val pickUpCar by taskActivity(returnClaimForm) {
        acceptTask({owner}) {
            // TODO Do something, maybe talk with garage
        }
    }

    val end by endNode(pickUpCar)
}
