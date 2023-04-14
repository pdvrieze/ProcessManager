package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.CompletedClaimForm
import io.github.pdvrieze.pma.agfil.data.Invoice
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableXPathCondition

val agfilProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("agfilClaimHandling") {
    val claimIdInput = input<ClaimId>("claimId")

    val start by startNode

    val checkPolicyValidity by serviceActivity(start, listOf(), ServiceNames.agfilService, claimIdInput) { claimId ->
        val claim = service.getAccidentInfo(authToken, claimId)
        val policy = service.getPolicy(authToken, claim.customerId, claim.carRegistration)
        policy != null // TODO have more complex validation (like claim category)
    }

    val invalidSplit by split(checkPolicyValidity) {
        min = 1
        max = 1
    }

    val notifyInvalidClaim by serviceActivity(
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

    val terminateClaim by serviceActivity(
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

    val sendClaimForm by serviceActivity(
        predecessor = handleValidSplit,
        authorizationTemplates = listOf(),
        service = ServiceNames.agfilService,
        input = claimIdInput
    ) { claimId ->
        service.internal.sendClaimFormToCustomer(authToken, claimId)
    }

    val receiveCompletedClaimForm by eventNode(sendClaimForm, CompletedClaimForm.serializer())

    val processCompletedClaimForm by serviceActivity(receiveCompletedClaimForm, listOf(), ServiceNames.agfilService) { claimForm ->
        service.internal.processCompletedClaimForm(authToken, claimForm)
    }

    val notifyLeeCs by serviceActivity(handleValidSplit, listOf(), ServiceNames.leeCsService, claimIdInput) { claimId ->
        service.startClaimProcessing(authToken, claimId)
    }

    val receiveInvoice by eventNode(notifyLeeCs, Invoice.serializer())

    val validJoin by join(processCompletedClaimForm, receiveInvoice) {
        min = 2
        max = 2
    }

    val makeReconciliation by serviceActivity(validJoin, listOf(), ServiceNames.agfilService) {
        // Do nothing at this point
    }

    val payInvoice by serviceActivity(makeReconciliation, listOf(), ServiceNames.agfilService, receiveInvoice) {invoice ->
        service.internal.payGarageInvoice(authToken, invoice)
    }

    val finishJoin by join(terminateClaim, payInvoice) {
        min = 1
        max = 1
    }

    val end by endNode(finishJoin)
}
