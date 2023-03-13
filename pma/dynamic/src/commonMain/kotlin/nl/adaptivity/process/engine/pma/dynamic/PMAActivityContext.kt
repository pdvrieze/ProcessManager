package nl.adaptivity.process.engine.pma

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.PermissionScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableActivity

abstract class PMAActivityContext<A : PMAActivityContext<A>>(private val processNode: IProcessNodeInstance) :
    ActivityInstanceContext {
    abstract override val processContext: PMAProcessInstanceContext<A>

    private val pendingPermissions = ArrayDeque<PendingPermission>()

    lateinit var taskListService: TaskList

    override val node: ExecutableActivity get() = processNode.node as ExecutableActivity

    override val state: NodeInstanceState get() = processNode.state

    override val nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>
        get() = processNode.handle

    inline fun <R> acceptBrowserActivity(browser: Browser, action: TaskList.Context.() -> R): R {
        acceptActivityImpl(browser) // This will initialise the task list and then delegate to it
        return taskListService.contextImpl(browser).action()
    }

    @PublishedApi
    internal fun acceptActivityImpl(browser: Browser) {
        ensureTaskList(browser)
        processContext.engineService.registerActivityToTaskList(taskListService, nodeInstanceHandle)

        val authorizationCode = taskListService.acceptActivity(
            browser.loginToService(taskListService),
            browser.user,
            pendingPermissions,
            nodeInstanceHandle
        )
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

    private fun ensureTaskList(browser: Browser) {
        val taskUser = browser.user
        if (::taskListService.isInitialized) {
            if (taskListService.principal != taskUser) {
                throw UnsupportedOperationException("Attempting to change the user for an activity after it has already been set")
            }
        } else {
            taskListService = processContext.contextFactory.getOrCreateTaskListForUser(taskUser)
        }
    }

    fun serviceTask(): AuthorizationCode {
        if (::taskListService.isInitialized) {
            throw UnsupportedOperationException("Attempting to mark as service task an activity that has already been marked for users")
        }
        val clientServiceId = processContext.generalClientService.serviceId
        val serviceAuthorization = with(processContext) {
            engineService.createAuthorizationCode(
                clientServiceId,
                this@PMAActivityContext.nodeInstanceHandle,
                authService,
                CommonPMAPermissions.IDENTIFY,
                pendingPermissions
            )
        }

        check(pendingPermissions.isEmpty()) { "Pending permissions should be empty after a service task is created" }

        return serviceAuthorization
    }

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerTaskPermission(service: Service, scope: PermissionScope) {
        pendingPermissions.add(PendingPermission(null, service, scope))
    }

    /**
     * TODO Function that registers permissions for the task. This should be done based upon task definition
     *      and in acceptActivity.
     */
    fun registerDelegatePermission(
        clientService: Service,
        service: Service,
        scope: PermissionScope
    ) {
        val delegateScope =
            CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(clientService.serviceId, service, scope)
        pendingPermissions.add(PendingPermission(null, clientService, delegateScope))
    }

    class PendingPermission(
        val clientId: String? = null,
        val service: Service,
        val scope: PermissionScope
    )

}
