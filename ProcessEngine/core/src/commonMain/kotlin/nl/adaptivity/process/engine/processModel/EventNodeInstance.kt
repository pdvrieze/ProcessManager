package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.overlay
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.PIHandle
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.processModel.engine.ExecutableEventNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat

class EventNodeInstance : ProcessNodeInstance<EventNodeInstance> {

    override val node: ExecutableEventNode
        get() = super.node as ExecutableEventNode

    constructor(
        node: ExecutableProcessNode,
        predecessors: Iterable<PNIHandle>,
        processInstanceBuilder: ProcessInstance.Builder,
        hProcessInstance: PIHandle,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: PNIHandle,
        state: NodeInstanceState,
        results: Iterable<ProcessData>,
        failureCause: Throwable?
    ) : super(
        node,
        predecessors,
        processInstanceBuilder,
        hProcessInstance,
        owner,
        entryNo,
        handle,
        state,
        results,
        failureCause
    )

    constructor(builder: Builder) : super(builder)


    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder {
        return ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder : ProcessNodeInstance.Builder<ExecutableEventNode, EventNodeInstance> {
        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess,
            messageService: IMessageService<*>
        ): Boolean {
            return node.isThrowing
        }

        override fun canTakeTaskAutomatically(): Boolean {
            return ! node.isThrowing
        }

        override fun doTakeTask(engineData: MutableProcessEngineDataAccess, assignedUser: PrincipalCompat?): Boolean {
            this.assignedUser = assignedUser
            return true // No actual activity
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return true // The implementation is finish
        }

    }

    class BaseBuilder(
        node: ExecutableEventNode,
        predecessor: PNIHandle,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending,
    ) : ProcessNodeInstance.BaseBuilder<ExecutableEventNode, EventNodeInstance>(
        node, listOf(predecessor), processInstanceBuilder, owner, entryNo, handle, state
    ), Builder {
        override fun build(): EventNodeInstance {
            return EventNodeInstance(this)
        }
    }

    class ExtBuilder(instance: EventNodeInstance, processInstanceBuilder: ProcessInstance.Builder) :
        ProcessNodeInstance.ExtBuilder<ExecutableEventNode, EventNodeInstance>(instance, processInstanceBuilder),
        Builder {
        override var node: ExecutableEventNode by overlay { base.node }

        override fun build(): EventNodeInstance {
            return if (changed) EventNodeInstance(this).also { invalidateBuilder(it) } else base
        }
    }

}
