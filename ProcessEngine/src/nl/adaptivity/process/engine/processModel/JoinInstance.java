package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.Join;


public class JoinInstance extends ProcessNodeInstance {

  public JoinInstance(Join pNode, Collection<ProcessNodeInstance> pPredecessors, ProcessInstance pProcessInstance) {
    super(pNode, pPredecessors, pProcessInstance);
    for (ProcessNodeInstance predecessor: pPredecessors) {
      if (predecessor.getState()==TaskState.Complete) {
        aComplete+=1;
      } else {
        aSkipped+=1;
      }
    }
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

  public boolean addPredecessor(ProcessNodeInstance pPredecessor) {
    if (canAddNode()) {
      getDirectPredecessors().add(pPredecessor);
      if (pPredecessor.getState()==TaskState.Complete) {
        aComplete+=1;
      } else {
        aSkipped+=1;
      }
      return true;
    }
    return false;
  }

  public boolean isFinished() {
    return getState()==TaskState.Complete|| getState()==TaskState.Failed;
  }

  @Override
  public <T> boolean startTask(IMessageService<T, ProcessNodeInstance> pMessageService) {
    final Join join = getNode();
    if (join.startTask(pMessageService, this)) {
      if (getTotal()==join.getPredecessors().size()) {
        return true;
      }
      if (aComplete>=join.getMin()) {
        if (aComplete>=join.getMax()) {
          return true;
        }
        return getProcessInstance().getActivePredecessorsFor(join).size()==0;
      }
      return false;
    }
    return false;
  }

  @Override
  public <T> boolean provideTask(IMessageService<T, ProcessNodeInstance> pMessageService) {
    if (! isFinished()) {
      return getNode().provideTask(pMessageService, this);
    }
    Collection<ProcessNodeInstance> directSuccessors = getProcessInstance().getDirectSuccessors(this);
    boolean canAdd = false;
    for(ProcessNodeInstance directSuccessor: directSuccessors) {
      if (directSuccessor.getState()==TaskState.Started || directSuccessor.getState()==TaskState.Complete) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

  private boolean canAddNode() {
    if (! isFinished()) {
      return true;
    }
    Collection<ProcessNodeInstance> directSuccessors = getProcessInstance().getDirectSuccessors(this);
    boolean canAdd = false;
    for(ProcessNodeInstance directSuccessor: directSuccessors) {
      if (directSuccessor.getState()==TaskState.Started || directSuccessor.getState()==TaskState.Complete) {
        canAdd = false;
        break;
      }
      canAdd = true;
    }
    return canAdd;
  }

}
