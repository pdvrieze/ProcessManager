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
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthScope
import nl.adaptivity.process.engine.test.loanOrigination.auth.AuthToken
import nl.adaptivity.process.engine.test.loanOrigination.auth.LoanPermissions
import nl.adaptivity.process.engine.test.loanOrigination.auth.Service
import nl.adaptivity.process.engine.test.loanOrigination.systems.TaskList
import nl.adaptivity.util.security.Principal
import java.util.*

class LoanActivityContext(override val processContext:LoanProcessContext, private val baseContext: ActivityInstanceContext): ActivityInstanceContext by baseContext {
    override var owner: Principal = baseContext.owner
        private set

    private val pendingPermissions = ArrayDeque<(AuthToken)->Unit>()

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
                engineService.serviceAuth,
                taskList.serviceId,
                hNodeInstance,
                engineService,
                LoanPermissions.UPDATE_ACTIVITY_STATE.context(hNodeInstance)
                                               )
        }

        val taskIdentityToken = taskList.registerToken(taskListToEngineAuthToken)
        while(pendingPermissions.isNotEmpty()) {
            val pendingPermission = pendingPermissions.removeFirst()
            pendingPermission(taskIdentityToken)
        }
    }

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerTaskPermission(service: Service, scope: AuthScope) {
        pendingPermissions.add { taskIdToken ->
            processContext.authService.grantPermission(processContext.loanContextFactory.engineClientAuth, taskIdToken, service, scope)
        }
    }

    lateinit var taskList: TaskList

}
