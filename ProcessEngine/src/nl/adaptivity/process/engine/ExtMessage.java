package nl.adaptivity.process.engine;



public class ExtMessage {

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

  public long getHandle() {
    if (aHandle<0) {
      throw new IllegalStateException("Handle unset");
    }
    return aHandle;
  }
  
  public long getProcessInstanceHandle() {
    return aProcessInstanceHandle;
  }
  
  @Override
  public String toString() {
    return aHandle+": "+aPayload;
  }

  public long getReplyTo() {
    return aReplyTo;
  }

  public static ExtMessage complete(HProcessInstance pProcessInstance, long pHandle) {
    return new ExtMessage(pProcessInstance, -1, null, pHandle);
  }

  public boolean hasHandle() {
    return aHandle>=0;
  }

  public Payload getPayload() {
    return aPayload;
  }

}
