package io.github.pdvrieze.pma.agfil.services

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.kotlin.arrayMap
import kotlin.random.Random

abstract class RunnableProcessBackedService : AbstractRunnableUiService {

    protected val processEngine: ProcessEngine<StubProcessTransaction>
    protected val random: Random

    constructor(
        serviceName: String,
        authService: AuthService,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        vararg processes: ExecutableProcessModel
    ) : super(authService, serviceName) {
        this.processEngine = processEngine
        this.random = random
        this.processHandles= ensureProcessHandles(processEngine, serviceAuth, processes)
    }


    constructor(
        serviceAuth: PmaIdSecretAuthInfo,
        authService: AuthService,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        vararg processes: ExecutableProcessModel,
    ) : super(authService, serviceAuth) {
        this.processEngine = processEngine
        this.random = random
        this.processHandles= ensureProcessHandles(processEngine, serviceAuth, processes)
    }

    protected val processHandles: Array<Handle<ExecutableProcessModel>>

    companion object {

        private fun ensureProcessHandles(
            processEngine: ProcessEngine<StubProcessTransaction>,
            serviceAuth: PmaIdSecretAuthInfo,
            processes: Array<out ExecutableProcessModel>
        ) : Array<Handle<ExecutableProcessModel>> {
            return processEngine.inTransaction { tr ->
                val existingModels = getProcessModels(tr.readableEngineData, serviceAuth.principal)
                    .map { it.withPermission() }
                    .filter { it.uuid != null }
                    .associate { (it.uuid!!) to it.handle }

                processes.arrayMap { model ->
                    existingModels[model.uuid] ?: addProcessModel(tr, model, serviceAuth.principal).handle
                }
            }
        }

    }
}
