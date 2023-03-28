package nl.adaptivity.process.engine.pma.dynamic.runtime

import io.github.pdvrieze.process.processModel.dynamicProcessModel.OutputRef
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.RunnablePmaActivity
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.pma.runtime.PMAProcessContextFactory
import nl.adaptivity.process.engine.pma.runtime.PMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.applyData
import nl.adaptivity.process.engine.processModel.getDefines
import java.security.Principal

abstract class DynamicPMAActivityContext<I : Any, O : Any, A : DynamicPMAActivityContext<I, O, A>>(
    override val processNode: IProcessNodeInstance
) : PMAActivityContext<A>() {
    override val node: RunnablePmaActivity<I, O, *> get() = processNode.node as RunnablePmaActivity<I, O, *>

    final override lateinit var taskListService: TaskList
        private set

    abstract override val processContext: DynamicPMAProcessInstanceContext<*>

    private val pendingPermissions = ArrayDeque<PendingPermission>()

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
                this@DynamicPMAActivityContext.nodeInstanceHandle,
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
    fun registerTaskPermission(service: Service, scope: AuthScope) {
        pendingPermissions.add(PendingPermission(null, service, scope))
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
            CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(clientService.serviceId, service, scope)
        pendingPermissions.add(PendingPermission(null, clientService, delegateScope))
    }

    class PendingPermission(
        val clientId: String? = null,
        val service: Service,
        val scope: AuthScope
    )

    fun nodeResult(reference: OutputRef<I>): I {
//        return processContext.nodeResult(processNode.node, reference) as T
        val defines = (this /*as A*/).getDefines(processContext.processInstance)
        return node.getInputData(defines)

    }

}

interface DynamicPMAProcessInstanceContext<A : DynamicPMAActivityContext<*, *, A>> : PMAProcessInstanceContext<A> {
    val processInstance: IProcessInstance
    val authService: AuthService
    val engineService: EngineService
    val generalClientService: GeneralClientService
    override val contextFactory: DynamicPMAProcessContextFactory<A>


    fun <I: Any, O: Any, C : DynamicPMAActivityContext<I, O, C>> nodeResult(node: RunnablePmaActivity<I, O, C>, reference: OutputRef<O>): I {
        val defines = node.defines.map {
            // TODO the cast shouldn't be needed
            it.applyData(processInstance, this as ActivityInstanceContext)
        }

        return node.getInputData(defines)

    }

}

interface DynamicPMAProcessContextFactory<A : PMAActivityContext<A>> : PMAProcessContextFactory<A> {
    override fun getOrCreateTaskListForUser(principal: Principal): TaskList
}
