/*
 * Copyright (c) 2019.
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
import net.devrieze.util.Transaction
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.*
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.engine.impl.Logger
import nl.adaptivity.process.engine.processModel.AbstractProcessEngineDataAccess
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.*
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Principal
import java.sql.SQLException
import java.util.*
import java.util.logging.Level
import javax.activation.DataSource
import javax.naming.Context
import javax.naming.InitialContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException


private const val MODEL_CACHE_SIZE = 5
private const val NODE_CACHE_SIZE = 100
private const val INSTANCE_CACHE_SIZE = 10


private fun <T : ProcessTransaction, V : Any> wrapInstanceCache(base: MutableTransactionedHandleMap<V, T>,
                                                                cacheSize: Int): MutableTransactionedHandleMap<V, T> {
    if (cacheSize <= 0) {
        return base
    }
    return CachingHandleMap(base, cacheSize)
}

private fun wrapDBInstanceCache(base: ProcessInstanceMap,
                                cacheSize: Int): MutableTransactionedHandleMap<SecureObject<ProcessInstance>, ProcessDBTransaction> {
    if (cacheSize <= 0) {
        return base
    }
    return ProcessInstanceMap.Cache<ProcessDBTransaction>(base, cacheSize)
}

private fun <T : ProcessTransaction> wrapNodeCache(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T>,
                                                   cacheSize: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T> {
    if (cacheSize <= 0) {
        return base
    }
    return CachingHandleMap(base, cacheSize, { tr, pni, handle ->
        if (pni.withPermission().getHandle() == handle) {
            pni
        } else {
            val piBuilder = tr.readableEngineData.instance(
                pni.withPermission().hProcessInstance).withPermission().builder()
            pni.withPermission().builder(piBuilder).also { it.handle = handle(handle) }.build()
        }
    })

}

private fun <T : ProcessTransaction> wrapModelCache(base: IMutableProcessModelMap<T>,
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

    class DelegateProcessEngineData<T : ProcessTransaction>(
        private val transactionFactory: ProcessTransactionFactory<T>,
        override val processModels: IMutableProcessModelMap<T>,
        override val processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>,
        override val processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T>,
        private val messageService: IMessageService<*>,
        override val logger: nl.adaptivity.process.engine.impl.Logger) : IProcessEngineData<T>(), TransactionFactory<T> {

        private inner class DelegateEngineDataAccess(transaction: T) : AbstractProcessEngineDataAccess<T>(transaction) {
            override val instances: MutableHandleMap<SecureObject<ProcessInstance>>
                get() = this@DelegateProcessEngineData.processInstances.withTransaction(transaction)
            override val nodeInstances: MutableHandleMap<SecureObject<ProcessNodeInstance<*>>>
                get() = this@DelegateProcessEngineData.processNodeInstances.withTransaction(transaction)
            override val processModels: IMutableProcessModelMapAccess
                get() = this@DelegateProcessEngineData.processModels.withTransaction(transaction)

            override val logger: nl.adaptivity.process.engine.impl.Logger
                get() = this@DelegateProcessEngineData.logger

            override fun messageService(): IMessageService<*> {
                return messageService
            }

            override fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
                this@DelegateProcessEngineData.invalidateCachePM(handle)
            }

            override fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance>>) {
                this@DelegateProcessEngineData.invalidateCachePI(handle)
            }

            override fun invalidateCachePNI(handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) {
                this@DelegateProcessEngineData.invalidateCachePNI(handle)
            }

            override fun handleFinishedInstance(handle: ComparableHandle<SecureObject<ProcessInstance>>) {
                // Ignore the completion for now. Just keep it in the engine.
            }
        }

        override fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess = DelegateEngineDataAccess(
            transaction)

        override fun startTransaction(): T = transactionFactory.startTransaction(this)

        override fun isValidTransaction(transaction: Transaction): Boolean {
            return transaction is ProcessTransaction && transaction.readableEngineData == this
        }
    }

    class DBProcessEngineData(
        private val messageService: IMessageService<*>,
        override val logger: nl.adaptivity.process.engine.impl.Logger
                              ) : IProcessEngineData<ProcessDBTransaction>() {


        private inner class DBEngineDataAccess(transaction: ProcessDBTransaction) : AbstractProcessEngineDataAccess<ProcessDBTransaction>(
            transaction) {
            override val instances: MutableHandleMap<SecureObject<ProcessInstance>>
                get() = this@DBProcessEngineData.processInstances.withTransaction(transaction)
            override val nodeInstances: MutableHandleMap<SecureObject<ProcessNodeInstance<*>>>
                get() = this@DBProcessEngineData.processNodeInstances.withTransaction(transaction)
            override val processModels: IMutableProcessModelMapAccess
                get() = this@DBProcessEngineData.processModels.withTransaction(transaction)

            override val logger: nl.adaptivity.process.engine.impl.Logger
                get() = this@DBProcessEngineData.logger

            override fun messageService(): IMessageService<*> {
                return this@DBProcessEngineData.messageService
            }

            override fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
                this@DBProcessEngineData.invalidateCachePM(handle)
            }

            override fun invalidateCachePI(handle: Handle<SecureObject<ProcessInstance>>) {
                this@DBProcessEngineData.invalidateCachePI(handle)
            }

            override fun invalidateCachePNI(handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) {
                this@DBProcessEngineData.invalidateCachePNI(handle)
            }

            override fun handleFinishedInstance(handle: ComparableHandle<SecureObject<ProcessInstance>>) {
                // Do nothing at this point. In the future, this will probably lead the node intances to be deleted.
            }
        }

        private val context = InitialContext().lookup("java:/comp/env") as Context

        private val dbResource: javax.sql.DataSource by lazy {
            DbSet.resourceNameToDataSource(context, DB_RESOURCE).also { dataSource ->
                ProcessEngineDB.connect(dataSource) {
                    ProcessEngineDB.ensureTables(this)
                    commit()
                }
            }
        }

        lateinit var engine: ProcessEngine<ProcessDBTransaction>

        override val processInstances by lazy {
            wrapDBInstanceCache(ProcessInstanceMap(this, engine), INSTANCE_CACHE_SIZE)
        }

        override val processNodeInstances by lazy {
            wrapNodeCache(ProcessNodeInstanceMap(this, engine), NODE_CACHE_SIZE)
        }

        override val processModels = wrapModelCache<ProcessDBTransaction>(ProcessModelMap(this), MODEL_CACHE_SIZE)

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

    fun invalidateModelCache(handle: Handle<SecureObject<ExecutableProcessModel>>) {
        engineData.invalidateCachePM(handle)
    }

    /**
     * Get all process models loaded into the engine.
     *
     * @param engineData The source of process data
     *
     * @return The list of process models.
     */
    fun getProcessModels(engineData: ProcessEngineDataAccess,
                         user: Principal): Iterable<SecuredObject<ExecutableProcessModel>> {
        mSecurityProvider.ensurePermission(Permissions.LIST_MODELS, user)
        if (user == SYSTEMPRINCIPAL) return engineData.processModels
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
    fun addProcessModel(transaction: TRXXX,
                        basepm: RootProcessModel.Builder,
                        user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        mSecurityProvider.ensurePermission(Permissions.ADD_MODEL, user)

        return engineData.inWriteTransaction(transaction) {
            val pastHandle = basepm.uuid?.let { uuid ->
                processModels[uuid]
            }

            if (pastHandle != null && pastHandle.isValid) {
                updateProcessModel(transaction, pastHandle, ExecutableProcessModel(basepm, false), user)
            } else {
                val uuid = basepm.uuid ?: UUID.randomUUID().also { basepm.uuid = it }

                basepm.owner.let { baseOwner ->
                    mSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, user, baseOwner)
                }

                val pm = ExecutableProcessModel(basepm, false)

                ProcessModelRef(pm.name, processModels.put(pm), uuid)
            }

        }

    }

    fun addProcessModel(transaction: TRXXX,
                        pm: ExecutableProcessModel,
                        user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        mSecurityProvider.ensurePermission(Permissions.ADD_MODEL, user)

        return engineData.inWriteTransaction(transaction) {
            val pastHandle = pm.uuid?.let { uuid ->
                processModels[uuid]
            }

            if (pastHandle != null && pastHandle.valid) {
                updateProcessModel(transaction, pastHandle, pm, user)
            } else {
                val uuid = pm.uuid ?: throw ProcessException("Missing UUID for process model")

                pm.owner.let { baseOwner ->
                    mSecurityProvider.ensurePermission(Permissions.ASSIGN_OWNERSHIP, user, baseOwner)
                }

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
    fun getProcessModel(transaction: TRXXX,
                        handle: Handle<ExecutableProcessModel>,
                        user: Principal): ExecutableProcessModel? {
        return engineData.inWriteTransaction(transaction) {
            getProcessModel(this, handle, user)
        }
    }

    fun getProcessModel(dataAccess: ProcessEngineDataAccess,
                        handle: Handle<SecureObject<ExecutableProcessModel>>,
                        user: Principal): ExecutableProcessModel? {
        return dataAccess.processModels[handle]?.withPermission(mSecurityProvider, SecureObject.Permissions.READ,
                                                                user) { processModel ->
            if (processModel.uuid == null && dataAccess is MutableProcessEngineDataAccess) {
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
     *
     * @param newName The new name
     */
    fun renameProcessModel(user: Principal, handle: Handle<ExecutableProcessModel>, newName: String) {
        engineData.inWriteTransaction(user, mSecurityProvider.ensurePermission(Permissions.FIND_MODEL, user)) {
            processModels[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.RENAME,
                                                                     user) { pm ->
                mSecurityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm)

                processModels[handle] = pm.update { name=newName } // set it to ensure update on the database
            }
        }
    }

    fun updateProcessModel(transaction: TRXXX,
                           handle: Handle<SecureObject<ExecutableProcessModel>>,
                           processModel: RootProcessModel<*>,
                           user: Principal): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        engineData.inWriteTransaction(transaction) {
            return updateProcessModel(this, handle, processModel, user)
        }
    }

    fun updateProcessModel(engineData: MutableProcessEngineDataAccess,
                           handle: Handle<SecureObject<ExecutableProcessModel>>,
                           processModel: RootProcessModel<*>,
                           user: Principal): ExecutableProcessModelRef {
        val oldModel = engineData.processModels[handle] ?: throw HandleNotFoundException(
            "The model did not exist, instead post a new model.")

        if (oldModel.owner == SYSTEMPRINCIPAL) throw IllegalStateException("The old model has no owner")

        mSecurityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel)
        mSecurityProvider.ensurePermission(Permissions.UPDATE_MODEL, user, oldModel)

        if (processModel.owner == SYSTEMPRINCIPAL) { // If no owner was set, use the old one.
            ExecutableProcessModel(processModel.builder().apply { owner = oldModel.owner })
        } else if (oldModel.owner.name != processModel.owner.name) {
            mSecurityProvider.ensurePermission(Permissions.CHANGE_OWNERSHIP, user, oldModel)
        }
        if (!engineData.processModels.contains(handle)) {
            throw HandleNotFoundException("The process model with handle $handle could not be found")
        }

        return (processModel as? ExecutableProcessModel ?: ExecutableProcessModel.from(processModel)).apply {
            engineData.processModels[handle] = this
        }.ref
    }

    @Throws(SQLException::class)
    fun removeProcessModel(transaction: TRXXX, handle: Handle<ExecutableProcessModel>, user: Principal): Boolean {
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
     *
     * @return All instances.
     */
    fun getOwnedProcessInstances(transaction: TRXXX, user: Principal): Iterable<ProcessInstance> {
        mSecurityProvider.ensurePermission(Permissions.LIST_INSTANCES, user)
        // If security allows this, return an empty list.
        engineData.inReadonlyTransaction(transaction) {
            return instances.map {
                it.withPermission(mSecurityProvider, SecureObject.Permissions.READ, user) { it }
            }.filter { instance -> instance.owner.name == user.name }
        }
    }


    /**
     * Get all process instances visible to the user.

     * @param user The current user in relation to whom we need to find the
     * *          instances.
     *
     * @return All instances.
     */
    fun getVisibleProcessInstances(transaction: TRXXX, user: Principal): Iterable<ProcessInstance> {
        engineData.inReadonlyTransaction(transaction) {
            return instances.map { it.withPermission() }.filter {
                mSecurityProvider.hasPermission(SecureObject.Permissions.READ, user, it)
            }
        }
    }

    @Throws(SQLException::class)
    fun getProcessInstance(transaction: TRXXX,
                           handle: ComparableHandle<SecureObject<ProcessInstance>>,
                           user: Principal): ProcessInstance {
        return engineData.inReadonlyTransaction(transaction) {
            instances[handle].shouldExist(handle).withPermission(mSecurityProvider, Permissions.VIEW_INSTANCE, user) {
                it
            }
        }
    }

    fun tickleInstance(transaction: TRXXX, handle: Long, user: Principal): Boolean {
        return tickleInstance(transaction, handle(handle = handle), user)
    }

    fun tickleInstance(transaction: TRXXX,
                       handle: ComparableHandle<SecureObject<ProcessInstance>>,
                       user: Principal): Boolean {
        transaction.writableEngineData.run {
            invalidateCachePM(getInvalidHandle())
            invalidateCachePI(getInvalidHandle())
            invalidateCachePNI(getInvalidHandle())

            (instances[handle] ?: return false).withPermission(mSecurityProvider, Permissions.TICKLE_INSTANCE, user) {
                it.update(transaction.writableEngineData) {
                    tickle(transaction.writableEngineData, messageService)
                }

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
    private fun startProcess(transaction: TRXXX,
                             user: Principal?,
                             model: SecureObject<ExecutableProcessModel>,
                             name: String,
                             uuid: UUID,
                             parentActivity: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                             payload: Node?): HProcessInstance {

        if (user == null) {
            throw HttpResponseException(HttpURLConnection.HTTP_FORBIDDEN,
                                        "Annonymous users are not allowed to start processes")
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
                assert(instance.getHandle().handleValue == resultHandle.handleValue)
                instance.initialize(transaction.writableEngineData)
            }.let { instance ->
                commit()

                try {
                    instance.start(transaction.writableEngineData, payload)
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Error starting instance (it is already stored)", e)
                    throw e
                }
            }

            return HProcessInstance(resultHandle)
        }
    }

    /**
     * Convenience method to start a process based upon a process model handle.

     * @param handle The process model to start a new instance for.
     *
     * @param name The name of the new instance.
     *
     * @param uuid The UUID for the instances. Helps with synchronization errors not exploding into mass instantiation.
     * @param payload The payload representing the parameters for the process.
     * @return A Handle to the [ProcessInstance].
     */
    fun startProcess(transaction: TRXXX,
                     user: Principal,
                     handle: Handle<SecureObject<ExecutableProcessModel>>,
                     name: String,
                     uuid: UUID,
                     payload: Node?): HProcessInstance {
        engineData.inWriteTransaction(transaction) {
            processModels[handle].shouldExist(handle)
        }.let { processModel ->
            return startProcess(transaction, user, processModel, name, uuid, getInvalidHandle(), payload)
        }
    }

    /**
     * Get the task with the given handle.


     * @param transaction
     *
     * @param handle The handle of the task.
     *
     * @return The handle
     *
     * @throws SQLException
     *
     * @todo change the parameter to a handle object.
     */
    @Throws(SQLException::class)
    fun getNodeInstance(transaction: TRXXX,
                        handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                        user: Principal): ProcessNodeInstance<*>? {
        engineData.inReadonlyTransaction(transaction) {
            return nodeInstances[handle]?.withPermission(mSecurityProvider, SecureObject.Permissions.READ, user) {
                it
            }
        }
    }

    @Throws(SQLException::class)
    fun finishInstance(transaction: TRXXX, hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>) {
        // TODO evict these nodes from the cache (not too bad to keep them though)
        //    for (ProcessNodeInstance childNode:pProcessInstance.getProcessNodeInstances()) {
        //      getNodeInstances().invalidateModelCache(childNode);
        //    }
        // TODO retain instance
        engineData.invalidateCachePI(hProcessInstance)
        engineData.inWriteTransaction(transaction) { instances.remove(hProcessInstance) }
    }

    @Throws(SQLException::class)
    fun cancelInstance(transaction: TRXXX,
                       handle: Handle<SecureObject<ProcessInstance>>,
                       user: Principal): ProcessInstance {
        engineData.inWriteTransaction(transaction) {
            instances[handle].shouldExist(handle).withPermission(mSecurityProvider, Permissions.CANCEL,
                                                                 user) { instance ->
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
     *
     * @param newState The new state
     *
     * @return
     *
     * @throws SQLException
     */
    fun updateTaskState(transaction: TRXXX,
                        handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                        newState: NodeInstanceState,
                        user: Principal): NodeInstanceState {
        val engineData = transaction.writableEngineData
        run {

            engineData.nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider,
                                                                                SecureObject.Permissions.UPDATE,
                                                                                user) { task ->

                val pi = engineData.instance(task.hProcessInstance).withPermission()

                var finalState = newState // must be initialized due to capture use
                synchronized(pi) {
                    // XXX Should not be needed if pi is immutable
                    pi.update(engineData) {
                        finalState = updateChild(handle) {
                            when (newState) {
                                Sent -> throw IllegalArgumentException(
                                    "Updating task state to initial state not possible")
                                Acknowledged -> state = newState
                                Taken -> takeTask(engineData)
                                Started -> startTask(engineData)
                                Complete -> throw IllegalArgumentException(
                                    "Finishing a task must be done by a separate method")
                            // TODO don't just make up a failure cause
                                Failed -> failTask(engineData, IllegalArgumentException("Missing failure cause"))
                                Cancelled -> cancel(engineData)
                                else -> throw IllegalArgumentException("Unsupported state :" + newState)
                            }
                        }.state
                    }
                }
                return finalState
            }

        }
    }

    @Throws(SQLException::class)
    fun finishTask(transaction: TRXXX,
                   handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                   payload: Node?,
                   user: Principal): ProcessNodeInstance<*> {
        engineData.inWriteTransaction(transaction) {
            val dataAccess = this
            nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.UPDATE,
                                                                     user) { task ->
                val pi = instance(task.hProcessInstance).withPermission()
                try {
                    synchronized(pi) {
                        pi.update(dataAccess) {
                            updateChild(handle) {
                                finishTask(dataAccess, payload)
                            }
                        }
                        return dataAccess.nodeInstance(handle).withPermission()
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
     *
     * @param resultSource The source that is parsed into DOM nodes and then passed on
     * *          to [.finishTask]
     */
    fun finishedTask(transaction: TRXXX,
                     handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                     resultSource: DataSource?,
                     user: Principal) {
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
     * @param handle
     * @throws SQLException
     */
    fun cancelledTask(transaction: TRXXX,
                      handle: net.devrieze.util.ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                      user: Principal) {
        updateTaskState(transaction, handle, Cancelled, user)
    }

    fun errorTask(transaction: TRXXX,
                  handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>,
                  cause: Throwable,
                  user: Principal) {
        engineData.inWriteTransaction(transaction) {
            nodeInstances[handle].shouldExist(handle).withPermission(mSecurityProvider, SecureObject.Permissions.UPDATE,
                                                                     user) { task ->
                instance(task.hProcessInstance).withPermission().update(this) {
                    updateChild(task) {
                        failTask(this@inWriteTransaction, cause)
                    }
                }
            }
        }
    }

    @Throws(SQLException::class)
    fun updateStorage(transaction: TRXXX, processInstance: ProcessInstance) {
        val handle = processInstance.getHandle()
        if (!handle.isValid) {
            throw IllegalArgumentException("You can't update storage state of an unregistered node")
        }
        engineData.inWriteTransaction(transaction) {
            instances[handle] = processInstance
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
        fun newInstance(
            messageService: IMessageService<*>,
            logger: Logger
                       ): ProcessEngine<ProcessDBTransaction> {
            // TODO enable optional caching
            val engineData = DBProcessEngineData(messageService, logger)
            val pe = ProcessEngine(messageService, engineData)
            engineData.engine = pe // STILL NEEDED to initialize the engine as the factories require the engine
            return pe
        }

        @JvmStatic
        @JvmName("newTestInstance")
        @PublishedApi
        internal fun <T : ProcessTransaction> newTestInstance(messageService: IMessageService<*>,
                                                              transactionFactory: ProcessTransactionFactory<T>,
                                                              processModels: IMutableProcessModelMap<T>,
                                                              processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>,
                                                              processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T>,
                                                              autoTransition: Boolean,
                                                              logger: Logger): ProcessEngine<T> {

            val engineData = ProcessEngine.DelegateProcessEngineData(transactionFactory, processModels,
                                                                     processInstances, processNodeInstances,
                                                                     messageService, logger)

            return ProcessEngine(messageService, engineData)
        }
    }

}
