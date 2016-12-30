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

package nl.adaptivity.process

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.IXmlMessage
import java.sql.SQLException


/**
 * Interface signifying that the object can be used to send messages. The message provided is an opaque type
 *
 * @author Paul de Vrieze
 *
 * @param MSG_T The type signifying a message that can then be sent. This is used to be able to treat the message as opaque
 */
interface IMessageService<MSG_T> {

  /**
   * Create a message.
   *
   * @param message The message to create (for later sending)
   * *
   * @return The sendable message that can be sent.
   */
  fun createMessage(message: IXmlMessage?): MSG_T

  /**
   * Send a message.

   * @param engineData The transaction to use in sending.
   * *
   * @param protoMessage The message to send. (Created by
   * *          [.createMessage]).
   * *
   * @param instance The task instance to link the sending to.
   * *
   * @return `true` or lack of failure, `false` on failure.
   * *
   * @throws SQLException
   */
  fun sendMessage(engineData: MutableProcessEngineDataAccess, protoMessage: MSG_T, instance: ProcessNodeInstance): Boolean

  /**
   * Get the endpoint belonging to the messenger. (Where can replies go)
   * @return The descriptor of the local endpoint.
   */
  val localEndpoint: EndpointDescriptor
}
