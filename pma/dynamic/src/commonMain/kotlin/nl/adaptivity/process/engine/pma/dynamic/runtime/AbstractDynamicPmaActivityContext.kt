package nl.adaptivity.process.engine.pma.dynamic.runtime

import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRef
import io.github.pdvrieze.process.processModel.dynamicProcessModel.OutputRef
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.AuthorizationCode
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.getDefines
import nl.adaptivity.process.processModel.MessageActivity
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableCompositeActivity
import nl.adaptivity.serialutil.nonNullSerializer
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML

abstract class AbstractDynamicPmaActivityContext<AIC : DynamicPmaActivityContext<AIC, BIC>, BIC: TaskBuilderContext.BrowserContext<AIC, BIC>>(
    override val activityInstance: IProcessNodeInstance
) : DynamicPmaActivityContext<AIC, BIC> {
    override val node: ExecutableActivity get() = activityInstance.node as ExecutableActivity

    abstract override val processContext: DynamicPmaProcessInstanceContext<AIC>

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

    @Deprecated("Not used at all", level = DeprecationLevel.ERROR)
    fun serviceTask(): AuthorizationCode {
        val clientServiceId = processContext.generalClientService.serviceInstanceId
        val serviceAuthorization = with(processContext) {
            engineService.createAuthorizationCode(
                clientServiceId.serviceId,
                this@AbstractDynamicPmaActivityContext.nodeInstanceHandle,
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
        val result: ProcessData? = when (val nodeRef = reference.nodeRef) {
            null -> processContext.processInstance.inputs.firstOrNull { it.name == reference.propertyName }

            else -> {
                activityInstance.resolvePredecessor(processContext.processInstance, nodeRef.id)
                    ?.getResult(reference.propertyName)
            }
        }


        val valueReader = result?.contentStream ?: return null

        val deserializer: DeserializationStrategy<T> = reference.serializer.nonNullSerializer()
        return XML.decodeFromReader(deserializer, valueReader)
    }

    fun <InT> nodeResult(reference: OutputRef<InT>): InT {
//        return processContext.nodeResult(processNode.node, reference) as T
        val defines = (this /*as A*/).getDefines(processContext.processInstance)
        when (val n = node) {
            is ExecutableCompositeActivity -> {
                val data =  defines.firstOrNull { it.name == reference.propertyName } ?: error("No result found for ${reference} in executable activity")
                val ser: DeserializationStrategy<Any> = (data as? RunnableActivity.DefineType<InT>)?.run { deserializer.nonNullSerializer() } ?: error("Define cannot be deserialized")

                return XML.decodeFromReader(ser, data.contentStream) as InT
            }
            is AbstractRunnableActivity<*,*,*> -> {
                return n.getInputData(defines) as InT
            }
        }
        error("No result found for ${reference}")

    }

    override fun resolveBrowser(principal: PrincipalCompat): Browser {
        return processContext.resolveBrowser(principal)
    }

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean {
        val restrictions = (node as? MessageActivity)?.accessRestrictions ?: return true

        return principal != null &&
            restrictions.hasAccess(this, principal, ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY)
    }
}

