package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.*
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.GarageService
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.dynamic.uiServiceLogin
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.util.multiplatform.PrincipalCompat

fun repairProcess(owner: PrincipalCompat, ownerService: ServiceId<GarageService>) =
    runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("insuranceCarRepair (${owner.name})", owner) {

    val claimId = input<ClaimId>("claim")
    val accidentInfo = input<AccidentInfo>("accidentInfo")

    val start by startNode

    val onReceiveCar by eventNode(start, CarRegistration.serializer())

    val handleReceiveCar: DataNodeHandle<Unit> by taskActivity(onReceiveCar, input = combine(onReceiveCar named "registration", claimId named "claimId")) {
        acceptTask({  randomGarageReceptionist(ownerService) }) { (carRegistration, claimId) ->
            uiServiceLogin(ownerService) {
                service.internal.registerCarReceipt(authToken, claimId, carRegistration)
            }
        }
    }

    val estimateRepairCost: DataNodeHandle<Money> by taskActivity(handleReceiveCar, input = claimId) {
        acceptTask({ randomMechanic(ownerService) }) { claimId ->
            val costs = randomRepairCosts()
            uiServiceLogin(ownerService) {
                service.internal.recordEstimatedRepairCost(authToken, claimId, costs)
            }
            costs
        }
    }

    val sendEstimate: DataNodeHandle<Unit> by serviceActivity(
        estimateRepairCost,
        listOf(
            ContextScopeTemplate { LEECS.SEND_GARAGE_ESTIMATE(nodeData(claimId))}
        ),
        ServiceNames.leeCsService,
        input = combine(estimateRepairCost named "estimate", claimId named "claimId", accidentInfo named "accidentInfo")
    ) { (estimate, claimId, accidentInfo) ->
        service.sendGarageEstimate(authToken, Estimate(claimId, accidentInfo.carRegistration, estimate))
    }

    val onRepairAgreed: DataNodeHandle<RepairAgreement> by eventNode(sendEstimate, RepairAgreement.serializer())

    val repairCar: DataNodeHandle<Unit> by serviceActivity(onRepairAgreed, listOf(), ownerService) { repairAgreement ->
        service.internal.repairCar(authToken, repairAgreement)
    }

    val sendInvoice: DataNodeHandle<InvoiceId> by serviceActivity(repairCar, listOf(), ownerService, input = onRepairAgreed) { agreement->
        service.sendInvoice(agreement)
    }

    val onReceivePayment by eventNode(sendInvoice, Payment.serializer())

    val confirmPayment by serviceActivity(onReceivePayment, listOf(), ownerService) { payment ->
        service.internal.handlePayment(authToken, payment)
    }

    val closeRecord by serviceActivity(confirmPayment, listOf(), ownerService, claimId) { claimId ->
        service.internal.closeRecord(authToken, claimId)
    }

    val end by endNode(closeRecord)

}
