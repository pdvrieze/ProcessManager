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

import io.github.pdvrieze.kotlinsql.UnmanagedSql
import io.github.pdvrieze.kotlinsql.monadic.*
import net.devrieze.util.*
import net.devrieze.util.db.DbSet
import net.devrieze.util.security.*
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.db.ProcessEngineDB
import nl.adaptivity.process.engine.impl.LoggerCompat
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.process.engine.processModel.NodeInstanceState.*
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
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
import javax.xml.parsers.ParserConfigurationException


private const val MODEL_CACHE_SIZE = 5
private const val NODE_CACHE_SIZE = 100
private const val INSTANCE_CACHE_SIZE = 10


private fun <T : ContextProcessTransaction, V : Any> wrapInstanceCache(
    base: MutableTransactionedHandleMap<V, T>,
    cacheSize: Int
): MutableTransactionedHandleMap<V, T> {
    if (cacheSize <= 0) {
        return base
    }
    return CachingHandleMap(base, cacheSize)
}

private fun wrapDBInstanceCache(
    base: ProcessInstanceMap,
    cacheSize: Int
): MutableTransactionedHandleMap<SecureProcessInstance, ProcessDBTransaction> {
    val b:  MutableTransactionedHandleMap<*, *> = base
    if (cacheSize <= 0) {
        return base
    }
    return ProcessInstanceMap.Cache(base, cacheSize)
}

private fun <T : ContextProcessTransaction> wrapNodeCache(
    base: MutableTransactionedHandleMap<SecureProcessNodeInstance, T>,
    cacheSize: Int
): MutableTransactionedHandleMap<SecureProcessNodeInstance, T> {
    if (cacheSize <= 0) {
        return base
    }
    return CachingHandleMap(base, cacheSize) { tr, securePNI, handle ->
        val pni = securePNI.withPermission()
        if (pni.handle == handle) {
            securePNI
        } else {
            val piBuilder = tr.readableEngineData.instance(pni.hProcessInstance).withPermission().builder()
            pni.builder(piBuilder).also { it.handle = handle }.build()
        }
    }

}

private fun <T : ContextProcessTransaction> wrapModelCache(
    base: IMutableProcessModelMap<T>,
    cacheSize: Int
): IMutableProcessModelMap<T> {
    if (cacheSize <= 0) {
        return base
    }
    return CachingProcessModelMap(base, cacheSize)
}


/**
 * This class represents the process engine. XXX make sure this is thread safe!!
 */
class ProcessEngine<TR : ContextProcessTransaction, C : ActivityInstanceContext> {

    private val messageService: IMessageService<*>
    private val engineData: IProcessEngineData<TR, C>
    private val tickleQueue = ArrayDeque<PIHandle>()
    private var securityProvider: SecurityProvider = OwnerOnlySecurityProvider("admin")
    private val processContextFactory: ProcessContextFactory<C>

    constructor(
        messageService: IMessageService<*>,
        engineData: IProcessEngineData<TR, C>,
        processContextFactory: ProcessContextFactory<C>
    ) {
        this.messageService = messageService
        this.engineData = engineData
        this.processContextFactory = processContextFactory
    }

    constructor(
        messageService: IMessageService<*>,
        transactionFactory: ProcessTransactionFactory<TR>,
        processModels: IMutableProcessModelMap<TR>,
        processInstances: MutableTransactionedHandleMap<SecureProcessInstance, TR>,
        processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, TR>,
        autoTransition: Boolean,
        logger: LoggerCompat,
        processContextFactory: ProcessContextFactory<C>
    ) {
        this.messageService = messageService
        this.engineData = DelegateProcessEngineData(
            transactionFactory, processModels,
            processInstances, processNodeInstances,
            messageService, logger,
            this
        )
        this.processContextFactory = processContextFactory
    }

    class DelegateProcessEngineData<T : ContextProcessTransaction, C : ActivityInstanceContext>(
        private val transactionFactory: ProcessTransactionFactory<T>,
        override val processModels: IMutableProcessModelMap<T>,
        override val processInstances: MutableTransactionedHandleMap<SecureProcessInstance, T>,
        override val processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, T>,
        private val messageService: IMessageService<*>,
        override val logger: LoggerCompat,
        private val engine: ProcessEngine<T, C>
    ) : IProcessEngineData<T, C>(), TransactionFactory<T> {

        private inner class DelegateEngineDataAccess(transaction: T) :
            AbstractProcessEngineDataAccess<T, C>(transaction) {
            override val processContextFactory: ProcessContextFactory<C>
                get() = this@DelegateProcessEngineData.engine.processContextFactory
            override val instances: MutableHandleMap<SecureProcessInstance>
                get() = this@DelegateProcessEngineData.processInstances.withTransaction(transaction)
            override val nodeInstances: MutableHandleMap<SecureProcessNodeInstance>
                get() = this@DelegateProcessEngineData.processNodeInstances.withTransaction(transaction)
            override val processModels: IMutableProcessModelMapAccess
                get() = this@DelegateProcessEngineData.processModels.withTransaction(transaction)

            override val logger: LoggerCompat
                get() = this@DelegateProcessEngineData.logger

            override fun messageService(): IMessageService<*> {
                return messageService
            }

            override fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
                this@DelegateProcessEngineData.invalidateCachePM(handle)
            }

            override fun invalidateCachePI(handle: PIHandle) {
                this@DelegateProcessEngineData.invalidateCachePI(handle)
            }

            override fun invalidateCachePNI(handle: PNIHandle) {
                this@DelegateProcessEngineData.invalidateCachePNI(handle)
            }

            override fun handleFinishedInstance(handle: PIHandle) {
                processContextFactory.onProcessFinished(this, handle)
                // Ignore the completion for now. Just keep it in the engine.
            }

            override fun queueTickle(instanceHandle: PIHandle) {
                this@DelegateProcessEngineData.queueTickle(instanceHandle)
            }
        }

        override fun createWriteDelegate(transaction: T): MutableProcessEngineDataAccess<C> = DelegateEngineDataAccess(
            transaction
        )

        override fun startTransaction(): T = transactionFactory.startTransaction(this)

        override fun isValidTransaction(transaction: Transaction): Boolean {
            return transaction is ContextProcessTransaction && transaction.readableEngineData == this
        }

        override fun queueTickle(instanceHandle: PIHandle) {
            engine.queueTickle(instanceHandle)
        }
    }

    class DBProcessEngineData<C : ActivityInstanceContext>(
        private val messageService: IMessageService<*>,
        override val logger: LoggerCompat
    ) : IProcessEngineData<ProcessDBTransaction, C>(),
        DBTransactionFactory<ProcessDBTransaction, ProcessEngineDB> {

        private inner class DBEngineDataAccess(transaction: ProcessDBTransaction) :
            AbstractProcessEngineDataAccess<ProcessDBTransaction, C>(transaction) {

            override val processContextFactory: ProcessContextFactory<C>
                get() = this@DBProcessEngineData.engine.processContextFactory
            override val instances: MutableHandleMap<SecureProcessInstance>
                get() = this@DBProcessEngineData.processInstances.withTransaction(transaction)
            override val nodeInstances: MutableHandleMap<SecureProcessNodeInstance>
                get() = this@DBProcessEngineData.processNodeInstances.withTransaction(transaction)
            override val processModels: IMutableProcessModelMapAccess
                get() = this@DBProcessEngineData.processModels.withTransaction(transaction)

            override val logger: LoggerCompat
                get() = this@DBProcessEngineData.logger

            override fun messageService(): IMessageService<*> {
                return this@DBProcessEngineData.messageService
            }

            override fun invalidateCachePM(handle: Handle<SecureObject<ExecutableProcessModel>>) {
                this@DBProcessEngineData.invalidateCachePM(handle)
            }

            override fun invalidateCachePI(handle: PIHandle) {
                this@DBProcessEngineData.invalidateCachePI(handle)
            }

            override fun invalidateCachePNI(handle: PNIHandle) {
                this@DBProcessEngineData.invalidateCachePNI(handle)
            }

            override fun handleFinishedInstance(handle: PIHandle) {
                processContextFactory.onProcessFinished(this, handle)
                // Do nothing at this point. In the future, this will probably lead the node intances to be deleted.
            }

            override fun queueTickle(instanceHandle: PIHandle) {
                this@DBProcessEngineData.queueTickle(instanceHandle)
            }
        }

        private val context = InitialContext().lookup("java:/comp/env") as Context

        private val dbResource: javax.sql.DataSource by lazy {
            DbSet.resourceNameToDataSource(context, DB_RESOURCE).also { dataSource ->
                ProcessEngineDB(dataSource) {
                    ensureTables().commit()
                }
            }
        }

        lateinit var engine: ProcessEngine<ProcessDBTransaction, C>

        override val processInstances: MutableTransactionedHandleMap<SecureProcessInstance, ProcessDBTransaction> by lazy {
            wrapDBInstanceCache(ProcessInstanceMap(this, engine), INSTANCE_CACHE_SIZE)
        }

        override val processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, ProcessDBTransaction> by lazy {
            wrapNodeCache(ProcessNodeInstanceMap(this, engine), NODE_CACHE_SIZE)
        }

        override val processModels = wrapModelCache(ProcessModelMap(this), MODEL_CACHE_SIZE)

        override fun createWriteDelegate(transaction: ProcessDBTransaction): MutableProcessEngineDataAccess<C> {
            return DBEngineDataAccess(transaction)
        }

        override fun startTransaction(): ProcessDBTransaction {
            val conn = MonadicDBConnection(dbResource.connection, ProcessEngineDB)

            return ProcessDBTransaction(TransactionBuilder(conn), this)
        }

        @OptIn(UnmanagedSql::class)
        override fun asTransaction(dbReceiver: DBReceiver<ProcessEngineDB>): ProcessDBTransaction {
            return when (dbReceiver) {
                is ConnectionSource -> {
                    val tb = TransactionBuilder(MonadicDBConnection(dbReceiver.getRawConnection(), dbReceiver.db))

                    ProcessDBTransaction(tb, this)
                }

                is DBTransactionContext -> ProcessDBTransaction(dbReceiver, this@DBProcessEngineData)
            }
        }

        override fun isValidTransaction(transaction: Transaction): Boolean {
            return transaction is ProcessDBTransaction
        }

        override fun queueTickle(instanceHandle: PIHandle) {
            engine.queueTickle(instanceHandle)
        }
    }


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
    fun getProcessModels(
        engineData: ProcessEngineDataAccess<*>,
        user: Principal
    ): Iterable<SecuredObject<ExecutableProcessModel>> {
        securityProvider.ensurePermission(ProcessEnginePermissions.LIST_MODELS, user)
        if (user == SYSTEMPRINCIPAL) return engineData.processModels
        return engineData.processModels.mapNotNull {
            it.ifPermitted(securityProvider, SecureObject.Permissions.READ, user)
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
    fun addProcessModel(
        transaction: TR,
        basepm: RootProcessModel.Builder,
        user: Principal
    ): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        securityProvider.ensurePermission(ProcessEnginePermissions.ADD_MODEL, user)

        return engineData.inWriteTransaction(transaction) {
            val pastHandle = basepm.uuid?.let { uuid ->
                processModels[uuid]
            }

            if (pastHandle != null && pastHandle.isValid) {
                updateProcessModel(transaction, pastHandle, ExecutableProcessModel(basepm, false), user)
            } else {
                val uuid = basepm.uuid ?: UUID.randomUUID().also { basepm.uuid = it }

                basepm.owner.let { baseOwner ->
                    securityProvider.ensurePermission(ProcessEnginePermissions.ASSIGN_OWNERSHIP, user, baseOwner)
                }

                val pm = ExecutableProcessModel(basepm, false)

                ProcessModelRef<ExecutableProcessNode, ExecutableProcessModel>(pm.name, processModels.put(pm), uuid)
            }

        }

    }

    fun addProcessModel(
        transaction: TR,
        pm: ExecutableProcessModel,
        user: Principal
    ): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        securityProvider.ensurePermission(ProcessEnginePermissions.ADD_MODEL, user)

        return engineData.inWriteTransaction(transaction) {
            val pastHandle = pm.uuid?.let { uuid ->
                processModels[uuid]
            }

            if (pastHandle != null && pastHandle.isValid) {
                updateProcessModel(transaction, pastHandle, pm, user)
            } else {
                val uuid = pm.uuid ?: throw ProcessException("Missing UUID for process model")

                pm.owner.let { baseOwner ->
                    securityProvider.ensurePermission(ProcessEnginePermissions.ASSIGN_OWNERSHIP, user, baseOwner)
                }

                ProcessModelRef<ExecutableProcessNode, ExecutableProcessModel>(pm.name, processModels.put(pm), uuid)
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
    fun getProcessModel(
        transaction: TR,
        handle: Handle<ExecutableProcessModel>,
        user: Principal
    ): ExecutableProcessModel? {
        return engineData.inWriteTransaction(transaction) {
            getProcessModel(this, handle, user)
        }
    }

    fun getProcessModel(
        dataAccess: ProcessEngineDataAccess<*>,
        handle: Handle<SecureObject<ExecutableProcessModel>>,
        user: Principal
    ): ExecutableProcessModel? {
        return dataAccess.processModels[handle]?.withPermission(
            securityProvider, SecureObject.Permissions.READ,
            user
        ) { processModel ->
            if (processModel.uuid == null && dataAccess is MutableProcessEngineDataAccess<*>) {
                processModel.update {
                    uuid = UUID.randomUUID()
                }.apply {
                    dataAccess.processModels[handle] = this
                }
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
        engineData.inWriteTransaction(
            user,
            securityProvider.ensurePermission(ProcessEnginePermissions.FIND_MODEL, user)
        ) {
            processModels[handle].shouldExist(handle).withPermission(
                securityProvider, SecureObject.Permissions.RENAME,
                user
            ) { pm ->
                securityProvider.ensurePermission(SecureObject.Permissions.RENAME, user, pm)

                processModels[handle] = pm.update { name = newName } // set it to ensure update on the database
            }
        }
    }

    fun updateProcessModel(
        transaction: TR,
        handle: Handle<SecureObject<ExecutableProcessModel>>,
        processModel: RootProcessModel<*>,
        user: Principal
    ): IProcessModelRef<ExecutableProcessNode, ExecutableProcessModel> {
        return engineData.inWriteTransaction(transaction) {
            updateProcessModel(this, handle, processModel, user)
        }
    }

    fun updateProcessModel(
        engineData: MutableProcessEngineDataAccess<C>,
        handle: Handle<SecureObject<ExecutableProcessModel>>,
        processModel: RootProcessModel<*>,
        user: Principal
    ): ExecutableProcessModelRef {
        val oldModel = engineData.processModels[handle] ?: throw HandleNotFoundException(
            "The model did not exist, instead post a new model."
        )

        if (oldModel.owner == SYSTEMPRINCIPAL) throw IllegalStateException("The old model has no owner")

        securityProvider.ensurePermission(SecureObject.Permissions.READ, user, oldModel)
        securityProvider.ensurePermission(ProcessEnginePermissions.UPDATE_MODEL, user, oldModel)

        if (processModel.owner == SYSTEMPRINCIPAL) { // If no owner was set, use the old one.
            ExecutableProcessModel(processModel.builder().apply { owner = oldModel.owner })
        } else if (oldModel.owner.name != processModel.owner.name) {
            securityProvider.ensurePermission(ProcessEnginePermissions.CHANGE_OWNERSHIP, user, oldModel)
        }
        if (!engineData.processModels.contains(handle)) {
            throw HandleNotFoundException("The process model with handle $handle could not be found")
        }

        return (processModel as? ExecutableProcessModel ?: ExecutableProcessModel.from(processModel)).apply {
            engineData.processModels[handle] = this
        }.ref
    }

    @Throws(SQLException::class)
    fun removeProcessModel(transaction: TR, handle: Handle<ExecutableProcessModel>, user: Principal): Boolean {
        engineData.inWriteTransaction(transaction) {
            val oldModel = processModels[handle].shouldExist(handle)
            securityProvider.ensurePermission(SecureObject.Permissions.DELETE, user, oldModel)

            if (processModels.remove(handle)) {
                transaction.commit()
                return true
            }
            return false
        }
    }

    fun setSecurityProvider(securityProvider: SecurityProvider) {
        this.securityProvider = securityProvider
    }

    /**
     * Get all process instances owned by the user.

     * @param user The current user in relation to whom we need to find the
     * *          instances.
     *
     * @return All instances.
     */
    fun getOwnedProcessInstances(transaction: TR, user: Principal): Iterable<ProcessInstance> {
        securityProvider.ensurePermission(ProcessEnginePermissions.LIST_INSTANCES, user)
        // If security allows this, return an empty list.
        engineData.inReadonlyTransaction(transaction) {
            return instances.map {
                it.withPermission(securityProvider, SecureObject.Permissions.READ, user) { it } as ProcessInstance
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
    fun getVisibleProcessInstances(transaction: TR, user: Principal): Iterable<ProcessInstance> {
        engineData.inReadonlyTransaction(transaction) {
            return instances.map { it.withPermission() }.filter {
                securityProvider.hasPermission(SecureObject.Permissions.READ, user, it)
            }
        }
    }

    @Throws(SQLException::class)
    fun getProcessInstance(
        transaction: TR,
        handle: PIHandle,
        user: Principal
    ): ProcessInstance {
        return engineData.inReadonlyTransaction(transaction) {
            instances[handle].shouldExist(handle)
                .withPermission(securityProvider, ProcessEnginePermissions.VIEW_INSTANCE, user) {
                    it
                }
        }
    }

    fun tickleInstance(transaction: TR, handle: Long, user: Principal): Boolean {
        return tickleInstance(transaction, if (handle < 0) Handle.invalid() else Handle(handle), user)
    }

    fun queueTickle(instanceHandle: PIHandle) {
        if (instanceHandle !in tickleQueue) {
            tickleQueue.add(instanceHandle)
        }
    }

    fun tickleInstance(
        transaction: TR,
        handle: PIHandle,
        user: Principal, processingTickles: Boolean = false
    ): Boolean {
        try {
            transaction.writableEngineData.run {
                invalidateCachePM(Handle.invalid())
                invalidateCachePI(Handle.invalid())
                invalidateCachePNI(Handle.invalid())

                securityProvider.ensurePermission(
                    ProcessEnginePermissions.TICKLE_INSTANCE,
                    user,
                    (instances[handle] ?: return false)
                )

                updateInstance(handle) {
                    tickle(this@run, messageService)
                }
                return true
            }
        } finally {
            processTickleQueue(transaction, processingTickles)
        }
    }

    fun processTickleQueue(transaction: TR, processingTickles: Boolean = false) {
        if (processingTickles) return
        while (tickleQueue.isNotEmpty()) {
            val instanceHandle = tickleQueue.removeFirst()
            engineData.inWriteTransaction(transaction) {
                tickleInstance(transaction, instanceHandle, SYSTEMPRINCIPAL, true)
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
    @OptIn(ProcessInstanceStorage::class)
    private fun startProcess(
        transaction: TR,
        user: Principal?,
        model: SecureObject<ExecutableProcessModel>,
        name: String,
        uuid: UUID,
        parentActivity: PNIHandle,
        payload: CompactFragment?
    ): PIHandle {

        if (user == null) {
            throw HttpResponseException(
                HttpURLConnection.HTTP_FORBIDDEN,
                "Annonymous users are not allowed to start processes"
            )
        }
        val unstoredInstance =
            model.withPermission(securityProvider, ExecutableProcessModel.Permissions.INSTANTIATE, user) {
                ProcessInstance(transaction.writableEngineData, it, parentActivity) {
                    this.instancename = name
                    this.uuid = uuid
                    this.state = State.NEW
                    this.owner = user
                }
            }

        val resultHandle: Handle<ProcessInstance>
        engineData.inWriteTransaction(transaction) {
            resultHandle = instances.put(unstoredInstance)
            updateInstance(resultHandle) {
                initialize()
            }
            commit()
            updateInstance(resultHandle) {
                try {
                    start(this@inWriteTransaction, payload)
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Error starting instance (it is already stored)", e)
                    throw e
                }
            }
        }
        processTickleQueue(transaction)
        return resultHandle
    }

    /**
     * Convenience method to start a process based upon a process model handle.
     *
     * @param handle The process model to start a new instance for.
     *
     * @param name The name of the new instance.
     *
     * @param uuid The UUID for the instances. Helps with synchronization errors not exploding into mass instantiation.
     * @param payload The payload representing the parameters for the process.
     * @return A Handle to the [ProcessInstance].
     */
    fun startProcess(
        transaction: TR,
        user: Principal,
        handle: Handle<SecureObject<ExecutableProcessModel>>,
        name: String,
        uuid: UUID,
        payload: CompactFragment?
    ): PIHandle {
        engineData.inWriteTransaction(transaction) {
            processModels[handle].shouldExist(handle)
        }.let { processModel ->
            return startProcess(transaction, user, processModel, name, uuid, Handle.invalid(), payload)
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
    fun getNodeInstance(
        transaction: TR,
        handle: PNIHandle,
        user: Principal
    ): ProcessNodeInstance<*, *>? {
        engineData.inReadonlyTransaction(transaction) {
            return nodeInstances[handle]?.withPermission(securityProvider, SecureObject.Permissions.READ, user) {
                it
            }
        }
    }

    @Throws(SQLException::class)
    fun finishInstance(transaction: TR, hProcessInstance: PIHandle) {
        // TODO evict these nodes from the cache (not too bad to keep them though)
        //    for (ProcessNodeInstance childNode:pProcessInstance.getProcessNodeInstances()) {
        //      getNodeInstances().invalidateModelCache(childNode);
        //    }
        // TODO retain instance
        engineData.invalidateCachePI(hProcessInstance)
        engineData.inWriteTransaction(transaction) { instances.remove(hProcessInstance) }
    }

    @Throws(SQLException::class)
    fun cancelInstance(
        transaction: TR,
        handle: PIHandle,
        user: Principal
    ): ProcessInstance {
        engineData.inWriteTransaction(transaction) {
            instances[handle].shouldExist(handle).withPermission(
                securityProvider, ProcessEnginePermissions.CANCEL,
                user
            ) { instance ->
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
    }

    /**
     * Cancel all process instances and tasks in the engine.
     * @throws SQLException
     */
    @Throws(SQLException::class)
    fun cancelAll(transaction: TR, user: Principal) {
        securityProvider.ensurePermission(ProcessEnginePermissions.CANCEL_ALL, user)
        engineData.inWriteTransaction(transaction) {
            (nodeInstances as MutableHandleMap).clear()
            instances.clear()
        }
    }


    /**
     * Update the state of the given task
     *
     * @param handle Handle to the process instance.
     * @param newState The new state
     * @return the new state
     *
     * @throws SQLException
     */
    fun updateTaskState(
        transaction: TR,
        handle: PNIHandle,
        newState: NodeInstanceState,
        user: Principal
    ): NodeInstanceState {
        try {
            val engineData = transaction.writableEngineData
            engineData.nodeInstances[handle]
                .shouldExist(handle)
                .withPermission(securityProvider, SecureObject.Permissions.UPDATE, user) { task ->

                    var finalState: NodeInstanceState = newState
                    engineData.updateInstance(task.hProcessInstance) {
                        synchronized(this.base) {
                            finalState = updateChild(handle) {
                                when (newState) {
                                    Sent -> throw IllegalArgumentException("Updating task state to initial state not possible")
                                    Acknowledged -> {
                                        if (state == Taken) {
                                            assignedUser = null
                                        }
                                        state = newState
                                    }

                                    Taken -> takeTask(engineData, assignedUser)
                                    Started -> startTask(engineData)
                                    Complete -> throw IllegalArgumentException("Finishing a task must be done by a separate method")
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
        } finally {
            processTickleQueue(transaction)
        }

    }

    /**
     * Update the state of the given task
     *
     * @param handle Handle to the process instance.
     * @param newState The new state
     * @return the new state
     *
     * @throws SQLException
     */
    fun takeTask(
        transaction: TR,
        handle: PNIHandle,
        assignedUserName: String,
        user: PrincipalCompat
    ): NodeInstanceState {
        val assignedUser = when (assignedUserName) {
            user.name -> user
            else -> getPrincipal(assignedUserName)
        }
        try {
            val engineData = transaction.writableEngineData
            engineData.nodeInstances[handle]
                .shouldExist(handle)
                .withPermission(securityProvider, SecureObject.Permissions.UPDATE, user) { task ->

                    var finalState: NodeInstanceState = Taken
                    engineData.updateInstance(task.hProcessInstance) {
                        synchronized(this.base) {
                            finalState = updateChild(handle) {
                                takeTask(engineData, assignedUser)
                            }.state
                        }
                    }

                    return finalState
                }
        } finally {
            processTickleQueue(transaction)
        }

    }

    fun getPrincipal(userName: String) = processContextFactory.getPrincipal(userName)

    @Throws(SQLException::class)
    fun finishTask(
        transaction: TR,
        handle: PNIHandle,
        payload: ICompactFragment?,
        user: Principal
    ): ProcessNodeInstance<*, *> {
        try {
            securityProvider.ensurePermission(
                SecureObject.Permissions.UPDATE,
                user,
                transaction.readableEngineData.nodeInstance(handle).shouldExist(handle)
            )
            transaction.readableEngineData.nodeInstance(handle)
            with(transaction.writableEngineData) {
                return updateNodeInstance(handle) {
                    finishTask(this@with, payload)
                }.withPermission()
            }
        } finally {
            processTickleQueue(transaction)
        }
    }

    /**
     * This method is primarilly a convenience method for
     * [.finishTask].
     *
     * TODO make this work properly and not depend on InputStreams
     *
     * @param handle The handle to finish.
     *
     * @param resultSource The source that is parsed into DOM nodes and then passed on
     *            to [.finishTask]
     *
     */
    fun finishedTask(
        transaction: TR,
        handle: PNIHandle,
        resultSource: DataSource?,
        user: Principal
    ) {
        try {
            val reader = resultSource?.let { XmlStreaming.newReader(it.inputStream, "UTF8") }
            val xml = reader?.siblingsToFragment()
            finishTask(transaction, handle, xml, user)

        } catch (e: ParserConfigurationException) {
            throw MessagingException(e)
        } catch (e: SAXException) {
            throw MessagingException(e)
        } catch (e: SQLException) {
            throw MessagingException(e)
        } catch (e: IOException) {
            throw MessagingException(e)
        } finally {
            processTickleQueue(transaction)
        }

    }

    /**
     * Handle the fact that this task has been cancelled.
     *
     * @param transaction
     * @param handle
     * @throws SQLException
     */
    fun cancelledTask(
        transaction: TR,
        handle: PNIHandle,
        user: Principal
    ) {
        updateTaskState(transaction, handle, Cancelled, user)
        processTickleQueue(transaction)
    }

    fun errorTask(
        transaction: TR,
        handle: PNIHandle,
        cause: Throwable,
        user: Principal
    ) {
        with(transaction.writableEngineData) {
            securityProvider.ensurePermission(
                SecureObject.Permissions.UPDATE,
                user,
                nodeInstance(handle)
            )
            updateNodeInstance(handle) {
                failTask(this@with, cause)
            }
        }
        processTickleQueue(transaction)
    }

    @Throws(SQLException::class)
    fun updateStorage(transaction: TR, processInstance: ProcessInstance) {
        val handle: PIHandle = processInstance.handle
        if (!handle.isValid) {
            throw IllegalArgumentException("You can't update storage state of an unregistered node")
        }
        engineData.inWriteTransaction(transaction) {
            instances[handle] = processInstance
        }
    }

    fun startTransaction(): TR {
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
        @JvmName("newInstance")
        operator fun <T : ProcessDBTransaction> invoke(
            messageService: IMessageService<*>,
            engineData: IProcessEngineData<T, ActivityInstanceContext>
        ): ProcessEngine<T, ActivityInstanceContext> {
            return ProcessEngine(messageService, engineData, ProcessContextFactory.DEFAULT)
        }

        @JvmStatic
        fun newInstance(
            messageService: IMessageService<*>,
            logger: LoggerCompat
        ): ProcessEngine<ProcessDBTransaction, ActivityInstanceContext> {
            // TODO enable optional caching
            val engineData = DBProcessEngineData<ActivityInstanceContext>(messageService, logger)
            val pe = ProcessEngine(messageService, engineData)
            engineData.engine = pe // STILL NEEDED to initialize the engine as the factories require the engine
            return pe
        }

        @JvmStatic
        @JvmName("newTestInstance")
        fun <T : ContextProcessTransaction> newTestInstance(
            messageService: IMessageService<*>,
            transactionFactory: ProcessTransactionFactory<T>,
            processModels: IMutableProcessModelMap<T>,
            processInstances: MutableTransactionedHandleMap<SecureProcessInstance, T>,
            processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, T>,
            autoTransition: Boolean,
            logger: LoggerCompat
        ): ProcessEngine<T, ActivityInstanceContext> {


            return ProcessEngine(
                messageService, transactionFactory, processModels,
                processInstances, processNodeInstances,
                autoTransition, logger,
                ProcessContextFactory.DEFAULT
            )
        }

        @JvmStatic
        @JvmName("newTestInstance")
        @PublishedApi
        internal fun <T : ContextProcessTransaction, C : ActivityInstanceContext> newTestInstance(
            messageService: IMessageService<*>,
            transactionFactory: ProcessTransactionFactory<T>,
            processModels: IMutableProcessModelMap<T>,
            processInstances: MutableTransactionedHandleMap<SecureProcessInstance, T>,
            processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, T>,
            autoTransition: Boolean,
            logger: LoggerCompat,
            processContextFactory: ProcessContextFactory<C>
        ): ProcessEngine<T, C> {


            return ProcessEngine(
                messageService, transactionFactory, processModels,
                processInstances, processNodeInstances,
                autoTransition, logger,
                processContextFactory
            )
        }
    }

}
