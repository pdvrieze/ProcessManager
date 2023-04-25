package nl.adaptivity.process.engine.pma.dynamic.runtime

import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.RunningMessageService
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.PmaServiceResolver
import nl.adaptivity.process.messaging.InvokableMethod

abstract class AbstractDynamicPmaContextFactory<C: DynamicPmaActivityContext<C, *>> : DynamicPmaProcessContextFactory<C> {

    abstract val services: List<Service>

    override val serviceResolver: PmaServiceResolver = object : PmaServiceResolver, RunningMessageService.ServiceResolver{
        override fun resolve(service: InvokableMethod): Any {
            return services.first {
                it.serviceName.serviceName == service.endpoint.serviceName?.localPart
            }
        }

        override fun <S : Service> resolveService(serviceName: ServiceName<S>): S {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(services.firstOrNull { it.serviceName == serviceName } as S?) { "No service found for name $serviceName" }
        }

        override fun <S : Service> resolveService(serviceId: ServiceId<S>): S {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(services.firstOrNull { it.serviceInstanceId == serviceId } as S?) {
                "No service found for id $serviceId"
            }
        }
    }

}
