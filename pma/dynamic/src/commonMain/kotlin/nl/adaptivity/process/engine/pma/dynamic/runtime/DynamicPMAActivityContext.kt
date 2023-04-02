package nl.adaptivity.process.engine.pma.dynamic.runtime

import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRef
import io.github.pdvrieze.process.processModel.dynamicProcessModel.OutputRef
import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.IProcessInstance
import nl.adaptivity.process.engine.pma.*
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.pma.runtime.PMAProcessContextFactory
import nl.adaptivity.process.engine.pma.runtime.PMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.applyData
import nl.adaptivity.process.engine.processModel.getDefines
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.serialutil.nonNullSerializer
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import java.security.Principal

interface IDynamicPMAActivityContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>> :
    PMAActivityContext<AIC> {

    fun browserContext(browser: Browser): BIC
    fun resolveBrowser(principal: PrincipalCompat): Browser
}

abstract class DynamicPMAActivityContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: TaskBuilderContext.BrowserContext<AIC, BIC>>(
    override val processNode: IProcessNodeInstance
) : IDynamicPMAActivityContext<AIC, BIC> {
    override val node: RunnablePmaActivity<*, *, *> get() = processNode.node as RunnablePmaActivity<*, *, *>

    abstract override val processContext: DynamicPMAProcessInstanceContext<AIC>

    private val pendingPermissions = ArrayDeque<PendingPermission>()

/*
    inline fun <R> acceptBrowserActivity(browser: Browser, action: TaskList.Context.() -> R): R {
        acceptActivityImpl(browser) // This will initialise the task list and then delegate to it
        return taskListService.contextImpl(browser).action()
    }
*/

/*
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


        */
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
        *//*

    }
*/

/*
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
*/

    fun serviceTask(): AuthorizationCode {
        val clientServiceId = processContext.generalClientService.serviceInstanceId
        val serviceAuthorization = with(processContext) {
            engineService.createAuthorizationCode(
                clientServiceId.serviceId,
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
            CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(clientService.serviceInstanceId.serviceId, service, scope)
        pendingPermissions.add(PendingPermission(null, clientService, delegateScope))
    }

    class PendingPermission(
        val clientId: String? = null,
        val service: Service,
        val scope: AuthScope
    )

    fun <T: Any> nodeData(reference: InputRef<T>): T? {
        val valueReader = processNode.resolvePredecessor(processContext.processInstance, reference.propertyName)
            ?.getResult(reference.propertyName)?.contentStream ?: return null

        val deserializer: DeserializationStrategy<T> = reference.serializer.nonNullSerializer()
        return XML.decodeFromReader(deserializer, valueReader)
    }

    fun <InT> nodeResult(reference: OutputRef<InT>): InT {
//        return processContext.nodeResult(processNode.node, reference) as T
        val defines = (this /*as A*/).getDefines(processContext.processInstance)
        return node.getInputData(defines) as InT

    }

}

interface DynamicPMAProcessInstanceContext<A : DynamicPMAActivityContext<A, *>> : PMAProcessInstanceContext<A> {
    val processInstance: IProcessInstance
    val authService: AuthService
    val engineService: EngineService
    val generalClientService: GeneralClientService
    override val contextFactory: DynamicPMAProcessContextFactory<A>


    fun <I: Any, O: Any, C : DynamicPMAActivityContext<C, *>> nodeResult(node: RunnablePmaActivity<I, O, C>, reference: OutputRef<O>): I {
        val defines = node.defines.map {
            // TODO the cast shouldn't be needed
            it.applyData(processInstance, this as ActivityInstanceContext)
        }

        return node.getInputData(defines)

    }

    fun taskListFor(principal: PrincipalCompat): TaskListService

}

interface DynamicPMAProcessContextFactory<A : PMAActivityContext<A>> : PMAProcessContextFactory<A> {
    override fun getOrCreateTaskListForUser(principal: Principal): TaskList
    override fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskList>
    fun <S: Service> resolveService(serviceId: ServiceName<S>): S
}
