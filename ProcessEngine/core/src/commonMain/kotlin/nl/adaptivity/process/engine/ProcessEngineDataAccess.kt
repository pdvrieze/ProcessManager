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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.HandleMap
import net.devrieze.util.MutableHandleMap
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.impl.Logger
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel

interface ProcessEngineDataAccess {
    val instances: HandleMap<SecureObject<ProcessInstance>>

    fun instance(handle: Handle<SecureObject<ProcessInstance>>) = instances[handle].mustExist(handle)

    val nodeInstances: HandleMap<SecureObject<ProcessNodeInstance<*>>>

    fun nodeInstance(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) =
        nodeInstances[handle].mustExist(handle)

    val processModels: IProcessModelMapAccess

    fun processModel(handle: Handle<SecureObject<ExecutableProcessModel>>) = processModels[handle].mustExist(handle)
    fun queueTickle(instanceHandle: ComparableHandle<SecureObject<ProcessInstance>>)

    val logger: Logger
}

interface MutableProcessEngineDataAccess : ProcessEngineDataAccess {

    fun messageService(): IMessageService<*>

    override val instances: MutableHandleMap<SecureObject<ProcessInstance>>

    override val processModels: IMutableProcessModelMapAccess

    fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>)

    fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance>>)

    fun invalidateCachePNI(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>)

    fun commit()

    fun rollback()

    /** Handle a process instance completing. This allows the policy of deleting or not to be delegated here. */
    fun handleFinishedInstance(handle: ComparableHandle<SecureObject<ProcessInstance>>)
}
