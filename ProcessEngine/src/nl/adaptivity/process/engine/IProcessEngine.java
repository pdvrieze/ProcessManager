package nl.adaptivity.process.engine;

public interface IProcessEngine{
  
  public HProcessInstance startProcess(ProcessModel pModel);

  public void postMessage(MessageHandle pMessageHandle, ExtMessage pMessage) throws InvalidMessageException;

  public void fireMessage(InternalMessage pMessage);

  public void setMessageListener(ProcessMessageListener pProcessEngine);

  public void finishInstance(ProcessInstance pProcessInstance);

  public void cancelAll();

  @Deprecated
  public long ensureMessageHandle(InternalMessage pMessage);
}
