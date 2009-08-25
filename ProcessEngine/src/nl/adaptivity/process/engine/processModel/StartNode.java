package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.engine.ProcessInstance;



public class StartNode extends ProcessNode {

  public StartNode() {
    super((ProcessNode) null);
  }

  private static final long serialVersionUID = 7779338146413772452L;

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance, ProcessNodeInstance pPredecessor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean condition() {
    return true;
  }

}
