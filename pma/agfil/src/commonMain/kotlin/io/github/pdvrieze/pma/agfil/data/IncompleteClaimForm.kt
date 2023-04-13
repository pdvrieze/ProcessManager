package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class IncompleteClaimForm(val claimId: ClaimId, val carRegistration: CarRegistration, val policyHolder: String) {

    fun fill(): CompletedClaimForm = CompletedClaimForm(claimId, carRegistration, policyHolder)
}

@Serializable
data class CompletedClaimForm(val claimId: ClaimId, val carRegistration: CarRegistration, val policyHolder: String)
