package io.github.pdvrieze.pma.agfil.data

import io.github.pdvrieze.pma.agfil.services.PolicyHolderService
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.pma.models.ServiceId

@Serializable
data class AccidentInfo(
    val customerId: CustomerId,
    val customerServiceId: ServiceId<@Contextual PolicyHolderService>,
    val carRegistration: CarRegistration,
    val accidentDetails: String
)
