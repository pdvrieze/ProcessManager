package nl.adaptivity.process.engine.processModel;

import nl.adaptivity.process.engine.InternalMessage;
import nl.adaptivity.process.engine.Payload;
import nl.adaptivity.process.engine.ProcessInstance;


public class ProcessNodeInstance {
  
  private final ProcessNode aNode;
  private final InternalMessage aMessage;
  private Payload aPayload;

  public ProcessNodeInstance(ProcessNode pNode, InternalMessage pMessage) {
    super();
    aNode = pNode;
    aMessage = pMessage;
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

}
