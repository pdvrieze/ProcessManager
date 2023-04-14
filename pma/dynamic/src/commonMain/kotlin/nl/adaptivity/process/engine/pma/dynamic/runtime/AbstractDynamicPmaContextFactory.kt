package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.ServiceResolver

abstract class AbstractDynamicPmaContextFactory<C: DynamicPmaActivityContext<C, *>> : DynamicPmaProcessContextFactory<C> {

    abstract val services: List<Service>

    override val serviceResolver: ServiceResolver = object : ServiceResolver{

        override fun <S : Service> resolveService(serviceName: ServiceName<S>): S {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(services.firstOrNull { it.serviceName == serviceName } as S?) { "No service found for name $serviceName" }
        }

        override fun <S : Service> resolveService(serviceId: ServiceId<S>): S {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(services.firstOrNull { it.serviceInstanceId == serviceId } as S?) { "No service found for id $serviceId" }
        }
    }

}
