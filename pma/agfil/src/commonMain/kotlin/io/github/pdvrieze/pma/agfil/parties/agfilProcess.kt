package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.Claim
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.CompletedClaimForm
import io.github.pdvrieze.pma.agfil.data.Invoice
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import io.github.pdvrieze.process.processModel.dynamicProcessModel.DataNodeHandle
import io.github.pdvrieze.process.processModel.dynamicProcessModel.NodeHandle
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.*
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableXPathCondition

val agfilProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("agfilClaimHandling") {
    val claimIdInput = input<ClaimId>("claimId")

    val start by startNode

    val retrieveClaim: DataNodeHandle<Claim> by serviceActivity(
        start,
        listOf(ContextScopeTemplate { CLAIM.READ(nodeData(claimIdInput)) }),
        ServiceNames.agfilService,
        claimIdInput,
    ) { claimId ->
        service.getFullClaim(authToken, claimId)
    }

    val checkPolicyValidity: DataNodeHandle<Boolean> by serviceActivity(
        retrieveClaim,
        listOf(ContextScopeTemplate { GET_POLICY(nodeData(retrieveClaim).accidentInfo.customerId) }),
        ServiceNames.agfilService,
    ) { claim ->
        val policy = service.getPolicy(authToken, claim.accidentInfo.customerId, claim.accidentInfo.carRegistration)
        policy != null // TODO have more complex validation (like claim category)
    }

    val invalidSplit by split(checkPolicyValidity) {
        min = 1
        max = 1
    }

    val notifyInvalidClaim: NodeHandle<*> by serviceActivity(
        predecessor = invalidSplit,
        authorizationTemplates = listOf(
            ContextScopeTemplate { AGFIL.INTERNAL.NOTIFIY_INVALID_CLAIM(nodeData(claimIdInput)) }
        ),
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
        authorizationTemplates = listOf(
            ContextScopeTemplate { AGFIL.INTERNAL.TERMINATE_CLAIM(nodeData(claimIdInput)) }
        ),
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
        authorizationTemplates = listOf(ContextScopeTemplate {
            val claim = nodeData(retrieveClaim)
            val customerService = claim.accidentInfo.customerServiceId
            UnionPermissionScope(
                AGFIL.INTERNAL.SEND_CLAIM_FORM(claim.id),
                DELEGATED_PERMISSION.context(customerService, POLICYHOLDER.SEND_CLAIM_FORM(claim.id))
            )
        }),
        service = ServiceNames.agfilService,
        input = claimIdInput
    ) { claimId ->
        service.internal.sendClaimFormToCustomer(authToken, claimId)
    }

    val receiveCompletedClaimForm: DataNodeHandle<CompletedClaimForm> by eventNode(sendClaimForm, CompletedClaimForm.serializer())

    val processCompletedClaimForm: DataNodeHandle<*> by serviceActivity(
        predecessor = receiveCompletedClaimForm,
        authorizationTemplates = listOf(
            ContextScopeTemplate { AGFIL.INTERNAL.PROCESS_CLAIM_FORM(nodeData(claimIdInput)) }
        ),
        service = ServiceNames.agfilService
    ) { claimForm ->
        service.internal.processCompletedClaimForm(authToken, claimForm)
    }

    val notifyLeeCs: DataNodeHandle<*> by serviceActivity(
        predecessor = handleValidSplit,
        authorizationTemplates = listOf(
            ContextScopeTemplate { LEECS.START_PROCESSING(nodeData(claimIdInput)) }
        ),
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
        authorizationTemplates = listOf(
            ContextScopeTemplate {
                val invoice = nodeData(receiveInvoice)
                val garageService = invoice.garage.serviceId
                UnionPermissionScope(
                    AGFIL.INTERNAL.PAY_GARAGE_INVOICE(invoice.claimId),
                    DELEGATED_PERMISSION.context(garageService, GARAGE.NOTIFY_INVOICE_PAID(invoice.invoiceId))
                )
            },
//            delegatePermissions(ServiceNames.agfilService, ContextScopeTemplate { AGFIL.INTERNAL.PAY_GARAGE_INVOICE(nodeData(claimIdInput)) })
        ),
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
