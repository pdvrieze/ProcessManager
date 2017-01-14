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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.*
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.*
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.engine.processModel.AbstractProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.*
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.processModel.uuid
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Principal
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


private fun <T : ProcessTransaction, V:Any> wrapInstanceCache(base: MutableTransactionedHandleMap<V, T>,
                                                              cacheSize: Int): MutableTransactionedHandleMap<V, T> {
  if (cacheSize <= 0) {
    return base
  }
  return CachingHandleMap<V, T>(base, cacheSize)
}

private fun <T : ProcessTransaction> wrapNodeCache(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, T>,
                                                   cacheSize: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, T> {
  if (cacheSize <= 0) {
    return base
  }
  return CachingHandleMap<SecureObject<ProcessNodeInstance>, T>(base, cacheSize, { pni, handle -> if (pni.withPermission().getHandle()==handle) pni else pni.withPermission().builder().apply{ this.handle = Handles.handle(handle)}.build() })
}

private fun <T : ProcessTransaction, V:Any> wrapModelCache(base: IMutableProcessModelMap<T>,
                                                           cacheSize: Int): IMutableProcessModelMap<T> {
  if (cacheSize <= 0) {
    return base
  }
  return CachingProcessModelMap(base, cacheSize)
}


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
class ProcessEngine<TRXXX : ProcessTransaction>(private val messageService: IMessageService<*>,
                                                private val engineData: IProcessEngineData<TRXXX>) {

  class DelegateProcessEngineData<T: ProcessTransaction>(
      private val transactionFactory: ProcessTransactionFactory<T>,
      override val processModels: IMutableProcessModelMap<T>,
      override val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>,
      override val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, T>,
      private val messageService: IMessageService<*>) : IProcessEngineData<T>(), TransactionFactory<T> {

    private inner class DelegateEngineDataAccess(transaction: T) : AbstractProcessEngineDataAccess<T>(transaction) {
      override val instances: MutableHandleMap<SecureObject<ProcessInstance>>
        get() = this@DelegateProcessEngineData.processInstances.withTransaction(transaction)
      override val nodeInstances: MutableHandleMap<SecureObject<ProcessNodeInstance>>
        get() = this@DelegateProcessEngineData.processNodeInstances.withTransaction(transaction)
      override val processModels: IMutableProcessModelMapAccess
        get() = this@DelegateProcessEngineData.processModels.withTransaction(transaction)

      override fun messageService(): IMessageService<*> {
        return messageService
      }

      override fun invalidateCachePM(handle: Handle<out SecureObject<ExecutableProcessModel>>) {
        this@DelegateProcessEngineData.invalidateCachePM(handle)
      }

      override fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance>>) {
        this@DelegateProcessEngineData.invalidateCachePI(handle)
      }

      override fun invalidateCachePNI(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>) {
        this@DelegateProcessEngineData.invalidateCachePNI(handle)
      }

      override fun handleFinishedInstance(handle: ComparableHandle<out SecureObject<ProcessInstance>>) {
        // Ignore the completion for now. Just keep it in the engine.
      }
    }

    override fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess = DelegateEngineDataAccess(transaction)

    override fun startTransaction(): T = transactionFactory.startTransaction(this)

    override fun isValidTransaction(pTransaction: Transaction?): Boolean {
      return pTransaction is ProcessTransaction && pTransaction.readableEngineData==this
    }
  }

  class DBProcessEngineData(private val messageService: IMessageService<*>) : IProcessEngineData<ProcessDBTransaction>() {


    private inner class DBEngineDataAccess(transaction: ProcessDBTransaction) : AbstractProcessEngineDataAccess<ProcessDBTransaction>(transaction) {
      override val instances: MutableHandleMap<SecureObject<ProcessInstance>>
        get() = this@DBProcessEngineData.processInstances.withTransaction(transaction)
      override val nodeInstances: MutableHandleMap<SecureObject<ProcessNodeInstance>>
        get() = this@DBProcessEngineData.processNodeInstances.withTransaction(transaction)
      override val processModels: IMutableProcessModelMapAccess
        get() = this@DBProcessEngineData.processModels.withTransaction(transaction)

      override fun messageService(): IMessageService<*> {
        return this@DBProcessEngineData.messageService
      }

      override fun invalidateCachePM(handle: Handle<out SecureObject<ExecutableProcessModel>>) {
        this@DBProcessEngineData.invalidateCachePM(handle)
      }

      override fun invalidateCachePI(handle: Handle<out SecureObject<ProcessInstance>>) {
        this@DBProcessEngineData.invalidateCachePI(handle)
      }

      override fun invalidateCachePNI(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>) {
        this@DBProcessEngineData.invalidateCachePNI(handle)
      }

      override fun handleFinishedInstance(handle: ComparableHandle<out SecureObject<ProcessInstance>>) {
        // Do nothing at this point. In the future, this will probably lead the node intances to be deleted.
      }
    }

    private val dbResource: javax.sql.DataSource by lazy {
      val context = InitialContext().lookup("java:/comp/env") as Context
      DbSet.resourceNameToDataSource(context, DB_RESOURCE)
    }

    lateinit var engine : ProcessEngine<ProcessDBTransaction>

    override val processInstances by lazy { wrapInstanceCache(ProcessInstanceMap(this, engine), INSTANCE_CACHE_SIZE) }

    override val processNodeInstances by lazy { wrapNodeCache(ProcessNodeInstanceMap(this, engine), NODE_CACHE_SIZE) }

    override val processModels = wrapModelCache<ProcessDBTransaction, Any>(ProcessModelMap(this), MODEL_CACHE_SIZE)

    override fun createWriteDelegate(transaction: ProcessDBTransaction): MutableProcessEngineDataAccess {
      return DBEngineDataAccess(transaction)
    }

    override fun startTransaction(): ProcessDBTransaction {
      return ProcessDBTransaction(dbResource, ProcessEngineDB, this)
    }

    override fun isValidTransaction(transaction: Transaction): Boolean {
      return transaction is ProcessDBTransaction
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
    LIST_MODELS,
    CHANGE_OWNERSHIP,
    VIEW_INSTANCE,
    CANCEL,
    LIST_INSTANCES,
    TICKLE_INSTANCE,
    TICKLE_NODE,
    START_PROCESS;
  }

  private var mSecurityProvider: SecurityProvider = OwnerOnlySecurityProvider("admin")

  fun invalidateModelCache(handle: Handle<out SecureObject<ExecutableProcessModel>>) {
    engineData.invalidateCachePM(handle)
  }

  fun invalidateInstanceCache(handle: ComparableHandle<out SecureObject<ProcessInstance>>) {
    engineData.invalidateCachePI(handle)
  }

  fun invalidateNodeCache(handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>) {
    engineData.invalidateCachePNI(handle)
  }

  /**
   * Get all process models loaded into the engine.
   *
   * @return The list of process models.
   * *
   * @param transaction
   */
  fun getProcessModels(engineData: ProcessEngineDataAccess, user: Principal): Iterable<SecuredObject<ExecutableProcessModel>> {
    mSecurityProvider.ensurePermission(Permissions.LIST_MODELS, user)
    if (user == SecurityProvider.SYSTEMPRINCIPAL) return engineData.processModels
    return engineData.processModels.mapNotNull {
      it.ifPermitted(mSecurityProvider, SecureObject.Permissions.READ, user)
    }
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
  fun addProcessModel(transaction: TRXXX, basepm: RootProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel> {
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
        } ?: user.apply { basepm.owner=this }

        val pm = ExecutableProcessModel.from(basepm)

        ProcessModelRef(pm.name, processModels.put(pm), uuid)
      }

    }

  }

  /**
   * Get the process model with the given handle.
   *
   * @param handle The handle to the process model.
   *
   * @return The processModel.
   *
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun getProcessModel(transaction: TRXXX, handle: Handle<out ExecutableProcessModel>, user: Principal): ExecutableProcessModel? {
    return engineData.inWriteTransaction(transaction) {
      getProcessModel(this, handle, user)
    }
  }

  fun getProcessModel(dataAccess: ProcessEngineDataAccess, handle: Handle<out SecureObject<ExecutableProcessModel>>, user: Principal): ExecutableProcessModel? {
    return dataAccess.processModels[handle]?.withPermission(mSecurityProvider, SecureObject.Permissions.READ, user) { processModel ->
      if (processModel.uuid ==null && dataAccess is MutableProcessEngineDataAccess) {
        processModel.update {
          uuid = UUID.randomUUID()
        }.apply { dataAccess.processModels[handle] = this }
      } else {
        processModel
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
  fun renameProcessModel(user: Principal, handle: Handle<out ExecutableProcessModel>, newName: String) {
    engineData.inWriteTransaction(user, mSecurityProvider.ensurePermission(Permissions.FIND_MODEL, user)) {
      processModels[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.RENAME, user) { pm->
        mSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm)
        pm.setName(newName)
        processModels[handle]= pm // set it to ensure update on the database
      }
    }
  }

  @Throws(FileNotFoundException::class, SQLException::class)
  fun updateProcessModel(transaction: TRXXX, handle: Handle<out SecureObject<ExecutableProcessModel>>, processModel: RootProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel> {
    engineData.inWriteTransaction(transaction) {
      return updateProcessModel(this, handle, processModel, user)
    }
  }

  fun updateProcessModel(engineData: MutableProcessEngineDataAccess, handle: Handle<out SecureObject<ExecutableProcessModel>>, processModel: RootProcessModelBase<*, *>, user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableModelCommon, ExecutableProcessModel> {
    val oldModel = engineData.processModels[handle] ?: throw FileNotFoundException("The model did not exist, instead post a new model.")

    if (oldModel.owner == SecurityProvider.SYSTEMPRINCIPAL) throw IllegalStateException("The old model has no owner")

    mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel)
    mSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, user, oldModel)

    if (processModel.owner == SecurityProvider.SYSTEMPRINCIPAL) { // If no owner was set, use the old one.
      processModel.owner = oldModel.owner
    } else if (oldModel.owner.name != processModel.owner.name) {
      mSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, user, oldModel)
    }
    if (!engineData.processModels.contains(handle)) {
      throw FileNotFoundException("The process model with handle $handle could not be found")
    }

    return (processModel as? ExecutableProcessModel ?: ExecutableProcessModel.from(processModel)).apply {
      engineData.processModels[handle] = this
    }.getRef()
  }

  @Throws(SQLException::class)
  fun removeProcessModel(transaction: TRXXX, handle: Handle<out ExecutableProcessModel>, user: Principal): Boolean {
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
  fun getOwnedProcessInstances(transaction: TRXXX, user: Principal): Iterable<ProcessInstance> {
    mSecurityProvider.ensurePermission(Permissions.LIST_INSTANCES, user)
    // If security allows this, return an empty list.
    engineData.inReadonlyTransaction(transaction){
      return instances.map {
        it.withPermission(mSecurityProvider, SecureObject.Permissions.READ, user) { it }
      }.filter { instance -> instance.owner.name== user.name }
    }
  }


  /**
   * Get all process instances visible to the user.

   * @param user The current user in relation to whom we need to find the
   * *          instances.
   * *
   * @return All instances.
   */
  fun getVisibleProcessInstances(transaction: TRXXX, user: Principal): Iterable<ProcessInstance> {
    engineData.inReadonlyTransaction(transaction) {
      return instances.map { it.withPermission() }.filter { mSecurityProvider.hasPermission(SecureObject.Permissions.READ, user, it) }
    }
  }

  @Throws(SQLException::class)
  fun getProcessInstance(transaction: TRXXX, handle: ComparableHandle<out SecureObject<ProcessInstance>>, user: Principal): ProcessInstance {
    return engineData.inReadonlyTransaction(transaction) {
      instances[handle].shouldExist(handle).withPermission(mSecurityProvider, Permissions.VIEW_INSTANCE, user) {
        it
      }
    }
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: TRXXX, handle: Long, user: Principal): Boolean {
    return tickleInstance(transaction, Handles.handle(handle), user)
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun tickleInstance(transaction: TRXXX, handle: ComparableHandle<out SecureObject<ProcessInstance>>, user: Principal): Boolean {
    transaction.writableEngineData.run {
      invalidateCachePM(Handles.getInvalid())
      invalidateCachePI(Handles.getInvalid())
      invalidateCachePNI(Handles.getInvalid())

      (instances[handle] ?: return false).withPermission(mSecurityProvider, Permissions.TICKLE_INSTANCE, user) {

        it.tickle(transaction, messageService)

        return true
      }

    }

  }

  /**
   * Create a new process instance started by this process.
   *
   * @param transaction
   *
   * @param model The model to create and start an instance of.
   *
   * @param name The name of the new instance.
   *
   * @param payload The payload representing the parameters for the process.
   *
   * @return A Handle to the [ProcessInstance].
   *
   * @throws SQLException When database operations fail.
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  private fun startProcess(transaction: TRXXX,
                           user: Principal?,
                           model: SecureObject<ExecutableProcessModel>,
                           name: String,
                           uuid: UUID,
                           parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance>>,
                           payload: Node?): HProcessInstance {

    if (user == null) {
      throw HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN, "Annonymous users are not allowed to start processes")
    }
    val instance = model.withPermission(mSecurityProvider, ExecutableProcessModel.Permissions.INSTANTIATE, user) {
      ProcessInstance(transaction.writableEngineData, it, parentActivity) {
        this.instancename = name
        this.uuid = uuid
        this.state = State.NEW
        this.owner = user
      }
    }

    engineData.inWriteTransaction(transaction) {
      val resultHandle = instances.put(instance)
      instance(resultHandle).withPermission().let { instance ->
        assert(instance.getHandle().handleValue==resultHandle.handleValue)
        instance.initialize(transaction)
      }.let { instance ->
        commit()

        try {
          instance.start(transaction, messageService, payload)
        } catch (e: Exception) {
          Logger.getLogger(javaClass.name).log(Level.WARNING, "Error starting instance (it is already stored)", e)
          throw e
        }
      }

      return HProcessInstance(resultHandle)
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
  fun startProcess(transaction: TRXXX,
                   user: Principal,
                   handle: Handle<SecureObject<ExecutableProcessModel>>,
                   name: String,
                   uuid: UUID,
                   payload: Node?): HProcessInstance {
    engineData.inWriteTransaction(transaction) {
      processModels[handle].shouldExist(handle)
    }.let { processModel ->
      return startProcess(transaction, user, processModel, name, uuid, Handles.getInvalid(), payload)
    }
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
  fun getNodeInstance(transaction: TRXXX,
                      handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>,
                      user: Principal): ProcessNodeInstance? {
    engineData.inReadonlyTransaction(transaction) {
      return nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.READ, user) {
        it
      }
    }
  }

  /**
   * Finish the process instance.
   *
   * @param transaction
   *
   * @param processInstance The process instance to finish.
   *
   * @throws SQLException
   *
   * @todo evaluate whether this should not retain some results
   */
  @Deprecated("")
  @Throws(SQLException::class)
  fun finishInstance(transaction: TRXXX, processInstance: ProcessInstance) {
    finishInstance(transaction, processInstance.getHandle())
  }

  @Throws(SQLException::class)
  fun finishInstance(transaction: TRXXX, hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>) {
    // TODO evict these nodes from the cache (not too bad to keep them though)
    //    for (ProcessNodeInstance childNode:pProcessInstance.getProcessNodeInstances()) {
    //      getNodeInstances().invalidateModelCache(childNode);
    //    }
    // TODO retain instance
    engineData.invalidateCachePI(hProcessInstance)
    engineData.inWriteTransaction(transaction) { instances.remove(hProcessInstance) }
  }

  @Throws(SQLException::class)
  fun cancelInstance(transaction: TRXXX, handle: Handle<out SecureObject<ProcessInstance>>, user: Principal): ProcessInstance {
    engineData.inWriteTransaction(transaction) {
      instances.get(handle).shouldExist(handle).withPermission(mSecurityProvider, Permissions.CANCEL, user) { instance ->
        try {
          // Should be removed internally to the map.
          //      getNodeInstances().removeAll(pTransaction, ProcessNodeInstanceMap.COL_HPROCESSINSTANCE+" = ?",Long.valueOf(pHandle.getHandle()));
          if (instances.remove(instance.getHandle())) {
            return instance
          }
          throw ProcessException("The instance could not be cancelled")
        } catch (e: SQLException) {
          throw ProcessException("The instance could not be cancelled", e)
        }
      }
    }
  }

  /**
   * Cancel all process instances and tasks in the engine.
   * @throws SQLException
   */
  @Throws(SQLException::class)
  fun cancelAll(transaction: TRXXX, user: Principal) {
    mSecurityProvider.ensurePermission(Permissions.CANCEL_ALL, user)
    engineData.inWriteTransaction(transaction) {
      (nodeInstances as MutableHandleMap).clear()
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
  fun updateTaskState(transaction: TRXXX, handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, newState: NodeInstanceState, user: Principal): NodeInstanceState {
    transaction.writableEngineData.run {

      nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.UPDATE, user) { task ->

        val pi = instance(task.hProcessInstance).withPermission()

        synchronized(pi) { // XXX Should not be needed if pi is immutable
          when (newState) {
            Sent         -> throw IllegalArgumentException("Updating task state to initial state not possible")
            Acknowledged -> return task.update(transaction.writableEngineData) { state = newState }.node.state // Record the state, do nothing else.
            Taken        -> ProcessInstance.Updater(pi).takeTask(transaction.writableEngineData, task).node.state
            Started      -> task.startTask(this, pi)
            Complete     -> throw IllegalArgumentException("Finishing a task must be done by a separate method")
          // TODO don't just make up a failure cause
            Failed       -> task.failTask(this, pi, IllegalArgumentException("Missing failure cause"))
            Cancelled    -> task.cancelTask(this, pi)
            else         -> throw IllegalArgumentException("Unsupported state :" + newState)
          }
          return task.state
        }
      }
    }
  }

  @Throws(SQLException::class)
  fun finishTask(transaction: TRXXX, handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, payload: Node?, user: Principal): ProcessNodeInstance {
    engineData.inWriteTransaction(transaction) {
      nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.UPDATE, user) { task ->
        val pi = instance(task.hProcessInstance).withPermission()
        try {
          synchronized(pi) {
            return pi.finishTask(this, task, payload).node
          }
        } catch (e: Exception) {
          engineData.invalidateCachePNI(handle)
          engineData.invalidateCachePI(pi.getHandle())
          throw e
        }
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
  fun finishedTask(transaction: TRXXX, handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, resultSource: DataSource?, user: Principal) {
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

  /**
   * Handle the fact that this task has been cancelled.
   *
   * @param transaction
   * *
   * @param handle
   * *
   * @throws SQLException
   */
  @Throws(SQLException::class, FileNotFoundException::class)
  fun cancelledTask(transaction: TRXXX, handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, user: Principal) {
    updateTaskState(transaction, handle, Cancelled, user)
  }

  @Throws(SQLException::class, FileNotFoundException::class)
  fun errorTask(transaction: TRXXX, handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>, cause: Throwable, user: Principal) {
    engineData.inWriteTransaction(transaction) {
      nodeInstances.get(handle).shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.UPDATE, user) { task ->
        task.failTask(this, instance(task.hProcessInstance).withPermission(), cause)
      }
    }
  }

  @Throws(SQLException::class)
  fun updateStorage(transaction: TRXXX, processInstance: ProcessInstance) {
    val handle = processInstance.getHandle()
    if (!handle.valid) {
      throw IllegalArgumentException("You can't update storage state of an unregistered node")
    }
    engineData.inWriteTransaction(transaction) {
      instances[handle]= processInstance
    }
  }

  fun startTransaction(): TRXXX {
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
    fun newInstance(messageService: IMessageService<*>): ProcessEngine<ProcessDBTransaction> {
      // TODO enable optional caching
      val engineData = DBProcessEngineData(messageService)
      val pe = ProcessEngine(messageService, engineData)
      engineData.engine = pe // STILL NEEDED to initialize the engine as the factories require the engine
      return pe
    }

    @JvmStatic
    @JvmName("newTestInstance")
    internal fun <T : ProcessTransaction> newTestInstance(messageService: IMessageService<*>,
                                                          transactionFactory: ProcessTransactionFactory<T>,
                                                          processModels: IMutableProcessModelMap<T>,
                                                          processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>,
                                                          processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, T>,
                                                          autoTransition: Boolean): ProcessEngine<T> {

      val engineData = ProcessEngine.DelegateProcessEngineData<T>(transactionFactory, processModels, processInstances, processNodeInstances, messageService)

      return ProcessEngine(messageService, engineData)
    }
  }

}
