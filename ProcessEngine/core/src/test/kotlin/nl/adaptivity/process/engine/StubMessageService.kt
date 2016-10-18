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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.Transaction
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import java.util.*


/**
 * Created by pdvrieze on 16/10/16.
 */
class StubMessageService(private val mLocalEndpoint: EndpointDescriptor) : IMessageService<IXmlMessage, Transaction, ProcessNodeInstance<Transaction>> {

  var mMessages: MutableList<IXmlMessage> = ArrayList()
  private val mMessageNodes = ArrayList<Handle<ProcessNodeInstance<Transaction>>>()

  override fun createMessage(message: IXmlMessage): IXmlMessage {
    return message
  }

  fun clear() {
    mMessageNodes.clear()
    mMessages.clear()
  }

  fun getMessageNode(i: Int): Handle<ProcessNodeInstance<Transaction>> {
    return mMessageNodes[i]
  }

  override fun getLocalEndpoint(): EndpointDescriptor {
    return mLocalEndpoint
  }

  override fun sendMessage(transaction: Transaction,
                           protoMessage: IXmlMessage,
                           instance: ProcessNodeInstance<Transaction>): Boolean {

    val instantiatedContent = instance.instantiateXmlPlaceholders(transaction,
                                                                XMLFragmentStreamReader.from(protoMessage.messageBody),
                                                                false)
    val processedMessage = XmlMessage(protoMessage.service,
                                    protoMessage.endpoint,
                                    protoMessage.operation,
                                    protoMessage.url,
                                    protoMessage.method,
                                    protoMessage.contentType,
                                    instantiatedContent)

    processedMessage.setContent(instantiatedContent.namespaces, instantiatedContent.content)
    mMessages.add(processedMessage)
    mMessageNodes.add(instance.handle)
    return true
  }
}
