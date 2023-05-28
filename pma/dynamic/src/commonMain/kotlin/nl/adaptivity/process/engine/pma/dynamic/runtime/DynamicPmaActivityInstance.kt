package nl.adaptivity.process.engine.pma.dynamic.runtime

import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivityInstance
import io.github.pdvrieze.process.processModel.dynamicProcessModel.GenericRunnableMessage
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableMessage
import io.github.pdvrieze.process.processModel.dynamicProcessModel.impl.payload
import net.devrieze.util.Handle
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.pma.EngineService
import nl.adaptivity.process.engine.pma.dynamic.model.PmaAction
import nl.adaptivity.process.engine.pma.dynamic.model.PmaAction.BrowserAction
import nl.adaptivity.process.engine.pma.dynamic.model.PmaAction.ServiceAction
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.Service
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.tryCreateTask
import nl.adaptivity.process.engine.processModel.tryRunTask
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

class DynamicPmaActivityInstance<InputT : Any, OutputT : Any, C : DynamicPmaActivityContext<C, *>>(
    builder: Builder<InputT, OutputT, C>
) : AbstractRunnableActivityInstance<InputT, OutputT, C, RunnablePmaActivity<InputT, OutputT, C>, DynamicPmaActivityInstance<InputT, OutputT, C>>(
    builder
) {

    val isBrowserTask: Boolean get() = node.action is BrowserAction<*, *, *, *>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<InputT, OutputT, C> {
        return ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<InputT : Any, OutputT : Any, C : DynamicPmaActivityContext<C, *>> :
        AbstractRunnableActivityInstance.Builder<InputT, OutputT, C, RunnablePmaActivity<InputT, OutputT, C>, DynamicPmaActivityInstance<InputT, OutputT, C>> {

        val isBrowserTask: Boolean get() = node.action is BrowserAction<*, *, *, *>

        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess,
            messageService: IMessageService<*>
        ): Boolean {
            val contextFactory = engineData.processContextFactory as DynamicPmaProcessContextFactory<C>
            val aic = contextFactory.newActivityInstanceContext(engineData, this)
            val processContext = aic.processContext
            return when (val a = node.action) {
                is BrowserAction<InputT, OutputT, C, *> -> {
                    val taskLists = contextFactory.getOrCreateTaskListForRestrictions(node.accessRestrictions)
                    for (taskList in taskLists) {
                        processContext.engineService.doPostTaskToTasklist(taskList, handle)
                    }
                    val principal = a.action.principalProvider(aic).name
                    sendTakeTaskMessage<EngineService>(engineData, messageService, aic, principal)
                    false
                }

                else -> {
                    sendTakeTaskMessage<EngineService>(engineData, messageService, aic)
                    false
                }
            }
        }

        override fun canTakeTaskAutomatically(): Boolean {
            return node.accessRestrictions == null && super.canTakeTaskAutomatically()
        }

        override fun doTakeTask(engineData: MutableProcessEngineDataAccess, assignedUser: PrincipalCompat?): Boolean {
            val contextFactory = engineData.processContextFactory as DynamicPmaProcessContextFactory<C>
            val aic = contextFactory.newActivityInstanceContext(engineData, this)
            val processContext: DynamicPmaProcessInstanceContext<C> = aic.processContext

            when (val action: PmaAction<InputT, OutputT, C> = node.action) {
                is BrowserAction<InputT, OutputT, C, *> -> {
                    val user = with(action.action) { aic.principalProvider() }
                    val taskList = processContext.contextFactory.getOrCreateTaskListForUser(user)
                    val browser = aic.resolveBrowser(user)

                    val pendingPermissions = node.authorizationTemplates
                        .mapNotNull { it.instantiateScope(aic) }
                        .mapNotNull {
                            val service: Service?
                            val scope: AuthScope?
                            when (it) {
                                is CommonPMAPermissions.DELEGATED_PERMISSION.DelegateContextScope -> {
                                    service = it.serviceId?.let { id -> aic.processContext.contextFactory.serviceResolver.resolveService(id) }
                                    scope = it.childScope
                                }

                                else -> {
                                    service = taskList
                                    scope = it
                                }
                            }
                            if (service!=null && scope != null) {
                                AbstractDynamicPmaActivityContext.PendingPermission(user.name, service, scope)
                            } else null
                        }

                    val taskListToken = browser.loginToService(taskList)

                    val authCode = taskList.acceptActivity(taskListToken, user, pendingPermissions, aic.nodeInstanceHandle)
                    browser.addToken(processContext.authService, authCode) // TODO tidy this up

                    return super.doTakeTask(engineData, user)
                }

                else ->
                    return super.doTakeTask(engineData, assignedUser)
            }
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            fun <C: DynamicPmaActivityContext<C, *>> doRun(
                contextFactory: ProcessContextFactory<C>,
                builtNodeInstance: DynamicPmaActivityInstance<InputT, OutputT, C>
            ) : CompactFragment? {
                val aic: C = contextFactory.newActivityInstanceContext(engineData, builtNodeInstance)

                val input: InputT = with(builtNodeInstance) { aic.getInputData(processInstanceBuilder) }

                val ac: PmaAction<InputT, OutputT, C> = builtNodeInstance.node.action

                val result: OutputT = when (ac) {
                    is BrowserAction<InputT, OutputT, C, *> ->
                        ac.action.invoke(aic, input)

                    is ServiceAction<InputT, OutputT, C, *> -> {
                        val action = ac.action
                        aic.action(input)
                    }
                }

                    //aic.action(input)

                return node.outputSerializer?.let { os ->
                    payload(result, os)
                }

            }

            val shouldProgress = tryCreateTask { node.canStartTaskAutoProgress(this) }

            if (shouldProgress) {
                val contextFactory = engineData.processContextFactory as ProcessContextFactory<C>

                val resultFragment = tryRunTask {
                    val builtNodeInstance: DynamicPmaActivityInstance<InputT, OutputT, C> = build()
                    doRun(contextFactory, builtNodeInstance)
                }
                val aic = contextFactory.newActivityInstanceContext(engineData, this)
                sendFinishTaskMessage(engineData, engineData.messageService(), aic, resultFragment ?: CompactFragment(""))
//                finishTask(engineData, resultFragment)
            }
            return false // we call finish ourselves, so don't call it afterwards.
        }
    }


    class BaseBuilder<I : Any, O : Any, C : DynamicPmaActivityContext<C, *>>(
        node: RunnablePmaActivity<I, O, C>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : AbstractRunnableActivityInstance.BaseBuilder<I, O, C, RunnablePmaActivity<I, O, C>, DynamicPmaActivityInstance<I, O, C>>(
        node, predecessor, processInstanceBuilder, owner,
        entryNo, assignedUser, handle, state
    ), Builder<I, O, C> {

        override fun build(): DynamicPmaActivityInstance<I, O, C> {
            return DynamicPmaActivityInstance(this)
        }
    }

    class ExtBuilder<I : Any, O : Any, C : DynamicPmaActivityContext<C, *>>(
        base: DynamicPmaActivityInstance<I, O, C>,
        processInstanceBuilder: ProcessInstance.Builder
    ) : AbstractRunnableActivityInstance.ExtBuilder<I, O, C, RunnablePmaActivity<I, O, C>, DynamicPmaActivityInstance<I, O, C>>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override fun build(): DynamicPmaActivityInstance<I, O, C> {
            return if (changed) DynamicPmaActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }
}

private fun <S> createAndSendRunnableMessage(
    engineData: ProcessEngineDataAccess,
    messageService: IMessageService<*>,
    activityInstanceContext: DynamicPmaActivityContext<*, *>,
    destination: InvokableMethod = activityInstanceContext.processContext.engineService.runMessageMethod,
    body: S.() -> Unit
) {
    fun <MSG_T> impl(messageService: IMessageService<MSG_T>) {
        val protoMessage = messageService.createMessage(GenericRunnableMessage(destination, body))
        messageService.sendMessage(engineData, protoMessage, activityInstanceContext)
    }
    impl(messageService)
}

private fun <S> sendTakeTaskMessage(
    engineData: ProcessEngineDataAccess,
    messageService: IMessageService<*>,
    activityInstanceContext: DynamicPmaActivityContext<*, *>,
    assignedUser: String? = null,
    pniHandle: PNIHandle = activityInstanceContext.nodeInstanceHandle,
    destination: InvokableMethod = activityInstanceContext.processContext.engineService.runMessageMethod,
) {
    fun <MSG_T> impl(messageService: IMessageService<MSG_T>) {
        val protoMessage = messageService.createMessage(TakeTaskMessage(destination, pniHandle, assignedUser))
        messageService.sendMessage(engineData, protoMessage, activityInstanceContext)
    }
    impl(messageService)
}

private fun sendFinishTaskMessage(
    engineData: ProcessEngineDataAccess,
    messageService: IMessageService<*>,
    activityInstanceContext: DynamicPmaActivityContext<*, *>,
    payload: ICompactFragment = CompactFragment(""),
    pniHandle: PNIHandle = activityInstanceContext.nodeInstanceHandle,
    destination: InvokableMethod = activityInstanceContext.processContext.engineService.runMessageMethod,
) {
    fun <MSG_T> impl(messageService: IMessageService<MSG_T>) {
        val protoMessage = messageService.createMessage(FinishProcessMessage(destination, pniHandle, payload))
        messageService.sendMessage(engineData, protoMessage, activityInstanceContext)
    }
    impl(messageService)
}

private class TakeTaskMessage(
    override val targetMethod: InvokableMethod,
    val pniHandle: PNIHandle,
    val assignedUser: String?
) : RunnableMessage<EngineService> {
    override val messageBody: ICompactFragment
        get() = CompactFragment(listOf(XmlEvent.NamespaceImpl("pe", Constants.PROCESS_ENGINE_NS)), "<pe:take>${this}</pe:take>")

    private fun <TR : ContextProcessTransaction> runImpl(processEngine: ProcessEngine<TR>) {
        processEngine.inTransaction { tr ->
            if (assignedUser!=null) {
                processEngine.takeTask(tr, pniHandle, assignedUser, SYSTEMPRINCIPAL)
            } else {
                processEngine.updateTaskState(tr, pniHandle, NodeInstanceState.Taken, SYSTEMPRINCIPAL)
            }
        }
    }

    override fun run(target: EngineService) = runImpl(target.processEngine)

    override fun toString(): String {
        return "takeTask($pniHandle, $assignedUser)"
    }
}

private class FinishProcessMessage(override val targetMethod: InvokableMethod, val pniHandle: PNIHandle, val payload: ICompactFragment) : RunnableMessage<EngineService> {
    override fun run(target: EngineService) = runImpl(target.processEngine)

    private fun <TR : ContextProcessTransaction> runImpl(engine: ProcessEngine<TR>) {
        engine.inTransaction {
            engine.finishTask(it, pniHandle, payload, SYSTEMPRINCIPAL)
        }
    }

    override fun toString(): String = "finish($pniHandle, $payload)"

    override val messageBody: ICompactFragment get() = CompactFragment(toString())
}
