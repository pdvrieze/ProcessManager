package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap;

import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;


public class ProcessEngine implements IProcessEngine {

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();
  
  private final HandleMap<InternalMessage> aMessageMap = new HandleMap<InternalMessage>();
  
  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private ProcessMessageListener aMessageListener;

  @Override
  public HProcessInstance startProcess(ProcessModel pModel, Payload pPayload) {
    ProcessInstance instance = pModel.createInstance(this, pPayload);
    HProcessInstance result = new HProcessInstance(aInstanceMap.put(instance));
    instance.start();
    return result;
  }

  @Override
  public void postMessage(MessageHandle pHOrigMessage, ExtMessage pMessage) throws InvalidMessageException {
    // Get the (outgoing) message this is a reply to
    InternalMessage repliedMessage = retrieveMessage(pHOrigMessage);
    verifyMessage(repliedMessage, pMessage);

    final ProcessInstance processInstance = repliedMessage.getProcessInstance();

    ProcessNodeInstance pos = processInstance.getProcesNodeInstanceFor(repliedMessage);
    
    pos.finish(pMessage.getPayload(), processInstance);
    aMessageMap.remove(pHOrigMessage.getHandle());
  }

  private void verifyMessage(InternalMessage pRepliedMessage, ExtMessage pMessage) throws InvalidMessageException {
    if (pRepliedMessage==null) {
      throw new InvalidMessageException("The message replied to can not be found", pMessage);
    }
    if (! pRepliedMessage.isValidReply(pMessage)) {
      throw new InvalidMessageException("The message is not in reply to the original", pMessage);
    }
    
    // TODO further validation
  }

  private InternalMessage retrieveMessage(MessageHandle pHandle) {
    return aMessageMap.get(pHandle.getHandle());
  }

  @Override
  public void fireMessage(InternalMessage pMessage) {
    ensureMessageHandle(pMessage);
    
    ExtMessage extVersion = pMessage.externalize();
    
    getMessageListener().fireMessage(extVersion);
  }

  public void setMessageListener(ProcessMessageListener messageListener) {
    aMessageListener = messageListener;
  }

  public ProcessMessageListener getMessageListener() {
    return aMessageListener;
  }

  @Override
  public void finishInstance(ProcessInstance pProcessInstance) {
    aMessageListener.fireFinishedInstance(pProcessInstance.getHandle());
    aInstanceMap.remove(pProcessInstance);
  }

  @Override
  public void cancelAll() {
    for(ProcessInstance instance: aInstanceMap) {
      aMessageListener.cancelInstance(instance.getHandle());
      aInstanceMap.remove(instance);
    }
  }

  private long ensureMessageHandle(InternalMessage pMessage) {
    if (pMessage.hasHandle()) {
      return pMessage.getHandle();
    }
    return aMessageMap.put(pMessage);
  }

  public Iterable<ProcessModel> getProcessModels() {
    return aProcessModels;
    
  }

  public long addProcessModel(ProcessModel pPm) {
    return aProcessModels.put(pPm);
  }

}
