package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class Estimate(
    val claimId: ClaimId,
    val registration: CarRegistration,
    val estimatedCosts: Money
) {
}
