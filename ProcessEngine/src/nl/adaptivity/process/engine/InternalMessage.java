package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.HandleAware;


public class InternalMessage implements HandleAware {

  private static final long serialVersionUID = 3875411205581115538L;
  private long aHandle = -1;
  private final Payload aPayload;
  private final ProcessInstance aProcessInstance;
  
  public InternalMessage(ProcessInstance pProcessInstance, Payload pPayload) {
    aPayload = pPayload;
    aProcessInstance = pProcessInstance;
  }

  @Override
  public long getHandle() {
    if (aHandle<0) {
      throw new IllegalStateException("Handle unset");
    }
    return aHandle;
  }
  
  public void setHandle(long pHandle) {
    aHandle = pHandle;
  }

  public ProcessInstance getProcessInstance() {
    return aProcessInstance;
  }

  public boolean isValidReply(ExtMessage pMessage) {
    return pMessage.getReplyTo()==aHandle;
  }
  
  @Override
  public String toString() {
    return aHandle+": "+aPayload;
  }

  public boolean hasHandle() {
    return aHandle>=0;
  }

  public ExtMessage externalize() {
    return new ExtMessage(aProcessInstance, aHandle, aPayload, -1);
  }

}
