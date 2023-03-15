package nl.adaptivity.process.engine.test

import net.devrieze.util.Handle
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList
import nl.adaptivity.xmlutil.QName
import kotlin.test.BeforeTest
import nl.adaptivity.xmlutil.util.CompactFragment

typealias ProcessEngineFactory<C> = (StubMessageService<C>, ProcessTransactionFactory<StubProcessTransaction<C>, C>)-> ProcessEngine<StubProcessTransaction<C>, C>

open class ProcessEngineTestSupport<C: ActivityInstanceContext>() {
    private val localEndpoint = EndpointDescriptorImpl(
        QName.valueOf("processEngine"),
        "processEngine",
        URI.create("http://localhost/")
    )
    protected val stubMessageService: StubMessageService<C> =
        StubMessageService<C>(localEndpoint)
    protected val stubTransactionFactory = object :
        ProcessTransactionFactory<StubProcessTransaction<C>, C> {
        override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction<C>, C>): StubProcessTransaction<C> {
            return StubProcessTransaction<C>(engineData)
        }
    }
    protected val modelOwnerPrincipal = SimplePrincipal("modelOwner")
    private val ProcessInstance<C>.sortedFinished
        get() = finished.sortedBy { it.handleValue }
    private val ProcessInstance<C>.sortedActive
        get() = active.sortedBy { it.handleValue }
    private val ProcessInstance<C>.sortedCompleted
        get() = completedEndnodes.sortedBy { it.handleValue }

    protected inline fun <R> testProcess(
        model: ExecutableProcessModel,
        payload: CompactFragment? = null,
        createProcessContextFactory: () -> ProcessContextFactory<C>,
        body: (ProcessEngine<StubProcessTransaction<C>, C>, ContextProcessTransaction<C>, ExecutableProcessModel, HProcessInstance) -> R
    ): R {
        val processEngine = defaultEngineFactory(stubMessageService, stubTransactionFactory, createProcessContextFactory())
        processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, modelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, modelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            return body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    protected inline fun <R> testProcess(processEngineFactory: ProcessEngineFactory<C>, model: ExecutableProcessModel, payload: nl.adaptivity.xmlutil.util.CompactFragment? = null, body: (ProcessEngine<StubProcessTransaction<C>, C>, ContextProcessTransaction<C>, ExecutableProcessModel, HProcessInstance) -> R): R {
        val processEngine = processEngineFactory(stubMessageService, stubTransactionFactory)
        processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, modelOwnerPrincipal).handle
            val instanceHandle = processEngine.startProcess(transaction, modelOwnerPrincipal, modelHandle, "testInstance",
                UUID.randomUUID(), payload)

            return body(processEngine, transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    protected inline fun <R> testRawEngine(
        action: (ProcessEngine<StubProcessTransaction<C>, C>) -> R,
        createProcessContextFactory: () -> ProcessContextFactory<C>
    ): R {
        val processEngine = defaultEngineFactory(stubMessageService, stubTransactionFactory, createProcessContextFactory())
        return action(processEngine)
    }

    protected fun ContextProcessTransaction<C>.getInstance(instanceHandle: HProcessInstance): ProcessInstance<C> {
        return readableEngineData.instance(instanceHandle).withPermission()
    }

    protected fun StubMessageService<C>.messageNode(transaction: ContextProcessTransaction<C>, index: Int): ProcessNodeInstance<*, C> {
        return transaction.readableEngineData.nodeInstance(this.messages[index].source).withPermission()
    }

    protected fun ProcessInstance<C>.child(transaction: ContextProcessTransaction<C>, name: String) : ProcessNodeInstance<*, C> {
        return getChild(name, 1)?.withPermission() ?: throw AssertionError("No node instance for node id ${name} found")
    }

    protected fun ProcessInstance<C>.assertFinishedHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*, *>>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedFinished))
    }

    protected fun ProcessInstance<C>.assertFinished(vararg handles: IProcessNodeInstance<*>) = apply {
        val actual = ArrayList(sortedFinished)
        val expected = handles.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e: AssertionError) {
            throw AssertionError("Expected finished list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance<C>.assertActiveHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*, *>>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedActive))
    }

    protected fun ProcessInstance<C>.assertActive(vararg handles: IProcessNodeInstance<*>) = apply {
        val actual = ArrayList(sortedActive)
        val expected = handles.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e: AssertionError) {
            throw AssertionError("Expected active list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance<C>.assertCompletedHandles(handles: Array<Handle<SecureObject<ProcessNodeInstance<*, *>>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedCompleted))
    }

    protected fun ProcessInstance<C>.assertCompleted(vararg nodes: IProcessNodeInstance<*>): ProcessInstance<C> {
        val actual = ArrayList(sortedCompleted)
        val expected = nodes.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
        assertEquals(expected, actual)
        return this
    }

    protected fun IProcessNodeInstance<C>.assertStarted() = apply {
        assertEquals(NodeInstanceState.Started, this.state)
    }

    protected fun ProcessInstance3.assertIsStarted() = apply {
        assertEquals(ProcessInstance.State.STARTED, this.state)
    }

    protected fun ProcessInstance3.assertIsFinished() = apply {
        assertEquals(ProcessInstance.State.FINISHED, this.state)
    }

    private fun IProcessNodeInstance<C>.assertSent() = apply {
        assertState(NodeInstanceState.Sent)
    }

    protected fun IProcessNodeInstance<C>.assertPending() = apply {
        assertState(NodeInstanceState.Pending)
    }

    protected fun IProcessNodeInstance<C>.assertAcknowledged() = apply {
        assertState(NodeInstanceState.Acknowledged)
    }

    protected fun IProcessNodeInstance<C>.assertComplete() = apply {
        assertState(NodeInstanceState.Complete)
    }

    protected fun IProcessNodeInstance<C>.assertState(state: NodeInstanceState) {
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

    companion object {

        @JvmStatic
        @JvmName("newTestInstance")
        @PublishedApi
        internal fun <T : ContextProcessTransaction<C>, C : ActivityInstanceContext> newTestProcessEngineInstance(
            messageService: IMessageService<*, C>,
            transactionFactory: ProcessTransactionFactory<T, C>,
            processModels: IMutableProcessModelMap<T>,
            processInstances: MutableTransactionedHandleMap<SecureObject<ProcessInstance<C>>, T>,
            processNodeInstances: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, C>>, T>,
            autoTransition: Boolean,
            logger: Logger,
            processContextFactory: ProcessContextFactory<C>
        ): ProcessEngine<T, C> {


            return ProcessEngine(
                messageService, transactionFactory, processModels,
                processInstances, processNodeInstances,
                autoTransition, logger,
                processContextFactory
            )
        }


        fun <C: ActivityInstanceContext> defaultEngineFactory(messageService: StubMessageService<C>, transactionFactory: ProcessTransactionFactory<StubProcessTransaction<C>, C>, contextFactory: ProcessContextFactory<C>): ProcessEngine<StubProcessTransaction<C>, C> {
            return newTestProcessEngineInstance(
                messageService,
                transactionFactory,
                cacheModels<Any, C>(MemProcessModelMap(), 3),
                cacheInstances(
                    nl.adaptivity.process.MemTransactionedHandleMap(),
                    1
                ),
                cacheNodes<Any, C>(
                    nl.adaptivity.process.MemTransactionedHandleMap(
                        ::PNI_SET_HANDLE
                    ), 2
                ), true, Logger.getAnonymousLogger(),
                contextFactory
            )
        }

        fun <C: ActivityInstanceContext> PNI_SET_HANDLE(transaction: StubProcessTransaction<C>, pni: SecureObject<ProcessNodeInstance<ProcessNodeInstance<*, C>, C>>, handle: PNIHandle): SecureObject<ProcessNodeInstance<*, C>>? {
            if (pni.withPermission().handle == handle) {
                return pni
            }
            val piBuilder = transaction.readableEngineData.instance(pni.withPermission().hProcessInstance).withPermission().builder()
            return pni.withPermission().builder(piBuilder).also { it.handle = handle.coerce() }.build()
        }

        fun <V:Any, C : ActivityInstanceContext> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction<C>>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction<C>> {
            return net.devrieze.util.CachingHandleMap(base, count)
        }

        fun <V, C: ActivityInstanceContext> cacheNodes(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, C>>, StubProcessTransaction<C>>, count: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*, C>>, StubProcessTransaction<C>> {
            return net.devrieze.util.CachingHandleMap(base, count, ::PNI_SET_HANDLE)
        }

        fun <V, C: ActivityInstanceContext> cacheModels(base: IMutableProcessModelMap<StubProcessTransaction<C>>, count: Int): IMutableProcessModelMap<StubProcessTransaction<C>> {
            return CachingProcessModelMap(base, count)
        }

    }

}

typealias PNIHandle = Handle<SecureObject<ProcessNodeInstance<*, *>>>
