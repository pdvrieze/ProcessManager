package nl.adaptivity.process.engine;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.devrieze.util.HandleMap;
import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.StringCache;
import net.devrieze.util.StringCacheImpl;
import net.devrieze.util.security.DenyingSecurityProvider;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;

import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.ProcessModelRef;

/**
 * This class represents the process engine.
 * XXX make sure this is thread safe!!
 */
public class ProcessEngine /* implements IProcessEngine*/ {

  
  public enum Permissions implements SecurityProvider.Permission {
    ADD_MODEL, ASSIGN_OWNERSHIP, VIEW_ALL_INSTANCES, CANCEL_ALL,
    ;

  }

  private final HandleMap<ProcessInstance> aInstanceMap = new HandleMap<ProcessInstance>();

  private final HandleMap<ProcessNodeInstance> aTaskMap = new HandleMap<ProcessNodeInstance>();

  private final HandleMap<ProcessModel> aProcessModels = new HandleMap<ProcessModel>();

  private final IMessageService<?, ProcessNodeInstance> aMessageService;

  private final StringCache aStringCache = new StringCacheImpl.SafeStringCache();

  private SecurityProvider aSecurityProvider = new DenyingSecurityProvider();

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
  public ProcessModelRef addProcessModel(ProcessModel pPm, Principal pUser) {
    aSecurityProvider.ensurePermission(Permissions.ADD_MODEL, pUser);
    
    if (pPm.getOwner()==null) {
      pPm.setOwner(pUser);
    } else if(!pUser.getName().equals(pPm.getOwner().getName())) {
      aSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, pUser, pPm.getOwner());
    }

    pPm.cacheStrings(aStringCache);
    return new ProcessModelRef(pPm.getName(), aProcessModels.put(pPm));
  }

  /**
   * Get the process model with the given handle.
   * @param pHandle The handle to the process model.
   * @return The processModel.
   * @deprecated In favour of {@link #getProcessModel(HProcessModel)}
   */
  @Deprecated
  public ProcessModel getProcessModel(long pHandle, Principal pUser) {
    final ProcessModel result = aProcessModels.get(pHandle);
    if (result!=null) {
      aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
    }
    return result;
  }

  /**
   * Get the process model with the given handle.
   * @param pHandle The handle to the process model.
   * @return The processModel.
   */
  public ProcessModel getProcessModel(Handle<ProcessModel> pHandle, Principal pUser) {
    final ProcessModel result = aProcessModels.get(pHandle);
    if (result!=null) {
      aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
    }
    return result;
  }

  /**
   * Rename the process model with the given handle.
   * @param pHandle The handle to use.
   * @param pName The process model
   */
  public void renameProcessModel(Principal pUser, Handle<ProcessModel> pHandle, String pName) {
    ProcessModel pm = aProcessModels.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, pUser, pm);
    pm.setName(pName);
  }

  public void setSecurityProvider(SecurityProvider pSecurityProvider) {
    aSecurityProvider = pSecurityProvider;
  }
  
  /**
   * Get all process instances owned by the user.
   * @param pUser The current user in relation to whom we need to find the instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getOwnedProcessInstances(Principal pUser) {
    List<ProcessInstance> result = new ArrayList<ProcessInstance>();
    for(ProcessInstance instance: aInstanceMap) {
      if(instance.getOwner().getName().equals(pUser.getName())) {
        result.add(instance);
      }
    }
    return result;
  }
  
  /**
   * Get all process instances visible to the user.
   * @param pUser The current user in relation to whom we need to find the instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getVisibleProcessInstances(Principal pUser) {
    List<ProcessInstance> result = new ArrayList<ProcessInstance>();
    for(ProcessInstance instance: aInstanceMap) {
      if(aSecurityProvider.hasPermission(SecureObject.Permissions.READ, pUser, instance)) {
        result.add(instance);
      }
    }
    return result;
  }
  
  /**
   * Get all process instances known. Note that most users should not have permission to do this.
   * @param pUser The user that wants to perform this action.
   * @return The instances.
   */
  public Iterable<ProcessInstance> getAllProcessInstances(Principal pUser) {
    aSecurityProvider.ensurePermission(Permissions.VIEW_ALL_INSTANCES, pUser);
    return aInstanceMap;
  }

  /**
   * Create a new process instance started by this process.
   * @param pModel The model to create and start an instance of.
   * @param pName The name of the new instance.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   */
  public HProcessInstance startProcess(Principal pUser, ProcessModel pModel, String pName, Node pPayload) {
    if (pUser==null) {
      throw new MyMessagingException("Annonymous processes are not allowed");
    }
    aSecurityProvider.ensurePermission(ProcessModel.Permissions.INSTANTIATE, pUser);
    ProcessInstance instance = new ProcessInstance(pUser, pModel, pName, this);
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
  public HProcessInstance startProcess(Principal pUser, Handle<ProcessModel> pProcessModel, String pName, Node pPayload) {
    return startProcess(pUser, aProcessModels.get(pProcessModel), pName, pPayload);
  }

  /**
   * Get the task with the given handle.
   * @param pHandle The handle of the task.
   * @return The handle
   * @todo change the parameter to a handle object.
   */
  public ProcessNodeInstance getTask(long pHandle, Principal pUser) {
    final ProcessNodeInstance result = aTaskMap.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
    return result;
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
  public void cancelAll(Principal pUser) {
    aSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, pUser);
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
  public TaskState updateTaskState(Handle<ProcessNodeInstance> pHandle, TaskState pNewState, Principal pUser) {
    ProcessNodeInstance task = aTaskMap.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    ProcessInstance pi = task.getProcessInstance();
    synchronized(pi) {
      switch (pNewState) {
        case Sent:
          throw new IllegalArgumentException("Updating task state to initial state not possible");
        case Acknowledged:
          task.setState(pNewState); // Record the state, do nothing else.
          break;
        case Taken:
          pi.takeTask(aMessageService, task);
          break;
        case Started:
          pi.startTask(aMessageService, task);
          break;
        case Complete:
          throw new IllegalArgumentException("Finishing a task must be done by a separate method");
        case Failed:
          pi.failTask(aMessageService, task);
          break;
        case Cancelled:
          pi.cancelTask(aMessageService, task);
          break;
        default:
          throw new IllegalArgumentException("Unsupported state");
      }
      return task.getState();
    }
  }

  public TaskState finishTask(Handle<ProcessNodeInstance> pHandle, Node pPayload, Principal pUser) {
    ProcessNodeInstance task = aTaskMap.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    ProcessInstance pi = task.getProcessInstance();
    synchronized (pi) {
      pi.finishTask(aMessageService, task, pPayload);
      final TaskState newState = task.getState();
      if (newState==TaskState.Complete) {
        aTaskMap.remove(task);
      }
      return newState;
    }
  }

  /**
   * This method is primarilly a convenience method for {@link #finishTask(Handle, Node)}.
   * @param pHandle The handle to finish.
   * @param pResult The source that is parsed into DOM nodes and then passed on to {@link #finishTask(Handle, Node)}
   */
  public void finishedTask(Handle<ProcessNodeInstance> pHandle, DataSource pResult, Principal pUser) {
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
      finishTask(pHandle, xml, pUser);

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
  public void cancelledTask(Handle<ProcessNodeInstance> pHandle, Principal pUser) {
    updateTaskState(pHandle, TaskState.Cancelled, pUser);
  }

  public void errorTask(Handle<ProcessNodeInstance> pHandle, Throwable pCause, Principal pUser) {
    ProcessNodeInstance task = aTaskMap.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    ProcessInstance pi = task.getProcessInstance();
    pi.failTask(aMessageService, task, pCause);
  }

}
