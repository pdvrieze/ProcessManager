package io.github.pdvrieze.process.processModel.dynamicProcessModel.impl

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableMessage
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.process.processModel.IXmlMessage

/**
 * A message service that can run [RunnableMessage] instances.
 */
class RunningMessageService(private val serviceResolver: ServiceResolver): IMessageService<IXmlMessage> {

    private val messages: ArrayDeque<IXmlMessage> = ArrayDeque()

    override val localEndpoint: EndpointDescriptor
        get() = TODO("not implemented")

    override fun createMessage(message: IXmlMessage): IXmlMessage {
        return message
    }

    override fun sendMessage(
        engineData: ProcessEngineDataAccess,
        protoMessage: IXmlMessage,
        activityInstanceContext: ActivityInstanceContext,
        authData: AuthorizationInfo?
    ): MessageSendingResult {
        messages.add(protoMessage)
        return MessageSendingResult.ACKNOWLEDGED
    }

    @Suppress("UNCHECKED_CAST") // this can not be safe in any way
    fun processMessages() {
        while (messages.isNotEmpty()) {
            val message = messages.removeFirst() as RunnableMessage<Any>
            System.out.println("MSGQUEUE - $message")
            val resolvedService = serviceResolver.resolve(message.targetMethod)
            message.run(resolvedService)
        }
    }

    fun interface ServiceResolver {
        fun resolve(service: InvokableMethod): Any
    }
}
