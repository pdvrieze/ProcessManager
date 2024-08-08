package io.github.pdvrieze.pma.agfil.services

import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import net.devrieze.util.Handle
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.ServiceBase
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UnionPermissionScope
import nl.adaptivity.process.engine.pma.runtime.PermissionScope
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.kotlin.arrayMap
import nl.adaptivity.xmlutil.util.CompactFragment
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
        logger: Logger = authService.logger,
        vararg processes: ExecutableProcessModel
    ) : super(authService, adminAuthInfo, serviceName, logger) {
        this.processEngineService = processEngineService
        val engineScope = UnionPermissionScope(
            PermissionScope(ProcessEnginePermissions.LIST_MODELS),
            PermissionScope(ProcessEnginePermissions.ADD_MODEL),
            PermissionScope(ProcessEnginePermissions.START_PROCESS),
        )
        val authCode = authService.getAuthorizationCode(adminAuthInfo, serviceInstanceId.serviceId, serviceInstanceId.serviceId, processEngineService.serviceInstanceId,
            engineScope
        )
        engineToken = authServiceClient.exchangeAuthCode(authCode)

        this.random = random
        this.processHandles= ensureProcessHandles(processEngineService, engineToken, processes)
    }

    constructor(
        serviceName: ServiceName<S>,
        authService: AuthService,
        adminAuthInfo: PmaAuthInfo,
        processEngineService: EngineService,
        random: Random,
        logger: Logger = authService.logger,
        vararg processFactories: RunnableProcessBackedService<S>.() -> ExecutableProcessModel
    ) : super(authService, adminAuthInfo, serviceName, logger) {
        this.processEngineService = processEngineService
        this.random = random

        val engineScope = UnionPermissionScope(
            PermissionScope(ProcessEnginePermissions.LIST_MODELS),
            PermissionScope(ProcessEnginePermissions.ADD_MODEL),
            PermissionScope(ProcessEnginePermissions.START_PROCESS),
            PermissionScope(ProcessEnginePermissions.ASSIGN_OWNERSHIP),
        )
        val authCode = authService.getAuthorizationCode(adminAuthInfo, serviceInstanceId.serviceId, serviceInstanceId.serviceId, processEngineService.serviceInstanceId,
            engineScope
        )
        engineToken = authServiceClient.exchangeAuthCode(authCode)

        this.processHandles= ensureProcessHandles(processEngineService, engineToken, processFactories.arrayMap { it(this) })
    }

    protected val processHandles: Array<Handle<ExecutableProcessModel>>

    protected inline fun <reified T> deliverEvent(handle: PNIHandle, payload: T) {
        processEngineService.deliverEvent(handle, payload(payload, serializer()))
    }

    protected fun <T> deliverEvent(handle: PNIHandle, payload: T, payloadSerializer: SerializationStrategy<T>) {
        processEngineService.deliverEvent(handle, payload(payload, payloadSerializer))
    }

    protected fun deliverEvent(handle: PNIHandle, payload: CompactFragment? = null) {
        processEngineService.deliverEvent(handle, payload)
    }

    protected inline fun <reified T> deliverEvent(piHandle: PIHandle, nodeId: Identified, payload: T) {
        processEngineService.deliverEvent(piHandle, nodeId, payload(payload, serializer()))
    }

    protected fun <T> deliverEvent(piHandle: PIHandle, nodeId: Identified, payload: T, payloadSerializer: SerializationStrategy<T>) {
        processEngineService.deliverEvent(piHandle, nodeId, payload(payload, payloadSerializer))
    }

    protected fun deliverEvent(piHandle: PIHandle, nodeId: Identified, payload: CompactFragment? = null) {
        processEngineService.deliverEvent(piHandle, nodeId, payload)
    }

    protected inline fun <reified T> startProcess(handle: Handle<ExecutableProcessModel>, payload: T): PIHandle {
        return startProcess(handle, payload, serializer())
    }

    protected fun <T> startProcess(handle: Handle<ExecutableProcessModel>, payload: T, payloadSerializer: SerializationStrategy<T>): PIHandle {
        return startProcess(handle, payload(payload, payloadSerializer))
    }

    protected fun startProcess(handle: Handle<ExecutableProcessModel>, payload: CompactFragment?=null): PIHandle {
        return processEngineService.startProcess(engineToken, handle, payload)
    }



    companion object {

        private fun ensureProcessHandles(
            processEngineService: EngineService,
            engineToken: PmaAuthToken,
            processes: Array<out ExecutableProcessModel>
        ) : Array<Handle<ExecutableProcessModel>> { // TODO use processEngineService
            val reOwnedProcesses = processes.arrayMap { when(it.owner.name) {
                engineToken.principal.name -> it
                else -> ExecutableProcessModel(it.builder().apply { owner = engineToken.principal })
            } }
            return processEngineService.ensureProcessHandles(engineToken, reOwnedProcesses)
        }

    }
}
