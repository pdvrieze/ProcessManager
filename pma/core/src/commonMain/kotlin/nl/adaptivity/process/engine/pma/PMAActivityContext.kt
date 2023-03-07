package nl.adaptivity.process.engine.pma

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat

abstract class PMAActivityContext<A: PMAActivityContext<A>>(private val baseContext: ActivityInstanceContext): ActivityInstanceContext, PMAProcessInstanceContext<A> {
    abstract override val processContext: PMAProcessInstanceContext<A>

    override val processInstanceHandle: Handle<SecureObject<ProcessInstance>>
        get() = processContext.processInstanceHandle

    override val contextFactory: PMAProcessContextFactory<A>
        get() = processContext.contextFactory

    override val authService: AuthService get() = processContext.authService

    override val engineService: EngineService get() = processContext.engineService

    override val generalClientService: GeneralClientService get() = processContext.generalClientService

    final override var owner: PrincipalCompat = baseContext.owner
        private set

    private val pendingPermissions = ArrayDeque<PendingPermission>()

    lateinit var taskListService: TaskList

    override val node: ProcessNode get() = baseContext.node

    override val state: NodeInstanceState get() = baseContext.state

    override val nodeInstanceHandle: Handle<SecureObject<ProcessNodeInstance<*>>>
        get() =  baseContext.nodeInstanceHandle

    inline fun <R> acceptBrowserActivity(browser: Browser, action: TaskList.Context.() -> R): R {
        acceptActivityImpl(browser) // This will initialise the task list and then delegate to it
        return taskListService.contextImpl(browser).action()
    }

    @PublishedApi
    internal fun acceptActivityImpl(browser: Browser) {
        ensureTaskList(browser)
        processContext.engineService.registerActivityToTaskList(taskListService, nodeInstanceHandle)

        val authorizationCode= taskListService.acceptActivity(browser.loginToService(taskListService), browser.user, pendingPermissions, nodeInstanceHandle)
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
            taskListService = contextFactory.getOrCreateTaskListForUser(taskUser)
            owner = taskUser
        }
    }

    fun serviceTask(): AuthorizationCode {
        if (::taskListService.isInitialized) {
            throw UnsupportedOperationException("Attempting to mark as service task an activity that has already been marked for users")
        }
        val clientServiceId = processContext.generalClientService.serviceId
        val serviceAuthorization = with(processContext) {
            engineService.createAuthorizationCode(clientServiceId, this@PMAActivityContext.nodeInstanceHandle, authService, CommonPMAPermissions.IDENTIFY, pendingPermissions)
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
    fun registerDelegatePermission(clientService: Service, service: Service, scope: PermissionScope) {
        val delegateScope = CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(clientService.serviceId, service, scope)
        pendingPermissions.add(PendingPermission(null, clientService, delegateScope))
    }

    class PendingPermission(val clientId: String? = null, val service: Service, val scope: PermissionScope)

}
