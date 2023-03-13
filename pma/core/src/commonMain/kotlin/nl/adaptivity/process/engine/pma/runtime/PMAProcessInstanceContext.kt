package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessInstanceContext

interface PMAProcessInstanceContext<A: PMAActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>

}
