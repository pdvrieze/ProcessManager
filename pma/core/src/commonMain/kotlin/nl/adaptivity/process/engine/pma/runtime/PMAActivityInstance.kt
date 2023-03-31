package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.Handle
import net.devrieze.util.overlay
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.pma.models.IPMAMessageActivity
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.tryCreateTask
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.util.multiplatform.PrincipalCompat

class PMAActivityInstance <C : PMAActivityContext<C>> : ProcessNodeInstance<PMAActivityInstance<C>> {

    constructor(
        node: IPMAMessageActivity<C>,
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

    constructor(builder: Builder<C>) : super(builder)

    @Suppress("UNCHECKED_CAST")
    override val node: IPMAMessageActivity<C>
        get() = super.node as IPMAMessageActivity<C>

    override fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<C> {
        return ExtBuilder(this, processInstanceBuilder)
    }

    interface Builder<C :PMAActivityContext<C>> : ProcessNodeInstance.Builder<IPMAMessageActivity<C>, PMAActivityInstance<C>> {
        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess,
            messageService: IMessageService<*>
        ): Boolean {

            fun <AIC : PMAActivityContext<AIC>, MSG_T> createAndSendMessage(
                contextFactory: PMAProcessContextFactory<AIC>,
                messageService: IMessageService<MSG_T>
            ) : MessageSendingResult {
                @Suppress("UNCHECKED_CAST")
                val node: IPMAMessageActivity<AIC> = node as IPMAMessageActivity<AIC>

                val message = node.message ?: XmlMessage()
                val aic = contextFactory.newActivityInstanceContext(engineData, this)
                val authorizations = node.authorizationTemplates.mapNotNull { it.instantiateScope(aic) }
                val authData = aic.requestAuthData(messageService, message.targetService, authorizations)

                val preparedMessage = messageService.createMessage(message)


                return tryCreateTask {
                    messageService.sendMessage(engineData, preparedMessage, aic, authData)
                }
            }

            val sendingResult = createAndSendMessage(engineData.processContextFactory as PMAProcessContextFactory<*>, messageService)

            when (sendingResult) {
                MessageSendingResult.SENT -> state = NodeInstanceState.Sent
                MessageSendingResult.ACKNOWLEDGED -> state = NodeInstanceState.Acknowledged
                MessageSendingResult.FAILED -> failTaskCreation(ProcessException("Failure to send message"))
            }
            store(engineData)
            return false
        }

        override fun canTakeTaskAutomatically(): Boolean = false

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return node.canTakeTaskAutoProgress(createActivityContext(engineData), this, assignedUser)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess): Boolean {
            return node.canStartTaskAutoProgress(this)
        }

    }

    class BaseBuilder<C: PMAActivityContext<C>>(
        node: IPMAMessageActivity<*>,
        predecessor: PNIHandle?,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        override var assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<IPMAMessageActivity<C>, PMAActivityInstance<C>>(
        node as IPMAMessageActivity<C>, listOfNotNull(predecessor), processInstanceBuilder, owner,
        entryNo, handle, state
    ), Builder<C> {
        override fun build(): PMAActivityInstance<C> {
            return PMAActivityInstance(this)
        }
    }

    class ExtBuilder<C: PMAActivityContext<C>>(
        base: PMAActivityInstance<C>,
        processInstanceBuilder: ProcessInstance.Builder
    ) : ProcessNodeInstance.ExtBuilder<IPMAMessageActivity<C>, PMAActivityInstance<C>>(
        base,
        processInstanceBuilder
    ), Builder<C> {
        override var node: IPMAMessageActivity<C> by overlay { base.node }

        override fun build(): PMAActivityInstance<C> {
            return if(changed) PMAActivityInstance(this).also { invalidateBuilder(it) } else base
        }
    }
}
