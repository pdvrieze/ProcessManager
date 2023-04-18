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

package nl.adaptivity.process.engine.test.loanOrigination

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessEngine
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.pma.dynamic.runtime.impl.nextString
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.EnumeratedTaskList
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.set
import kotlin.random.Random

class LoanContextFactory(log: Logger, random: Random):
    AbstractLoanContextFactory<LoanActivityContext>(log, random) {

    private val processContexts = mutableMapOf<PIHandle, LoanProcessContext>()
    private val taskLists = mutableMapOf<Principal, TaskList<*>>()

    override fun newActivityInstanceContext(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ): LoanActivityContext {
        val instanceHandle = processNodeInstance.hProcessInstance
        nodes[processNodeInstance.handle] = processNodeInstance.node.id
        val processContext = getProcessContext(engineDataAccess, instanceHandle)
        return LoanActivityContext(processContext, processNodeInstance)
    }

    fun getProcessContext(
        engineDataAccess: ProcessEngineDataAccess,
        instanceHandle: PIHandle
    ): LoanProcessContext = processContexts.getOrPut(instanceHandle) { LoanProcessContextImpl(engineDataAccess, this, instanceHandle) }

    override fun onProcessFinished(
        engineDataAccess: ProcessEngineDataAccess,
        processInstance: PIHandle
    ) {
        processContexts.remove(processInstance)
    }

    override fun onActivityTermination(
        engineDataAccess: ProcessEngineDataAccess,
        processNodeInstance: IProcessNodeInstance
    ) {

        val context: LoanProcessContext = getProcessContext(engineDataAccess, processNodeInstance.hProcessInstance)

        with(engineService) { context.onActivityTermination(processNodeInstance) }
    }

    fun getOrCreateTaskListForUser(principal: PrincipalCompat): TaskList<*> {
        val serviceName = ServiceName<EnumeratedTaskList>("TaskList(${principal.name})")
        return taskLists.getOrPut(principal) {
            log.log(Level.INFO, "Creating tasklist service for ${principal.name}")
            val clientAuth = adminAuthServiceClient.registerClient(serviceName, Random.nextString())

            val t = EnumeratedTaskList(serviceName, authService, engineService, clientAuth, listOf(principal))

            adminAuthServiceClient.registerGlobalPermission(principal, t, CommonPMAPermissions.ACCEPT_TASK)

            // TODO, use an activity specific permission/token instead.
            adminAuthServiceClient.registerGlobalPermission(
                SimplePrincipal(engineService.serviceInstanceId.serviceId) as PrincipalCompat,
                t,
                CommonPMAPermissions.POST_TASK
            )
            t
        }
    }

}


