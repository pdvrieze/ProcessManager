package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.messaging.InvokableMethod

interface PmaProcessInstanceContext<out A: PmaActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>

    fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return contextFactory.resolveService(targetService)
    }

}
