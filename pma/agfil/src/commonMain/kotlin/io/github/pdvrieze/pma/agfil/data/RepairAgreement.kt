package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class RepairAgreement(val claimId: ClaimId, val carRegistration: CarRegistration, val agreedCosts: Money) {
}
