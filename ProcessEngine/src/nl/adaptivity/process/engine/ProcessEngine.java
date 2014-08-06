package nl.adaptivity.process.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.activation.DataSource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.StringCache;
import net.devrieze.util.StringCacheImpl;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.db.DbSet;
import net.devrieze.util.security.PermissiveProvider;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.engine.IProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
public class ProcessEngine /* implements IProcessEngine */{

  public static final String DBRESOURCENAME="java:/comp/env/jdbc/processengine";


  public enum Permissions implements SecurityProvider.Permission {
    ADD_MODEL,
    ASSIGN_OWNERSHIP,
    VIEW_ALL_INSTANCES,
    CANCEL_ALL,
    UPDATE_MODEL,
    CHANGE_OWNERSHIP,
    VIEW_INSTANCE,
    CANCEL, LIST_INSTANCES, ;

  }

  private final StringCache aStringCache = new StringCacheImpl();

  private final javax.sql.DataSource aDBResource = DbSet.resourceNameToDataSource(DBRESOURCENAME);

  private final ProcessInstanceMap aInstanceMap = new ProcessInstanceMap(aDBResource, this);

  private final ProcessNodeInstanceMap aNodeInstanceMap = new ProcessNodeInstanceMap(aDBResource, this, aStringCache);

  private final ProcessModelMap aProcessModels = new ProcessModelMap(aDBResource, aStringCache);

  private final IMessageService<?, ProcessNodeInstance> aMessageService;

  private SecurityProvider aSecurityProvider = new PermissiveProvider();

  /**
   * Create a new process engine.
   *
   * @param pMessageService The service to use for actual sending of messages by
   *          activities.
   */
  public ProcessEngine(final IMessageService<?, ProcessNodeInstance> pMessageService) {
    aMessageService = pMessageService;
  }

  /**
   * Get all process models loaded into the engine.
   *
   * @return The list of process models.
   */
  public Iterable<ProcessModelImpl> getProcessModels(DBTransaction pTransaction) {
    return aProcessModels.iterable(pTransaction);
  }

  /**
   * Add a process model to the engine.
   *
   * @param pPm The process model to add.
   * @return The processModel to add.
   * @throws SQLException
   */
  public IProcessModelRef<ProcessNodeImpl> addProcessModel(DBTransaction pTransaction, final ProcessModelImpl pPm, final Principal pUser) throws SQLException {
    aSecurityProvider.ensurePermission(Permissions.ADD_MODEL, pUser);

    if (pPm.getOwner() == null) {
      pPm.setOwner(pUser);
    } else if (!pUser.getName().equals(pPm.getOwner().getName())) {
      aSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, pUser, pPm.getOwner());
    }

    pPm.cacheStrings(aStringCache);
    UUID uuid = pPm.getUuid();
    if (uuid==null) { uuid = UUID.randomUUID(); pPm.setUuid(uuid); }
    return new ProcessModelRef(pPm.getName(), aProcessModels.put(pTransaction, pPm), uuid);
  }

  /**
   * Get the process model with the given handle.
   *
   * @param pHandle The handle to the process model.
   * @return The processModel.
   * @throws SQLException
   */
  public ProcessModelImpl getProcessModel(DBTransaction pTransaction, final Handle<? extends ProcessModelImpl> pHandle, final Principal pUser) throws SQLException {
    final ProcessModelImpl result = aProcessModels.get(pTransaction, pHandle);
    if (result != null) {
      aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
      if (result.getUuid()==null) { result.setUuid(UUID.randomUUID());aProcessModels.set(pTransaction, pHandle, result); }
    }
    return result;
  }

  /**
   * Rename the process model with the given handle.
   *
   * @param pHandle The handle to use.
   * @param pName The process model
   */
  public void renameProcessModel(final Principal pUser, final Handle<? extends ProcessModelImpl> pHandle, final String pName) {
    try (DBTransaction transaction= new DBTransaction(aDBResource)) {
      final ProcessModelImpl pm = aProcessModels.get(transaction, pHandle);
      aSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, pUser, pm);
      pm.setName(pName);
      aProcessModels.set(transaction, pHandle, pm); // set it to ensure update on the database
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ProcessModelRef updateProcessModel(DBTransaction pTransaction, Handle<? extends ProcessModelImpl> pHandle, ProcessModelImpl pProcessModel, Principal pUser) throws FileNotFoundException, SQLException {
    ProcessModelImpl oldModel = aProcessModels.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, oldModel);
    aSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, pUser, oldModel);

    if (pProcessModel.getOwner()==null) { // If no owner was set, use the old one.
      pProcessModel.setOwner(oldModel.getOwner());
    } else if (!oldModel.getOwner().getName().equals(pProcessModel.getOwner().getName())) {
      aSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, pUser, oldModel);
    }
    if(! (pHandle!=null && aProcessModels.contains(pTransaction, pHandle))) {
      throw new FileNotFoundException("The process model with handle "+pHandle+" could not be found");
    }
    aProcessModels.set(pTransaction, pHandle, pProcessModel);
    return ProcessModelRef.get(pProcessModel.getRef());
  }

  public boolean removeProcessModel(DBTransaction transaction, Handle<? extends ProcessModelImpl> pHandle, Principal pUser) throws SQLException {
    ProcessModelImpl oldModel = aProcessModels.get(transaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.DELETE, pUser, oldModel);

    if (aProcessModels.remove(transaction, pHandle)) {
      transaction.commit();
      return true;
    }
    return false;
  }

  public void setSecurityProvider(final SecurityProvider pSecurityProvider) {
    aSecurityProvider = pSecurityProvider;
  }

  /**
   * Get all process instances owned by the user.
   *
   * @param pUser The current user in relation to whom we need to find the
   *          instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getOwnedProcessInstances(DBTransaction pTransaction, final Principal pUser) {
    aSecurityProvider.ensurePermission(Permissions.LIST_INSTANCES, pUser);
    // If security allows this, return an empty list.
    final List<ProcessInstance> result = new ArrayList<>();
    for (final ProcessInstance instance : aInstanceMap.iterable(pTransaction)) {
      if ((pUser==null && instance.getOwner()==null) || (pUser!=null && instance.getOwner().getName().equals(pUser.getName()))) {
        result.add(instance);
      }
    }
    return result;
  }

  /**
   * Get all process instances visible to the user.
   *
   * @param pUser The current user in relation to whom we need to find the
   *          instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getVisibleProcessInstances(DBTransaction pTransaction, final Principal pUser) {
    final List<ProcessInstance> result = new ArrayList<>();
    for (final ProcessInstance instance : aInstanceMap.iterable(pTransaction)) {
      if (aSecurityProvider.hasPermission(SecureObject.Permissions.READ, pUser, instance)) {
        result.add(instance);
      }
    }
    return result;
  }

  public ProcessInstance getProcessInstance(DBTransaction pTransaction, long pHandle, Principal pUser) throws SQLException {
    ProcessInstance instance = aInstanceMap.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(Permissions.VIEW_INSTANCE, pUser, instance);
    return instance;
  }

  /**
   * Create a new process instance started by this process.
   *
   * @param pModel The model to create and start an instance of.
   * @param pName The name of the new instance.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   * @throws SQLException When database operations fail.
   */
  public HProcessInstance startProcess(DBTransaction pTransaction, final Principal pUser, final ProcessModelImpl pModel, final String pName, final Node pPayload) throws SQLException {
    if (pUser == null) {
      throw new HttpResponseException(HttpServletResponse.SC_FORBIDDEN, "Annonymous users are not allowed to start processes");
    }
    aSecurityProvider.ensurePermission(ProcessModelImpl.Permissions.INSTANTIATE, pUser);
    final ProcessInstance instance = new ProcessInstance(pUser, pModel, pName, null, this);

    final HProcessInstance result = new HProcessInstance(aInstanceMap.put(pTransaction, instance));
    instance.initialize(pTransaction);

    instance.start(pTransaction, aMessageService, pPayload);
    return result;
  }

  /**
   * Convenience method to start a process based upon a process model handle.
   *
   * @param pProcessModel The process model to start a new instance for.
   * @param pName The name of the new instance.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   * @throws SQLException
   */
  public HProcessInstance startProcess(DBTransaction pTransaction, final Principal pUser, final Handle<? extends ProcessModelImpl> pProcessModel, final String pName, final Node pPayload) throws SQLException {
    return startProcess(pTransaction, pUser, aProcessModels.get(pTransaction, pProcessModel), pName, pPayload);
  }

  /**
   * Get the task with the given handle.
   *
   * @param pHandle The handle of the task.
   * @return The handle
   * @throws SQLException
   * @todo change the parameter to a handle object.
   */
  public ProcessNodeInstance getNodeInstance(DBTransaction pTransaction, final long pHandle, final Principal pUser) throws SQLException {
    final ProcessNodeInstance result = aNodeInstanceMap.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
    return result;
  }

  /**
   * Finish the process instance.
   *
   * @param pProcessInstance The process instance to finish.
   * @throws SQLException
   * @todo evaluate whether this should not retain some results
   */
  public void finishInstance(DBTransaction pTransaction, final ProcessInstance pProcessInstance) throws SQLException {
    aInstanceMap.remove(pTransaction, pProcessInstance);
  }

  public ProcessInstance cancelInstance(DBTransaction pTransaction, long pHandle, Principal pUser) throws SQLException {
    ProcessInstance result = aInstanceMap.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(Permissions.CANCEL, pUser, result);
    try {
      aNodeInstanceMap.removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle));
      if(aInstanceMap.remove(pTransaction, result)) {
        return result;
      }
      throw new ProcessException("The instance could not be cancelled");
    } catch (SQLException e) {
      throw new ProcessException("The instance could not be cancelled", e);
    }
  }

  /**
   * Cancel all process instances and tasks in the engine.
   * @throws SQLException
   */
  public void cancelAll(DBTransaction pTransaction, final Principal pUser) throws SQLException {
    aSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, pUser);
    aNodeInstanceMap.clear(pTransaction);
    aInstanceMap.clear(pTransaction);
  }


  /**
   * Update the state of the given task
   *
   * @param pHandle Handle to the process instance.
   * @param pNewState The new state
   * @return
   * @throws SQLException
   */
  public TaskState updateTaskState(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final TaskState pNewState, final Principal pUser) throws SQLException {
    final ProcessNodeInstance task = aNodeInstanceMap.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    final ProcessInstance pi = task.getProcessInstance();
    synchronized (pi) {
      switch (pNewState) {
        case Sent:
          throw new IllegalArgumentException("Updating task state to initial state not possible");
        case Acknowledged:
          task.setState(pTransaction, pNewState); // Record the state, do nothing else.
          break;
        case Taken:
          pi.takeTask(pTransaction, aMessageService, task);
          break;
        case Started:
          pi.startTask(pTransaction, aMessageService, task);
          break;
        case Complete:
          throw new IllegalArgumentException("Finishing a task must be done by a separate method");
        case Failed:
          pi.failTask(pTransaction, aMessageService, task, null);
          break;
        case Cancelled:
          pi.cancelTask(pTransaction, aMessageService, task);
          break;
        default:
          throw new IllegalArgumentException("Unsupported state");
      }
      return task.getState();
    }
  }

  public TaskState finishTask(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final Node pPayload, final Principal pUser) throws SQLException {
    final ProcessNodeInstance task = aNodeInstanceMap.get(pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    final ProcessInstance pi = task.getProcessInstance();
    synchronized (pi) {
      pi.finishTask(pTransaction, aMessageService, task, pPayload);
      return task.getState();
    }
  }

  /**
   * This method is primarilly a convenience method for
   * {@link #finishTask(Handle, Node)}.
   *
   * @param pHandle The handle to finish.
   * @param pResult The source that is parsed into DOM nodes and then passed on
   *          to {@link #finishTask(Handle, Node)}
   */
  public void finishedTask(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final DataSource pResult, final Principal pUser) {
    InputSource result;
    try {
      result = pResult==null ? null : new InputSource(pResult.getInputStream());
    } catch (final IOException e) {
      throw new MessagingException(e);
    }
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document xml = db.parse(result);
      finishTask(pTransaction, pHandle, xml, pUser);

    } catch (final ParserConfigurationException e) {
      throw new MessagingException(e);
    } catch (final SAXException e) {
      throw new MessagingException(e);
    } catch (final IOException e) {
      throw new MessagingException(e);
    } catch (SQLException e) {
      try {
        pTransaction.rollback();
      } catch (SQLException e1) {
        MessagingException ex = new MessagingException(e1);
        ex.addSuppressed(e);;
        throw ex;
      }
      throw new MessagingException(e);
    }

  }

  public long registerNodeInstance(final DBTransaction pTransaction, final ProcessNodeInstance pInstance) throws SQLException {
    if (pInstance.getHandle() >= 0) {
      throw new IllegalArgumentException("Process node already registered");
    }
    return aNodeInstanceMap.put(pTransaction, pInstance);
  }

  /**
   * Handle the fact that this task has been cancelled.
   *
   * @param pHandle
   * @throws SQLException
   */
  public void cancelledTask(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final Principal pUser) throws SQLException {
    updateTaskState(pTransaction, pHandle, TaskState.Cancelled, pUser);
  }

  public void errorTask(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final Throwable pCause, final Principal pUser) throws SQLException {
    final ProcessNodeInstance task = aNodeInstanceMap.get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    final ProcessInstance pi = task.getProcessInstance();
    pi.failTask(pTransaction, aMessageService, task, pCause);
  }

  public void updateStorage(DBTransaction pTransaction, ProcessNodeInstance pProcessNodeInstance) throws SQLException {
    if (pProcessNodeInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    aNodeInstanceMap.set(pTransaction, pProcessNodeInstance, pProcessNodeInstance);
  }

  public void updateStorage(DBTransaction pTransaction, ProcessInstance pProcessInstance) throws SQLException {
    if (pProcessInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    aInstanceMap.set(pTransaction, pProcessInstance, pProcessInstance);
  }

  public DBTransaction startTransaction() {
    try {
      return new DBTransaction(aDBResource);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public EndpointDescriptor getLocalEndpoint() {
    return aMessageService.getLocalEndpoint();
  }

}
