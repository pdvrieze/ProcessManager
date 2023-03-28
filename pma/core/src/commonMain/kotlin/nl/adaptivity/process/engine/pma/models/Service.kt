package nl.adaptivity.process.engine.pma.models

sealed interface Service {
    val serviceId: String
}

interface TaskListService: Service

interface AutomatedService: Service

interface UIService: Service {
}

@JvmInline
value class ServiceId(val serviceId: String)
