/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.HandleMap
import net.devrieze.util.MutableHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.SecureProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel

interface ProcessEngineDataAccess<C: ActivityInstanceContext> {
    val processContextFactory: ProcessContextFactory<C>
    val instances: HandleMap<SecureProcessInstance>

    fun instance(handle: PIHandle): SecureProcessInstance = instances[handle].mustExist(handle)

    val nodeInstances: HandleMap<SecureProcessNodeInstance>

    fun nodeInstance(handle: PNIHandle): SecureProcessNodeInstance =
        nodeInstances[handle].mustExist(handle)

    val processModels: IProcessModelMapAccess

    fun processModel(handle: Handle<SecureObject<ExecutableProcessModel>>) = processModels[handle].mustExist(handle)
    fun queueTickle(instanceHandle: PIHandle)

    val logger: LoggerCompat
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("getPIHandle")
inline internal operator fun <C: ActivityInstanceContext> HandleMap<SecureProcessInstance>.get(handle: PIHandle): SecureProcessInstance? {
    @Suppress("UNCHECKED_CAST")
    return get(handle as PIHandle)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("getPNIHandle")
inline internal operator fun <C: ActivityInstanceContext> HandleMap<SecureProcessNodeInstance>.get(handle: PNIHandle): SecureProcessNodeInstance? {
    @Suppress("UNCHECKED_CAST")
    return get(handle as PNIHandle)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("setPIHandle")
inline internal operator fun <C: ActivityInstanceContext> MutableHandleMap<SecureProcessInstance>.set(handle: PIHandle, value: ProcessInstance): SecureProcessInstance? {
    @Suppress("UNCHECKED_CAST")
    return set(handle as PIHandle, value)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("setPNIHandle")
inline internal operator fun <C: ActivityInstanceContext> MutableHandleMap<SecureProcessNodeInstance>.set(handleCompat: PNIHandle, value: ProcessNodeInstance<*,C>): SecureProcessNodeInstance? {
    @Suppress("UNCHECKED_CAST")
    return set(handle = handleCompat as PNIHandle,value)
}

interface MutableProcessEngineDataAccess<C: ActivityInstanceContext> : ProcessEngineDataAccess<C> {

    fun messageService(): IMessageService<*>

    override val instances: MutableHandleMap<SecureProcessInstance>

    override val processModels: IMutableProcessModelMapAccess

    fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>)

    fun invalidateCachePI(handle: PIHandle)

    fun invalidateCachePNI(handle: PNIHandle)

    fun commit()

    fun rollback()

    /** Handle a process instance completing. This allows the policy of deleting or not to be delegated here. */
    fun handleFinishedInstance(handle: PIHandle)

    @OptIn(ProcessInstanceStorage::class)
    fun updateInstance(
        hProcessInstance: PIHandle,
        transform: ProcessInstance.ExtBuilder.() -> Unit
    ) {
        try {
            (instances[hProcessInstance] ?: throw ProcessException("Unexpected invalid handle: $hProcessInstance"))
                .withPermission()
                .update(this, transform)
        } catch (e: Exception) {
            invalidateCachePI(hProcessInstance)
            throw e
        }
    }

    fun updateNodeInstance(
        hNodeInstance: PNIHandle,
        transform: ProcessNodeInstance.Builder<*, *, *>.() -> Unit
    ): SecureProcessNodeInstance {
        updateInstance(nodeInstance(hNodeInstance).shouldExist(hNodeInstance).withPermission().hProcessInstance) {
            try {
                updateChild(hNodeInstance, transform)
            } catch (e: Exception) {
                invalidateCachePNI(hNodeInstance)
                throw e
            }
        }
        return nodeInstance(hNodeInstance)
    }
}
