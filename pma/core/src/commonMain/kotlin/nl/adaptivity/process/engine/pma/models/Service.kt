package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

sealed interface Service {
    val serviceId: String
}

interface TaskListService: Service {
    /**
     * Does this service support this particular user
     */
    fun servesFor(principal: PrincipalCompat): Boolean
    fun acceptActivity(aic: PMAActivityContext<*>, user: PrincipalCompat)
}

interface AutomatedService: Service

interface UIService: Service {
}

@JvmInline
value class ServiceId<S: Service>(val serviceId: String)
