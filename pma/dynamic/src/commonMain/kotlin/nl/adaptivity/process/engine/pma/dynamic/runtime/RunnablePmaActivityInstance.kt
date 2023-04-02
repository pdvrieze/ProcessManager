package nl.adaptivity.process.engine.pma.dynamic.runtime

import PmaAction
import PmaBrowserAction
import PmaServiceAction
import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivityInstance
import net.devrieze.util.Handle
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.tryCreateTask
import nl.adaptivity.process.engine.processModel.tryRunTask
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment

class RunnablePmaActivityInstance<InputT : Any, OutputT : Any, C : DynamicPMAActivityContext<C, *>>(
    builder: Builder<InputT, OutputT, C>
) : AbstractRunnableActivityInstance<InputT, OutputT, C, RunnablePmaActivity<InputT, OutputT, C>, RunnablePmaActivityInstance<InputT, OutputT, C>>(
    builder
) {

    val isBrowserTask: Boolean get() = node.action is PmaBrowserAction<*, *, *, *>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<InputT, OutputT, C> {
        return RunnablePmaActivityInstance.ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<InputT : Any, OutputT : Any, C : DynamicPMAActivityContext<C, *>> :
        AbstractRunnableActivityInstance.Builder<InputT, OutputT, C, RunnablePmaActivity<InputT, OutputT, C>, RunnablePmaActivityInstance<InputT, OutputT, C>> {

        val isBrowserTask: Boolean get() = node.action is PmaBrowserAction<*, *, *, *>

        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess,
            messageService: IMessageService<*>
        ): Boolean {
            val contextFactory = engineData.processContextFactory as DynamicPMAProcessContextFactory<C>
            val aic = contextFactory.newActivityInstanceContext(engineData, this)
            val processContext = aic.processContext
            return when (node.action) {
                is PmaBrowserAction<*, *, *, *> -> {
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
            val contextFactory = engineData.processContextFactory as DynamicPMAProcessContextFactory<C>
            val aic = contextFactory.newActivityInstanceContext(engineData, this)
            val processContext = aic.processContext

            when (val action: PmaAction<InputT, OutputT, C> = node.action) {
                is PmaBrowserAction<InputT, OutputT, C, *> -> {
                    val taskList = processContext.taskListFor(action.action.principal)
                    taskList.acceptActivity(aic, action.action.principal)
                    return false // TODO Verify that this is valid as accepting is done by hand
                }

                else ->
                    return super.doTakeTask(engineData, assignedUser)
            }
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            fun <C: DynamicPMAActivityContext<C, *>> doRun(
                contextFactory: ProcessContextFactory<C>,
                builtNodeInstance: RunnablePmaActivityInstance<InputT, OutputT, C>
            ) : CompactFragment? {
                val aic: C = contextFactory.newActivityInstanceContext(engineData, builtNodeInstance)

                val input: InputT = with(builtNodeInstance) { aic.getInputData(processInstanceBuilder) }

                val ac: PmaAction<InputT, OutputT, C> = builtNodeInstance.node.action

                val result: OutputT = when (ac) {
                    is PmaBrowserAction<InputT, OutputT, C, *> ->
                        ac.action.invoke(aic, input)

                    is PmaServiceAction<InputT,OutputT,C,*> -> {
                        val action = ac.action
                        aic.action(input)
                    }

                    else -> error("Should not be reachable (PmaAction is sealed)")
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
                    val builtNodeInstance: RunnablePmaActivityInstance<InputT, OutputT, C> = build()
                    val contextFactory = engineData.processContextFactory as ProcessContextFactory<C>
                    doRun(contextFactory, builtNodeInstance)
                }

                finishTask(engineData, resultFragment)
            }
            return false // we call finish ourselves, so don't call it afterwards.
        }
    }


    class BaseBuilder<I : Any, O : Any, C : DynamicPMAActivityContext<C, *>>(
        node: RunnablePmaActivity<I, O, C>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : AbstractRunnableActivityInstance.BaseBuilder<I, O, C, RunnablePmaActivity<I, O, C>, RunnablePmaActivityInstance<I, O, C>>(
        node, predecessor, processInstanceBuilder, owner,
        entryNo, assignedUser, handle, state
    ), Builder<I, O, C> {

        override fun build(): RunnablePmaActivityInstance<I, O, C> {
            return RunnablePmaActivityInstance(this)
        }
    }

    class ExtBuilder<I : Any, O : Any, C : DynamicPMAActivityContext<C, *>>(
        base: RunnablePmaActivityInstance<I, O, C>,
        processInstanceBuilder: ProcessInstance.Builder
    ) : AbstractRunnableActivityInstance.ExtBuilder<I, O, C, RunnablePmaActivity<I, O, C>, RunnablePmaActivityInstance<I, O, C>>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C> {

        override fun build(): RunnablePmaActivityInstance<I, O, C> {
            return if (changed) RunnablePmaActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }
}
