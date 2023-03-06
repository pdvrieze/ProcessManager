package nl.adaptivity.process.engine.pma

import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface PMAProcessInstanceContext<A: PMAActivityContext<A>>: ProcessInstanceContext {

    abstract val contextFactory: ProcessContextFactory<A>
    abstract val authService: AuthService
    abstract val engineService: EngineService
    abstract val generalClientService: GeneralClientService

    @Deprecated("Should just be maintained by the engine service")
    abstract fun taskList(user: PrincipalCompat): TaskList

}
