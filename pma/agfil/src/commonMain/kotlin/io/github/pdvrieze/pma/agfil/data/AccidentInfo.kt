package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable

@Serializable
data class AccidentInfo(val customerId: CustomerId, val carRegistration: CarRegistration, val accidentDetails: String)
