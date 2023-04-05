package nl.adaptivity.process.engine.pma.dynamic.runtime

import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivityInstance
import net.devrieze.util.Handle
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
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
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment

class DynamicPmaActivityInstance<InputT : Any, OutputT : Any, C : DynamicPmaActivityContext<C, *>>(
    builder: Builder<InputT, OutputT, C>
) : AbstractRunnableActivityInstance<InputT, OutputT, C, RunnablePmaActivity<InputT, OutputT, C>, DynamicPmaActivityInstance<InputT, OutputT, C>>(
    builder
) {

    val isBrowserTask: Boolean get() = node.action is BrowserAction<*, *, *, *>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<InputT, OutputT, C> {
        return DynamicPmaActivityInstance.ExtBuilder(this, processInstanceBuilder)
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
            return when (node.action) {
                is BrowserAction<*, *, *, *> -> {
                    val taskLists = contextFactory.getOrCreateTaskListForRestrictions(node.accessRestrictions)
                    for (taskList in taskLists) {
                        processContext.engineService.doPostTaskToTasklist(taskList, handle)
                    }
                    true
                }

                else -> {

                    true
                }
            }
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
                                    service = it.serviceId?.let { id -> aic.processContext.contextFactory.resolveService(id) }
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
                    CompactFragment { writer ->
                        XML.defaultInstance.encodeToWriter(writer, os, result)
                    }
                }

            }

            val shouldProgress = tryCreateTask { node.canStartTaskAutoProgress(this) }

            if (shouldProgress) {

                val resultFragment = tryRunTask {
                    val builtNodeInstance: DynamicPmaActivityInstance<InputT, OutputT, C> = build()
                    val contextFactory = engineData.processContextFactory as ProcessContextFactory<C>
                    doRun(contextFactory, builtNodeInstance)
                }

                finishTask(engineData, resultFragment)
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
