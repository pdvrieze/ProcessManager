package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.HandleAware;


public class Message implements IMessage, HandleAware {

  private static final long serialVersionUID = 3875411205581115538L;
  private long aHandle = -1;
  private final String aPayload;
  private final long aReplyTo;
  private final long aProcessInstanceHandle;

  public Message(ProcessInstance pProcessInstance, String pPayload) {
    this(pProcessInstance, pPayload, -1);
  }
  
  public Message(ProcessInstance pProcessInstance, String pPayload, long pReplyTo) {
    aPayload = pPayload;
    aReplyTo = pReplyTo;
    aProcessInstanceHandle = pProcessInstance.getHandle();
  }
  
  public Message(HProcessInstance pProcessInstance, String pPayload, long pReplyTo) {
    aPayload = pPayload;
    aReplyTo = pReplyTo;
    aProcessInstanceHandle = pProcessInstance.getHandle();
  }

  @Override
  public IProcessCursor getCursor() {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException("Not yet implemented");

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

  @Override
  public long getProcessInstanceHandle() {
    return aProcessInstanceHandle;
  }

  @Override
  public boolean isValidReply(IMessage pMessage) {
    return pMessage.getReplyTo()==aHandle;
  }

  public static IMessage complete(ProcessInstance pProcessInstance, long pHandle) {
    return new Message(pProcessInstance, null, pHandle);
  }
  
  @Override
  public String toString() {
    return aHandle+": "+aPayload;
  }

  @Override
  public long getReplyTo() {
    return aReplyTo;
  }

  public static IMessage complete(HProcessInstance pProcessInstance, long pHandle) {
    return new Message(pProcessInstance, null, pHandle);
  }

  public boolean hasHandle() {
    return aHandle>=0;
  }

}
