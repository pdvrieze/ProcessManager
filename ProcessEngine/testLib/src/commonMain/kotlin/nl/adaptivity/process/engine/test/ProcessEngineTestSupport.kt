package nl.adaptivity.process.engine.test

import net.devrieze.util.CachingHandleMap
import net.devrieze.util.Handle
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.dropStack
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.net.URI
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.test.assertEquals
import nl.adaptivity.xmlutil.QName
import kotlin.test.BeforeTest
import nl.adaptivity.xmlutil.util.CompactFragment

typealias ProcessEngineFactory<A> = (StubMessageService, ProcessTransactionFactory<StubProcessTransaction>)-> ProcessEngine<StubProcessTransaction, A>

open class ProcessEngineTestSupport {
    private val localEndpoint = EndpointDescriptorImpl(
        QName.valueOf("processEngine"),
        "processEngine",
        URI.create("http://localhost/")
    )
    protected val stubMessageService: StubMessageService =
        StubMessageService(localEndpoint)
    protected val stubTransactionFactory = object :
        ProcessTransactionFactory<StubProcessTransaction> {
        override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
            return StubProcessTransaction(engineData)
        }
    }
    protected val modelOwnerPrincipal = SimplePrincipal("pdvrieze")
    private val ProcessInstance.sortedFinished
        get() = finished.sortedBy { it.handleValue }
    private val ProcessInstance.sortedActive
        get() = active.sortedBy { it.handleValue }
    private val ProcessInstance.sortedCompleted
        get() = completedEndnodes.sortedBy { it.handleValue }

    protected inline fun <R> testProcess(model: ExecutableProcessModel, payload: CompactFragment? = null, body: (ProcessEngine<StubProcessTransaction, ActivityInstanceContext>, ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R): R {
        val processEngine = defaultEngineFactory(stubMessageService, stubTransactionFactory, ProcessContextFactory)
        processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, modelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, modelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            return body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    protected inline fun <AIC: ActivityInstanceContext, R> testProcess(processEngineFactory: ProcessEngineFactory<AIC>, model: ExecutableProcessModel, payload: nl.adaptivity.xmlutil.util.CompactFragment? = null, body: (ProcessEngine<StubProcessTransaction, AIC>, ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R): R {
        val processEngine = processEngineFactory(stubMessageService, stubTransactionFactory)
        processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, modelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, modelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            return body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    protected inline fun <R> testRawEngine(action: (ProcessEngine<StubProcessTransaction, ActivityInstanceContext>)-> R): R {
        val processEngine = defaultEngineFactory(stubMessageService, stubTransactionFactory, ProcessContextFactory)
        return action(processEngine)
    }

    protected fun ProcessTransaction.getInstance(instanceHandle: HProcessInstance): ProcessInstance {
        return readableEngineData.instance(instanceHandle).withPermission()
    }

    protected fun StubMessageService.messageNode(transaction: ProcessTransaction, index: Int): ProcessNodeInstance<*> {
        return transaction.readableEngineData.nodeInstance(this._messages[index].source).withPermission()
    }

    protected fun ProcessInstance.child(transaction: ProcessTransaction, name: String) : ProcessNodeInstance<*> {
        return getChild(name, 1)?.withPermission() ?: throw AssertionError("No node instance for node id ${name} found")
    }

    protected fun ProcessInstance.assertFinishedHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*>>>>) = apply {
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

    private fun ProcessInstance.assertActiveHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*>>>>) = apply {
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

    private fun ProcessInstance.assertCompletedHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*>>>>) = apply {
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
        stubMessageService.clear()
    }

    fun assertEqualsXml(expected: String?, actual: String?) {
        assertEquals(
            expected,
            actual,
        )
    }

    companion object {

        @JvmStatic
        @JvmName("newTestInstance")
        @PublishedApi
        internal fun <T : ProcessTransaction, A : ActivityInstanceContext> newTestProcessEngineInstance(
            messageService: IMessageService<*>,
            transactionFactory: ProcessTransactionFactory<T>,
            processModels: IMutableProcessModelMap<T>,
            processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance>, T>,
            processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, T>,
            autoTransition: Boolean,
            logger: Logger,
            processContextFactory: ProcessContextFactory<A>
        ): ProcessEngine<T, A> {


            return ProcessEngine(
                messageService, transactionFactory, processModels,
                processInstances, processNodeInstances,
                autoTransition, logger,
                processContextFactory
            )
        }


        fun <A: ActivityInstanceContext> defaultEngineFactory(messageService: StubMessageService, transactionFactory: ProcessTransactionFactory<StubProcessTransaction>, contextFactory: ProcessContextFactory<A>): ProcessEngine<StubProcessTransaction, A> {
            return newTestProcessEngineInstance(
                messageService,
                transactionFactory,
                cacheModels<Any>(MemProcessModelMap(), 3),
                cacheInstances(
                    nl.adaptivity.process.MemTransactionedHandleMap(),
                    1
                ),
                cacheNodes<Any>(
                    nl.adaptivity.process.MemTransactionedHandleMap(
                        PNI_SET_HANDLE
                    ), 2
                ), true, Logger.getAnonymousLogger(),
                contextFactory
            )
        }

        internal val PNI_SET_HANDLE = fun(transaction: StubProcessTransaction, pni: SecureObject<ProcessNodeInstance<*>>, handle: PNIHandle): SecureObject<ProcessNodeInstance<*>>? {
            if (pni.withPermission().handle == handle) {
                return pni
            }
            val piBuilder = transaction.readableEngineData.instance(pni.withPermission().hProcessInstance).withPermission().builder()
            return pni.withPermission().builder(piBuilder).also { it.handle = handle }.build()
        }

        internal fun <V:Any> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction> {
            return net.devrieze.util.CachingHandleMap<V, StubProcessTransaction>(
                base,
                count
            )
        }

        internal fun <V> cacheNodes(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction> {
            return net.devrieze.util.CachingHandleMap(base, count, PNI_SET_HANDLE)
        }

        internal fun <V> cacheModels(base: IMutableProcessModelMap<StubProcessTransaction>, count: Int): IMutableProcessModelMap<StubProcessTransaction> {
            return CachingProcessModelMap(base, count)
        }

    }

}

typealias PNIHandle = Handle<SecureObject<ProcessNodeInstance<*>>>
