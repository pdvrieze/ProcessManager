package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.overlay
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.getDefines
import nl.adaptivity.util.multiplatform.PrincipalCompat

abstract class AbstractRunnableActivityInstance<InputT : Any, OutputT : Any, C : ActivityInstanceContext, out NodeT : AbstractRunnableActivity<InputT, OutputT, *>, InstT : AbstractRunnableActivityInstance<InputT, OutputT, *, NodeT, InstT>>(
    builder: Builder<InputT, OutputT, C, NodeT, InstT>
) : ProcessNodeInstance<InstT>(builder) {

    @Suppress("UNCHECKED_CAST")
    override val node: NodeT get() = super.node as NodeT

    fun ActivityInstanceContext.getInputData(nodeInstanceSource: IProcessInstance): InputT {
        val defines = getDefines(nodeInstanceSource)
        return this@AbstractRunnableActivityInstance.node.getInputData(defines)
    }

    interface Builder<InputT : Any, OutputT : Any, ContextT : ActivityInstanceContext, NodeT : AbstractRunnableActivity<InputT, OutputT, *>, InstT : AbstractRunnableActivityInstance<InputT, OutputT, *, NodeT, InstT>> :
        ProcessNodeInstance.Builder<NodeT, InstT> {

        override var assignedUser: PrincipalCompat?

        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess,
            messageService: IMessageService<*>
        ): Boolean {
            val node = node
            node.canProvideTaskAutoProgress(engineData, this)
            return node.onActivityProvided(engineData, this)
        }

        override fun canTakeTaskAutomatically(): Boolean = node.onActivityProvided == RunnableActivity.OnActivityProvided.DEFAULT

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return node.canTakeTaskAutoProgress(createActivityContext(engineData), this, assignedUser)
        }
    }

    abstract class BaseBuilder<
        I : Any,
        O : Any,
        C : ActivityInstanceContext,
        NodeT : AbstractRunnableActivity<I, O, *>,
        InstT : AbstractRunnableActivityInstance<I, O, C, NodeT, InstT>
        >(
        node: NodeT,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        final override var assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<NodeT, InstT>(
        node = node,
        predecessors = listOfNotNull(predecessor),
        processInstanceBuilder = processInstanceBuilder,
        owner = owner,
        entryNo = entryNo,
        handle = handle,
        state = state
    ), Builder<I, O, C, NodeT, InstT> {
        override fun invalidateBuilder(engineData: ProcessEngineDataAccess) {
            engineData.nodeInstances[handle]?.withPermission()?.let { n ->
                val newBase = n as InstT
                node = newBase.node
                predecessors.replaceBy(newBase.predecessors)
                owner = newBase.owner
                state = newBase.state
                assignedUser = newBase.assignedUser
            }
        }
    }

    abstract class ExtBuilder<
        I : Any,
        O : Any,
        C : ActivityInstanceContext,
        NodeT : AbstractRunnableActivity<I, O, *>,
        InstT : AbstractRunnableActivityInstance<I, O, C, NodeT, InstT>
        >(
        base: InstT,
        processInstanceBuilder: ProcessInstance.Builder
    ) : ProcessNodeInstance.ExtBuilder<NodeT, InstT>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C, NodeT, InstT> {

        final override var node: NodeT by overlay { base.node }

        final override var assignedUser: PrincipalCompat? by overlay { base.assignedUser }

    }

}
