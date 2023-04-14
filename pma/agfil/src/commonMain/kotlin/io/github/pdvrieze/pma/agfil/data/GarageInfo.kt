package io.github.pdvrieze.pma.agfil.data

import io.github.pdvrieze.pma.agfil.services.GarageService
import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.pma.models.ServiceName

@Serializable
data class GarageInfo(val name: String, private val serviceName: String) {
    val service: ServiceName<GarageService> get() = ServiceName(serviceName)
}
