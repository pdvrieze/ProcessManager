package nl.adaptivity.process.engine.test

import net.devrieze.util.CachingHandleMap
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.security.*
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.*
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList
import nl.adaptivity.xmlutil.QName
import kotlin.test.BeforeTest
import nl.adaptivity.xmlutil.util.CompactFragment

typealias ProcessEngineFactory = (IMessageService<*>, ProcessTransactionFactory<StubProcessTransaction>)-> ProcessEngine<StubProcessTransaction>

open class ProcessEngineTestSupport(
    private val localEndpoint: EndpointDescriptorImpl = EndpointDescriptorImpl(
        QName.valueOf("processEngine"),
        "processEngine",
        URI.create("http://localhost/")
    ),
    protected val messageService: StubMessageService = StubMessageService(localEndpoint)
) {
    protected val stubTransactionFactory = object :
        ProcessTransactionFactory<StubProcessTransaction> {
        override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
            return StubProcessTransaction(engineData)
        }
    }
    val testModelOwnerPrincipal = object : RolePrincipal {
        override fun hasRole(role: String): Boolean = role == "admin"

        override fun getName(): String = "modelOwner"
    }

    private val ProcessInstance.sortedFinished
        get() = finished.sortedBy { it.handleValue }
    private val ProcessInstance.sortedActive
        get() = active.sortedBy { it.handleValue }
    private val ProcessInstance.sortedCompleted
        get() = completedEndnodes.sortedBy { it.handleValue }

    inline fun <R> testProcess(
        model: ExecutableProcessModel,
        payload: CompactFragment? = null,
        noinline createProcessContextFactory: () -> ProcessContextFactory<*>,
        body: (ProcessEngine<StubProcessTransaction>, StubProcessTransaction, ExecutableProcessModel, PIHandle) -> R
    ): R {
        val processEngine = doCreateRawEngine(createProcessContextFactory)
        return processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, testModelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, testModelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    protected inline fun <R> testProcess(
        processEngineFactory: ProcessEngineFactory,
        model: ExecutableProcessModel,
        payload: CompactFragment? = null,
        body: (ProcessEngine<StubProcessTransaction>, StubProcessTransaction, ExecutableProcessModel, PIHandle) -> R
    ): R {
        val processEngine = processEngineFactory(messageService, stubTransactionFactory)
        return processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, testModelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, testModelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    inline fun <R> testRawEngine(
        noinline createProcessContextFactory: () -> ProcessContextFactory<*>,
        action: (ProcessEngine<StubProcessTransaction>) -> R,
    ): R {
        return action(doCreateRawEngine(createProcessContextFactory))
    }

    @PublishedApi
    internal fun doCreateRawEngine(createProcessContextFactory: () -> ProcessContextFactory<*>): ProcessEngine<StubProcessTransaction> {
        return defaultEngineFactory(messageService, stubTransactionFactory, createProcessContextFactory())
    }

    protected fun ContextProcessTransaction.getInstance(instanceHandle: PIHandle): ProcessInstance {
        return readableEngineData.instance(instanceHandle).withPermission()
    }

    protected fun <R> ContextProcessTransaction.withInstance(
        instanceHandle: PIHandle,
        body: ContextProcessTransaction.(ProcessInstance) -> R
    ): R {
        return body(getInstance(instanceHandle))
    }

    protected fun StubMessageService.messageNode(transaction: ContextProcessTransaction, index: Int): ProcessNodeInstance<*> {
        return transaction.readableEngineData.nodeInstance(this.messages[index].source).withPermission()
    }

    protected fun ProcessInstance.child(transaction: ContextProcessTransaction, name: String) : ProcessNodeInstance<*> {
        return getChild(name, 1)?.withPermission() ?: throw AssertionError("No node instance for node id ${name} found")
    }

    protected fun ProcessInstance.assertFinishedHandles(handles: Array<PNIHandle>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedFinished))
    }

    protected fun ProcessInstance.assertFinished(vararg handles: IProcessNodeInstance) = apply {
        val actual = ArrayList(sortedFinished)
        val expected = handles.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e: AssertionError) {
            throw AssertionError("Expected finished list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance.assertActiveHandles(handles: Array<PNIHandle>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedActive))
    }

    protected fun ProcessInstance.assertActive(vararg handles: IProcessNodeInstance) = apply {
        val actual = ArrayList(sortedActive)
        val expected = handles.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e: AssertionError) {
            throw AssertionError("Expected active list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance.assertCompletedHandles(handles: Array<PNIHandle>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedCompleted))
    }

    protected fun ProcessInstance.assertCompleted(vararg nodes: IProcessNodeInstance): ProcessInstance {
        val actual = ArrayList(sortedCompleted)
        val expected = nodes.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        assertEquals(expected, actual)
        return this
    }

    protected fun IProcessNodeInstance.assertStarted() = apply {
        assertEquals(NodeInstanceState.Started, this.state)
    }

    protected fun ProcessInstance.assertIsStarted() = apply {
        assertEquals(ProcessInstance.State.STARTED, this.state)
    }

    protected fun ProcessInstance.assertIsFinished() = apply {
        assertEquals(ProcessInstance.State.FINISHED, this.state)
    }

    private fun IProcessNodeInstance.assertSent() = apply {
        assertState(NodeInstanceState.Sent)
    }

    protected fun IProcessNodeInstance.assertPending() = apply {
        assertState(NodeInstanceState.Pending)
    }

    protected fun IProcessNodeInstance.assertAcknowledged() = apply {
        assertState(NodeInstanceState.Acknowledged)
    }

    protected fun IProcessNodeInstance.assertComplete() = apply {
        assertState(NodeInstanceState.Complete)
    }

    protected fun IProcessNodeInstance.assertState(state: NodeInstanceState) {
        try {
            assertEquals(
                state, this.state,
                "Node ${this.node.id}(${this.handle}) should be in the ${state.name} state"
            )
        } catch (e: AssertionError) {
            throw nl.adaptivity.dropStack(e, 2)
        }
    }

    @BeforeTest
    fun beforeTest() {
        (messageService as? StubMessageService)?.clear()
    }

    companion object {

        @JvmStatic
        @JvmName("newTestInstance")
        @PublishedApi
        internal fun <T : ContextProcessTransaction> newTestProcessEngineInstance(
            messageService: IMessageService<*>,
            transactionFactory: ProcessTransactionFactory<T>,
            processModels: IMutableProcessModelMap<T>,
            processInstances: MutableTransactionedHandleMap<SecureProcessInstance, T>,
            processNodeInstances: MutableTransactionedHandleMap<SecureProcessNodeInstance, T>,
            autoTransition: Boolean,
            logger: Logger,
            processContextFactory: ProcessContextFactory<*>
        ): ProcessEngine<T> {


            return ProcessEngine(
                messageService, transactionFactory, processModels,
                processInstances, processNodeInstances,
                autoTransition, logger,
                processContextFactory
            ).apply {
                setSecurityProvider(OwnerOrObjectSecurityProvider(setOf("admin")))
            }
        }


        fun defaultEngineFactory(messageService: IMessageService<*>, transactionFactory: ProcessTransactionFactory<StubProcessTransaction>, contextFactory: ProcessContextFactory<*>): ProcessEngine<StubProcessTransaction> {
            return newTestProcessEngineInstance<StubProcessTransaction>(
                messageService,
                transactionFactory,
                cacheModels(MemProcessModelMap(), 3),
                cacheInstances(
                    MemTransactionedHandleMap(),
                    1
                ),
                cacheNodes(
                    MemTransactionedHandleMap(
                        ::PNI_SET_HANDLE
                    ), 2
                ), true, Logger.getAnonymousLogger(),
                contextFactory
            )
        }

        fun PNI_SET_HANDLE(transaction: StubProcessTransaction, pni: SecureProcessNodeInstance, handle: PNIHandle): SecureProcessNodeInstance? {
            if (pni.withPermission().handle == handle) {
                return pni
            }
            val piBuilder = transaction.readableEngineData.instance(pni.withPermission().hProcessInstance).withPermission().builder()
            return pni.withPermission().builder(piBuilder).also { it.handle = handle }.build()
        }

        fun <V:Any> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction> {
            return CachingHandleMap(base, count)
        }

        fun cacheNodes(base: MutableTransactionedHandleMap<SecureProcessNodeInstance, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<SecureProcessNodeInstance, StubProcessTransaction> {
            return CachingHandleMap(base, count, ::PNI_SET_HANDLE)
        }

        fun cacheModels(base: IMutableProcessModelMap<StubProcessTransaction>, count: Int): IMutableProcessModelMap<StubProcessTransaction> {
            return CachingProcessModelMap(base, count)
        }

    }

}


inline fun <R> ProcessEngineTestSupport.testRawEngine(
    action: (ProcessEngine<StubProcessTransaction>) -> R,
): R {
    return testRawEngine({ ProcessContextFactory.DEFAULT }, action)
}

inline fun <R> ProcessEngineTestSupport.testProcess(
    model: ExecutableProcessModel,
    payload: CompactFragment? = null,
    body: (ProcessEngine<StubProcessTransaction>, StubProcessTransaction, ExecutableProcessModel, PIHandle) -> R
): R {
    return testProcess(model, payload, {ProcessContextFactory.DEFAULT}, body)
}
