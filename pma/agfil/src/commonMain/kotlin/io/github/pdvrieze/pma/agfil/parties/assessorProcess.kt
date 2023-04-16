package io.github.pdvrieze.pma.agfil.parties

import io.github.pdvrieze.pma.agfil.contexts.AgfilActivityContext
import io.github.pdvrieze.pma.agfil.contexts.AgfilBrowserContext
import io.github.pdvrieze.pma.agfil.data.Claim
import io.github.pdvrieze.pma.agfil.services.AssessorService
import io.github.pdvrieze.pma.agfil.services.ServiceNames
import nl.adaptivity.process.engine.pma.dynamic.model.runnablePmaProcess
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.util.multiplatform.PrincipalCompat

fun assessorProcess(principal: PrincipalCompat, ownerService: ServiceId<AssessorService>) = runnablePmaProcess<AgfilActivityContext, AgfilBrowserContext>(
    name = "assessor (${principal.name}) assess car",
    owner = principal
) {
    val claimInput = input<Claim>("claim")
    val start by startNode
    val assessDamage by serviceActivity(start, listOf(), ownerService, claimInput) {claim ->
        service.internal.assessDamage(authToken, claim)
    }
    val negotiateRepairCosts by serviceActivity(
        predecessor = assessDamage,
        authorizationTemplates = listOf(),
        service = ownerService,
        input = combine(claimInput named "claim", assessDamage named "assessment")
    ) { (claim, assessment) ->
        service.internal.negotiateRepairCosts(authToken, claim, assessment)
    }
    val sendAgreementDetails by serviceActivity(negotiateRepairCosts, listOf(), ServiceNames.leeCsService) { agreedCosts ->
        service.sendAssessedCosts(authToken, agreedCosts)
    }
    val end by endNode(sendAgreementDetails)
}
