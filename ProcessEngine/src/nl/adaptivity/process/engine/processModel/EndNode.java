package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.engine.ProcessInstance;



public class EndNode extends ProcessNode{

  public EndNode(ProcessNode pPrevious) {
    super(pPrevious);
  }

  private static final long serialVersionUID = 220908810658246960L;

  @Override
  public boolean condition() {
    return true;
  }

  @Override
  public void start(Collection<ProcessNodeInstance> pThreads, ProcessInstance pProcessInstance) {
    pProcessInstance.finish();
  }

}
