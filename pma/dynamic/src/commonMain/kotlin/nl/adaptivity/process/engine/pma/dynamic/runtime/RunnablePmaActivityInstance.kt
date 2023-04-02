package nl.adaptivity.process.engine.pma.dynamic.runtime

import PmaAction
import PmaBrowserAction
import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.AbstractRunnableActivityInstance
import net.devrieze.util.Handle
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.util.multiplatform.PrincipalCompat

class RunnablePmaActivityInstance<I : Any, O : Any, C : DynamicPMAActivityContext<C, *>>(
    builder: Builder<I, O, C>
) : AbstractRunnableActivityInstance<I, O, C, RunnablePmaActivity<I, O, C>, RunnablePmaActivityInstance<I, O, C>>(
    builder
) {

    val isBrowserTask: Boolean get() = node.action is PmaBrowserAction<*, *, *, *>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<I, O, C> {
        return RunnablePmaActivityInstance.ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<I : Any, O : Any, C : DynamicPMAActivityContext<C, *>> :
        AbstractRunnableActivityInstance.Builder<I, O, C, RunnablePmaActivity<I, O, C>, RunnablePmaActivityInstance<I, O, C>> {

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

            when (val action: PmaAction<I, O, C> = node.action) {
                is PmaBrowserAction<I, O, C, *> -> {
                    val taskList = processContext.taskListFor(action.action.principal)
                    taskList.acceptActivity(aic, action.action.principal)
                    return true
                }

                else ->
                    return super.doTakeTask(engineData, assignedUser)
            }
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            TODO("not implemented")
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
