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
import net.devrieze.util.security.PermissiveProvider
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
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessModelBase
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelImpl
import nl.adaptivity.process.processModel.engine.ProcessModelRef
import nl.adaptivity.process.processModel.engine.ProcessModelRef.get
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl.ExecutableSplitFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB

import javax.activation.DataSource
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Principal
import java.sql.Connection
import java.sql.SQLException
import java.util.ArrayList
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
class ProcessEngine<T : Transaction> /* implements IProcessEngine */ {


  private class MyDBTransactionFactory internal constructor() : TransactionFactory<DBTransaction> {
    private val mContext: Context

    private var mDBResource: javax.sql.DataSource? = null

    init {
      try {
        val ic = InitialContext()
        mContext = ic.lookup("java:/comp/env") as Context
      } catch (e: NamingException) {
        throw RuntimeException(e)
      }

    }

    private val dbResource: javax.sql.DataSource
      get() {
        return mDBResource?: DbSet.resourceNameToDataSource(mContext, DB_RESOURCE).apply { mDBResource = this }
      }

    override fun startTransaction(): DBTransaction {
      return DBTransaction(dbResource, ProcessEngineDB)
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
      return mDBResource!!.connection
    }

    override fun isValidTransaction(transaction: Transaction): Boolean {
      return transaction is DBTransaction
    }
  }

  enum class Permissions : SecurityProvider.Permission {
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

  private val mStringCache = StringCacheImpl()
  private val mTransactionFactory: TransactionFactory<out T>

  private lateinit var instances: MutableTransactionedHandleMap<ProcessInstance<T>, T>

  private lateinit var nodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>

  private lateinit var mProcessModels: IMutableProcessModelMap<T>

  private val mMessageService: IMessageService<*, T, ProcessNodeInstance<T>>

  private var mSecurityProvider: SecurityProvider = PermissiveProvider()

  /**
   * Create a new process engine.

   * @param messageService The service to use for actual sending of messages by
   * *          activities.
   */
  protected constructor(messageService: IMessageService<*, T, ProcessNodeInstance<T>>, transactionFactory: TransactionFactory<T>) {
    mMessageService = messageService
    mTransactionFactory = transactionFactory
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
                      transactionFactory: TransactionFactory<out T>,
                      processModels: IMutableProcessModelMap<T>,
                      processInstances: MutableTransactionedHandleMap<ProcessInstance<T>, T>,
                      processNodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>) {
    mMessageService = messageService
    mProcessModels = processModels
    mTransactionFactory = transactionFactory
    instances = processInstances
    nodeInstances = processNodeInstances
  }

  fun invalidateModelCache(handle: Handle<out ProcessModelImpl>) {
    mProcessModels!!.invalidateCache(handle)
  }

  fun invalidateInstanceCache(handle: Handle<out ProcessInstance<T>>) {
    instances!!.invalidateCache(handle)
  }

  fun invalidateNodeCache(handle: Handle<out ProcessNodeInstance<T>>) {
    nodeInstances!!.invalidateCache(handle)
  }

  /**
   * Get all process models loaded into the engine.

   * @return The list of process models.
   * *
   * @param transaction
   */
  fun getProcessModels(transaction: T): Iterable<ProcessModelImpl> {
    return processModels.iterable(transaction)
  }

  /**
   * Add a process model to the engine.


   * @param transaction
   * *
   * @param basepm The process model to add.
   * *
   * @return The processModel to add.
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun addProcessModel(transaction: T, basepm: ProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ProcessModelImpl> {
    mSecurityProvider.ensurePermission(Permissions.ADD_MODEL, user)

    val uuid: UUID = basepm.getUuid()?.apply {
      processModels.getModelWithUuid(transaction, this)?.let { handle ->
        if (handle.valid) {
          // When this model already exists update it. That function will verify permission for that operation
          return updateProcessModel(transaction, handle, basepm, user)
        }
      }
    } ?: UUID.randomUUID().apply { basepm.setUuid(this) }

    basepm.getOwner()?.let { baseOwner ->
      mSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, user, baseOwner)
    } ?: user.apply { basepm.setOwner(this) }

    val pm = ProcessModelImpl.from(basepm).apply { cacheStrings(mStringCache) }

    return ProcessModelRef(pm.name, processModels.put(transaction, pm), uuid)
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
    return processModels.inMutableTransaction(transaction) {
      this[handle]?.apply {
        mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, this)
        normalize(ExecutableSplitFactory())
        if (uuid == null) {
          setUuid(UUID.randomUUID())
          this@inMutableTransaction[handle] = this
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
    try {
      startTransaction().use { transaction ->
        val pm = processModels[transaction, handle] ?: throw FileNotFoundException("The process model with the handle $handle does not exist")
        mSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm)
        pm.name = newName
        processModels.set(transaction, handle, pm) // set it to ensure update on the database
      }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  @Throws(FileNotFoundException::class, SQLException::class)
  fun updateProcessModel(transaction: T, handle: Handle<out ProcessModelImpl>, processModel: ProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ProcessModelImpl> {
    processModels.inMutableTransaction(transaction) {
      val oldModel = this[handle] ?: throw FileNotFoundException("The model did not exist, instead post a new model.")

      val oldModelOwner = oldModel.owner ?: throw IllegalStateException("The old model has no owner")

      mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel)
      mSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, user, oldModel)

      if (processModel.getOwner() == null) { // If no owner was set, use the old one.
        processModel.setOwner(oldModelOwner)
      } else if (oldModelOwner.name != processModel.getOwner()!!.name) {
        mSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, user, oldModel)
      }
      if (!processModels.contains(transaction, handle)) {
        throw FileNotFoundException("The process model with handle $handle could not be found")
      }

      return (processModel as? ProcessModelImpl ?: ProcessModelImpl.from(processModel)).apply {
        set(handle, this)
      }.ref
    }
  }

  @Throws(SQLException::class)
  fun removeProcessModel(transaction: T, handle: Handle<out ProcessModelImpl>, user: Principal): Boolean {
    val oldModel = processModels[transaction, handle]
    mSecurityProvider.ensurePermission(SecureObject.Permissions.DELETE, user, oldModel)

    @Suppress("UNCHECKED_CAST")
    if (mProcessModels == null) {

      // TODO Hack to use the db backed implementation here
      val transactionFactory = mTransactionFactory as TransactionFactory<DBTransaction>
      val map = ProcessModelMap(transactionFactory, mStringCache) as IMutableProcessModelMap<T>
      mProcessModels = map
    }

    if (mProcessModels!!.remove(transaction, handle)) {
      transaction.commit()
      return true
    }
    return false
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
    return instances?.inTransaction(transaction) {
        this.iterator().asSequence().filter { instance -> instance.owner.name==user?.name }.toList()
    } ?: emptyList()
  }


  private // TODO Hack to use the db backed implementation here
  val processModels: IMutableProcessModelMap<T>
    get() {
      return mProcessModels?: (ProcessModelMap(mTransactionFactory as TransactionFactory<DBTransaction>, mStringCache) as IMutableProcessModelMap<T>)
            .apply { mProcessModels = this }
    }

  /**
   * Get all process instances visible to the user.

   * @param user The current user in relation to whom we need to find the
   * *          instances.
   * *
   * @return All instances.
   */
  fun getVisibleProcessInstances(transaction: T, user: Principal): Iterable<ProcessInstance<*>> {
    return instances?.iterable(transaction)?.filter { mSecurityProvider.hasPermission(SecureObject.Permissions.READ, user, it) } ?: emptyList()
  }

  @Throws(SQLException::class)
  fun getProcessInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): ProcessInstance<T> {
    val instance = instances.inTransaction(transaction) { get(handle).shouldExist(handle) }
    mSecurityProvider.ensurePermission(Permissions.VIEW_INSTANCE, user, instance)
    return instance
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: T, handle: Long, user: Principal): Boolean {
    return tickleInstance(transaction, Handles.handle<ProcessInstance<T>>(handle), user)
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): Boolean {
    processModels.invalidateCache() // TODO be more specific
    nodeInstances.invalidateCache() // TODO be more specific
    instances.invalidateCache(handle)
    val instance = instances.get(transaction, handle) ?: return false
    mSecurityProvider.ensurePermission(Permissions.TICKLE_INSTANCE, user, instance)
    instance.tickle(transaction, mMessageService)
    return true
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleNode(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, user: Principal) {
    nodeInstances.invalidateCache(handle)
    val nodeInstance = nodeInstances.get(transaction, handle)
    mSecurityProvider.ensurePermission(Permissions.TICKLE_NODE, user, nodeInstance)
    if (nodeInstance == null) {
      throw FileNotFoundException("The node instance with the given handle does not exist")
    }
    for (hPredecessor in nodeInstance.directPredecessors) {
      tickleNode(transaction, hPredecessor, user)
    }
    nodeInstance.tickle(transaction, mMessageService)
    nodeInstances.invalidateCache(handle)
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
                           model: ProcessModelImpl?,
                           name: String,
                           uuid: UUID,
                           payload: Node?): HProcessInstance<T> {
    if (model == null) throw FileNotFoundException("The process model does not exist")
    if (user == null) {
      throw HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN,
                                  "Annonymous users are not allowed to start processes")
    }
    mSecurityProvider.ensurePermission(ProcessModelImpl.Permissions.INSTANTIATE, user)
    val instance = ProcessInstance(user, model, name, uuid, State.NEW, this)

    val result = HProcessInstance(instances.put(transaction, instance))
    instance.initialize(transaction)
    transaction.commit()
    try {
      instance.start(transaction, mMessageService, payload)
    } catch (e: Exception) {
      Logger.getLogger(javaClass.name).log(Level.WARNING, "Error starting instance (it is already stored)", e)
    }

    return result
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
    val processModel = processModels[transaction, handle]
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
    val result = nodeInstances.get(transaction, handle)
    mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, result)
    return result
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
    instances!!.remove(transaction, hProcessInstance)
  }

  @Throws(SQLException::class)
  fun cancelInstance(transaction: T, handle: Handle<out ProcessInstance<T>>, user: Principal): ProcessInstance<*> {
    val result = instances.get(transaction, handle)
    mSecurityProvider.ensurePermission(Permissions.CANCEL, user, result)
    try {
      // Should be removed internally to the map.
      //      getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle.getHandle()));
      if (instances!!.remove(transaction, result!!.handle)) {
        return result
      }
      throw ProcessException("The instance could not be cancelled")
    } catch (e: SQLException) {
      throw ProcessException("The instance could not be cancelled", e)
    }

  }

  /**
   * Cancel all process instances and tasks in the engine.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun cancelAll(transaction: T, user: Principal) {
    mSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, user)
    nodeInstances.clear(transaction)
    instances.clear(transaction)
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
    val task = nodeInstances.get(transaction,
                                 handle) ?: throw FileNotFoundException("The given instance does not exist")
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
    val pi = task.processInstance

    synchronized(pi) {
      when (newState) {
        Sent         -> throw IllegalArgumentException("Updating task state to initial state not possible")
        Acknowledged -> task.setState(transaction,
                                                                             newState) // Record the state, do nothing else.
        Taken        -> pi.takeTask(transaction, mMessageService, task)
        IProcessNodeInstance.NodeInstanceState.Started      -> pi.startTask(transaction, mMessageService, task)
        IProcessNodeInstance.NodeInstanceState.Complete     -> throw IllegalArgumentException("Finishing a task must be done by a separate method")
        // TODO don't just make up a failure cause
        IProcessNodeInstance.NodeInstanceState.Failed       -> pi.failTask(transaction, mMessageService, task, IllegalArgumentException("Missing failure cause"))
        IProcessNodeInstance.NodeInstanceState.Cancelled    -> pi.cancelTask(transaction, mMessageService, task)
        else                                                -> throw IllegalArgumentException("Unsupported state :" + newState)
      }
      return task.state
    }
  }

  @Throws(SQLException::class)
  fun finishTask(transaction: T, handle: Handle<out ProcessNodeInstance<T>>, payload: Node?, user: Principal): NodeInstanceState {
    val task = nodeInstances.get(transaction, handle)
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
    val pi = task!!.processInstance
    try {
      synchronized(pi) {
        pi.finishTask(transaction, mMessageService, task, payload)
        return task.state
      }
    } catch (e: Exception) {
      nodeInstances.invalidateCache(handle)
      instances.invalidateCache(pi.handle)
      throw e
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
    return nodeInstances.put(transaction, instance)
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
    val task = nodeInstances.get(transaction,
                                 handle) ?: throw FileNotFoundException("The given node instance does not exist")
    mSecurityProvider.ensurePermission(SecureObject.Permissions.UPDATE, user, task)
    val pi = task.processInstance
    pi.failTask(transaction, mMessageService, task, cause)
  }

  @Throws(SQLException::class)
  fun updateStorage(transaction: T, processNodeInstance: ProcessNodeInstance<T>) {
    val handle = processNodeInstance.handle
    if (!handle.valid) {
      throw IllegalArgumentException("You can't update storage state of an unregistered node")
    }
    nodeInstances.set(transaction, handle, processNodeInstance)
  }

  @Throws(SQLException::class)
  fun removeNodeInstance(transaction: T, handle: ComparableHandle<ProcessNodeInstance<T>>): Boolean {
    return nodeInstances!!.remove(transaction, handle)
  }

  @Throws(SQLException::class)
  fun updateStorage(transaction: T, processInstance: ProcessInstance<T>) {
    val handle = processInstance.handle
    if (!handle.valid) {
      throw IllegalArgumentException("You can't update storage state of an unregistered node")
    }
    instances.set(transaction, handle, processInstance)
  }

  fun startTransaction(): T {
    return mTransactionFactory.startTransaction()
  }

  val localEndpoint: EndpointDescriptor
    get() = mMessageService.localEndpoint

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
      val transactionFactory = MyDBTransactionFactory()
      val pe = ProcessEngine(messageService, transactionFactory)
      pe.instances = wrapCache(ProcessInstanceMap(transactionFactory, pe), INSTANCE_CACHE_SIZE)
      pe.nodeInstances = wrapCache(ProcessNodeInstanceMap(transactionFactory, pe), NODE_CACHE_SIZE)
      pe.mProcessModels = wrapCache<DBTransaction, Any>(ProcessModelMap(transactionFactory, pe.mStringCache),
                                                        MODEL_CACHE_SIZE)
      return pe
    }

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

    @JvmStatic
    @JvmName("newTestInstance")
    internal fun <T : Transaction> newTestInstance(messageService: IMessageService<*, T, ProcessNodeInstance<T>>,
                                                   transactionFactory: TransactionFactory<out T>,
                                                   processModels: IMutableProcessModelMap<T>,
                                                   processInstances: MutableTransactionedHandleMap<ProcessInstance<T>, T>,
                                                   processNodeInstances: MutableTransactionedHandleMap<ProcessNodeInstance<T>, T>,
                                                   autoTransition: Boolean): ProcessEngine<T> {
      return ProcessEngine(messageService, transactionFactory, processModels, processInstances, processNodeInstances)
    }
  }

}
