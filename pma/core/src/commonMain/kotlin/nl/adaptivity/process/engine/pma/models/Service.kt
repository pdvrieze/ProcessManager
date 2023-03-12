package nl.adaptivity.process.engine.pma.models

sealed interface Service {
    val serviceId: String
}

interface TaskListService: Service

interface AutomatedService: Service

interface UIService: Service {
}
