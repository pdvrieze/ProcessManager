package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.pma.agfil.services.ServiceNames.agfilService
import io.github.pdvrieze.pma.agfil.services.ServiceNames.europAssistService
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RoleRestriction
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.dynamic.uiServiceLogin
import java.util.*

val europAssistProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("EuropAssistHandleCall", uuid = UUID.randomUUID()) {
    val registration = input<CarRegistration>("carRegistration")
    val claimInfo = input<String>("claimInfo")
    val claimIdInput = input<ClaimId>("claimId")

    val callInfo = input<CallerInfo>("callerInfo")
    val start by startNode // TODO add inputs


    val registerClaim: DataNodeHandle<AccidentInfo> by taskActivity(
        predecessor = start,
        permissions = listOf(delegatePermissions(agfilService, FIND_CUSTOMER_ID)),
        input = combine(registration named "registration", claimInfo named "claimInfo", callInfo named "callInfo"),
        accessRestrictions = RoleRestriction("ea:callhandler")
    ) {
        acceptTask({ randomEaCallHandler() }) {(registration, claimInfo, callInfo) ->
            val customerId = uiServiceLogin(agfilService) {
                service.findCustomerId(authToken, callInfo)
            }
            AccidentInfo(customerId, callInfo.serviceId, registration, randomAccidentDetails())
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
        authorizationTemplates = listOf(AGFIL.CLAIM.CREATE),
        input = combine(validateInfo named "validated", registerClaim named "claim", claimIdInput named "claimId"),
        service = ServiceNames.agfilService
    ) {(validated, claim, claimId) ->
        if (!validated) throw IllegalArgumentException("Storing an invalid claim is invalid")

        service.recordClaimInDatabase(authToken, claim, claimId)
    }

    val pickGarage: DataNodeHandle<GarageInfo> by taskActivity(
        predecessor = recordClaim,
        permissions = listOf(
            delegatePermissions(europAssistService,
                EUROP_ASSIST.PICK_GARAGE,
                delegatePermissions(agfilService, AGFIL.LIST_GARAGES))),
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
        authorizationTemplates = listOf(
            ContextScopeTemplate {
                EUROP_ASSIST.INTERNAL.ASSIGN_GARAGE(nodeData(recordClaim))
            },
            delegatePermissions(agfilService,
                ContextScopeTemplate { CLAIM.RECORD_ASSIGNED_GARAGE(nodeData(recordClaim)) },
                ContextScopeTemplate { AGFIL.GET_CUSTOMER_INFO(nodeData(registerClaim).customerId)}
            )
        ),
        service = europAssistService,
        input = combine(pickGarage named "garage", recordClaim named "claimId", registerClaim named "accidentInfo"),
        action = { (garage, claimId, accidentInfo) ->
            service.internal.assignGarage(authToken, garage, claimId, accidentInfo)
        }
    )

    val notifyAgfilClaimAssigned: DataNodeHandle<Unit> by serviceActivity(
        assignGarage,
        authorizationTemplates = listOf(ContextScopeTemplate{ CLAIM.NOTIFY_ASSIGNED(nodeData(recordClaim)) }),
        service = agfilService,
        input = combine(assignGarage named "assignedGarage", recordClaim named "claimId", registerClaim named "accidentInfo")
    ) { (garage, claimId, accidentInfo) ->
        service.notifyClaimAssigned(authToken, claimId, accidentInfo, garage)
    }

    val end by endNode(notifyAgfilClaimAssigned)
}
