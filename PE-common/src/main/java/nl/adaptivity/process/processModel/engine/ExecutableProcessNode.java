package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Transaction;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.IProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;

import java.sql.SQLException;
import java.util.List;


/**
 * Created by pdvrieze on 23/11/15.
 */
public interface ExecutableProcessNode extends ProcessNode<ExecutableProcessNode, ProcessModelImpl> {

  /**
   * Should this node be able to be provided?
   *
   *
   * @param transaction
   * @param instance The instance against which the condition should be evaluated.
   * @return <code>true</code> if the node can be started, <code>false</code> if
   *         not.
   */
  boolean condition(Transaction transaction, IProcessNodeInstance<?> instance);

  /**
   * Take action to make task available
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically taken
   */
  <T, U extends IProcessNodeInstance<U>> boolean provideTask(Transaction transaction, IMessageService<T, U> messageService, U instance) throws SQLException;

  /**
   * Take action to accept the task (but not start it yet)
   *
   * @param messageService The message service to use for the communication.
   * @param instance The processnode instance involved.
   * @return <code>true</code> if the task can/must be automatically started
   */
  <T, U extends IProcessNodeInstance<U>> boolean takeTask(IMessageService<T, U> messageService, U instance);

  <T, U extends IProcessNodeInstance<U>> boolean startTask(IMessageService<T, U> messageService, U instance);


  List<? extends XmlResultType> getResults();

  List<? extends XmlDefineType> getDefines();

  void setId(String s);
}
