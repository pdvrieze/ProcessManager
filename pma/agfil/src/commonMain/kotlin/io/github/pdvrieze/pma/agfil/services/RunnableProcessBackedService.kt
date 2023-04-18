package io.github.pdvrieze.pma.agfil.services

import net.devrieze.util.Handle
import nl.adaptivity.process.engine.ContextProcessTransaction
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.runtime.DefaultAuthServiceClient
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.engine.pma.runtime.PermissionScope
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.util.kotlin.arrayMap
import java.util.logging.Logger
import kotlin.random.Random

abstract class RunnableProcessBackedService<S: RunnableProcessBackedService<S>> : ServiceBase<S> {

    protected val processEngineService: EngineService
    protected val engineToken: PmaAuthToken
    protected val random: Random

    constructor(
        serviceName: ServiceName<S>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngineService: EngineService,
        random: Random,
        vararg processes: ExecutableProcessModel
    ) : this(serviceName, authService, adminAuthInfo, processEngineService, random, authService.logger, *processes)

    constructor(
        serviceName: ServiceName<S>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngineService: EngineService,
        random: Random,
        logger: Logger,
        vararg processes: ExecutableProcessModel
    ) : super(authService, adminAuthInfo, serviceName, logger) {
        this.processEngineService = processEngineService
        val engineScope = UnionPermissionScope(
            PermissionScope(ProcessEnginePermissions.LIST_MODELS),
            PermissionScope(ProcessEnginePermissions.ADD_MODEL),
            PermissionScope(ProcessEnginePermissions.START_PROCESS),
        )
        val authCode = authService.getAuthorizationCode(adminAuthInfo, serviceInstanceId.serviceId, processEngineService.serviceInstanceId,
            engineScope
        )
        engineToken = authServiceClient.exchangeAuthCode(authCode)

        this.random = random
        this.processHandles= ensureProcessHandles(processEngineService, authServiceClient, processes)
    }

    constructor(
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        authService: AuthService,
        processEngineService: EngineService,
        random: Random,
        vararg processes: ExecutableProcessModel,
    ) : this (serviceAuth, serviceName, authService, processEngineService, random, authService.logger, *processes)

    constructor(
        serviceAuth: PmaIdSecretAuthInfo,
        serviceName: ServiceName<S>,
        authService: AuthService,
        processEngineService: EngineService,
        random: Random,
        logger: Logger,
        vararg processes: ExecutableProcessModel,
    ) : super(authService, serviceAuth, serviceName, logger) {
        this.processEngineService = processEngineService
        this.random = random
        this.processHandles= ensureProcessHandles(processEngineService, authServiceClient, processes)

        val engineScope = UnionPermissionScope(
            PermissionScope(ProcessEnginePermissions.LIST_MODELS),
            PermissionScope(ProcessEnginePermissions.ADD_MODEL),
            PermissionScope(ProcessEnginePermissions.START_PROCESS),
        )
        val authCode = authService.getAuthorizationCode(authServiceClient.originatingClientAuth, serviceAuth.id, processEngineService.serviceInstanceId,
            engineScope
        )
        engineToken = authServiceClient.exchangeAuthCode(authCode)

    }

    protected val processHandles: Array<Handle<ExecutableProcessModel>>

    companion object {

        private fun ensureProcessHandles(
            processEngineService: EngineService,
            authServiceClient: DefaultAuthServiceClient,
            processes: Array<out ExecutableProcessModel>
        ) : Array<Handle<ExecutableProcessModel>> { // TODO use processEngineService
            fun <TR: ContextProcessTransaction> impl(processEngine: ProcessEngine<TR>): Array<Handle<ExecutableProcessModel>> {
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
            return impl(processEngineService.processEngine)
        }

    }
}
