package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.CompletedClaimForm
import io.github.pdvrieze.pma.agfil.data.GarageInfo
import io.github.pdvrieze.pma.agfil.data.IncompleteClaimForm
import io.github.pdvrieze.pma.agfil.services.PolicyHolder
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.util.multiplatform.PrincipalCompat

fun policyHolderProcess(owner: PrincipalCompat, ownerService: ServiceId<PolicyHolder>) = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("get_car_fixed", owner) {

    val start by startNode

    val reportClaim by serviceActivity(start, listOf(), ServiceNames.europAssistService) {
        service.phoneClaim(authToken, agfilProcessContext.carRegistration, "Random Accident info")
    }

    val onGarageAssigned: DataNodeHandle<GarageInfo> by eventNode(reportClaim, GarageInfo.serializer())

    val sendCar by serviceActivity(
        predecessor = onGarageAssigned,
        authorizationTemplates = listOf(),
        service = ownerService,
        input = combine(reportClaim named "claimId", onGarageAssigned named "garage")
    ) { (claimId, garage) ->
        service.internal.sendCar(authToken, agfilProcessContext.carRegistration, claimId, garage)
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

    val pickUpCar by serviceActivity(returnClaimForm, listOf(), ownerService, input = reportClaim) { claimId ->
        service.internal.pickupCar(authToken, claimId)
    }

    val end by endNode(pickUpCar)
}
