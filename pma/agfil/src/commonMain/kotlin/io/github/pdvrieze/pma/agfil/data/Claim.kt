package io.github.pdvrieze.pma.agfil.data

interface Claim {
    val id: ClaimId
    val accidentInfo: AccidentInfo
    val outcome: Outcome
    val assignedGarageInfo: GarageInfo?

    enum class Outcome {
        Terminated,
        Completed,
        Undecided,
        Pending
    }

}
