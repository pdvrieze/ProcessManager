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

package nl.adaptivity.process.engine.test.loanOrigination.systems

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.LoanActivityContext
import nl.adaptivity.process.engine.test.loanOrigination.auth.*
import java.security.Principal
import java.util.*

class EngineService(
    private val engineData: ProcessEngineDataAccess,
    authService: AuthService,
    serviceAuth: IdSecretAuthInfo
                   ) : ServiceImpl(authService, serviceAuth) {

    override fun getServiceState(): String = ""

    /**
     * Accept the activity and return an authorization code for the user to identify itself with in relation to
     * the activity.
     */
    fun acceptActivity(
        authToken: AuthToken,
        nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>,
        principal: Principal,
        pendingPermissions: ArrayDeque<LoanActivityContext.PendingPermission>
                      ): AuthorizationCode {
        logMe(nodeInstanceHandle, principal)
        validateAuthInfo(authToken, LoanPermissions.ACCEPT_TASK(nodeInstanceHandle)) // TODO mark correct expected permission
        if (nodeInstanceHandle!= authToken.nodeInstanceHandle) throw IllegalArgumentException("Mismatch with node instances")
        // Should register owner.
//        val taskAuthorizationCode = authService.createAuthorizationCode(serviceAuth, authToken.principal.name, authToken.nodeInstanceHandle, this)
        return authService.createAuthorizationCode(serviceAuth, principal.name, authToken.nodeInstanceHandle, authService, LoanPermissions.IDENTIFY).also { authorizationCode ->

            // Also use the result to register permissions for it
            while (pendingPermissions.isNotEmpty()) {
                val pendingPermission = pendingPermissions.removeFirst()
                authService.grantPermission(
                    serviceAuth,
                    authorizationCode,
                    authService,
                    LoanPermissions.GRANT_PERMISSION.restrictTo(principal.name, pendingPermission.service, pendingPermission.scope))
            }

        }
    }


    fun registerActivityToTaskList(taskList: TaskList, pniHandle: Handle<SecureObject<ProcessNodeInstance<*>>>) {
        logMe(pniHandle)

        val permissions = listOf(LoanPermissions.UPDATE_ACTIVITY_STATE(pniHandle),
                                 LoanPermissions.ACCEPT_TASK(pniHandle))
        val taskListToEngineAuthToken = authService.createAuthorizationCode(
            serviceAuth,
            taskList.serviceId,
            pniHandle,
            this,
            UnionPermissionScope(permissions)
                                                                       )

        taskList.registerToken(taskListToEngineAuthToken)
    }

}
