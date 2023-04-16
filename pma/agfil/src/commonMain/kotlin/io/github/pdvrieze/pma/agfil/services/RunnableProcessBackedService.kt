package io.github.pdvrieze.pma.agfil.services

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.StubProcessTransaction
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaIdSecretAuthInfo
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.kotlin.arrayMap
import java.util.logging.Logger
import kotlin.random.Random

abstract class RunnableProcessBackedService<S: RunnableProcessBackedService<S>> : ServiceBase<S> {

    protected val processEngine: ProcessEngine<StubProcessTransaction>
    protected val random: Random

    constructor(
        serviceName: ServiceName<S>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        vararg processes: ExecutableProcessModel
    ) : this(serviceName, authService, adminAuthInfo, processEngine, random, authService.logger, *processes)

    constructor(
        serviceName: ServiceName<S>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        logger: Logger,
        vararg processes: ExecutableProcessModel
    ) : super(authService, adminAuthInfo, serviceName, logger) {
        this.processEngine = processEngine
        this.random = random
        this.processHandles= ensureProcessHandles(processEngine, authServiceClient, processes)
    }

    constructor(
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        authService: AuthService,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        vararg processes: ExecutableProcessModel,
    ) : this (serviceAuth, serviceName, authService, processEngine, random, authService.logger, *processes)

    constructor(
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        authService: AuthService,
        processEngine: ProcessEngine<StubProcessTransaction>,
        random: Random,
        logger: Logger,
        vararg processes: ExecutableProcessModel,
    ) : super(authService, serviceAuth, serviceName, logger) {
        this.processEngine = processEngine
        this.random = random
        this.processHandles= ensureProcessHandles(processEngine, authServiceClient, processes)
    }

    protected val processHandles: Array<Handle<ExecutableProcessModel>>

    companion object {

        private fun ensureProcessHandles(
            processEngine: ProcessEngine<StubProcessTransaction>,
            authServiceClient: DefaultAuthServiceClient,
            processes: Array<out ExecutableProcessModel>
        ) : Array<Handle<ExecutableProcessModel>> { // TODO use processEngineService
            return processEngine.inTransaction { tr ->
                val existingModels = getProcessModels(tr.readableEngineData, authServiceClient.principal)
                    .map { it.withPermission() }
                    .filter { it.uuid != null }
                    .associate { (it.uuid!!) to it.handle }

                processes.arrayMap { model ->
                    existingModels[model.uuid] ?: addProcessModel(tr, model, authServiceClient.principal).handle
                }
            }
        }

    }
}
