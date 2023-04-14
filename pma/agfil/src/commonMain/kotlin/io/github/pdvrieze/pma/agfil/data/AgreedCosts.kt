package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class AgreedCosts(
    val claimId: ClaimId,
    val carRegistration: CarRegistration,
    val assesorId: Int,
    val amount: Money
) {
}
