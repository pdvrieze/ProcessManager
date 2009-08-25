package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.HandleAware;


public class ExtMessage implements IExtMessage {

  private static final long serialVersionUID = 3875411205581115538L;
  private final long aHandle;
  private final Payload aPayload;
  private final long aReplyTo;
  private final long aProcessInstanceHandle;

  public ExtMessage(ProcessInstance pProcessInstance, long pHandle, Payload pPayload) {
    this(pProcessInstance, pHandle, pPayload, -1);
  }
  
  public ExtMessage(ProcessInstance pProcessInstance, long pHandle, Payload pPayload, long pReplyTo) {
    aHandle = pHandle;
    aPayload = pPayload;
    aReplyTo = pReplyTo;
    aProcessInstanceHandle = pProcessInstance.getHandle();
  }
  
  public ExtMessage(HProcessInstance pProcessInstance, long pHandle, Payload pPayload, long pReplyTo) {
    aHandle = pHandle;
    aPayload = pPayload;
    aReplyTo = pReplyTo;
    aProcessInstanceHandle = pProcessInstance.getHandle();
  }

  @Override
  public long getHandle() {
    if (aHandle<0) {
      throw new IllegalStateException("Handle unset");
    }
    return aHandle;
  }
  
  @Override
  public long getProcessInstanceHandle() {
    return aProcessInstanceHandle;
  }

  @Override
  public boolean isValidReply(IExtMessage pMessage) {
    return pMessage.getReplyTo()==aHandle;
  }

  public static IExtMessage complete(ProcessInstance pProcessInstance, long pHandle) {
    return new ExtMessage(pProcessInstance, -1, null, pHandle);
  }
  
  @Override
  public String toString() {
    return aHandle+": "+aPayload;
  }

  @Override
  public long getReplyTo() {
    return aReplyTo;
  }

  public static IExtMessage complete(HProcessInstance pProcessInstance, long pHandle) {
    return new ExtMessage(pProcessInstance, -1, null, pHandle);
  }

  public boolean hasHandle() {
    return aHandle>=0;
  }

  @Override
  public Payload getPayload() {
    return aPayload;
  }

}
