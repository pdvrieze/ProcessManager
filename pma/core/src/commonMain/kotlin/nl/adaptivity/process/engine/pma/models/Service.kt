package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.util.multiplatform.PrincipalCompat

interface Service {
    val serviceName: ServiceName<Service>
    val serviceInstanceId: ServiceId<Service>
}

interface TaskListService: Service {
    override val serviceName: ServiceName<TaskListService>
    override val serviceInstanceId: ServiceId<TaskListService>
    /**
     * Does this service support this particular user
     */
    fun servesFor(principal: PrincipalCompat): Boolean
}

interface AutomatedService: Service {
    override val serviceName: ServiceName<AutomatedService>
    override val serviceInstanceId: ServiceId<AutomatedService>
}

interface UiService: Service {
    override val serviceName: ServiceName<UiService>
    override val serviceInstanceId: ServiceId<UiService>
}

@JvmInline
value class ServiceName<out S: Service>(val serviceName: String)

@JvmInline
value class ServiceId<out S: Service>(val serviceId: String)

interface ServiceResolver {
    fun <S: Service> resolve(serviceName: ServiceName<S>): S
}
