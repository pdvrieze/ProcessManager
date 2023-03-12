package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.ProcessInstanceContext

interface PMAProcessInstanceContext<A: PMAActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>
    val authService: AuthService
    val engineService: EngineService
    val generalClientService: GeneralClientService

}
