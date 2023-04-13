package io.github.pdvrieze.pma.agfil.services

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.dynamic.services.AbstractRunnableUiService
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.kotlin.arrayMap
import kotlin.random.Random

abstract class RunnableProcessBackedService(
    serviceName: String,
    authService: AuthService,
    protected val processEngine: ProcessEngine<StubProcessTransaction>,
    protected val random: Random,
    vararg processes: ExecutableProcessModel
): AbstractRunnableUiService(authService, serviceName) {

    protected val processHandles: Array<Handle<ExecutableProcessModel>> = processEngine.inTransaction { tr ->
        val existingModels = getProcessModels(tr.readableEngineData, serviceAuth.principal)
            .map { it.withPermission() }
            .filter { it.uuid!=null }
            .associate { (it.uuid!!) to it.handle }

        processes.arrayMap { model ->
            existingModels[model.uuid] ?: addProcessModel(tr, model, serviceAuth.principal).handle
        }
    }

}
