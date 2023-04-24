package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class Claim(
    val id: ClaimId,
    val accidentInfo: AccidentInfo,
    val outcome: Outcome,
    val assignedGarageInfo: GarageInfo?,
) {

    enum class Outcome {
        Terminated,
        Completed,
        Undecided,
        Pending,
        Invalid
    }

}
