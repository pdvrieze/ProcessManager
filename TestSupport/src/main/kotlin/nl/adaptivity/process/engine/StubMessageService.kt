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

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.instantiateXmlPlaceholders
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import java.util.*


/**
 * Created by pdvrieze on 16/10/16.
 */
open class StubMessageService(
    override val localEndpoint: EndpointDescriptor
) : IMessageService<IXmlMessage> {

    class ExtMessage(val base: IXmlMessage, val source: PNIHandle, val authData: AuthorizationInfo?) : IXmlMessage by base

    private var _messages = mutableListOf<ExtMessage>()

    val messages: List<ExtMessage> get() = _messages

    override fun createMessage(message: IXmlMessage): IXmlMessage {
        return message
    }

    fun clear() {
        _messages.clear()
    }

    fun getMessageNode(i: Int): PNIHandle {
        return _messages[i].source
    }

    override fun sendMessage(
        engineData: ProcessEngineDataAccess,
        protoMessage: IXmlMessage,
        activityInstanceContext: ActivityInstanceContext,
        authData: AuthorizationInfo?
    ): MessageSendingResult {
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
        val processedMessage = XmlMessage(protoMessage,
                                          instantiatedContent)

        _messages.add(ExtMessage(processedMessage, activityInstanceContext.nodeInstanceHandle, authData))

        return MessageSendingResult.ACKNOWLEDGED
    }
}
