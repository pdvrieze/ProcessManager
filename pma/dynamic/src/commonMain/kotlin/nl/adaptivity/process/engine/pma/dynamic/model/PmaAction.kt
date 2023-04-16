package nl.adaptivity.process.engine.pma.dynamic.model

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableAction
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

sealed class PmaAction<in I : Any, out O : Any, in C : DynamicPmaActivityContext<*, *>> {

    class BrowserAction<I : Any, O : Any, AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>>(
        val action: TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>
    ) : PmaAction<I, O, AIC>() {

    }



    sealed class ServiceAction<in I : Any, O : Any, C : DynamicPmaActivityContext<C, *>, S : AutomatedService>(
        val action: RunnableAction<I, O, C>
    ) : PmaAction<I, O, C>()

    class ServiceNameAction<in I: Any, O: Any, C: DynamicPmaActivityContext<C, *>, S: AutomatedService>(
        val serviceName: ServiceName<S>,
        action: RunnableAction<I,O,C>
    ): ServiceAction<I, O, C, S>(action)

    class ServiceIdAction<in I: Any, O: Any, C: DynamicPmaActivityContext<C, *>, S: AutomatedService>(
        val serviceId: ServiceId<S>,
        action: RunnableAction<I,O,C>
    ): ServiceAction<I, O, C, S>(action)
}
