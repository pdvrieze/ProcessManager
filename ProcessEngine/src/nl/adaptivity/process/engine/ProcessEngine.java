package nl.adaptivity.process.engine;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap;
import net.devrieze.util.HandleMap.Handle;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;


public class ProcessEngine implements IProcessEngine {

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();

  private final HandleMap<ProcessNodeInstance> aTaskMap = new HandleMap<ProcessNodeInstance>();

  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private ProcessMessageListener aMessageListener;

  private final IMessageService<?, ProcessNodeInstance> aMessageService;

  public ProcessEngine(IMessageService<?, ProcessNodeInstance> pMessageService) {
    aMessageService = pMessageService;
  }

  @Override
  public HProcessInstance startProcess(ProcessModel pModel, Payload pPayload) {
    ProcessInstance instance = new ProcessInstance(pModel, this);
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
    if (aMessageListener!=null) {
      aMessageListener.fireFinishedInstance(pProcessInstance.getHandle());
    }
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

  public TaskState updateTaskState(long pHandle, TaskState pNewState) {
    ProcessNodeInstance t = aTaskMap.get(pHandle);
    ProcessInstance pi = t.getProcessInstance();
    switch (pNewState) {
      case Available:
        throw new IllegalArgumentException("Updating task state to initial state not possible");
      case Taken:
        pi.takeTask(aMessageService, t);
        break;
      case Started:
        pi.startTask(aMessageService, t);
        break;
      case Complete:
        throw new IllegalArgumentException("Finishing a task must be done by a separate method");
      case Failed:
        pi.failTask(aMessageService, t);
        break;
      default:
        throw new IllegalArgumentException("Unsupported state");
    }
    return t.getState();
  }

  public TaskState finishTask(long pHandle, Node pPayload) {
    ProcessNodeInstance t = aTaskMap.get(pHandle);
    ProcessInstance pi = t.getProcessInstance();
    pi.finishTask(aMessageService, t, pPayload);
    final TaskState newState = t.getState();
    if (newState==TaskState.Complete) {
      aTaskMap.remove(t);
    }
    return newState;
  }

  public long registerMessage(ProcessNodeInstance pInstance) {
    if (pInstance.getHandle()>=0) {
      throw new IllegalArgumentException("Process node already registered");
    }
    return aTaskMap.put(pInstance);
  }

}
