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

package nl.adaptivity.process.engine.test

import net.devrieze.util.CachingHandleMap
import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handle
import net.devrieze.util.MutableTransactionedHandleMap
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.dropStack
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.*
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultComparisonFormatter
import java.net.URI
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList

open class ProcessEngineTestSupport(): BaseProcessEngineTestSupport<ActivityInstanceContext>(ProcessContextFactory.DEFAULT)

open class BaseProcessEngineTestSupport<A : ActivityInstanceContext>(val contextFactory: ProcessContextFactory<A>) {
    protected lateinit var processEngine: ProcessEngine<StubProcessTransaction, A>
    private val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"),
                                                       "processEngine",
                                                       URI.create("http://localhost/"))
    protected val stubMessageService: StubMessageService = StubMessageService(localEndpoint)
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

    protected inline fun <R> testProcess(model: ExecutableProcessModel, payload: CompactFragment? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R): R {
        processEngine.startTransaction().use { transaction ->

            val modelHandle = processEngine.addProcessModel(transaction, model, modelOwnerPrincipal)
            val instanceHandle = processEngine.startProcess(transaction, modelOwnerPrincipal, modelHandle, "testInstance", UUID.randomUUID(), payload)

            return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
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

    protected fun ProcessInstance.assertFinishedHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
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

    private fun ProcessInstance.assertActiveHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
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

    private fun ProcessInstance.assertCompletedHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
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
        assertEquals(State.STARTED, this.state)
    }

    protected fun ProcessInstance.assertIsFinished() = apply {
        assertEquals(State.FINISHED, this.state)
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
            assertEquals(state, this.state,
                         "Node ${this.node.id}(${this.handle}) should be in the ${state.name} state")
        } catch (e: AssertionError) {
            throw dropStack(e, 2)
        }
    }

    @BeforeEach
    fun beforeTest() {
        stubMessageService.clear()
        //    DelegateProcessEngineData<StubProcessTransaction> engineData =
        //            new DelegateProcessEngineData<>(mStubTransactionFactory,
        //                                            cache(new MemProcessModelMap(), 1),
        //                                            cache(new MemTransactionedHandleMap<>(), 1),
        //                                            cache(new MemTransactionedHandleMap<>(), 2));

        processEngine = ProcessEngine.newTestInstance(
            stubMessageService,
            stubTransactionFactory,
            cacheModels<Any>(MemProcessModelMap(), 3),
            cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
            cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>(PNI_SET_HANDLE), 2), true, Logger.getAnonymousLogger(),
            contextFactory
                                                     )
    }

    fun assertEqualsXml(expected: String?, actual: String?) {
        val diff = DiffBuilder.compare(expected)
            .withTest(actual)
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar()
            .build()

        if (diff.hasDifferences()) {
            assertEquals(expected, actual, diff.toString(DefaultComparisonFormatter()))
        }
    }

    companion object {

        internal val PNI_SET_HANDLE = fun(transaction: StubProcessTransaction, pni: SecureObject<ProcessNodeInstance<*>>, handle: Handle<SecureObject<ProcessNodeInstance<*>>>): SecureObject<ProcessNodeInstance<*>>? {
            if (pni.withPermission().handle == handle) {
                return pni
            }
            val piBuilder = transaction.readableEngineData.instance(pni.withPermission().hProcessInstance).withPermission().builder()
            return pni.withPermission().builder(piBuilder).also { it.handle = net.devrieze.util.handle(handle) }.build()
        }

        internal fun <V:Any> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction> {
            return CachingHandleMap<V, StubProcessTransaction>(base, count)
        }

        internal fun <V> cacheNodes(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction> {
            return CachingHandleMap(base, count, PNI_SET_HANDLE)
        }

        internal fun <V> cacheModels(base: IMutableProcessModelMap<StubProcessTransaction>, count: Int): IMutableProcessModelMap<StubProcessTransaction> {
            return CachingProcessModelMap(base, count)
        }

    }

}
