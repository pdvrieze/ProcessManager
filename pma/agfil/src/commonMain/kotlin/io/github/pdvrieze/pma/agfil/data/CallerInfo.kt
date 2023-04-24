package io.github.pdvrieze.pma.agfil.data

import io.github.pdvrieze.pma.agfil.services.PolicyHolderService
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.pma.models.ServiceId

@Serializable
data class CallerInfo(val name: String, val phoneNumber: String, val serviceId: ServiceId<@Contextual PolicyHolderService>)

