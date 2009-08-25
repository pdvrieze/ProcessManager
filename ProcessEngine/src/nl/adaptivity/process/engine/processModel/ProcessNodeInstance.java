package nl.adaptivity.process.engine.processModel;

import java.util.Collection;

import nl.adaptivity.process.engine.InternalMessage;
import nl.adaptivity.process.engine.Payload;
import nl.adaptivity.process.engine.ProcessInstance;


public class ProcessNodeInstance {
  
  private final ProcessNode aNode;
  private final InternalMessage aMessage;
  private Payload aPayload;
  private final Collection<ProcessNodeInstance> aPredecessors;

  public ProcessNodeInstance(ProcessNode pNode, InternalMessage pMessage, Collection<ProcessNodeInstance> pPredecessor) {
    super();
    aNode = pNode;
    aMessage = pMessage;
    aPredecessors = pPredecessor;
    if (aPredecessors==null && ! (pNode instanceof StartNode)) {
      throw new NullPointerException();
    }
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public InternalMessage getMessage() {
    return aMessage;
  }

  public void finish(Payload pPayload, ProcessInstance pProcessInstance) {
    aPayload = pPayload;
    pProcessInstance.finishThread(this);
  }

  public Payload getPayload() {
    return aPayload;
  }

  protected Collection<ProcessNodeInstance> getPredecessors() {
    return aPredecessors;
  }
  
}
