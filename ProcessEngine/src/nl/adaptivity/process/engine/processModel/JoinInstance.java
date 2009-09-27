package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.engine.ProcessInstance;
import nl.adaptivity.process.processModel.Join;


public class JoinInstance extends ProcessNodeInstance {

  public JoinInstance(Join pNode, Collection<ProcessNodeInstance> pPredecessors, ProcessInstance pProcessInstance) {
    super(pNode, null, pPredecessors, pProcessInstance);
  }

  private int aComplete = 0;
  private int aSkipped = 0;
  private boolean aFinished = false;

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

  public void addPredecessor(ProcessNodeInstance pPredecessor) {
    super.getPredecessors().add(pPredecessor);
  }

  public boolean isFinished() {
    return aFinished;
  }

  public void setFinished() {
    aFinished=true;
  }


}
