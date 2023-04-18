package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.CompletedClaimForm
import io.github.pdvrieze.pma.agfil.data.Invoice
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.NodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableXPathCondition

val agfilProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("agfilClaimHandling") {
    val claimIdInput = input<ClaimId>("claimId")

    val start by startNode

    val checkPolicyValidity: DataNodeHandle<Boolean> by serviceActivity(start, listOf(), ServiceNames.agfilService, claimIdInput) { claimId ->
        val claim = service.getAccidentInfo(authToken, claimId)
        val policy = service.getPolicy(authToken, claim.customerId, claim.carRegistration)
        policy != null // TODO have more complex validation (like claim category)
    }

    val invalidSplit by split(checkPolicyValidity) {
        min = 1
        max = 1
    }

    val notifyInvalidClaim: NodeHandle<*> by serviceActivity(
        predecessor = invalidSplit,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService,
        input = claimIdInput,
        configure = {
            condition = ExecutableCondition.OTHERWISE
        },
        action = { claimId ->
            service.internal.notifyInvalidClaim(authToken, claimId)
        }
    )

    val terminateClaim: NodeHandle<*> by serviceActivity(
        predecessor = notifyInvalidClaim,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService,
        input = claimIdInput
    ) { claimId ->
        service.internal.terminateClaim(authToken, claimId)
    }

    val handleValidSplit by split(invalidSplit) {
        min = 2
        max = 2
        condition = ExecutableXPathCondition("text()=true")
    }

    val sendClaimForm: NodeHandle<*> by serviceActivity(
        predecessor = handleValidSplit,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService,
        input = claimIdInput
    ) { claimId ->
        service.internal.sendClaimFormToCustomer(authToken, claimId)
    }

    val receiveCompletedClaimForm: DataNodeHandle<CompletedClaimForm> by eventNode(sendClaimForm, CompletedClaimForm.serializer())

    val processCompletedClaimForm: DataNodeHandle<*> by serviceActivity(
        predecessor = receiveCompletedClaimForm,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService
    ) { claimForm ->
        service.internal.processCompletedClaimForm(authToken, claimForm)
    }

    val notifyLeeCs: DataNodeHandle<*> by serviceActivity(
        predecessor = handleValidSplit,
        authorizationTemplates = listOf(),
        service = ServiceNames.leeCsService,
        input = claimIdInput
    ) { claimId ->
        service.startClaimProcessing(authToken, claimId)
    }

    val receiveInvoice: DataNodeHandle<Invoice> by eventNode(notifyLeeCs, Invoice.serializer())

    val validJoin by join(processCompletedClaimForm, receiveInvoice) {
        min = 2
        max = 2
    }

    val makeReconciliation: DataNodeHandle<*> by serviceActivity(
        predecessor = validJoin,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService
    ) {
        // Do nothing at this point
    }

    val payInvoice: DataNodeHandle<*> by serviceActivity(
        predecessor = makeReconciliation,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService,
        input = receiveInvoice
    ) { invoice ->
        service.internal.payGarageInvoice(authToken, invoice)
    }

    val finishJoin by join(terminateClaim, payInvoice) {
        min = 1
        max = 1
    }

    val end by endNode(finishJoin)
}
