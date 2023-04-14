package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class DamageAssessment(val registration: CarRegistration, val cost: Money) {

}
