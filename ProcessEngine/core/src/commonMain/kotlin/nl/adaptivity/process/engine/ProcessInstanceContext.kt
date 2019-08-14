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

import net.devrieze.util.ReadableHandleAware
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.util.security.Principal

interface ProcessInstanceContext: ReadableHandleAware<SecureObject<ProcessInstance>> {
}

interface ActivityInstanceContext: ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>> {
    val processContext: ProcessInstanceContext
    val node: ProcessNode
    val state: NodeInstanceState
    val owner: Principal
}

interface ProcessContextFactory<out A:ActivityInstanceContext> {
    fun newActivityInstanceContext(engineDataAccess: ProcessEngineDataAccess, processNodeInstance: IProcessNodeInstance): A

    companion object DEFAULT: ProcessContextFactory<ActivityInstanceContext> {
        override fun newActivityInstanceContext(
            engineDataAccess: ProcessEngineDataAccess,
            processNodeInstance: IProcessNodeInstance
                                               ): ActivityInstanceContext {
            return processNodeInstance
        }
    }
}

