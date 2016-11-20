/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.*
import net.devrieze.util.db.DBTransaction
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.OwnerOnlySecurityProvider
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.*
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap
import nl.adaptivity.process.processModel.ProcessModelBase
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl.ExecutableSplitFactory
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Principal
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.activation.DataSource
import javax.naming.Context
import javax.naming.InitialContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException


private const val MODEL_CACHE_SIZE = 5
private const val NODE_CACHE_SIZE = 100
private const val INSTANCE_CACHE_SIZE = 10


private fun <T : Transaction, V:Any> wrapCache(base: MutableTransactionedHandleMap<V, T>,
                                               cacheSize: Int): MutableTransactionedHandleMap<V, T> {
  if (cacheSize <= 0) {
    return base
  }
  return CachingHandleMap<V, T>(base, cacheSize)
}

private fun <T : Transaction, V:Any> wrapCache(base: IMutableProcessModelMap<T>,
                                               cacheSize: Int): IMutableProcessModelMap<T> {
  if (cacheSize <= 0) {
    return base
  }
  return CachingProcessModelMap(base, cacheSize)
}


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
class ProcessEngine<T : Transaction> /* implements IProcessEngine */ {

  class DelegateProcessEngineData<T: Transaction>(
        private val transactionFactory: TransactionFactory<T>,
        override val processModels: IMutableProcessModelMap<T>,
        override val processInstances: MutableTransactionedHandleMap<ProcessInstance<T>, T>,
        override val processNodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>) : IProcessEngineData<T>(), TransactionFactory<T> by transactionFactory{

    private inner class DelegateEngineDataAccess(private val transaction: T) : MutableProcessEngineDataAccess<T> {
      override val instances: MutableHandleMap<ProcessInstance<T>>
        get() = this@DelegateProcessEngineData.processInstances.withTransaction(transaction)
      override val nodeInstances: MutableHandleMap<ProcessNodeInstance<T>>
        get() = this@DelegateProcessEngineData.processNodeInstances.withTransaction(transaction)
      override val processModels: IMutableProcessModelMapAccess
        get() = this@DelegateProcessEngineData.processModels.withTransaction(transaction)

      override fun commit() = transaction.commit()

      override fun rollback() = transaction.rollback()
    }

    override fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess<T> = DelegateEngineDataAccess(transaction)
  }

  class DBProcessEngineData : IProcessEngineData<DBTransaction>() {


    private inner class DBEngineDataAccess(private val transaction: DBTransaction) : MutableProcessEngineDataAccess<DBTransaction> {
      override val instances: MutableHandleMap<ProcessInstance<DBTransaction>>
        get() = this@DBProcessEngineData.processInstances.withTransaction(transaction)
      override val nodeInstances: MutableHandleMap<ProcessNodeInstance<DBTransaction>>
        get() = this@DBProcessEngineData.processNodeInstances.withTransaction(transaction)
      override val processModels: IMutableProcessModelMapAccess
        get() = this@DBProcessEngineData.processModels.withTransaction(transaction)

      override fun commit() = transaction.commit()

      override fun rollback() = transaction.rollback()

    }

    private val dbResource: javax.sql.DataSource by lazy {
      val context = InitialContext().lookup("java:/comp/env") as Context
      DbSet.resourceNameToDataSource(context, DB_RESOURCE)
    }

    lateinit var engine : ProcessEngine<DBTransaction>

    override val processInstances by lazy { wrapCache(ProcessInstanceMap(this, engine), INSTANCE_CACHE_SIZE) }

    override val processNodeInstances by lazy { wrapCache(ProcessNodeInstanceMap(this, engine), NODE_CACHE_SIZE) }

    override val processModels = wrapCache<DBTransaction, Any>(ProcessModelMap(this), MODEL_CACHE_SIZE)

    override fun createWriteDelegate(transaction: DBTransaction): MutableProcessEngineDataAccess<DBTransaction> {
      return DBEngineDataAccess(transaction)
    }

    override fun startTransaction(): DBTransaction {
      return DBTransaction(dbResource, ProcessEngineDB)
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
      return dbResource.connection
    }

    override fun isValidTransaction(transaction: Transaction): Boolean {
      return transaction is DBTransaction
    }
  }


  enum class Permissions : SecurityProvider.Permission {
    /** Attempt to find a model */
    FIND_MODEL,
    ADD_MODEL,
    ASSIGN_OWNERSHIP,
    VIEW_ALL_INSTANCES,
    CANCEL_ALL,
    UPDATE_MODEL,
    CHANGE_OWNERSHIP,
    VIEW_INSTANCE,
    CANCEL,
    LIST_INSTANCES,
    TICKLE_INSTANCE,
    TICKLE_NODE
  }

  private val engineData:IProcessEngineData<T>

  private val messageService: IMessageService<*, T, ProcessNodeInstance<T>>

  private var mSecurityProvider: SecurityProvider = OwnerOnlySecurityProvider("admin")

  /**
   * Create a new process engine.

   * @param messageService The service to use for actual sending of messages by
   * *          activities.
   */
  protected constructor(messageService: IMessageService<*, T, ProcessNodeInstance<T>>, engineData: IProcessEngineData<T>, transactionFactory: TransactionFactory<T>) {
    this.messageService = messageService
    this.engineData = engineData
  }

  /**
   * Testing constructor that does not need database access
   * @param messageService
   * *
   * @param processModels
   * *
   * @param processInstances
   * *
   * @param processNodeInstances
   */
  private constructor(messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                      engineData: IProcessEngineData<T>,
                      transactionFactory: TransactionFactory<T>,
                      processModels: IMutableProcessModelMap<T>,
                      processInstances: MutableTransactionedHandleMap<ProcessInstance<T>, T>, processNodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>) {
    this.messageService = messageService
    this.engineData = engineData
  }

  fun invalidateModelCache(handle: Handle<out ProcessModelImpl>) {
    engineData.invalidateCachePM(handle)
  }

  fun invalidateInstanceCache(handle: Handle<out ProcessInstance<T>>) {
    engineData.invalidateCachePI(handle)
  }

  fun invalidateNodeCache(handle: Handle<out ProcessNodeInstance<T>>) {
    engineData.invalidateCachePNI(handle)
  }

  /**
   * Get all process models loaded into the engine.

   * @return The list of process models.
   * *
   * @param transaction
   */
  fun getProcessModels(transaction: T): Iterable<ProcessModelImpl> {
    return engineData.inReadonlyTransaction(transaction) { processModels }
  }

  /**
   * Add a process model to the engine.
   * @param transaction
   *
   * @param basepm The process model to add.
   *
   * @return The processModel to add.
   *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun addProcessModel(transaction: T, basepm: ProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ProcessModelImpl> {
    mSecurityProvider.ensurePermission(Permissions.ADD_MODEL, user)

    return engineData.inWriteTransaction(transaction) {
      val pastHandle = basepm.getUuid()?.let { uuid ->
        processModels[uuid]
      }

      if (pastHandle!=null && pastHandle.valid) {
        updateProcessModel(transaction, pastHandle, basepm, user)
      } else {
        val uuid = basepm.getUuid() ?: UUID.randomUUID().apply { basepm.setUuid(this) }

        basepm.owner.let { baseOwner ->
          mSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, user, baseOwner)
        } ?: user.apply { basepm.setOwner(this) }

        val pm = ProcessModelImpl.from(basepm)

        ProcessModelRef(pm.name, processModels.put(pm), uuid)
      }

    }

  }

  /**
   * Get the process model with the given handle.

   * @param handle The handle to the process model.
   * *
   * @return The processModel.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun getProcessModel(transaction: T, handle: Handle<out ProcessModelImpl>, user: Principal): ProcessModelImpl? {
    return engineData.inWriteTransaction(transaction) {
      processModels[handle]?.apply {
        mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, this)
        normalize(ExecutableSplitFactory())
        if (uuid == null) {
          setUuid(UUID.randomUUID())
          processModels[handle] = this
        }
      }
    }
  }

  /**
   * Rename the process model with the given handle.

   * @param handle The handle to use.
   * *
   * @param newName The new name
   */
  @Throws(FileNotFoundException::class)
  fun renameProcessModel(user: Principal, handle: Handle<out ProcessModelImpl>, newName: String) {
    engineData.inWriteTransaction(user, mSecurityProvider.ensurePermission(Permissions.FIND_MODEL, user)) {
      val pm = processModels[handle].shouldExist(handle) ?: throw FileNotFoundException("The process model with the handle $handle does not exist")
      mSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm)
      pm.name = newName
      processModels[handle]= pm // set it to ensure update on the database
    }
  }

  @Throws(FileNotFoundException::class, SQLException::class)
  fun updateProcessModel(transaction: T, handle: Handle<out ProcessModelImpl>, processModel: ProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ProcessModelImpl> {
    engineData.inWriteTransaction(transaction) {
      val oldModel = processModels[handle] ?: throw FileNotFoundException("The model did not exist, instead post a new model.")

      if (oldModel.owner==SecurityProvider.SYSTEMPRINCIPAL) throw IllegalStateException("The old model has no owner")

      mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel)
      mSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, user, oldModel)

      if (processModel.owner == SecurityProvider.SYSTEMPRINCIPAL) { // If no owner was set, use the old one.
        processModel.setOwner(oldModel.owner)
      } else if (oldModel.owner.name != processModel.owner.name) {
        mSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, user, oldModel)
      }
      if (!processModels.contains(handle)) {
        throw FileNotFoundException("The process model with handle $handle could not be found")
      }

      return (processModel as? ProcessModelImpl ?: ProcessModelImpl.from(processModel)).apply {
        processModels[handle]= this
      }.ref
    }
  }

  @Throws(SQLException::class)
  fun removeProcessModel(transaction: T, handle: Handle<out ProcessModelImpl>, user: Principal): Boolean {
    engineData.inWriteTransaction(transaction) {
      val oldModel = processModels[handle].shouldExist(handle)
      mSecurityProvider.ensurePermission(SecureObject.Permissions.DELETE, user, oldModel)

      if (processModels.remove(handle)) {
        transaction.commit()
        return true
      }
      return false
    }
  }

  fun setSecurityProvider(securityProvider: SecurityProvider) {
    mSecurityProvider = securityProvider
  }

  /**
   * Get all process instances owned by the user.

   * @param user The current user in relation to whom we need to find the
   * *          instances.
   * *
   * @return All instances.
   */
  fun getOwnedProcessInstances(transaction: T, user: Principal?): Iterable<ProcessInstance<*>> {
    mSecurityProvider.ensurePermission(Permissions.LIST_INSTANCES, user)
    // If security allows this, return an empty list.
    engineData.inReadonlyTransaction(transaction){
      return instances.filter { instance -> instance.owner.name==user?.name }
    }
  }


  /**
   * Get all process instances visible to the user.

   * @param user The current user in relation to whom we need to find the
   * *          instances.
   * *
   * @return All instances.
   */
  fun getVisibleProcessInstances(transaction: T, user: Principal): Iterable<ProcessInstance<*>> {
    engineData.inReadonlyTransaction(transaction) {
      return instances.filter { mSecurityProvider.hasPermission(SecureObject.Permissions.READ, user, it) }
    }
  }

  @Throws(SQLException::class)
  fun getProcessInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): ProcessInstance<T> {
    return engineData.inReadonlyTransaction(transaction) {
      instances[handle].shouldExist(handle).apply {
        mSecurityProvider.ensurePermission(Permissions.VIEW_INSTANCE, user, this)
      }
    }
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: T, handle: Long, user: Principal): Boolean {
    return tickleInstance(transaction, Handles.handle<ProcessInstance<T>>(handle), user)
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): Boolean {
    engineData.invalidateCachePM(Handles.getInvalid())
    engineData.invalidateCachePI(Handles.getInvalid())
    engineData.invalidateCachePNI(Handles.getInvalid())

    engineData.inWriteTransaction(transaction) {
      val instance = instances[handle] ?: return false
      mSecurityProvider.ensurePermission(Permissions.TICKLE_INSTANCE, user, instance)
      instance.tickle(transaction, messageService)
      return true

    }
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleNode(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, user: Principal) {
    engineData.invalidateCachePNI(handle)
    engineData.inWriteTransaction(transaction) {
      val nodeInstance = nodeInstances[handle].shouldExist(handle)
      mSecurityProvider.ensurePermission(Permissions.TICKLE_NODE, user, nodeInstance)

      nodeInstance.directPredecessors.forEach { hPredecessor -> tickleNode(transaction, hPredecessor, user) }

      nodeInstance.tickle(transaction, messageService)
    }
    engineData.invalidateCachePNI(handle)
  }

  /**
   * Create a new process instance started by this process.


   * @param transaction
   * *
   * @param model The model to create and start an instance of.
   * *
   * @param name The name of the new instance.
   * *
   * @param payload The payload representing the parameters for the process.
   * *
   * @return A Handle to the [ProcessInstance].
   * *
   * @throws SQLException When database operations fail.
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  private fun startProcess(transaction: T,
                           user: Principal?,
                           model: ProcessModelImpl,
                           name: String,
                           uuid: UUID,
                           payload: Node?): HProcessInstance<T> {

    if (user == null) {
      throw HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN, "Annonymous users are not allowed to start processes")
    }
    mSecurityProvider.ensurePermission(ProcessModelImpl.Permissions.INSTANTIATE, user)

    val instance = ProcessInstance(user, model, name, uuid, State.NEW, this)

    engineData.inWriteTransaction(transaction) {
      val result = instances.put(instance).apply {
        instance.initialize(transaction)
        commit()
      }

      try {
        instance.start(transaction, messageService, payload)
      } catch (e: Exception) {
        Logger.getLogger(javaClass.name).log(Level.WARNING, "Error starting instance (it is already stored)", e)
        throw e
      }

      return HProcessInstance(result)
    }
  }

  /**
   * Convenience method to start a process based upon a process model handle.

   * @param handle The process model to start a new instance for.
   * *
   * @param name The name of the new instance.
   * *
   * @param uuid The UUID for the instances. Helps with synchronization errors not exploding into mass instantiation.
   * *
   * @param payload The payload representing the parameters for the process.
   * *
   * @return A Handle to the [ProcessInstance].
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  fun startProcess(transaction: T,
                   user: Principal,
                   handle: Handle<out ProcessModelImpl>,
                   name: String,
                   uuid: UUID,
                   payload: Node?): HProcessInstance<T> {
    val processModel = engineData.inWriteTransaction(transaction) {  processModels[handle].shouldExist(handle) }
    return startProcess(transaction, user, processModel, name, uuid, payload)
  }

  /**
   * Get the task with the given handle.


   * @param transaction
   * *
   * @param handle The handle of the task.
   * *
   * @return The handle
   * *
   * @throws SQLException
   * *
   * @todo change the parameter to a handle object.
   */
  @Throws(SQLException::class)
  fun getNodeInstance(transaction: T,
                      handle: Handle<out ProcessNodeInstance<T>>,
                      user: Principal): ProcessNodeInstance<T>? {
    engineData.inReadonlyTransaction(transaction) {
      return nodeInstances[handle].shouldExist(handle).apply {
        mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, this)
      }
    }
  }

  /**
   * Finish the process instance.


   * @param transaction
   * *
   * @param processInstance The process instance to finish.
   * *
   * @throws SQLException
   * *
   * @todo evaluate whether this should not retain some results
   */
  @Deprecated("")
  @Throws(SQLException::class)
  fun finishInstance(transaction: T, processInstance: ProcessInstance<T>) {
    finishInstance(transaction, processInstance.handle)
  }

  @Throws(SQLException::class)
  fun finishInstance(transaction: T, hProcessInstance: Handle<out ProcessInstance<T>>) {
    // TODO evict these nodes from the cache (not too bad to keep them though)
    //    for (ProcessNodeInstance childNode:pProcessInstance.getProcessNodeInstances()) {
    //      getNodeInstances().invalidateModelCache(childNode);
    //    }
    // TODO retain instance
    engineData.invalidateCachePI(hProcessInstance)
    engineData.inWriteTransaction(transaction) { instances.remove(hProcessInstance) }
  }

  @Throws(SQLException::class)
  fun cancelInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): ProcessInstance<*> {
    engineData.inWriteTransaction(transaction) {
      val instance = instances.get(handle).shouldExist(handle)
      mSecurityProvider.ensurePermission(Permissions.CANCEL, user, instance)
      try {
        // Should be removed internally to the map.
        //      getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle.getHandle()));
        if (instances.remove(instance.handle)) {
          return instance
        }
        throw ProcessException("The instance could not be cancelled")
      } catch (e: SQLException) {
        throw ProcessException("The instance could not be cancelled", e)
      }
    }
  }

  /**
   * Cancel all process instances and tasks in the engine.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun cancelAll(transaction: T, user: Principal) {
    mSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, user)
    engineData.inWriteTransaction(transaction) {
      nodeInstances.clear()
      instances.clear()
    }
  }


  /**
   * Update the state of the given task

   * @param handle Handle to the process instance.
   * *
   * @param newState The new state
   * *
   * @return
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  fun updateTaskState(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, newState: NodeInstanceState, user: Principal): NodeInstanceState {
    engineData.inWriteTransaction(transaction) {

      val task = nodeInstances[handle].shouldExist(handle)
      mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
      val pi = task.processInstance

      synchronized(pi) {
        when (newState) {
          Sent                                             -> throw IllegalArgumentException("Updating task state to initial state not possible")
          Acknowledged                                     -> task.setState(transaction,
                                                                            newState) // Record the state, do nothing else.
          Taken                                            -> pi.takeTask(transaction, messageService, task)
          IProcessNodeInstance.NodeInstanceState.Started   -> pi.startTask(transaction, messageService, task)
          IProcessNodeInstance.NodeInstanceState.Complete  -> throw IllegalArgumentException("Finishing a task must be done by a separate method")
        // TODO don't just make up a failure cause
          IProcessNodeInstance.NodeInstanceState.Failed    -> pi.failTask(transaction,
                                                                          messageService,
                                                                          task,
                                                                          IllegalArgumentException("Missing failure cause"))
          IProcessNodeInstance.NodeInstanceState.Cancelled -> pi.cancelTask(transaction, messageService, task)
          else                                             -> throw IllegalArgumentException("Unsupported state :" + newState)
        }
        return task.state
      }
    }
  }

  @Throws(SQLException::class)
  fun finishTask(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, payload: Node?, user: Principal): NodeInstanceState {
    engineData.inWriteTransaction(transaction) {
      val task = nodeInstances[handle].shouldExist(handle)
      mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
      val pi = task.processInstance
      try {
        synchronized(pi) {
          pi.finishTask(transaction, messageService, task, payload)
          return task.state
        }
      } catch (e: Exception) {
        engineData.invalidateCachePNI(handle)
        engineData.invalidateCachePI(pi.handle)
        throw e
      }

    }

  }

  /**
   * This method is primarilly a convenience method for
   * [.finishTask].


   * @param handle The handle to finish.
   * *
   * @param resultSource The source that is parsed into DOM nodes and then passed on
   * *          to [.finishTask]
   */
  fun finishedTask(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, resultSource: DataSource?, user: Principal) {
    try {
      val result = resultSource?.let { InputSource(it.inputStream) }

      val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      val xml = result?.let { db.parse(it) }
      finishTask(transaction, handle, xml, user)

    } catch (e: ParserConfigurationException) {
      throw MessagingException(e)
    } catch (e: SAXException) {
      throw MessagingException(e)
    } catch (e: SQLException) {
      throw MessagingException(e)
    } catch (e: IOException) {
      throw MessagingException(e)
    }

  }

  @Throws(SQLException::class)
  fun <N : ProcessNodeInstance<T>> registerNodeInstance(transaction: T, instance: N): ComparableHandle<N> {
    if (instance.getHandleValue() >= 0) {
      throw IllegalArgumentException("Process node already registered ($instance)")
    }
    return engineData.inWriteTransaction(transaction) { Handles.handle(nodeInstances.put(instance)) }
  }

  /**
   * Handle the fact that this task has been cancelled.


   * @param transaction
   * *
   * @param handle
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  fun cancelledTask(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, user: Principal) {
    updateTaskState(transaction, handle, NodeInstanceState.Cancelled, user)
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun errorTask(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, cause: Throwable, user: Principal) {
    engineData.inWriteTransaction(transaction) {
      val task = nodeInstances.get(handle).shouldExist(handle)
      mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
      val pi = task.processInstance
      pi.failTask(transaction, messageService, task, cause)
    }
  }

  @Throws(SQLException::class)
  fun updateStorage(transaction: T, processNodeInstance: ProcessNodeInstance<T>) {
    val handle = processNodeInstance.handle
    if (!handle.valid) {
      throw IllegalArgumentException("You can't update storage state of an unregistered node")
    }
    engineData.inWriteTransaction(transaction) {
      nodeInstances[handle] = processNodeInstance
    }
  }

  @Throws(SQLException::class)
  fun removeNodeInstance(transaction: T, handle: ComparableHandle<ProcessNodeInstance<T>>): Boolean {
    return engineData.inWriteTransaction(transaction) { nodeInstances.remove(handle) }
  }

  @Throws(SQLException::class)
  fun updateStorage(transaction: T, processInstance: ProcessInstance<T>) {
    val handle = processInstance.handle
    if (!handle.valid) {
      throw IllegalArgumentException("You can't update storage state of an unregistered node")
    }
    engineData.inWriteTransaction(transaction) {
      instances[handle]= processInstance
    }
  }

  fun startTransaction(): T {
    return engineData.startTransaction()
  }

  val localEndpoint: EndpointDescriptor
    get() = messageService.localEndpoint

  companion object {

    private val MODEL_CACHE_SIZE = 5
    private val NODE_CACHE_SIZE = 100
    private val INSTANCE_CACHE_SIZE = 10

    val CONTEXT_PATH = "java:/comp/env"
    val DB_RESOURCE = "jdbc/processengine"
    val DBRESOURCENAME = CONTEXT_PATH + '/' + DB_RESOURCE

    @JvmStatic
    fun newInstance(messageService: IMessageService<*, DBTransaction, ProcessNodeInstance<DBTransaction>>): ProcessEngine<*> {
      // TODO enable optional caching
      val engineData = DBProcessEngineData()
      val pe = ProcessEngine(messageService, engineData, engineData)
      engineData.engine = pe // STILL NEEDED to initialize the engine as the factories require the engine
      return pe
    }

    @JvmStatic
    @JvmName("newTestInstance")
    internal fun <T : Transaction> newTestInstance(messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                                                   transactionFactory: TransactionFactory<T>,
                                                   processModels: IMutableProcessModelMap<T>,
                                                   processInstances: MutableTransactionedHandleMap<ProcessInstance<T>, T>,
                                                   processNodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>,
                                                   autoTransition: Boolean): ProcessEngine<T> {

      val engineData = ProcessEngine.DelegateProcessEngineData<T>(transactionFactory, processModels, processInstances, processNodeInstances)

      return ProcessEngine(messageService, engineData, engineData, processModels, processInstances, processNodeInstances)
    }
  }

}
