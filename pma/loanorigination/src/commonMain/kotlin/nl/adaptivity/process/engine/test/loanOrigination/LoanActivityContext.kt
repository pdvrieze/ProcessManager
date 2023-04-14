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

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.AuthorizationCode
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanActivityContext(
    override val processContext: LoanProcessContext,
    val processNode: IProcessNodeInstance
) : ActivityInstanceContext {
    override val node: ExecutableActivity
        get() = processNode.node as ExecutableActivity
    override val state: NodeInstanceState
        get() = processNode.state
    override val nodeInstanceHandle: PNIHandle
        get() = processNode.handle
    override val owner: PrincipalCompat
        get() = TODO("owner doesn't work") //processNode.owner
    override val assignedUser: PrincipalCompat?
        get() = processNode.assignedUser

    private val pendingPermissions = ArrayDeque<AbstractDynamicPmaActivityContext.PendingPermission>()

    final lateinit var taskListService: TaskList
        private set

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerTaskPermission(service: Service, scope: AuthScope) {
        pendingPermissions.add(AbstractDynamicPmaActivityContext.PendingPermission(null, service, scope))
    }

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerDelegatePermission(
        clientService: Service,
        service: Service,
        scope: AuthScope
    ) {
        val delegateScope =
            CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(clientService.serviceInstanceId.serviceId, service, scope)
        pendingPermissions.add(AbstractDynamicPmaActivityContext.PendingPermission(null, clientService, delegateScope))
    }

    fun serviceTask(): AuthorizationCode {
        if (::taskListService.isInitialized) {
            throw UnsupportedOperationException("Attempting to mark as service task an activity that has already been marked for users")
        }
        val clientServiceId = processContext.generalClientService.serviceInstanceId
        val serviceAuthorization = with(processContext) {
            engineService.createAuthorizationCode(
                clientServiceId,
                nodeInstanceHandle,
                authService,
                CommonPMAPermissions.IDENTIFY,
                pendingPermissions
            )
        }
        pendingPermissions.clear()

        return serviceAuthorization
    }

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean {

        val restrictions = (node as? RunnableActivity<*, *, *>)
            ?.accessRestrictions ?: return true
        return principal != null && restrictions.hasAccess(this, principal, ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY)
    }

    inline fun <R> acceptBrowserActivity(browser: Browser, action: TaskList.Context.() -> R): R {
        acceptActivityImpl(browser) // This will initialise the task list and then delegate to it
        return taskListService.contextImpl(browser).action()
    }

    private fun ensureTaskList(browser: Browser) {
        val taskUser = browser.user
        if (::taskListService.isInitialized) {
            if (taskListService.servesFor(taskUser)) {
                throw UnsupportedOperationException("Attempting to change the user for an activity after it has already been set")
            }
        } else {
            taskListService = processContext.contextFactory.getOrCreateTaskListForUser(taskUser)
        }
    }

    @PublishedApi
    internal fun acceptActivityImpl(browser: Browser) {
        ensureTaskList(browser)
        processContext.engineService.doPostTaskToTasklist(taskListService, nodeInstanceHandle)

        val authorizationCode = taskListService.acceptActivity(
            browser.loginToService(taskListService),
            browser.user,
            pendingPermissions,
            nodeInstanceHandle
        )
        pendingPermissions.clear()
        browser.addToken(processContext.authService, authorizationCode)


        /*

                val hNodeInstance = handle
                while (pendingPermissions.isNotEmpty()) {
                    val pendingPermission = pendingPermissions.removeFirst()
                    processContext.authService.grantPermission(
                        engineServiceAuth,
                        taskIdentityToken,
                        processContext.authService,
                        LoanPermissions.GRANT_PERMISSION.invoke(pendingPermission.service, pendingPermission.scope))
                }
                browser.addToken(taskIdentityToken)
        */
    }

}

