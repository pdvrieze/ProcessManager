package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.Handle
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.pma.models.PMAMessageActivity
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.tryCreateTask
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.util.multiplatform.PrincipalCompat

class PMAActivityInstance <C : PMAActivityContext<C>> : ProcessNodeInstance<PMAActivityInstance<C>, C> {
    constructor(
        node: PMAMessageActivity<C>,
        predecessors: Iterable<Handle<SecureObject<ProcessNodeInstance<*, C>>>>,
        processInstanceBuilder: ProcessInstance.Builder<*>,
        hProcessInstance: Handle<SecureObject<ProcessInstance<C>>>,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>>,
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

    constructor(builder: Builder<C>) : super(builder)

    @Suppress("UNCHECKED_CAST")
    override val node: PMAMessageActivity<C>
        get() = super.node as PMAMessageActivity<C>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder<C>): ExtBuilder<C> {
        return ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<C :PMAActivityContext<C>> : ProcessNodeInstance.Builder<PMAMessageActivity<C>, PMAActivityInstance<C>, C> {
        override fun <MSG_T> doProvideTask(
            engineData: MutableProcessEngineDataAccess<C>,
            messageService: IMessageService<MSG_T, C>
        ): Boolean {
            val message = node.message ?: XmlMessage()
            val aic = createActivityContext(engineData)
            val authorizations = node.authorizationTemplates.map { it.instantiateScope(aic) }
            aic.requestAuthData(messageService, message.targetService, authorizations)

            val preparedMessage = messageService.createMessage(message)


            val sendingResult = tryCreateTask {
                messageService.sendMessage(engineData, preparedMessage, aic)// TODO remove cast
            }

            when (sendingResult) {
                MessageSendingResult.SENT -> state = NodeInstanceState.Sent
                MessageSendingResult.ACKNOWLEDGED -> state = NodeInstanceState.Acknowledged
                MessageSendingResult.FAILED -> failTaskCreation(ProcessException("Failure to send message"))
            }
            store(engineData)
            return false


            TODO("not implemented")
        }

        override fun canTakeTaskAutomatically(): Boolean = false

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat?
        ): Boolean {
            TODO("not implemented")
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            TODO("not implemented")
        }

    }

    class BaseBuilder<C: PMAActivityContext<C>>(
        node: PMAMessageActivity<C>,
        predecessor: Handle<SecureObject<ProcessNodeInstance<*, C>>>?,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        owner: PrincipalCompat,
        entryNo: Int,
        override var assignedUser: PrincipalCompat? = null,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<PMAMessageActivity<C>, PMAActivityInstance<C>, C>(
        node, listOfNotNull(predecessor), processInstanceBuilder, owner,
        entryNo, handle, state
    ), Builder<C> {
        override fun build(): PMAActivityInstance<C> {
            return PMAActivityInstance(this)
        }
    }

    class ExtBuilder<C: PMAActivityContext<C>>(
        base: PMAActivityInstance<C>,
        processInstanceBuilder: ProcessInstance.Builder<C>
    ) : ProcessNodeInstance.ExtBuilder<PMAMessageActivity<C>, PMAActivityInstance<C>, C>(
        base,
        processInstanceBuilder
    ), Builder<C> {
        override var node: PMAMessageActivity<C> by overlay { base.node }

        override fun build(): PMAActivityInstance<C> {
            return if(changed) PMAActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }
}
