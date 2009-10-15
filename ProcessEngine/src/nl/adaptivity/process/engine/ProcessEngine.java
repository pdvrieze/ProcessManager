package nl.adaptivity.process.engine;

import org.w3c.dom.Node;

import net.devrieze.util.HandleMap;
import net.devrieze.util.HandleMap.Handle;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;


public class ProcessEngine /* implements IProcessEngine*/ {

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();

  private final HandleMap<ProcessNodeInstance> aTaskMap = new HandleMap<ProcessNodeInstance>();

  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private final IMessageService<?, ProcessNodeInstance> aMessageService;

  public ProcessEngine(IMessageService<?, ProcessNodeInstance> pMessageService) {
    aMessageService = pMessageService;
  }

  public HProcessInstance startProcess(ProcessModel pModel, String pName, Node pPayload) {
    ProcessInstance instance = new ProcessInstance(pModel, pName, this);
    HProcessInstance result = new HProcessInstance(aInstanceMap.put(instance));
    instance.start(aMessageService, pPayload);
    return result;
  }

  public HProcessInstance startProcess(Handle<ProcessModel> pProcessModel, String pName, Node pPayload) {
    return startProcess(aProcessModels.get(pProcessModel), pName, pPayload);
  }

  public ProcessNodeInstance retrieveTask(long pHandle) {
    return aTaskMap.get(pHandle);
  }

  public void finishInstance(ProcessInstance pProcessInstance) {
    aInstanceMap.remove(pProcessInstance);
  }

  public void cancelAll() {
    for(ProcessInstance instance: aInstanceMap) {
      aInstanceMap.remove(instance);
    }
    for(ProcessNodeInstance task: aTaskMap) {
      aTaskMap.remove(task);
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

  public void renameProcess(Handle<ProcessModel> pHandle, String pName) {
    ProcessModel pm = aProcessModels.get(pHandle);
    pm.setName(pName);
  }

  public ProcessModel getProcessModel(long pHandle) {
    return aProcessModels.get(pHandle);
  }

}
