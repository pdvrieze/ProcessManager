package nl.adaptivity.process.engine.processModel;

import java.sql.SQLException;
import java.util.Collection;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.Transaction;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.engine.JoinImpl;


public class JoinInstance extends ProcessNodeInstance {

  public JoinInstance(Transaction transaction, final JoinImpl node, final Collection<? extends Handle<? extends ProcessNodeInstance>> predecessors, final ProcessInstance processInstance) throws SQLException {
    super(node, predecessors, processInstance);
    for (final Handle<? extends ProcessNodeInstance> hpredecessor : predecessors) {
      ProcessNodeInstance predecessor = processInstance.getEngine().getNodeInstance(transaction, hpredecessor, SecurityProvider.SYSTEMPRINCIPAL);
      if (predecessor.getState() == TaskState.Complete) {
        aComplete += 1;
      } else {
        aSkipped += 1;
      }
    }
  }

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param node
   * @param processInstance
   */
  JoinInstance(JoinImpl node, ProcessInstance processInstance, TaskState state) {
    super(node, processInstance, state);
  }

  private int aComplete = 0;

  private int aSkipped = 0;

  public void incComplete() {
    aComplete++;
  }

  public int getTotal() {
    return aComplete + aSkipped;
  }

  public int getComplete() {
    return aComplete;
  }

  public void incSkipped() {
    aSkipped++;
  }

  @Override
  public JoinImpl getNode() {
    return (JoinImpl) super.getNode();
  }

  public boolean addPredecessor(Transaction transaction, final ProcessNodeInstance predecessor) throws SQLException {
    if (canAddNode(transaction)) {
      getDirectPredecessors().add(predecessor);
      if (predecessor.getState() == TaskState.Complete) {
        aComplete += 1;
      } else {
        aSkipped += 1;
      }
      return true;
    }
    return false;
  }

  public boolean isFinished() {
    return (getState() == TaskState.Complete) || (getState() == TaskState.Failed);
  }

  @Override
  public <T> boolean startTask(Transaction transaction, final IMessageService<T, ProcessNodeInstance> messageService) {
    final JoinImpl join = getNode();
    if (join.startTask(messageService, this)) {
      if (getTotal() == join.getPredecessors().size()) {
        return true;
      }
      if (aComplete >= join.getMin()) {
        if (aComplete >= join.getMax()) {
          return true;
        }
        try {
          return getProcessInstance().getActivePredecessorsFor(transaction, join).size() == 0;
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      return false;
    }
    return false;
  }

  @Override
  public <T> boolean provideTask(Transaction transaction, final IMessageService<T, ProcessNodeInstance> messageService) throws SQLException {
    if (!isFinished()) {
      return getNode().provideTask(transaction, messageService, this);
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(transaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(transaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

  private boolean canAddNode(Transaction transaction) throws SQLException {
    if (!isFinished()) {
      return true;
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(transaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(transaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

}
