package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.pma.agfil.data.AgreedCosts
import io.github.pdvrieze.pma.agfil.data.Claim
import io.github.pdvrieze.pma.agfil.data.DamageAssessment
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.models.AutomatedService

class AssessorService : AutomatedService, AutoService {
    val internal: Internal = Internal()

    /** From Lai's thesis */
    fun assignAssessor(): Unit = TODO()

    inner class Internal {

        fun assessDamage(authToken: PmaAuthToken, claim: Claim): DamageAssessment {
            withGarage(authToken, claim.assignedGarageInfo) {

                TODO("not implemented")
            }
        }

        fun negotiateRepairCosts(authToken: PmaAuthToken, claim: Claim, assessment: DamageAssessment): AgreedCosts {
            TODO("not implemented")
        }

    }
}
