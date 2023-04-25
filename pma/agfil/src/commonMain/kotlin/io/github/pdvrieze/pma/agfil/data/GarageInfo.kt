package io.github.pdvrieze.pma.agfil.data

import io.github.pdvrieze.pma.agfil.services.GarageService
import kotlinx.serialization.Serializable
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

@Serializable
data class GarageInfo(val name: String, private val serviceIdStr: String) {
    val serviceId: ServiceId<GarageService> get() = ServiceId(serviceIdStr)

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(name: String, serviceId: ServiceId<*>) = GarageInfo(name, serviceId.serviceId)
    }
}
