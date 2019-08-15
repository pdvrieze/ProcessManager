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
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import nl.adaptivity.process.engine.test.loanOrigination.systems.TaskList
import nl.adaptivity.util.security.Principal
import java.util.*

class LoanActivityContext(override val processContext:LoanProcessContext, private val baseContext: ActivityInstanceContext): ActivityInstanceContext by baseContext {
    override var owner: Principal = baseContext.owner
        private set

    private val pendingPermissions = ArrayDeque<PendingPermission>()

    fun acceptActivity(principal: SimplePrincipal) {
        if (::taskList.isInitialized) {
            if (taskList.principal != principal) {
                throw UnsupportedOperationException("Attempting to change the user for an activity after it has already been set")
            }
        } else {
            taskList = processContext.loanContextFactory.taskList(principal)
            owner = principal
        }
        val hNodeInstance = handle
        val taskListToEngineAuthToken = with (processContext) {
            authService.createAuthorizationCode(
                engineServiceAuth,
                taskList.serviceId,
                hNodeInstance,
                engineService,
                LoanPermissions.UPDATE_ACTIVITY_STATE.context(hNodeInstance)
                                               )
        }

        val taskIdentityToken = taskList.registerToken(taskListToEngineAuthToken)
        while(pendingPermissions.isNotEmpty()) {
            val pendingPermission = pendingPermissions.removeFirst()
            processContext.authService.grantPermission(engineServiceAuth, taskIdentityToken, pendingPermission.service, pendingPermission.scope)
        }
    }

    fun serviceTask(): AuthorizationCode {
        if (::taskList.isInitialized) {
            throw UnsupportedOperationException("Attempting to mark as service task an activity that has already been marked for users")
        }
        val serviceAuthorization = with(processContext) {
            authService.createAuthorizationCode(engineServiceAuth, generalClientService.serviceId, this@LoanActivityContext.handle, authService, LoanPermissions.IDENTIFY)
        }
        val tokenForAuthz = serviceAuthorization
        while(pendingPermissions.isNotEmpty()) {
            val pendingPermission = pendingPermissions.removeFirst()
            processContext.authService.grantPermission(engineServiceAuth, serviceAuthorization, pendingPermission.service, pendingPermission.scope)
        }

        return serviceAuthorization
    }

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerTaskPermission(service: Service, scope: AuthScope) {
        pendingPermissions.add(PendingPermission(null, service, scope))
    }

    lateinit var taskList: TaskList

    private val engineServiceAuth: IdSecretAuthInfo get() = processContext.loanContextFactory.engineClientAuth

    private class PendingPermission(val clientId: String? = null, val service: Service, val scope: AuthScope)
}
