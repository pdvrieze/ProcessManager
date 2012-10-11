package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.Join;


public class JoinInstance extends ProcessNodeInstance {

  public JoinInstance(final Join pNode, final Collection<? extends Handle<? extends ProcessNodeInstance>> pPredecessors, final ProcessInstance pProcessInstance) {
    super(pNode, pPredecessors, pProcessInstance);
    for (final Handle<? extends ProcessNodeInstance> hpredecessor : pPredecessors) {
      ProcessNodeInstance predecessor = pProcessInstance.getEngine().getNodeInstance(hpredecessor.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
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
  JoinInstance(Join pNode, ProcessInstance pProcessInstance, TaskState pState) {
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
  public Join getNode() {
    return (Join) super.getNode();
  }

  public boolean addPredecessor(final ProcessNodeInstance pPredecessor) {
    if (canAddNode()) {
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
  public <T> boolean startTask(final IMessageService<T, ProcessNodeInstance> pMessageService) {
    final Join join = getNode();
    if (join.startTask(pMessageService, this)) {
      if (getTotal() == join.getPredecessors().size()) {
        return true;
      }
      if (aComplete >= join.getMin()) {
        if (aComplete >= join.getMax()) {
          return true;
        }
        return getProcessInstance().getActivePredecessorsFor(join).size() == 0;
      }
      return false;
    }
    return false;
  }

  @Override
  public <T> boolean provideTask(final IMessageService<T, ProcessNodeInstance> pMessageService) {
    if (!isFinished()) {
      return getNode().provideTask(pMessageService, this);
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(hDirectSuccessor.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

  private boolean canAddNode() {
    if (!isFinished()) {
      return true;
    }
    final Collection<? extends Handle<? extends ProcessNodeInstance>> directSuccessors = getProcessInstance().getDirectSuccessors(this);
    boolean canAdd = false;
    for (final Handle<? extends ProcessNodeInstance> hDirectSuccessor : directSuccessors) {
      ProcessNodeInstance directSuccessor = getProcessInstance().getEngine().getNodeInstance(hDirectSuccessor.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
      if ((directSuccessor.getState() == TaskState.Started) || (directSuccessor.getState() == TaskState.Complete)) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

}
