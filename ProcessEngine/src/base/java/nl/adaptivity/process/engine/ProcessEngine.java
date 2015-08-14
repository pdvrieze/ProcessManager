package nl.adaptivity.process.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.devrieze.util.CachingDBHandleMap;
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
import nl.adaptivity.process.engine.ProcessInstance.State;
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

  private javax.sql.DataSource aDBResource = null;

  private ProcessInstanceMap aInstanceMap = new ProcessInstanceMap(aDBResource, this);

  private ProcessNodeInstanceMap aNodeInstanceMap = null;

  private ProcessModelMap aProcessModels = null;

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
    return getProcessModels().iterable(pTransaction);
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
    return new ProcessModelRef(pPm.getName(), getProcessModels().put(pTransaction, pPm), uuid);
  }

  /**
   * Get the process model with the given handle.
   *
   * @param pHandle The handle to the process model.
   * @return The processModel.
   * @throws SQLException
   */
  public ProcessModelImpl getProcessModel(DBTransaction pTransaction, final Handle<? extends ProcessModelImpl> pHandle, final Principal pUser) throws SQLException {
    final ProcessModelImpl result = getProcessModels().get(pTransaction, pHandle);
    if (result != null) {
      aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, result);
      if (result.getUuid()==null) { result.setUuid(UUID.randomUUID());getProcessModels().set(pTransaction, pHandle, result); }
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
    try (DBTransaction transaction= new DBTransaction(getDBResource())) {
      final ProcessModelImpl pm = getProcessModels().get(transaction, pHandle);
      aSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, pUser, pm);
      pm.setName(pName);
      getProcessModels().set(transaction, pHandle, pm); // set it to ensure update on the database
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ProcessModelRef updateProcessModel(DBTransaction pTransaction, Handle<? extends ProcessModelImpl> pHandle, ProcessModelImpl pProcessModel, Principal pUser) throws FileNotFoundException, SQLException {
    ProcessModelImpl oldModel = getProcessModels().get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.READ, pUser, oldModel);
    aSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, pUser, oldModel);

    if (pProcessModel.getOwner()==null) { // If no owner was set, use the old one.
      pProcessModel.setOwner(oldModel.getOwner());
    } else if (!oldModel.getOwner().getName().equals(pProcessModel.getOwner().getName())) {
      aSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, pUser, oldModel);
    }
    if(! (pHandle!=null && getProcessModels().contains(pTransaction, pHandle))) {
      throw new FileNotFoundException("The process model with handle "+pHandle+" could not be found");
    }
    getProcessModels().set(pTransaction, pHandle, pProcessModel);
    return ProcessModelRef.get(pProcessModel.getRef());
  }

  public boolean removeProcessModel(DBTransaction transaction, Handle<? extends ProcessModelImpl> pHandle, Principal pUser) throws SQLException {
    ProcessModelImpl oldModel = getProcessModels().get(transaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.DELETE, pUser, oldModel);

    if (getProcessModels().remove(transaction, pHandle.getHandle())) {
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
    for (final ProcessInstance instance : getInstances().iterable(pTransaction)) {
      if ((pUser==null && instance.getOwner()==null) || (pUser!=null && instance.getOwner().getName().equals(pUser.getName()))) {
        result.add(instance);
      }
    }
    return result;
  }

  private CachingDBHandleMap<ProcessInstance> getInstances() {
    if (aInstanceMap==null) {
      aInstanceMap = new ProcessInstanceMap(aDBResource, this);
    }
    return aInstanceMap;
  }

  private ProcessNodeInstanceMap getNodeInstances() {
    if (aNodeInstanceMap==null) {
      aNodeInstanceMap =  new ProcessNodeInstanceMap(aDBResource, this, aStringCache);
    }
    return aNodeInstanceMap;
  }


  private ProcessModelMap getProcessModels() {
    if (aProcessModels==null) {
      aProcessModels = new ProcessModelMap(aDBResource, aStringCache);
    }
    return aProcessModels;
  }/**
   * Get all process instances visible to the user.
   *
   * @param pUser The current user in relation to whom we need to find the
   *          instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getVisibleProcessInstances(DBTransaction pTransaction, final Principal pUser) {
    final List<ProcessInstance> result = new ArrayList<>();
    for (final ProcessInstance instance : getInstances().iterable(pTransaction)) {
      if (aSecurityProvider.hasPermission(SecureObject.Permissions.READ, pUser, instance)) {
        result.add(instance);
      }
    }
    return result;
  }

  public ProcessInstance getProcessInstance(DBTransaction pTransaction, long pHandle, Principal pUser) throws SQLException {
    ProcessInstance instance = getInstances().get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(Permissions.VIEW_INSTANCE, pUser, instance);
    return instance;
  }

  public boolean tickleInstance(long pHandle) {
    try (DBTransaction transaction=startTransaction()) {
      ProcessInstance instance = getInstances().getUncached(transaction, pHandle);
      if (instance==null) { return false; }
      instance.tickle(transaction, aMessageService);
      return true;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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
  public HProcessInstance startProcess(DBTransaction pTransaction, final Principal pUser, final ProcessModelImpl pModel, final String pName, final UUID pUuid, final Node pPayload) throws SQLException {
    if (pUser == null) {
      throw new HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN, "Annonymous users are not allowed to start processes");
    }
    aSecurityProvider.ensurePermission(ProcessModelImpl.Permissions.INSTANTIATE, pUser);
    final ProcessInstance instance = new ProcessInstance(pUser, pModel, pName, pUuid, State.NEW, this);

    final HProcessInstance result = new HProcessInstance(getInstances().put(pTransaction, instance));
    instance.initialize(pTransaction);
    pTransaction.commit();
    try {
      instance.start(pTransaction, aMessageService, pPayload);
    } catch (Exception e) {
      Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error starting instance (it is already stored)", e);
    }
    return result;
  }

  /**
   * Convenience method to start a process based upon a process model handle.
   *
   * @param pProcessModel The process model to start a new instance for.
   * @param pName The name of the new instance.
   * @param pUuid The UUID for the instances. Helps with synchronization errors not exploding into mass instantiation.
   * @param pPayload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   * @throws SQLException
   */
  public HProcessInstance startProcess(DBTransaction pTransaction, final Principal pUser, final Handle<? extends ProcessModelImpl> pProcessModel, final String pName, UUID pUuid, final Node pPayload) throws SQLException {
    return startProcess(pTransaction, pUser, getProcessModels().get(pTransaction, pProcessModel), pName, pUuid, pPayload);
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
    final ProcessNodeInstance result = getNodeInstances().get(pTransaction, pHandle);
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
    getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pProcessInstance.getHandle()));
    // TODO retain instance
    getInstances().remove(pTransaction, pProcessInstance.getHandle());
  }

  public ProcessInstance cancelInstance(DBTransaction pTransaction, long pHandle, Principal pUser) throws SQLException {
    ProcessInstance result = getInstances().get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(Permissions.CANCEL, pUser, result);
    try {
      getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle));
      if(getInstances().remove(pTransaction, result)) {
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
    getNodeInstances().clear(pTransaction);
    getInstances().clear(pTransaction);
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
    final ProcessNodeInstance task = getNodeInstances().get(pTransaction, pHandle);
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
          throw new IllegalArgumentException("Unsupported state :"+pNewState);
      }
      return task.getState();
    }
  }

  public TaskState finishTask(DBTransaction pTransaction, final Handle<ProcessNodeInstance> pHandle, final Node pPayload, final Principal pUser) throws SQLException {
    final ProcessNodeInstance task = getNodeInstances().get(pHandle);
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
    return getNodeInstances().put(pTransaction, pInstance);
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
    final ProcessNodeInstance task = getNodeInstances().get(pTransaction, pHandle);
    aSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, pUser, task);
    final ProcessInstance pi = task.getProcessInstance();
    pi.failTask(pTransaction, aMessageService, task, pCause);
  }

  public void updateStorage(DBTransaction pTransaction, ProcessNodeInstance pProcessNodeInstance) throws SQLException {
    if (pProcessNodeInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    getNodeInstances().set(pTransaction, pProcessNodeInstance, pProcessNodeInstance);
  }

  public void updateStorage(DBTransaction pTransaction, ProcessInstance pProcessInstance) throws SQLException {
    if (pProcessInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    getInstances().set(pTransaction, pProcessInstance, pProcessInstance);
  }

  public DBTransaction startTransaction() {
    try {
      return new DBTransaction(getDBResource());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private javax.sql.DataSource getDBResource() {
    if (aDBResource==null) {
      aDBResource = DbSet.resourceNameToDataSource(DBRESOURCENAME);
    }
    return aDBResource;
  }

  public EndpointDescriptor getLocalEndpoint() {
    return aMessageService.getLocalEndpoint();
  }

}
