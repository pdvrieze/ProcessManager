package nl.adaptivity.process.engine.pma.models

import kotlinx.serialization.Serializable
import nl.adaptivity.messaging.EndpointDescriptor
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
@Serializable
value class ServiceName<out S: Service>(val serviceName: String)

@JvmInline
@Serializable
value class ServiceId<out S: Service>(val serviceId: String)

fun <S: Service> EndpointDescriptor.toServiceId(): ServiceId<S> {
    val serviceNamePart = serviceName?.toString()
    val endpointPart = endpointName
    val endpointLocationPart = endpointLocation?.toString()
    return when {
        endpointPart!=null -> ServiceId("$serviceNamePart:$endpointPart")
        serviceNamePart!=null && endpointLocationPart==null -> ServiceId(serviceNamePart)
        serviceNamePart!=null -> ServiceId("$serviceNamePart:$endpointLocationPart")
        endpointLocationPart== null -> throw IllegalArgumentException("The endpoint is fully null")
        else -> ServiceId(endpointLocationPart)
    }
}

interface ServiceResolver {
    fun <S: Service> resolveService(serviceName: ServiceName<S>): S
    fun <S: Service> resolveService(serviceId: ServiceId<S>): S
}
