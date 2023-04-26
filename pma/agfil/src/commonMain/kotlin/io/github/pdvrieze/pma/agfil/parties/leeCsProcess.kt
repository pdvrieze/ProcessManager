package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.AgreedCosts
import io.github.pdvrieze.pma.agfil.data.ClaimId
import io.github.pdvrieze.pma.agfil.data.Estimate
import io.github.pdvrieze.pma.agfil.data.Invoice
import io.github.pdvrieze.pma.agfil.services.AgfilPermissions.*
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions.DELEGATED_PERMISSION
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.ContextScopeTemplate
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableXPathCondition
import nl.adaptivity.xmlutil.XmlEvent

val leeCsProcess = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>("LeeCsManageClaim") {
    val claimIdInput = input<ClaimId>("claimId")

    val start by startNode

    val retrieveAccidentInfo by serviceActivity(
        start,
        listOf(ContextScopeTemplate { CLAIM.READ(nodeData(claimIdInput)) }),
        ServiceNames.agfilService,
        claimIdInput
    ) { claimId ->
        // TODO When getting the service token, try to use the token used to start the process.
        service.getFullClaim(authToken, claimId)
    }

    val contactGarage by serviceActivity(
        retrieveAccidentInfo,
        listOf(
            ContextScopeTemplate {
                val accidentInfo = nodeData(retrieveAccidentInfo)
                UnionPermissionScope(
                    accidentInfo.assignedGarageInfo!!.serviceId.let { garageService ->
                        delegatePermissions(garageService, GARAGE.INFORM_INCOMING_CAR).instantiateScope(this)!!
                    },
                    LEECS.INTERNAL.CONTACT_GARAGE(accidentInfo.id)
                )
            },

            ),
        ServiceNames.leeCsService
    ) { claim ->
        // has to use delegate service because there are multiple garages.
        service.internal.contactGarage(authToken, claim)
    }

    val receiveEstimate by eventNode(contactGarage, Estimate.serializer())

    val splitClaim by split(receiveEstimate) {
        min = 1
        max = 1
    }

    val assignAssessor by serviceActivity(
        predecessor = splitClaim,
        authorizationTemplates = listOf(),
        service = ServiceNames.leeCsService,
        input = combine(receiveEstimate named "estimate", retrieveAccidentInfo named "accidentInfo"),
        configure = { condition = ExecutableCondition.OTHERWISE}
    ) { (estimate, accidentInfo) ->
        service.internal.assignAssessor(authToken, accidentInfo, estimate)
    }

    val receiveAssessorAgreedCosts by eventNode(assignAssessor, AgreedCosts.serializer())

    val joinClaim by join(splitClaim, receiveAssessorAgreedCosts) {
        val condNs = listOf(XmlEvent.NamespaceImpl(ProcessConsts.Engine.NSPREFIX, ProcessConsts.Engine.NAMESPACE))
        conditions[splitClaim.identifier] = ExecutableXPathCondition(condNs,"${ProcessConsts.Engine.NSPREFIX}:node('receiveEstimate')/estimatedCosts/text() < 500")
    }

    val agreeClaim by serviceActivity(
        joinClaim,
        listOf(ContextScopeTemplate {
            val claim = nodeData(retrieveAccidentInfo)
            val garageService = requireNotNull(claim.assignedGarageInfo).serviceId
            DELEGATED_PERMISSION.context(garageService, GARAGE.AGREE_REPAIR(claim.id))
        }),
        ServiceNames.leeCsService,
        retrieveAccidentInfo
    ) { claim ->
        service.internal.agreeClaim(authToken, claim)
    }

    val receiveInvoice by eventNode(agreeClaim, Invoice.serializer())

    val verifyInvoice by serviceActivity(
        predecessor = receiveInvoice,
        authorizationTemplates = listOf(
            ContextScopeTemplate { LEECS.INTERNAL.VERIFY_INVOICE(nodeData(claimIdInput)) }
        ),
        service = ServiceNames.leeCsService,
    ) { invoice ->
        service.internal.verifyInvoice(authToken, invoice)
    }

    val forwardInvoice by serviceActivity(
        verifyInvoice,
        listOf(
            ContextScopeTemplate{ CLAIM.REGISTER_INVOICE(nodeData(claimIdInput)) }
        ),
        ServiceNames.agfilService,
        receiveInvoice
    ) { invoice ->
        service.evForwardInvoice(authToken, invoice)
    }

    val end by endNode(forwardInvoice)

}
