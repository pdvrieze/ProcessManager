package nl.adaptivity.process.engine;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.*;
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
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.engine.*;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl.ExecutableSplitFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.activation.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
public class ProcessEngine<T extends Transaction> /* implements IProcessEngine */{

  private static class MyDBTransactionFactory implements TransactionFactory<DBTransaction> {
    private final Context mContext;

    private javax.sql.DataSource mDBResource = null;

    MyDBTransactionFactory() {
      InitialContext ic = null;
      try {
        ic = new InitialContext();
        mContext = (Context) ic.lookup("java:/comp/env");
      } catch (NamingException e) {
        throw new RuntimeException(e);
      }
    }

    private javax.sql.DataSource getDBResource() {
      if (mDBResource ==null) {
        if (mContext!=null) {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        } else {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        }
      }
      return mDBResource;
    }

    @Override
    public DBTransaction startTransaction() {
      try {
        return new DBTransaction(getDBResource());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Connection getConnection() throws SQLException {
      return mDBResource.getConnection();
    }

    @Override
    public boolean isValidTransaction(final Transaction transaction) {
      return transaction instanceof  DBTransaction && ((DBTransaction) transaction).providerEquals(mDBResource);
    }
  }

  public static final String CONTEXT_PATH = "java:/comp/env";
  public static final String DB_RESOURCE = "jdbc/processengine";
  public static final String DBRESOURCENAME= CONTEXT_PATH +'/'+ DB_RESOURCE;

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

  private final StringCache mStringCache = new StringCacheImpl();
  private final TransactionFactory<? extends T> mTransactionFactory;

  private TransactionedHandleMap<ProcessInstance, T> mInstanceMap;

  private TransactionedHandleMap<ProcessNodeInstance, T> mNodeInstanceMap = null;

  private TransactionedHandleMap<ProcessModelImpl, T> mProcessModels = null;

  private final IMessageService<?, ProcessNodeInstance> mMessageService;

  private SecurityProvider mSecurityProvider = new PermissiveProvider();

  /**
   * Create a new process engine.
   *
   * @param messageService The service to use for actual sending of messages by
   *          activities.
   */
  protected ProcessEngine(final IMessageService<?, ProcessNodeInstance> messageService, TransactionFactory transactionFactory) {
    mMessageService = messageService;
    mTransactionFactory = transactionFactory;
  }

  public static ProcessEngine newInstance(final IMessageService<?, ProcessNodeInstance> messageService) {
    ProcessEngine pe = new ProcessEngine(messageService, new MyDBTransactionFactory());
    pe.mInstanceMap = new ProcessInstanceMap(pe.mTransactionFactory, pe);
    pe.mNodeInstanceMap = new ProcessNodeInstanceMap(pe.mTransactionFactory, pe, pe.mStringCache);
    pe.mProcessModels = new ProcessModelMap(pe.mTransactionFactory, pe.mStringCache);
    return pe;
  }

  /**
   * Testing constructor that does not need database access
   * @param messageService
   * @param processModels
   * @param processInstances
   * @param processNodeInstances
   */
  private ProcessEngine(final IMessageService<?, ProcessNodeInstance> messageService,
                        TransactionFactory transactionFactory,
                        TransactionedHandleMap<ProcessModelImpl, T> processModels,
                        TransactionedHandleMap<ProcessInstance, T> processInstances,
                        TransactionedHandleMap<ProcessNodeInstance, T> processNodeInstances) {
    mMessageService = messageService;
    mProcessModels = processModels;
    mTransactionFactory = transactionFactory;
    mInstanceMap = processInstances;
    mNodeInstanceMap = processNodeInstances;
  }

  public void invalidateModelCache(final Handle<? extends ProcessModelImpl> handle) {
    mProcessModels.invalidateCache(handle);
  }

  public void invalidateInstanceCache(final Handle<? extends ProcessInstance> handle) {
    mInstanceMap.invalidateCache(handle);
  }

  public void invalidateNodeCache(final Handle<? extends ProcessNodeInstance> handle) {
    mNodeInstanceMap.invalidateCache(handle);
  }

  static <T extends Transaction>  ProcessEngine<T> newTestInstance(final IMessageService<?, ProcessNodeInstance> messageService,
                                                                TransactionFactory transactionFactory,
                                                                TransactionedHandleMap<ProcessModelImpl, T> processModels,
                                                                TransactionedHandleMap<ProcessInstance, T> processInstances,
                                                                TransactionedHandleMap<ProcessNodeInstance, T> processNodeInstances) {
    return new ProcessEngine<T>(messageService, transactionFactory, processModels, processInstances, processNodeInstances);
  }

  /**
   * Get all process models loaded into the engine.
   *
   * @return The list of process models.
   * @param transaction
   */
  public Iterable<? extends ProcessModelImpl> getProcessModels(T transaction) {
    return getProcessModels().iterable(transaction);
  }

  /**
   * Add a process model to the engine.
   *
   *
   * @param transaction
   * @param basepm The process model to add.
   * @return The processModel to add.
   * @throws SQLException
   */
  public IProcessModelRef<ExecutableProcessNode, ProcessModelImpl> addProcessModel(T transaction, final ProcessModelBase<?, ?> basepm, final Principal user) throws SQLException {
    mSecurityProvider.ensurePermission(Permissions.ADD_MODEL, user);

    if (basepm.getOwner() == null) {
      basepm.setOwner(user);
    } else if (!user.getName().equals(basepm.getOwner().getName())) {
      mSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, user, basepm.getOwner());
    }
    final ProcessModelImpl pm;
    pm = ProcessModelImpl.from(basepm);

    pm.cacheStrings(mStringCache);
    UUID uuid = pm.getUuid();
    if (uuid==null) { uuid = UUID.randomUUID(); pm.setUuid(uuid); }
    return new ProcessModelRef(pm.getName(), getProcessModels().put(transaction, pm), uuid);
  }

  /**
   * Get the process model with the given handle.
   *
   * @param handle The handle to the process model.
   * @return The processModel.
   * @throws SQLException
   */
  public ProcessModelImpl getProcessModel(T transaction, final Handle<? extends ProcessModelImpl> handle, final Principal user) throws SQLException {
    final ProcessModelImpl result = getProcessModels().get(transaction, handle);
    result.normalize(new ExecutableSplitFactory());
    if (result != null) {
      mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, result);
      if (result.getUuid()==null) { result.setUuid(UUID.randomUUID());
        getProcessModels().set(transaction, handle, result); }
    }
    return result;
  }

  /**
   * Rename the process model with the given handle.
   *
   * @param handle The handle to use.
   * @param name The process model
   */
  public void renameProcessModel(final Principal user, final Handle<? extends ProcessModelImpl> handle, final String name) {
    try (T transaction= startTransaction()) {
      final ProcessModelImpl pm = getProcessModels().get(transaction, handle);
      mSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm);
      pm.setName(name);
      getProcessModels().set(transaction, handle, pm); // set it to ensure update on the database
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ProcessModelRef updateProcessModel(T transaction, Handle<? extends ProcessModelImpl> handle, ProcessModelBase<?,?> processModel, Principal user) throws FileNotFoundException, SQLException {
    ProcessModelImpl oldModel = getProcessModels().get(transaction, handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel);
    mSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, user, oldModel);

    if (processModel.getOwner()==null) { // If no owner was set, use the old one.
      processModel.setOwner(oldModel.getOwner());
    } else if (!oldModel.getOwner().getName().equals(processModel.getOwner().getName())) {
      mSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, user, oldModel);
    }
    if(! (handle!=null && getProcessModels().contains(transaction, handle))) {
      throw new FileNotFoundException("The process model with handle "+handle+" could not be found");
    }
    getProcessModels().set(transaction, handle, processModel instanceof ProcessModelImpl? (ProcessModelImpl) processModel : ProcessModelImpl.from(processModel));
    return ProcessModelRef.get(processModel.getRef());
  }

  public boolean removeProcessModel(T transaction, Handle<? extends ProcessModelImpl> handle, Principal user) throws SQLException {
    ProcessModelImpl oldModel = getProcessModels().get(transaction, handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.DELETE, user, oldModel);

    if (getProcessModels().remove(transaction, handle)) {
      transaction.commit();
      return true;
    }
    return false;
  }

  public void setSecurityProvider(final SecurityProvider securityProvider) {
    mSecurityProvider = securityProvider;
  }

  /**
   * Get all process instances owned by the user.
   *
   * @param user The current user in relation to whom we need to find the
   *          instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getOwnedProcessInstances(T transaction, final Principal user) {
    mSecurityProvider.ensurePermission(Permissions.LIST_INSTANCES, user);
    // If security allows this, return an empty list.
    final List<ProcessInstance> result = new ArrayList<>();
    for (final ProcessInstance instance : getInstances().iterable(transaction)) {
      if ((user==null && instance.getOwner()==null) || (user!=null && instance.getOwner().getName().equals(user.getName()))) {
        result.add(instance);
      }
    }
    return result;
  }

  private TransactionedHandleMap<ProcessInstance, T> getInstances() {
    return mInstanceMap;
  }

  private TransactionedHandleMap<ProcessNodeInstance, T> getNodeInstances() {
    return mNodeInstanceMap;
  }


  private TransactionedHandleMap<ProcessModelImpl, T> getProcessModels() {
    if (mProcessModels ==null) {

      // TODO Hack to use the db backed implementation here
      @SuppressWarnings("raw")
      TransactionedHandleMap<ProcessModelImpl, T> tmp = (TransactionedHandleMap) new ProcessModelMap(mTransactionFactory, mStringCache);
      mProcessModels = tmp;
    }
    return mProcessModels;
  }/**
   * Get all process instances visible to the user.
   *
   * @param user The current user in relation to whom we need to find the
   *          instances.
   * @return All instances.
   */
  public Iterable<ProcessInstance> getVisibleProcessInstances(T transaction, final Principal user) {
    final List<ProcessInstance> result = new ArrayList<>();
    for (final ProcessInstance instance : getInstances().iterable(transaction)) {
      if (mSecurityProvider.hasPermission(SecureObject.Permissions.READ, user, instance)) {
        result.add(instance);
      }
    }
    return result;
  }

  public ProcessInstance getProcessInstance(T transaction, Handle<? extends ProcessInstance> handle, Principal user) throws SQLException {
    ProcessInstance instance = getInstances().get(transaction, handle); // XXX no generics needed
    mSecurityProvider.ensurePermission(Permissions.VIEW_INSTANCE, user, instance);
    return instance;
  }

  public boolean tickleInstance(long handle) {
    return tickleInstance(new HProcessInstance(handle));
  }

  public boolean tickleInstance(Handle<? extends ProcessInstance> handle) {
    try (T transaction=startTransaction()) {
      return tickleInstance(transaction, handle);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean tickleInstance(final T transaction, final Handle<? extends ProcessInstance> handle) throws
          SQLException {
    getInstances().invalidateCache(handle);
    ProcessInstance instance = getInstances().get(transaction, handle);
    if (instance==null) { return false; }
    instance.tickle(transaction, mMessageService);
    return true;
  }


  public void tickleNode(final T transaction, final Handle<? extends ProcessNodeInstance> handle) throws SQLException {
    getNodeInstances().invalidateCache(handle);
    ProcessNodeInstance nodeInstance = getNodeInstances().get(transaction, handle);
    for(Handle<? extends ProcessNodeInstance> hpredecessor: nodeInstance.getDirectPredecessors()) {
      tickleNode(transaction, hpredecessor);
    }
    nodeInstance.tickle(transaction, mMessageService);
    getNodeInstances().invalidateCache(handle);
  }

  /**
   * Create a new process instance started by this process.
   *
   *
   * @param transaction
   * @param model The model to create and start an instance of.
   * @param name The name of the new instance.
   * @param payload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   * @throws SQLException When database operations fail.
   */
  public HProcessInstance startProcess(T transaction, final Principal user, final ProcessModelImpl model, final String name, final UUID uuid, final Node payload) throws SQLException {
    if (user == null) {
      throw new HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN, "Annonymous users are not allowed to start processes");
    }
    mSecurityProvider.ensurePermission(ProcessModelImpl.Permissions.INSTANTIATE, user);
    final ProcessInstance instance = new ProcessInstance(user, model, name, uuid, State.NEW, this);

    final HProcessInstance result = new HProcessInstance(getInstances().put(transaction, instance));
    instance.initialize(transaction);
    transaction.commit();
    try {
      instance.start(transaction, mMessageService, payload);
    } catch (Exception e) {
      Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error starting instance (it is already stored)", e);
    }
    return result;
  }

  /**
   * Convenience method to start a process based upon a process model handle.
   *
   * @param handle The process model to start a new instance for.
   * @param name The name of the new instance.
   * @param uuid The UUID for the instances. Helps with synchronization errors not exploding into mass instantiation.
   * @param payload The payload representing the parameters for the process.
   * @return A Handle to the {@link ProcessInstance}.
   * @throws SQLException
   */
  public HProcessInstance startProcess(T transaction, final Principal user, final Handle<? extends ProcessModelImpl> handle, final String name, UUID uuid, final Node payload) throws SQLException {
    ProcessModelImpl processModel = getProcessModels().get(transaction, handle);
    return startProcess(transaction, user, processModel, name, uuid, payload);
  }

  /**
   * Get the task with the given handle.
   *
   *
   * @param transaction
   * @param handle The handle of the task.
   * @return The handle
   * @throws SQLException
   * @todo change the parameter to a handle object.
   */
  public ProcessNodeInstance getNodeInstance(T transaction, final Handle<? extends ProcessNodeInstance> handle, final Principal user) throws SQLException {
    final ProcessNodeInstance result = getNodeInstances().get(transaction, handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, result);
    return result;
  }

  /**
   * Finish the process instance.
   *
   *
   * @param transaction
   * @param processInstance The process instance to finish.
   * @throws SQLException
   * @todo evaluate whether this should not retain some results
   */
  public void finishInstance(T transaction, final ProcessInstance processInstance) throws SQLException {
    // TODO evict these nodes from the cache (not too bad to keep them though)
//    for (ProcessNodeInstance childNode:pProcessInstance.getProcessNodeInstances()) {
//      getNodeInstances().invalidateModelCache(childNode);
//    }
    // TODO retain instance
    TransactionedHandleMap<ProcessInstance, T> instances = getInstances();
    instances.remove(transaction, processInstance);
  }

  public ProcessInstance cancelInstance(T transaction, Handle<? extends ProcessInstance> handle, Principal user) throws SQLException {
    ProcessInstance result = getInstances().get(transaction, handle);
    mSecurityProvider.ensurePermission(Permissions.CANCEL, user, result);
    try {
      // Should be removed internally to the map.
//      getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle.getHandle()));
      if(getInstances().remove(transaction, result)) {
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
  public void cancelAll(T transaction, final Principal user) throws SQLException {
    mSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, user);
    getNodeInstances().clear(transaction);
    getInstances().clear(transaction);
  }


  /**
   * Update the state of the given task
   *
   * @param handle Handle to the process instance.
   * @param newState The new state
   * @return
   * @throws SQLException
   */
  public TaskState updateTaskState(T transaction, final Handle<ProcessNodeInstance> handle, final TaskState newState, final Principal user) throws SQLException {
    final ProcessNodeInstance task = getNodeInstances().get(transaction, handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task);
    final ProcessInstance pi = task.getProcessInstance();
    synchronized (pi) {
      switch (newState) {
        case Sent:
          throw new IllegalArgumentException("Updating task state to initial state not possible");
        case Acknowledged:
          task.setState(transaction, newState); // Record the state, do nothing else.
          break;
        case Taken:
          pi.takeTask(transaction, mMessageService, task);
          break;
        case Started:
          pi.startTask(transaction, mMessageService, task);
          break;
        case Complete:
          throw new IllegalArgumentException("Finishing a task must be done by a separate method");
        case Failed:
          pi.failTask(transaction, mMessageService, task, null);
          break;
        case Cancelled:
          pi.cancelTask(transaction, mMessageService, task);
          break;
        default:
          throw new IllegalArgumentException("Unsupported state :"+newState);
      }
      return task.getState();
    }
  }

  public TaskState finishTask(T transaction, final Handle<ProcessNodeInstance> handle, final Node payload, final Principal user) throws SQLException {
    final ProcessNodeInstance task = getNodeInstances().get(handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task);
    final ProcessInstance pi = task.getProcessInstance();
    synchronized (pi) {
      pi.finishTask(transaction, mMessageService, task, payload);
      return task.getState();
    }
  }

  /**
   * This method is primarilly a convenience method for
   * {@link #finishTask(Transaction, Handle, Node, Principal)}.
   *
   *
   * @param handle The handle to finish.
   * @param resultSource The source that is parsed into DOM nodes and then passed on
   *          to {@link #finishTask(Transaction, Handle, Node, Principal)}
   */
  public void finishedTask(T transaction, final Handle<ProcessNodeInstance> handle, final DataSource resultSource, final Principal user) {
    InputSource result;
    try {
      result = resultSource==null ? null : new InputSource(resultSource.getInputStream());
    } catch (final IOException e) {
      throw new MessagingException(e);
    }
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document xml = db.parse(result);
      finishTask(transaction, handle, xml, user);

    } catch (final ParserConfigurationException e) {
      throw new MessagingException(e);
    } catch (final SAXException e) {
      throw new MessagingException(e);
    } catch (final IOException e) {
      throw new MessagingException(e);
    } catch (SQLException e) {
      try {
        transaction.rollback();
      } catch (SQLException e1) {
        MessagingException ex = new MessagingException(e1);
        ex.addSuppressed(e);;
        throw ex;
      }
      throw new MessagingException(e);
    }

  }

  public long registerNodeInstance(final T transaction, final ProcessNodeInstance instance) throws SQLException {
    if (instance.getHandle() >= 0) {
      throw new IllegalArgumentException("Process node already registered");
    }
    return getNodeInstances().put(transaction, instance);
  }

  /**
   * Handle the fact that this task has been cancelled.
   *
   *
   * @param transaction
   * @param handle
   * @throws SQLException
   */
  public void cancelledTask(T transaction, final Handle<ProcessNodeInstance> handle, final Principal user) throws SQLException {
    updateTaskState(transaction, handle, TaskState.Cancelled, user);
  }

  public void errorTask(T transaction, final Handle<ProcessNodeInstance> handle, final Throwable cause, final Principal user) throws SQLException {
    final ProcessNodeInstance task = getNodeInstances().get(transaction, handle);
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task);
    final ProcessInstance pi = task.getProcessInstance();
    pi.failTask(transaction, mMessageService, task, cause);
  }

  public void updateStorage(T transaction, ProcessNodeInstance processNodeInstance) throws SQLException {
    if (processNodeInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    getNodeInstances().set(transaction, processNodeInstance, processNodeInstance);
  }

  public void updateStorage(T transaction, ProcessInstance processInstance) throws SQLException {
    if (processInstance.getHandle()<0) {
      throw new IllegalArgumentException("You can't update storage state of an unregistered node");
    }
    getInstances().set(transaction, processInstance, processInstance);
  }

  public T startTransaction() {
    return mTransactionFactory.startTransaction();
  }

  public EndpointDescriptor getLocalEndpoint() {
    return mMessageService.getLocalEndpoint();
  }

}
