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
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface ProcessInstanceContext {
    val processInstanceHandle : Handle<SecureObject<ProcessInstance>>
    fun instancesForName(name: Identified): List<IProcessNodeInstance>
}

interface ActivityInstanceContext {
    val processContext: ProcessInstanceContext
    val node: ProcessNode
    val state: NodeInstanceState
    val nodeInstanceHandle : Handle<SecureObject<ProcessNodeInstance<*>>>

    fun canBeAccessedBy(principal: PrincipalCompat): Boolean
}

interface ProcessContextFactory<out A : ActivityInstanceContext> {

    fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): A

    /**
     * Called to inform the factory that the activity is no longer active: completed, failed, cancelled etc.
     * This means any resources can be released.
     */
    fun onActivityTermination(engineDataAccess: ProcessEngineDataAccess, processNodeInstance: IProcessNodeInstance) {}

    /**
     * Called to inform the factory that the process is no longer active: completed, failed, cancelled etc.
     * This means any resources can be released.
     */
    fun onProcessFinished(
        engineDataAccess: ProcessEngineDataAccess,
        processInstance: Handle<SecureObject<ProcessInstance>>
    ) {
    }

    companion object DEFAULT : ProcessContextFactory<ActivityInstanceContext> {
        override fun newActivityInstanceContext(
            engineDataAccess: ProcessEngineDataAccess,
            processNodeInstance: IProcessNodeInstance
        ): ActivityInstanceContext {
            val processContext:ProcessInstanceContext = SimpleProcessContext(engineDataAccess.instance(processNodeInstance.hProcessInstance).withPermission())
            return SimpleActivityContext(processNodeInstance, processContext)
        }
    }

    private class SimpleProcessContext(
        private val processInstance: IProcessInstance
    ) : ProcessInstanceContext {
        override val processInstanceHandle: Handle<SecureObject<ProcessInstance>>
            get() = processInstance.handle

        override fun instancesForName(name: Identified): List<IProcessNodeInstance> {
            val id = name.id
            return processInstance.allChildNodeInstances().filter { it.node.id == id }.toList()
        }
    }

    private class SimpleActivityContext(
        private val nodeInstance: IProcessNodeInstance,
        override val processContext: ProcessInstanceContext
    ) : ActivityInstanceContext {
        override val node: ExecutableActivity get() = nodeInstance.node as ExecutableActivity
        override val state: NodeInstanceState get() = nodeInstance.state
        override val nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>
            get() = nodeInstance.handle

        override fun canBeAccessedBy(principal: PrincipalCompat): Boolean {
            val ar = (node.accessRestrictions ?: return true)
            return ar.hasAccess(null, principal)
        }
    }
}

