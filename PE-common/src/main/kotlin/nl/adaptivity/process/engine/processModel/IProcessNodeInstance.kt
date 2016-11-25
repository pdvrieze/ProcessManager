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

import net.devrieze.util.HandleMap
import net.devrieze.util.StringUtil
import net.devrieze.util.Transaction
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import org.w3c.dom.Node
import java.sql.SQLException
import javax.xml.bind.annotation.XmlRootElement


/**
 * Class representing the instantiation of an executable process node.

 * @author Paul de Vrieze
 * *
 * @param <V> The actual type of the implementing class.
</V> */
interface IProcessNodeInstance<T : Transaction, V : IProcessNodeInstance<T, V>> : HandleMap.ReadableHandleAware<SecureObject<V>> {

  @Throws(XmlException::class)
  fun serialize(transaction: T, out: XmlWriter)

  /**
   * Enumeration representing the various states a task can be in.

   * @author Paul de Vrieze
   */
  @XmlRootElement(name = "taskState", namespace = "http://adaptivity.nl/userMessageHandler")
  enum class NodeInstanceState private constructor(val isFinal: Boolean) {
    /**
     * Initial task state. The instance has been created, but has not been successfully sent to a receiver.
     */
    Pending(false),
    /**
     * Signifies that the task has failed to be created, a new attempt should be made.
     */
    FailRetry(false),
    /**
     * Indicates that the task has been communicated to a
     * handler, but receipt has not been acknowledged.
     */
    Sent(false),
    /**
     * State acknowledging reception of the task. Note that this is generally
     * only used by process aware services. It signifies that a task has been
     * received, but processing has not started yet.
     */
    Acknowledged(false),
    /**
     * Some tasks allow for alternatives (different users). Taken signifies that
     * the task has been claimed and others can not claim it anymore (unless
     * released again).
     */
    Taken(false),
    /**
     * Signifies that work on the task has actually started.
     */
    Started(false),
    /**
     * Signifies that the task is complete. This generally is the end state of a
     * task.
     */
    Complete(true),
    /**
     * Signifies that the task has failed for some reason.
     */
    Failed(true),
    /**
     * Signifies that the task has been cancelled (but not through a failure).
     */
    Cancelled(true);


    companion object {

      @JvmStatic
      fun fromString(string: CharSequence): NodeInstanceState? {
        val lowerCase = StringUtil.toLowerCase(string)
        for (candidate in values()) {
          if (lowerCase == candidate.name.toLowerCase()) {
            return candidate
          }
        }
        return null
      }
    }
  }

  /**
   * Get the state of the task.

   * @return the state.
   */
  val state: NodeInstanceState

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
  fun <U> provideTask(transaction: T, messageService: IMessageService<U, T, V>): V

  /**
   * Called by the processEngine to let the task be taken.

   * @param messageService Service to use for communication of change of state.
   * *
   * @return `true` if this stage has completed and the task should
   * *         be [started][.startTask].
   */
  @Throws(SQLException::class)
  fun <U> takeTask(transaction: T, messageService: IMessageService<U, T, V>): V

  /**
   * Called by the processEngine to let the system start the task.

   * @param messageService Service to use for communication of change of state.
   * *
   * @return `true` if the task has completed and
   * *         [.finishTask]  should be called.
   */
  @Throws(SQLException::class)
  fun <U> startTask(transaction: T, messageService: IMessageService<U, T, V>): V

  /**
   * Called by the processEngine to signify to the task that it is finished
   * (with the given payload).

   * @param payload The payload which is the result of the processing.
   */
  @Throws(SQLException::class)
  fun finishTask(transaction: T, payload: Node? = null): V

  /**
   * Called to signify that this task has failed.
   */
  @Throws(SQLException::class)
  fun failTask(transaction: T, cause: Throwable): V

  /**
   * Called to signify that creating this task has failed, a retry would be expected.
   */
  @Throws(SQLException::class)
  fun failTaskCreation(transaction: T, cause: Throwable): V

  /**
   * Called to signify that this task has been cancelled.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun cancelTask(transaction: T): V

  /**
   * Called to attempt to cancel the task if that is semantically valid.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun tryCancelTask(transaction: T): V

  /** Get the predecessor instance with the given node name.
   * @throws SQLException
   * *
   */
  @Throws(SQLException::class)
  fun resolvePredecessor(transaction: T, nodeName: String): V?

  /** Get the result instance with the given data name.  */
  @Throws(SQLException::class)
  fun getResult(transaction: T, name: String): ProcessData?
}
