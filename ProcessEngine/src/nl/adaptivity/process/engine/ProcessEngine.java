package nl.adaptivity.process.engine;

import java.io.IOException;

import javax.activation.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.devrieze.util.HandleMap;
import net.devrieze.util.HandleMap.Handle;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRef;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class represents the process engine.
 * XXX make sure this is thread safe!!
 */
public class ProcessEngine /* implements IProcessEngine*/ {

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();

  private final HandleMap<ProcessNodeInstance> aTaskMap = new HandleMap<ProcessNodeInstance>();

  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private final IMessageService<?, ProcessNodeInstance> aMessageService;

  /**
   * Create a new process engine.
   *
   * @param pMessageService The service to use for actual sending of messages by
   *          activities.
   */
  public ProcessEngine(IMessageService<?, ProcessNodeInstance> pMessageService) {
    aMessageService = pMessageService;
  }

  /**
   * Get all process models loaded into the engine.
   * @return The list of process models.
   */
  public Iterable<ProcessModel> getProcessModels() {
    return aProcessModels;
  }

  /**
   * Add a process model to the engine.
   * @param pPm The process model to add.
   * @return The processModel to add.
   */
  public ProcessModelRef addProcessModel(ProcessModel pPm) {
    return new ProcessModelRef(pPm.getName(), aProcessModels.put(pPm));
  }

  /**
   * Get the process model with the given handle.
   * @param pHandle The handle to the process model.
   * @return The processModel.
   * @deprecated In favour of {@link #getProcessModel(HProcessModel)}
   */
  @Deprecated
  public ProcessModel getProcessModel(long pHandle) {
    return aProcessModels.get(pHandle);
  }

  /**
   * Get the process model with the given handle.
   * @param pHandle The handle to the process model.
   * @return The processModel.
   */
  public ProcessModel getProcessModel(Handle<ProcessModel> pHandle) {
    return aProcessModels.get(pHandle);
  }

  /**
   * Rename the process model with the given handle.
   * @param pHandle The handle to use.
   * @param pName The process model
   */
  public void renameProcessModel(Handle<ProcessModel> pHandle, String pName) {
    ProcessModel pm = aProcessModels.get(pHandle);
    pm.setName(pName);
  }

  /**
   * Get all process instances known.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getInstances() {
    return aInstanceMap;
  }

  /**
   * Create a new process instance started by this process.
   * @param pModel The model to create and start an instance of.
   * @param pName The name of the new instance.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   */
  public HProcessInstance startProcess(ProcessModel pModel, String pName, Node pPayload) {
    ProcessInstance instance = new ProcessInstance(pModel, pName, this);
    HProcessInstance result = new HProcessInstance(aInstanceMap.put(instance));
    instance.start(aMessageService, pPayload);
    return result;
  }

  /**
   * Convenience method to start a process based upon a process model handle.
   * @param pProcessModel The process model to start a new instance for.
   * @param pName The name of the new instance.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   */
  public HProcessInstance startProcess(Handle<ProcessModel> pProcessModel, String pName, Node pPayload) {
    return startProcess(aProcessModels.get(pProcessModel), pName, pPayload);
  }

  /**
   * Get the task with the given handle.
   * @param pHandle The handle of the task.
   * @return The handle
   * @todo change the parameter to a handle object.
   */
  public ProcessNodeInstance getTask(long pHandle) {
    return aTaskMap.get(pHandle);
  }

  /**
   * Finish the process instance.
   *
   * @param pProcessInstance The process instance to finish.
   * @todo evaluate whether this should not retain some results
   */
  public void finishInstance(ProcessInstance pProcessInstance) {
    aInstanceMap.remove(pProcessInstance);
  }

  /**
   * Cancel all process instances and tasks in the engine.
   */
  public void cancelAll() {
    for(ProcessInstance instance: aInstanceMap) {
      aInstanceMap.remove(instance);
    }
    for(ProcessNodeInstance task: aTaskMap) {
      aTaskMap.remove(task);
    }
  }


  /**
   * Update the state of the given task
   * @param pHandle Handle to the process instance.
   * @param pNewState The new state
   * @return
   */
  public TaskState updateTaskState(Handle<ProcessNodeInstance> pHandle, TaskState pNewState) {
    ProcessNodeInstance t = aTaskMap.get(pHandle);
    ProcessInstance pi = t.getProcessInstance();
    synchronized(pi) {
      switch (pNewState) {
        case Sent:
          throw new IllegalArgumentException("Updating task state to initial state not possible");
        case Acknowledged:
          t.setState(pNewState); // Record the state, do nothing else.
          break;
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
        case Cancelled:
          pi.cancelTask(aMessageService, t);
        default:
          throw new IllegalArgumentException("Unsupported state");
      }
      return t.getState();
    }
  }

  public TaskState finishTask(Handle<ProcessNodeInstance> pHandle, Node pPayload) {
    ProcessNodeInstance t = aTaskMap.get(pHandle);
    ProcessInstance pi = t.getProcessInstance();
    synchronized (pi) {
      pi.finishTask(aMessageService, t, pPayload);
      final TaskState newState = t.getState();
      if (newState==TaskState.Complete) {
        aTaskMap.remove(t);
      }
      return newState;
    }
  }

  /**
   * This method is primarilly a convenience method for {@link #finishTask(Handle, Node)}.
   * @param pHandle The handle to finish.
   * @param pResult The source that is parsed into DOM nodes and then passed on to {@link #finishTask(Handle, Node)}
   */
  public void finishedTask(Handle<ProcessNodeInstance> pHandle, DataSource pResult) {
    InputSource result;
    try {
      result = new InputSource(pResult.getInputStream());
    } catch (IOException e) {
      throw new MyMessagingException(e);
    }
    DocumentBuilderFactory dbf= DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document xml = db.parse(result);
      finishTask(pHandle, xml);

    } catch (ParserConfigurationException e) {
      throw new MyMessagingException(e);
    } catch (SAXException e) {
      throw new MyMessagingException(e);
    } catch (IOException e) {
      throw new MyMessagingException(e);
    }

  }

  public long registerMessage(ProcessNodeInstance pInstance) {
    if (pInstance.getHandle()>=0) {
      throw new IllegalArgumentException("Process node already registered");
    }
    return aTaskMap.put(pInstance);
  }

  /**
   * Handle the fact that this task has been cancelled.
   * @param pHandle
   */
  public void cancelledTask(Handle<ProcessNodeInstance> pHandle) {
    updateTaskState(pHandle, TaskState.Cancelled);
  }

  public void errorTask(Handle<ProcessNodeInstance> pHandle, Throwable pCause) {
    ProcessNodeInstance task = aTaskMap.get(pHandle);
    ProcessInstance pi = task.getProcessInstance();
    pi.failTask(aMessageService, task, pCause);
  }

}
