package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import nl.adaptivity.process.engine.ProcessInstance;


public class Join extends ProcessNode{

  private static final long serialVersionUID = -8598245023280025173L;

  private final Collection<ProcessNode> aNodes;
  private final int aMin;
  private final int aMax;

  private boolean aFinished = false;

  private Join(Collection<ProcessNode> pNodes, int pMin, int pMax) {
    super(pNodes);
    aNodes = pNodes;
    aMin = pMin;
    aMax = Math.min(pNodes.size(), pMax);
    if (getMin() < 1 || getMin() > pNodes.size() || pMax < pMin) {
      throw new IllegalProcessModelException("Join range ("+pMin+", "+pMax+") must be sane");
    }
  }

  public static Join andJoin(ProcessNode... pNodes) {
    return new Join(Arrays.asList(pNodes), pNodes.length, pNodes.length);
  }

  public int getMax() {
    return aMax;
  }

  public int getMin() {
    return aMin;
  }

  @Override
  public boolean condition() {
    return true;
  }

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    if (aFinished) {
      return;
    }
    JoinInstance j = pProcessInstance.getInstance(this);
    j.incComplete();
    if (j.getComplete()>=aMin || j.getTotal()>=aMax) {
      cancelPredecessors(pProcessInstance);
      aFinished = true;
      pThreads.remove(j);
      pProcessInstance.removeJoin(j);
      startSuccessors(pThreads, pProcessInstance);
    } else {
      pThreads.add(j);
    }
  }

  @Override
  public void skip(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    if (aFinished) {
      return;
    }
    JoinInstance j = pProcessInstance.getInstance(this);
    j.incSkipped();
    throw new UnsupportedOperationException("Not yet correct");
//    if(j.getTotal()>=aMax) {
//      cancelPredecessors(pProcessInstance);
//      aFinished = true;
//      pThreads.remove(j);
//      pProcessInstance.removeJoin(j);
//      return startSuccessors(pProcessInstance);
//    }
//    return Arrays.<ProcessNodeInstance>asList(pProcessInstance.getInstance(this));
  }

  private void cancelPredecessors(ProcessInstance pProcessInstance) {
    // TODO Auto-generated method stub
    // 
  }

}
