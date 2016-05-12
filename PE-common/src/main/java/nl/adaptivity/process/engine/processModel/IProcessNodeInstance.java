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

package nl.adaptivity.process.engine.processModel;

import net.devrieze.util.HandleMap.ComparableHandle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.StringUtil;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlWriter;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlRootElement;

import java.sql.SQLException;


/**
 * Class representing the instantiation of an executable process node.
 *
 * @author Paul de Vrieze
 * @param <V> The actual type of the implementing class.
 */
public interface IProcessNodeInstance<T extends Transaction, V extends IProcessNodeInstance<T, V>> extends HandleAware<V>, ComparableHandle<V> {

  void serialize(T transaction, XmlWriter out) throws XmlException;

  /**
   * Enumeration representing the various states a task can be in.
   *
   * @author Paul de Vrieze
   */
  @XmlRootElement(name = "taskState", namespace = "http://adaptivity.nl/userMessageHandler")
  enum NodeInstanceState {
    /**
     * Initial task state. The instance has been created, but has not been successfully sent to a receiver.
     */
    Pending,
    /**
     * Signifies that the task has failed to be created, a new attempt should be made.
     */
    FailRetry, /**
     * Indicates that the task has been communicated to a
     * handler.
     */
    Sent,
    /**
     * State acknowledging reception of the task. Note that this is generally
     * only used by process aware services. It signifies that a task has been
     * received, but processing has not started yet.
     */
    Acknowledged,
    /**
     * Some tasks allow for alternatives (different users). Taken signifies that
     * the task has been claimed and others can not claim it anymore (unless
     * released again).
     */
    Taken,
    /**
     * Signifies that work on the task has actually started.
     */
    Started,
    /**
     * Signifies that the task is complete. This generally is the end state of a
     * task.
     */
    Complete,
    /**
     * Signifies that the task has failed for some reason.
     */
    Failed,
    /**
     * Signifies that the task has been cancelled (but not through a failure).
     */
    Cancelled;

    public static NodeInstanceState fromString(CharSequence string) {
      String lowerCase = StringUtil.toLowerCase(string);
      for(NodeInstanceState candidate: values()) {
        if (lowerCase.equals(candidate.name().toLowerCase())) {
          return candidate;
        }
      }
      return null;
    }
  }

  /**
   * Get the state of the task.
   *
   * @return the state.
   */
  NodeInstanceState getState();

  /**
   * Set the state of the task.
   *
   * @param transaction
   * @param newState The new state of the task.
   */
  void setState(T transaction, NodeInstanceState newState) throws SQLException;

  /**
   * Called by the processEngine so indicate starting of the task.
   *
   * @param messageService Service to use for communication of change of state.
   * @return <code>true</code> if this stage is complete and the engine should
   *         progress to {
   * @throws SQLException @link #takeTask(IMessageService)

   */
  <U> boolean provideTask(T transaction, IMessageService<U, T, V> messageService) throws SQLException;

  /**
   * Called by the processEngine to let the task be taken.
   *
   * @param messageService Service to use for communication of change of state.
   * @return <code>true</code> if this stage has completed and the task should
   *         be {@link #startTask(Transaction, IMessageService) started}.
   */
  <U> boolean takeTask(T transaction, IMessageService<U, T, V> messageService) throws SQLException;

  /**
   * Called by the processEngine to let the system start the task.
   *
   * @param messageService Service to use for communication of change of state.
   * @return <code>true</code> if the task has completed and
   *         {@link #finishTask(Transaction, Node)}  should be called.
   */
  <U> boolean startTask(T transaction, IMessageService<U, T, V> messageService) throws SQLException;

  /**
   * Called by the processEngine to signify to the task that it is finished
   * (with the given payload).
   *
   * @param payload The payload which is the result of the processing.
   */
  void finishTask(T transaction, Node payload) throws SQLException;

  /**
   * Called to signify that this task has failed.
   */
  void failTask(T transaction, Throwable cause) throws SQLException;

  /**
   * Called to signify that creating this task has failed, a retry would be expected.
   */
  void failTaskCreation(T transaction, Throwable cause) throws SQLException;

  /**
   * Called to signify that this task has been cancelled.
   * @throws SQLException
   */
  void cancelTask(T transaction) throws SQLException;

  /**
   * Called to attempt to cancel the task if that is semantically valid.
   * @throws SQLException
   */
  void tryCancelTask(T transaction) throws SQLException;

  /** Get the predecessor instance with the given node name.
   * @throws SQLException
   * */
  IProcessNodeInstance<T, V> getPredecessor(T transaction, String nodeName) throws SQLException;

  /** Get the result instance with the given data name. */
  ProcessData getResult(T transaction, String name) throws SQLException;
}
