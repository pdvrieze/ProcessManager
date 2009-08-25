package nl.adaptivity.process.engine.processModel;

import nl.adaptivity.process.engine.IMessage;
import nl.adaptivity.process.engine.ProcessInstance;


public class ProcessNodeInstance {
  
  private final ProcessNode aNode;
  private final long aMessageHandle;

  public ProcessNodeInstance(ProcessNode pNode, long pMessageHandle) {
    super();
    aNode = pNode;
    aMessageHandle = pMessageHandle;
  }

  public ProcessNode getNode() {
    return aNode;
  }

  public long getMessageHandle() {
    return aMessageHandle;
  }

  public void finish(IMessage pMessage, ProcessInstance pProcessInstance) {
    pProcessInstance.finishThread(this);
  }

}
