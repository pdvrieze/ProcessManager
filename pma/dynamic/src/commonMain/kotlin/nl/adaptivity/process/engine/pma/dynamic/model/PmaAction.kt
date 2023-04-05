package nl.adaptivity.process.engine.pma.dynamic.model

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableAction
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.ServiceName

sealed class PmaAction<I : Any, O : Any, C : DynamicPmaActivityContext<C, *>> {

class BrowserAction<I : Any, O : Any, AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>>(
    val action: TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>
) : PmaAction<I, O, AIC>() {

}

class ServiceAction<I : Any, O : Any, C : DynamicPmaActivityContext<C, *>, S : AutomatedService>(
    val serviceId: ServiceName<S>,
    val action: RunnableAction<I, O, C>
) : PmaAction<I, O, C>()

}
