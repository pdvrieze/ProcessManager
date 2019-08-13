/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.security.SecureObject
import net.devrieze.util.toComparableHandle
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode

/**
 * Simple base interface for process node instances that can also be implemented by builders
 */
interface IProcessNodeInstance/*: ReadableHandleAware<Any>*/ {
    val node: ExecutableProcessNode
    val predecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>

    @Deprecated("use property", ReplaceWith("handleXXX"))
    fun handle(): Handle<SecureObject<ProcessNodeInstance<*>>> = handleXXX.toComparableHandle() as Handle<SecureObject<ProcessNodeInstance<*>>>

    val handleXXX: Handle<SecureObject<ProcessNodeInstance<*>>>

    val entryNo: Int
    val state: NodeInstanceState
    fun builder(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance.Builder<*, *>

    fun build(processInstanceBuilder: ProcessInstance.Builder): ProcessNodeInstance<*> = builder(processInstanceBuilder).build()

    fun condition(engineData: ProcessEngineDataAccess, predecessor: IProcessNodeInstance) =
        node.condition(engineData, predecessor, this)

}
