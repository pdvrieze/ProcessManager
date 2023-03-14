/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.instantiateXmlPlaceholders
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import java.util.*


/**
 * Created by pdvrieze on 16/10/16.
 */
class StubMessageService<C: ActivityInstanceContext>(private val mLocalEndpoint: EndpointDescriptor) : IMessageService<IXmlMessage, C> {

    class ExtMessage(val base: IXmlMessage, val source: Handle<SecureObject<ProcessNodeInstance<*>>>) : IXmlMessage by base

    private var _messages = mutableListOf<ExtMessage>()

    val messages: List<ExtMessage> get() = _messages

    override fun createMessage(message: IXmlMessage): IXmlMessage {
        return message
    }

    fun clear() {
        _messages.clear()
    }

    fun getMessageNode(i: Int): Handle<SecureObject<ProcessNodeInstance<*>>> {
        return _messages[i].source
    }

    override val localEndpoint: EndpointDescriptor
        get() = mLocalEndpoint

    override fun sendMessage(engineData: ProcessEngineDataAccess,
                             protoMessage: IXmlMessage,
                             activityInstanceContext: C): MessageSendingResult {
        assert(activityInstanceContext.nodeInstanceHandle.isValid) { "Sending messages from invalid nodes is a bad idea (${activityInstanceContext})" }

        val instantiatedContent: ICompactFragment = if (! protoMessage.messageBody.isEmpty) {
            val processInstance = engineData.instance(activityInstanceContext.processContext.processInstanceHandle).withPermission()
            // This just creates a temporary copy
            activityInstanceContext.instantiateXmlPlaceholders(
                processInstance,
                protoMessage.messageBody.getXmlReader(),
                false,
                localEndpoint
            ) as ICompactFragment
        } else {
            CompactFragment(Collections.emptyList(), CharArray(0))
        }
        val processedMessage = XmlMessage(protoMessage.service,
                                          protoMessage.endpoint,
                                          protoMessage.operation,
                                          protoMessage.url,
                                          protoMessage.method,
                                          protoMessage.contentType,
                                          instantiatedContent)

        _messages.add(ExtMessage(processedMessage, activityInstanceContext.nodeInstanceHandle))

        return MessageSendingResult.ACKNOWLEDGED
    }
}
