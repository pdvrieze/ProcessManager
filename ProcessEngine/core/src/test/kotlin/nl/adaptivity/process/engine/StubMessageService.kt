/*
 * Copyright (c) 2016.
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

import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.util.xml.getXmlReader
import nl.adaptivity.xml.CompactFragment
import java.util.*


/**
 * Created by pdvrieze on 16/10/16.
 */
class StubMessageService(private val mLocalEndpoint: EndpointDescriptor) : IMessageService<IXmlMessage> {

  class ExtMessage(val base: IXmlMessage, val source: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) : IXmlMessage by base

  var _messages = mutableListOf<ExtMessage>()

  override fun createMessage(message: IXmlMessage?): IXmlMessage {
    return message?:XmlMessage()
  }

  fun clear() {
    _messages.clear()
  }

  fun getMessageNode(i: Int): net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>> {
    return _messages[i].source
  }

  override val localEndpoint: EndpointDescriptor
    get() = mLocalEndpoint

  override fun sendMessage(engineData: MutableProcessEngineDataAccess,
                           protoMessage: IXmlMessage,
                           instanceBuilder: ProcessNodeInstance.Builder<*,*>): Boolean {
    assert(instanceBuilder.handle.valid) { "Sending messages from invalid nodes is a bad idea (${instanceBuilder})" }

    val instantiatedContent = if (! protoMessage.messageBody.isEmpty) {
      // This just creates a temporary copy
      instanceBuilder.build().instantiateXmlPlaceholders(engineData,
                                                 protoMessage.messageBody.getXmlReader(),
                                                 false,
                                                 localEndpoint)
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

    processedMessage.setContent(instantiatedContent.namespaces, instantiatedContent.content)
    _messages.add(ExtMessage(processedMessage, instanceBuilder.handle))
    instanceBuilder.state = NodeInstanceState.Acknowledged
    instanceBuilder.store(engineData)
    return true
  }
}
