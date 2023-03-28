package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.Handle
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.overlay
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.serialization.XML

abstract class AbstractRunnableActivityInstance<I : Any, O : Any, C : ActivityInstanceContext, NodeT : RunnableActivity<I, O, *>, InstT : AbstractRunnableActivityInstance<I, O, *, NodeT, InstT>>(
    builder: Builder<I, O, C, NodeT, InstT>
) : ProcessNodeInstance<InstT, C>(builder) {

    @Suppress("UNCHECKED_CAST")
    override val node: NodeT get() = super.node as NodeT

    fun <C : ActivityInstanceContext> C.getInputData(nodeInstanceSource: IProcessInstance): I {
        val defines = getDefines(nodeInstanceSource)
        return this@AbstractRunnableActivityInstance.node.getInputData(defines)
    }

    interface Builder<I : Any, O : Any, C : ActivityInstanceContext, NodeT : RunnableActivity<I, O, *>, InstT : AbstractRunnableActivityInstance<I, O, *, NodeT, InstT>> :
        ProcessNodeInstance.Builder<NodeT, InstT, C> {

        override var assignedUser: PrincipalCompat?

        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess<*>,
            messageService: IMessageService<*>
        ): Boolean {
            val node = node
            node.canProvideTaskAutoProgress(engineData, this)
            return node.onActivityProvided(engineData, this)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess<*>): Boolean {
            val shouldProgress = tryCreateTask { node.canStartTaskAutoProgress(this) }

            if (shouldProgress) {

                val resultFragment = tryRunTask {
                    val build = build()
                    val icontext = engineData.processContextFactory.newActivityInstanceContext(engineData, this)
                    val input: I = with(build) { icontext.getInputData(processInstanceBuilder) }
                    val action: RunnableAction<I, O, ActivityInstanceContext> = node.action as RunnableAction<I, O, ActivityInstanceContext>
                    val context = engineData.processContextFactory.newActivityInstanceContext(engineData, this)
                    val result: O = context.action(input)

                    node.outputSerializer?.let { os ->
                        CompactFragment { writer ->
                            XML.defaultInstance.encodeToWriter(writer, os, result)
                        }
                    }
                }

                finishTask(engineData, resultFragment)
            }
            return false // we call finish ourselves, so don't call it afterwards.
        }

        override fun canTakeTaskAutomatically(): Boolean = node.onActivityProvided == RunnableActivity.OnActivityProvided.DEFAULT

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<*>,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return node.canTakeTaskAutoProgress(createActivityContext(engineData), this, assignedUser)
        }
    }

    abstract class BaseBuilder<
        I : Any,
        O : Any,
        C : ActivityInstanceContext,
        NodeT : RunnableActivity<I, O, *>,
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
    ) : ProcessNodeInstance.BaseBuilder<NodeT, InstT, C>(
        node = node,
        predecessors = listOfNotNull(predecessor),
        processInstanceBuilder = processInstanceBuilder,
        owner = owner,
        entryNo = entryNo,
        handle = handle,
        state = state
    ), Builder<I, O, C, NodeT, InstT> {
        override fun invalidateBuilder(engineData: ProcessEngineDataAccess<*>) {
            engineData.nodeInstances[handle]?.withPermission()?.let { n ->
                @Suppress("UNCHECKED_CAST")
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
        NodeT : RunnableActivity<I, O, *>,
        InstT : AbstractRunnableActivityInstance<I, O, C, NodeT, InstT>
        >(
        base: InstT,
        processInstanceBuilder: ProcessInstance.Builder
    ) : ProcessNodeInstance.ExtBuilder<NodeT, InstT, C>(
        base,
        processInstanceBuilder
    ), Builder<I, O, C, NodeT, InstT> {

        final override var node: NodeT by overlay { base.node }

        final override var assignedUser: PrincipalCompat? by overlay { base.assignedUser }

    }

}
