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

package nl.adaptivity.process.engine.processModel

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import org.w3c.dom.Node
import java.sql.SQLException

/**
 * ProcessNodeInstance parent interface has the methods needed in execution.
 */
interface IExecutableProcessNodeInstance<V: IExecutableProcessNodeInstance<V>> : IProcessNodeInstance<V> {

  @Throws(XmlException::class)
  fun serialize(transaction: ProcessTransaction,
                out: XmlWriter,
                localEndpoint: EndpointDescriptor)

  /**
   * Called by the processEngine so indicate starting of the task.

   * @param messageService Service to use for communication of change of state.
   * *
   * @return `true` if this stage is complete and the engine should
   * *         progress to {
   * *
   * @throws SQLException @link #takeTask(IMessageService)
   */
  @Throws(SQLException::class)
  fun <U> provideTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in V>): V

  /**
   * Called by the processEngine to let the task be taken.

   * @param messageService Service to use for communication of change of state.
   * *
   * @return `true` if this stage has completed and the task should
   * *         be [started][.startTask].
   */
  @Throws(SQLException::class)
  fun <U> takeTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in ProcessNodeInstance>): V

  /**
   * Called by the processEngine to let the system start the task.

   * @param messageService Service to use for communication of change of state.
   * *
   * @return `true` if the task has completed and
   * *         [.finishTask]  should be called.
   */
  @Throws(SQLException::class)
  fun <U> startTask(transaction: ProcessTransaction, messageService: IMessageService<U, ProcessTransaction, in V>): V

  /**
   * Called by the processEngine to signify to the task that it is finished
   * (with the given payload).

   * @param payload The payload which is the result of the processing.
   */
  @Throws(SQLException::class)
  fun finishTask(transaction: ProcessTransaction, payload: Node? = null): V

  /**
   * Called to signify that this task has failed.
   */
  @Throws(SQLException::class)
  fun failTask(transaction: ProcessTransaction, cause: Throwable): V

  /**
   * Called to signify that creating this task has failed, a retry would be expected.
   */
  @Throws(SQLException::class)
  fun failTaskCreation(transaction: ProcessTransaction, cause: Throwable): V

  /**
   * Called to signify that this task has been cancelled.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun cancelTask(transaction: ProcessTransaction): V

  /**
   * Called to attempt to cancel the task if that is semantically valid.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun tryCancelTask(transaction: ProcessTransaction): V

  /** Get the predecessor instance with the given node name.
   * @throws SQLException
   * *
   */
  @Throws(SQLException::class)
  fun resolvePredecessor(transaction: ProcessTransaction, nodeName: String): V?

  /** Get the result instance with the given data name.  */
  @Throws(SQLException::class)
  fun getResult(transaction: ProcessTransaction, name: String): ProcessData?

}