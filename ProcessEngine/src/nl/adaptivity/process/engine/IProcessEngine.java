package nl.adaptivity.process.engine;

public interface IProcessEngine{
  
  public HProcessInstance startProcess(ProcessModel pModel);

  public void postMessage(MessageHandle pMessageHandle, IMessage pMessage) throws InvalidMessageException;

  public void fireMessage(Message pMessage);

  public void setMessageListener(ProcessMessageListener pProcessEngine);

  public void finishInstance(ProcessInstance pProcessInstance);

  public void cancelAll();

  public long ensureMessageHandle(Message pMessage);
}
