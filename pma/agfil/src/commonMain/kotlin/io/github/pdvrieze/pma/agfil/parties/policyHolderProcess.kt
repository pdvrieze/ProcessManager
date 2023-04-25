package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.GarageService
import io.github.pdvrieze.pma.agfil.services.PolicyHolderService
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.util.multiplatform.PrincipalCompat

fun policyHolderProcess(owner: PrincipalCompat, ownerService: ServiceId<PolicyHolderService>) = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("policyHolder_get_car_fixed", owner) {

    val start by startNode

    val reportClaim: DataNodeHandle<ClaimId> by serviceActivity(
        start,
        listOf(
            POLICYHOLDER.INTERNAL.REPORT_CLAIM,
            delegatePermissions(ServiceNames.europAssistService, CommonPMAPermissions.IDENTIFY),
        ),
        ownerService
    ) {
        service.internal.reportClaim(authToken, processContext.processInstanceHandle, activityContext.callerInfo(owner, ownerService), agfilProcessContext.carRegistration)
    }


    val onGarageAssigned: DataNodeHandle<GarageInfo> by eventNode(reportClaim, GarageInfo.serializer())

    val sendCar by serviceActivity(
        predecessor = onGarageAssigned,
        authorizationTemplates = listOf(
            ContextScopeTemplate {
                UnionPermissionScope(
                    POLICYHOLDER.INTERNAL.SEND_CAR(processContext.carRegistration),
                    CommonPMAPermissions.DELEGATED_PERMISSION.context(nodeData(onGarageAssigned).serviceId, GARAGE.SEND_CAR(processContext.carRegistration))
                )
            },
        ),
        service = ownerService,
        input = combine(reportClaim named "claimId", onGarageAssigned named "garage")
    ) { (claimId, garage) ->
        service.internal.sendCar(authToken, agfilProcessContext.carRegistration, claimId, garage)
    }

    val receiveClaimForm by eventNode(predecessor = sendCar, messageSerializer = IncompleteClaimForm.serializer())

    val completedClaimForm: DataNodeHandle<CompletedClaimForm> by taskActivity(receiveClaimForm) {
        acceptTask({ owner }) {
            it.fill()
        }
    }

    val returnClaimForm by serviceActivity(
        completedClaimForm,
        listOf(ContextScopeTemplate { CLAIM.RETURN_FORM(nodeData(receiveClaimForm).claimId) }),
        ServiceNames.agfilService
    ) { claimForm ->
        service.evReturnClaimForm(authToken, claimForm)
    }

    // TODO be notified of car being fixed

    val pickUpCar by serviceActivity(returnClaimForm, listOf(), ownerService, input = reportClaim) { claimId ->
        service.internal.pickupCar(authToken, claimId)
    }

    val end by endNode(pickUpCar)
}
