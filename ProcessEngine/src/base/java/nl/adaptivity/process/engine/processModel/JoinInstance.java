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

  public JoinInstance(Transaction pTransaction, final JoinImpl pNode, final Collection<? extends Handle<? extends ProcessNodeInstance>> pPredecessors, final ProcessInstance pProcessInstance) throws SQLException {
    super(pNode, pPredecessors, pProcessInstance);
    for (final Handle<? extends ProcessNodeInstance> hpredecessor : pPredecessors) {
      ProcessNodeInstance predecessor = pProcessInstance.getEngine().getNodeInstance(pTransaction, hpredecessor, SecurityProvider.SYSTEMPRINCIPAL);
      if (predecessor.getState() == TaskState.Complete) {
        aComplete += 1;
      } else {
        aSkipped += 1;
      }
    }
  }

  /**
   * Constructor for ProcessNodeInstanceMap.
   * @param pNode
   * @param pProcessInstance
   */
  JoinInstance(JoinImpl pNode, ProcessInstance pProcessInstance, TaskState pState) {
    super(pNode, pProcessInstance, pState);
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

  public boolean addPredecessor(Transaction pTransaction, final ProcessNodeInstance pPredecessor) throws SQLException {
    if (canAddNode(pTransaction)) {
      getDirectPredecessors().add(pPredecessor);
      if (pPredecessor.getState() == TaskState.Complete) {
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
  public <T> boolean startTask(Transaction pTransaction, final IMessageService<T, ProcessNodeInstance> pMessageService) {
    final JoinImpl join = getNode();
    if (join.startTask(pMessageService, this)) {
      if (getTotal() == join.getPredecessors().size()) {
        return true;
      }
      if (aComplete >= join.getMin()) {
        if (aComplete >= join.getMax()) {
          return true;
        }
        try {
          return getProcessInstance().getActivePredecessorsFor(pTransaction, join).size() == 0;
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      return false;
    }
    return false;
  }

  @Override
  public <T> boolean provideTask(Transaction pTransaction, final IMessageService<T, ProcessNodeInstance> pMessageService) throws SQLException {
    if (!isFinished()) {
      return getNode().provideTask(pTransaction, pMessageService, this);
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(pTransaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(pTransaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

  private boolean canAddNode(Transaction pTransaction) throws SQLException {
    if (!isFinished()) {
      return true;
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(pTransaction, this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(pTransaction, hDirectSuccessor, SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

}
