package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap;
import net.devrieze.util.HandleMap.Handle;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;


public class ProcessEngine implements IProcessEngine {

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();

  private final HandleMap<Task> aTaskMap = new HandleMap<Task>();

  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private ProcessMessageListener aMessageListener;

  private final IMessageService<?> aMessageService;

  public ProcessEngine(IMessageService<?> pMessageService) {
    aMessageService = pMessageService;
  }

  @Override
  public HProcessInstance startProcess(ProcessModel pModel, Payload pPayload) {
    ProcessInstance instance = new ProcessInstance(pModel, this, pPayload);
    HProcessInstance result = new HProcessInstance(aInstanceMap.put(instance));
    instance.start(aMessageService);
    return result;
  }

  public HProcessInstance startProcess(Handle<ProcessModel> pProcessModel, Payload pPayload) {
    return startProcess(aProcessModels.get(pProcessModel), pPayload);
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

  public Task retrieveTask(long pHandle) {
    return aTaskMap.get(pHandle);
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

  public Iterable<ProcessModel> getProcessModels() {
    return aProcessModels;
  }

  public long addProcessModel(ProcessModel pPm) {
    return aProcessModels.put(pPm);
  }

  public Iterable<ProcessInstance> getInstances() {
    return aInstanceMap;
  }

  public void updateTaskState(long pHandle, TaskState pNewState) {
    // TODO Auto-generated method stub
    //
    throw new UnsupportedOperationException("Not yet implemented");

  }

  public long registerMessage(Task pInstance) {
    if (pInstance.getHandle()>=0) {
      throw new IllegalArgumentException("Process node already registered");
    }
    return aTaskMap.put(pInstance);
  }

}
