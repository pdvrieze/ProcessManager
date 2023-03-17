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
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel

interface ProcessEngineDataAccess<C: ActivityInstanceContext> {
    val processContextFactory: ProcessContextFactory<C>
    val instances: HandleMap<SecureObject<ProcessInstance<C>>>

    fun instance(handle: Handle<SecureObject<ProcessInstance<*>>>): SecureObject<ProcessInstance<C>> = instances[handle].mustExist(handle)

    val nodeInstances: HandleMap<SecureObject<ProcessNodeInstance<*, C>>>

    fun nodeInstance(handle: Handle<SecureObject<ProcessNodeInstance<*, *>>>): SecureObject<ProcessNodeInstance<*, C>> =
        nodeInstances[handle].mustExist(handle)

    val processModels: IProcessModelMapAccess

    fun processModel(handle: Handle<SecureObject<ExecutableProcessModel>>) = processModels[handle].mustExist(handle)
    fun queueTickle(instanceHandle: Handle<SecureObject<ProcessInstance<*>>>)

    val logger: LoggerCompat
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("getPIHandle")
inline internal operator fun <C: ActivityInstanceContext> HandleMap<SecureObject<ProcessInstance<C>>>.get(handle: Handle<SecureObject<ProcessInstance<*>>>): SecureObject<ProcessInstance<C>>? {
    @Suppress("UNCHECKED_CAST")
    return get(handle as Handle<SecureObject<ProcessInstance<C>>>)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("getPNIHandle")
inline internal operator fun <C: ActivityInstanceContext> HandleMap<SecureObject<ProcessNodeInstance<*, C>>>.get(handle: Handle<SecureObject<ProcessNodeInstance<*, *>>>): SecureObject<ProcessNodeInstance<*,C>>? {
    @Suppress("UNCHECKED_CAST")
    return get(handle as Handle<SecureObject<ProcessNodeInstance<*, C>>>)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("setPIHandle")
inline internal operator fun <C: ActivityInstanceContext> MutableHandleMap<SecureObject<ProcessInstance<C>>>.set(handle: Handle<SecureObject<ProcessInstance<*>>>, value: ProcessInstance<C>): SecureObject<ProcessInstance<C>>? {
    @Suppress("UNCHECKED_CAST")
    return set(handle as Handle<SecureObject<ProcessInstance<C>>>, value)
}

@Suppress("NOTHING_TO_INLINE")
@JvmName("setPNIHandle")
inline internal operator fun <C: ActivityInstanceContext> MutableHandleMap<SecureObject<ProcessNodeInstance<*, C>>>.set(handleCompat: Handle<SecureObject<ProcessNodeInstance<*, *>>>, value: ProcessNodeInstance<*,C>): SecureObject<ProcessNodeInstance<*,C>>? {
    @Suppress("UNCHECKED_CAST")
    return set(handle = handleCompat as Handle<SecureObject<ProcessNodeInstance<*, C>>>,value)
}

interface MutableProcessEngineDataAccess<C: ActivityInstanceContext> : ProcessEngineDataAccess<C> {

    fun messageService(): IMessageService<*, C>

    override val instances: MutableHandleMap<SecureObject<ProcessInstance<C>>>

    override val processModels: IMutableProcessModelMapAccess

    fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>)

    fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance<*>>>)

    fun invalidateCachePNI(handle: Handle<SecureObject<ProcessNodeInstance<*, *>>>)

    fun commit()

    fun rollback()

    /** Handle a process instance completing. This allows the policy of deleting or not to be delegated here. */
    fun handleFinishedInstance(handle: Handle<SecureObject<ProcessInstance<*>>>)

    @OptIn(ProcessInstanceStorage::class)
    fun updateInstance(
        hProcessInstance: Handle<SecureObject<ProcessInstance<*>>>,
        transform: ProcessInstance.ExtBuilder<C>.() -> Unit
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
        hNodeInstance: Handle<SecureObject<ProcessNodeInstance<*, *>>>,
        transform: ProcessNodeInstance.Builder<*, *, C>.() -> Unit
    ): SecureObject<ProcessNodeInstance<*, C>> {
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
