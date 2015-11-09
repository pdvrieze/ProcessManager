package nl.adaptivity.process.exec;

import java.sql.SQLException;

import javax.xml.bind.annotation.XmlRootElement;

import net.devrieze.util.Transaction;
import org.w3c.dom.Node;

import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.db.DBTransaction;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessData;


/**
 * Class representing the instantiation of an executable process node.
 *
 * @author Paul de Vrieze
 * @param <V> The actual type of the implementing class.
 */
public interface IProcessNodeInstance<V extends IProcessNodeInstance<V>> extends HandleAware<V> {

  /**
   * Enumeration representing the various states a task can be in.
   *
   * @author Paul de Vrieze
   */
  @XmlRootElement(name = "taskState", namespace = "http://adaptivity.nl/userMessageHandler")
  enum TaskState {
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
    Cancelled
  }

  /**
   * Get the state of the task.
   *
   * @return the state.
   */
  TaskState getState();

  /**
   * Set the state of the task.
   *
   * @param aNewState The new state of the task.
   * @param pTransaction
   * @param pNewState
   */
  void setState(Transaction pTransaction, TaskState pNewState) throws SQLException;

  /**
   * Called by the processEngine so indicate starting of the task.
   *
   * @param pMessageService Service to use for communication of change of state.
   * @return <code>true</code> if this stage is complete and the engine should
   *         progress to {
   * @throws SQLException @link #takeTask(IMessageService)

   */
  <T> boolean provideTask(Transaction pTransaction, IMessageService<T, V> pMessageService) throws SQLException;

  /**
   * Called by the processEngine to let the task be taken.
   *
   * @param pMessageService Service to use for communication of change of state.
   * @return <code>true</code> if this stage has completed and the task should
   *         be {@link #startTask(IMessageService) started}.
   */
  <T> boolean takeTask(Transaction pTransaction, IMessageService<T, V> pMessageService) throws SQLException;

  /**
   * Called by the processEngine to let the system start the task.
   *
   * @param pMessageService Service to use for communication of change of state.
   * @return <code>true</code> if the task has completed and
   *         {@link #finishTask(Node)} should be called.
   */
  <T> boolean startTask(Transaction pTransaction, IMessageService<T, V> pMessageService) throws SQLException;

  /**
   * Called by the processEngine to signify to the task that it is finished
   * (with the given payload).
   *
   * @param pPayload The payload which is the result of the processing.
   */
  void finishTask(Transaction pTransaction, Node pPayload) throws SQLException;

  /**
   * Called to signify that this task has failed.
   */
  void failTask(Transaction pTransaction, Throwable pCause) throws SQLException;

  /**
   * Called to signify that creating this task has failed, a retry would be expected.
   */
  void failTaskCreation(Transaction pTransaction, Throwable pCause) throws SQLException;

  /**
   * Called to signify that this task has been cancelled.
   * @throws SQLException
   */
  void cancelTask(Transaction pTransaction) throws SQLException;

  /** Get the predecessor instance with the given node name.
   * @throws SQLException
   * */
  IProcessNodeInstance<V> getPredecessor(Transaction pTransaction, String pNodeName) throws SQLException;

  /** Get the result instance with the given data name. */
  ProcessData getResult(Transaction pTransaction, String pName) throws SQLException;
}
