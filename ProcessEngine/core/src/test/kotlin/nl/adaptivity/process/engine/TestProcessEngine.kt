/*
 * Copyright (c) 2018.
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
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.dropStack
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableStartNode
import nl.adaptivity.util.activation.Sources
import nl.adaptivity.xml.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xmlunit.XMLUnitException
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultComparisonFormatter
import java.io.*
import java.net.URI
import java.util.UUID
import javax.xml.bind.JAXB
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import kotlin.collections.ArrayList


/**
 * Created by pdvrieze on 18/08/15.
 */
class TestProcessEngine {

    internal lateinit var mProcessEngine: ProcessEngine<StubProcessTransaction>
    private val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"),
                                                       "processEngine",
                                                       URI.create("http://localhost/"))
    private val stubMessageService: StubMessageService = StubMessageService(localEndpoint)
    private val stubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
        override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
            return StubProcessTransaction(engineData)
        }
    }
    private val principal = SimplePrincipal("pdvrieze")

    private fun getXml(name: String): ByteArray? {
        javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name).use { reader ->
            val out = ByteArrayOutputStream()
            if (!InputStreamOutputStream.getInputStreamOutputStream(reader, out).get()) {
                return null
            }
            val byteArray = out.toByteArray()
            assertTrue(byteArray.isNotEmpty(), "Some bytes in the xml files are expected")
            return byteArray
        }
    }

    @Throws(XmlException::class)
    private fun getStream(name: String): XmlReader {
        return XmlStreaming.newReader(ByteArrayInputStream(getXml(name)!!), "UTF-8")
    }

    @Throws(XmlException::class)
    private fun getProcessModel(name: String): ExecutableProcessModel {
        return ExecutableProcessModel.deserialize(getStream(name))
    }

    private fun getDocument(name: String): Document {
        try {
            javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name).use { reader -> return documentBuilder.parse(reader) }
        } catch (e: Exception) {
            if (e is RuntimeException) {
                throw e
            }
            throw RuntimeException(e)
        }

    }

    @Throws(XmlException::class)
    private fun serializeToXmlCharArray(obj: Any): CharArray {
        return CharArrayWriter().apply {
            val caw = this
            if (obj is XmlSerializable) {
                XmlStreaming.newWriter(this).use { writer ->
                    obj.serialize(writer)
                }
            } else {
                JAXB.marshal(obj, caw)
            }
        }.toCharArray()
    }

    private inline fun <R> testProcess(model: ExecutableProcessModel, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R):R {
        mProcessEngine.startTransaction().use { transaction ->

            val modelHandle = mProcessEngine.addProcessModel(transaction, model, principal)
            val instanceHandle = mProcessEngine.startProcess(transaction, principal, modelHandle, "testInstance", UUID.randomUUID(), payload)

            return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
        }
    }

    private fun ProcessTransaction.getInstance(instanceHandle: HProcessInstance):ProcessInstance {
        return readableEngineData.instance(instanceHandle).withPermission()
    }

    private fun StubMessageService.messageNode(transaction: ProcessTransaction, index:Int): ProcessNodeInstance<*> {
        return transaction.readableEngineData.nodeInstance(this._messages[index].source).withPermission()
    }

    private fun ProcessInstance.child(transaction: ProcessTransaction, name: String) : ProcessNodeInstance<*> {
        return getChild(name, 1)?.withPermission() ?: throw AssertionError("No node instance for node id ${name} found")
    }

    private val ProcessInstance.sortedFinished
        get() = finished.sortedBy { it.handleValue }

    private val ProcessInstance.sortedActive
        get() = active.sortedBy { it.handleValue }

    private val ProcessInstance.sortedCompleted
        get() = completedEndnodes.sortedBy { it.handleValue }

    private fun ProcessInstance.assertFinishedHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedFinished))
    }

    private fun ProcessInstance.assertFinished(vararg handles: IProcessNodeInstance) = apply {
        val actual = ArrayList(sortedFinished)
        val expected = handles.asSequence().map { it.handle() }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e: AssertionError) {
            throw AssertionError("Expected finished list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance.assertActiveHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedActive))
    }

    private fun ProcessInstance.assertActive(vararg handles: IProcessNodeInstance) = apply {
        val actual = ArrayList(sortedActive)
        val expected = handles.asSequence().map { it.handle() }.sortedBy { it.handleValue }.toList()
        try {
            assertEquals(expected, actual)
        } catch (e:AssertionError) {
            throw AssertionError("Expected active list $expected, but found $actual").initCause(e)
        }
    }

    private fun ProcessInstance.assertCompletedHandles(vararg handles: ComparableHandle<SecureObject<ProcessNodeInstance<*>>>) = apply {
        assertEquals(handles.sorted(), ArrayList(sortedCompleted))
    }

    private fun ProcessInstance.assertCompleted(vararg nodes: IProcessNodeInstance): ProcessInstance {
        val actual = ArrayList(sortedCompleted)
        val expected = nodes.asSequence().map { it.handle() }.sortedBy { it.handleValue }.toList()
        assertEquals(expected, actual)
        return this
    }

    private fun IProcessNodeInstance.assertStarted() = apply {
        assertEquals(NodeInstanceState.Started, this.state)
    }

    private fun ProcessInstance.assertIsStarted() = apply {
        assertEquals(State.STARTED, this.state)
    }

    private fun ProcessInstance.assertIsFinished() = apply {
        assertEquals(State.FINISHED, this.state)
    }

    private fun IProcessNodeInstance.assertSent() = apply {
        assertState(NodeInstanceState.Sent)
    }

    private fun IProcessNodeInstance.assertPending() = apply {
        assertState(NodeInstanceState.Pending)
    }

    private fun IProcessNodeInstance.assertAcknowledged() = apply {
        assertState(NodeInstanceState.Acknowledged)
    }

    private fun IProcessNodeInstance.assertComplete() = apply {
        assertState(NodeInstanceState.Complete)
    }

    private fun IProcessNodeInstance.assertState(state: NodeInstanceState) {
        try {
            assertEquals(state, this.state,
                         "Node ${this.node.id}(${this.handle()}) should be in the ${state.name} state")
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

        mProcessEngine = ProcessEngine.newTestInstance(
            stubMessageService,
            stubTransactionFactory,
            cacheModels<Any>(MemProcessModelMap(), 3),
            cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
            cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance<*>>, StubProcessTransaction>(PNI_SET_HANDLE), 2), true)
    }

    fun assertEqualsXml(expected: ByteArray, actual: CharArray?) {
        if (actual==null) org.junit.jupiter.api.fail("No actual result")
        val expected = String(expected)
        val actual = String(actual)

        val expectedDoc = DocumentBuilderFactory
            .newInstance().apply { isNamespaceAware=true }
            .newDocumentBuilder()
            .parse(InputSource(StringReader(expected)))
        assertNotNull(expectedDoc)
        val diff = try {
            DiffBuilder.compare(expectedDoc)
                .withTest(actual)
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .build()
        } catch (e: XMLUnitException) {
            assertEquals(expected, actual, "Error comparing: ${e.message}")
            throw e
        }

        if (diff.hasDifferences()) {
            assertEquals(expected, actual,
                         diff.toString(DefaultComparisonFormatter()))
        }
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

    @Test
    @Throws(Exception::class)
    fun testExecuteSingleActivity() {
        val model = getProcessModel("testModel1.xml")
        val transaction = mProcessEngine.startTransaction()
        val engineData = transaction.writableEngineData
        val modelHandle = mProcessEngine.addProcessModel(transaction, model, principal)

        val instanceHandle = mProcessEngine.startProcess(transaction, principal, modelHandle, "testInstance1", UUID.randomUUID(), null)

        assertEquals(1, stubMessageService._messages.size)
        assertEquals(1L, stubMessageService.getMessageNode(0).handleValue)

        val expected = getXml("testModel1_task1.xml")!!

        val receivedChars = serializeToXmlCharArray(stubMessageService._messages[0].base)

        assertEqualsXml(expected, receivedChars)

        run {
            val processInstance = transaction.getInstance(instanceHandle).assertIsStarted()

            assertEquals(1, processInstance.active.size)

            processInstance.child(transaction, "start").assertComplete().let { start ->
                processInstance.assertFinished(start)
                assertTrue(start.node is ExecutableStartNode)
            }

            processInstance.assertCompleted() // no completions

            val taskNode = stubMessageService.messageNode(transaction, 0)
            taskNode.assertAcknowledged()
            processInstance.assertActive(taskNode)
            processInstance.update(engineData) {
                updateChild(taskNode) {
                    finishTask(engineData)
                    assertComplete()
                }
            }
        }

        run {
            val processInstance = transaction.getInstance(instanceHandle).assertIsFinished()
            val start = processInstance.child(transaction, "start")
            val ac = processInstance.child(transaction, "ac2")
            val end = processInstance.child(transaction, "end")
            processInstance.assertActive()
            processInstance.assertFinished(start, ac)
            processInstance.assertCompleted(end)

        }
    }

    @Test
    fun testConditionFalse() {
        val model = ExecutableProcessModel.build {
            owner = principal
            val start = startNode { id="start" }
            val ac = activity { id="ac"; predecessor=start.identifier; condition="false()" }
            val end = endNode { id="end"; predecessor=ac }
        }
        testProcess(model) { transaction, model, instanceHandle ->
            transaction.readableEngineData.instance(instanceHandle).withPermission().let { instance ->
                val start = instance.child(transaction, "start")
                val ac = instance.child(transaction, "ac").apply { assertState(NodeInstanceState.Skipped) }
                val end = instance.child(transaction, "end").apply { assertState(NodeInstanceState.Skipped) }
                instance.assertFinished(start, ac)
                assertEquals(State.SKIPPED, instance.state)
            }
        }
    }

    @Test
    fun testConditionTrue() {
        val model = ExecutableProcessModel.build {
            owner = principal
            val start = startNode { id="start" }
            val ac = activity { id="ac"; predecessor=start.identifier; condition="true()" }
            val end = endNode { id="end"; predecessor=ac }
        }
        testProcess(model) { transaction, model, instanceHandle ->
            transaction.readableEngineData.instance(instanceHandle).withPermission().let { instance ->
                val start = instance.child(transaction, "start")
                val ac = instance.child(transaction, "ac").assertAcknowledged()
//        val end = instance.child(transaction, "end").apply { assertState(NodeInstanceState.Skipped) }
                instance.assertFinished(start)
                assertEquals(State.STARTED, instance.state)
            }
        }
    }

    @Test
    fun testSplitJoin1() {
        testProcess(simpleSplitModel) { transaction, model, instanceHandle ->
            val engineData = transaction.writableEngineData
            run {
                run {
                    val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                    val start = instance.child(transaction, "start")
                    val split = instance.child(transaction, "split1").assertStarted()
                    val ac1 = instance.child(transaction, "ac1").assertAcknowledged()
                    val ac2 = instance.child(transaction, "ac2").assertAcknowledged()

                    instance.assertFinished(start)
                    instance.assertActive(split, ac1, ac2)

                    run {
                        val messageSources = stubMessageService._messages
                            .map { transaction.readableEngineData.nodeInstance(it.source).withPermission() }
                            .sortedBy { it.node.id }
                        assertEquals(listOf(ac1, ac2), messageSources)
                        stubMessageService._messages.forEach { msg ->
                            msg.source
                        }
                    }

                    instance.update(engineData) {
                        updateChild(ac1) {
                            finishTask(engineData)
                            assertComplete()
                        }
                    }

                }
                run {
                    val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                    val start = instance.child(transaction, "start")
                    val split = instance.child(transaction, "split1").assertStarted()
                    val ac1 = instance.child(transaction, "ac1").assertComplete()
                    val ac2 = instance.child(transaction, "ac2").assertAcknowledged()
                    val join = instance.child(transaction, "join1").assertPending()
                    instance.assertFinished(ac1, start)
                    instance.assertActive(ac2, split, join)
                    // check join is in the pending set

                    instance.update(transaction.writableEngineData) {
                        updateChild(ac2) {
                            startTask(transaction.writableEngineData)
                        }
                    }
                }
                run {
                    val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                    val start = instance.child(transaction, "start")
                    val split = instance.child(transaction, "split1").assertStarted()
                    val ac1 = instance.child(transaction, "ac1").assertComplete()
                    val ac2 = instance.child(transaction, "ac2").assertStarted()
                    val join = instance.child(transaction, "join1").assertPending()
                    instance.assertFinished(ac1, start)
                    instance.assertActive(ac2, split, join)

                    instance.update(engineData) {
                        updateChild(ac2) {
                            finishTask(engineData)
                            assertComplete()
                        }
                    }
                }

                run {
                    val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                    val start = instance.child(transaction, "start")
                    val split = instance.child(transaction, "split1").assertComplete()
                    val ac1 = instance.child(transaction, "ac1").assertComplete()
                    val ac2 = instance.child(transaction, "ac2").assertComplete()
                    val join = instance.child(transaction, "join1").assertComplete()
                    val end = instance.child(transaction, "end").assertComplete()

                    assertEquals(0, instance.active.size)
                    instance.assertFinished(start, split, ac1, ac2, join)
                    instance.assertCompleted(end)
                }

            }
            run {
                val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                assertEquals(State.FINISHED, instance.state)
            }
        }

    }

    @Test
    @Throws(Exception::class)
    fun testGetDataFromTask() {
        val model = getProcessModel("testModel2.xml")
        val transaction = mProcessEngine.startTransaction()
        val engineData = transaction.writableEngineData
        val modelHandle = mProcessEngine.addProcessModel(transaction, model, principal)

        val instanceHandle = mProcessEngine.startProcess(transaction, principal, modelHandle, "testInstance1", UUID.randomUUID(), null)

        assertEquals(1, stubMessageService._messages.size)

        assertEqualsXml(getXml("testModel2_task1.xml")!!, serializeToXmlCharArray(stubMessageService
                                                                                      ._messages[0].base))

        var ac1: ProcessNodeInstance<*> = mProcessEngine.getNodeInstance(transaction, stubMessageService.getMessageNode(0), principal) ?: throw AssertionError("Message node not found")// This should be 0 as it's the first activity

        ac1.node.results.let { r ->
            assertEquals("", r[0].contentString)
            assertEquals("name", r[0].getName())
            assertEquals("user", r[1].getName())
            assertEquals("/umh:result/umh:value[@name='user']/text()", r[0].getPath())
            assertEquals(null, r[1].getPath())

            assertEquals(listOf(XmlEvent.NamespaceImpl("umh", "http://adaptivity.nl/userMessageHandler")),
                         r[0].namespaces.sortedBy { it.prefix })
            assertEquals(listOf(XmlEvent.NamespaceImpl("", "http://adaptivity.nl/ProcessEngine/"),
                                XmlEvent.NamespaceImpl("umh", "http://adaptivity.nl/userMessageHandler")),
                         r[1].namespaces.sortedBy { it.prefix })

            val result2ExpectedContent = """|
                |      <user xmlns="" xmlns:jbi="http://adaptivity.nl/ProcessEngine/activity">
                |        <fullname>
                |          <jbi:value xpath="/umh:result/umh:value[@name='user']/text()"/>
                |        </fullname>
                |      </user>
                |    """.trimMargin("|")
            assertEquals(result2ExpectedContent, r[1].contentString)
        }

        stubMessageService.clear() // (Process the message)
        assertEquals(0, ac1.results.size)
        ac1 = mProcessEngine.finishTask(transaction, ac1.getHandle(), getDocument("testModel2_response1.xml"), principal)
        assertEquals(NodeInstanceState.Complete, ac1.state)
        ac1 = mProcessEngine.getNodeInstance(transaction, ac1.getHandle(), principal) ?: throw AssertionError("Node ${ac1.getHandle()} not found")
        assertEquals(2, ac1.results.size)
        val result1 = ac1.results[0]
        val result2 = ac1.results[1]
        assertEquals("name", result1.name)
        assertEquals("Paul", result1.content.contentString)
        assertEquals("user", result2.name)

        result2.content.contentString.let { actual ->
            val expected = "<user xmlns=''><fullname>Paul</fullname></user>"
            assertEqualsXml(expected, actual)
        }
        assertEquals(1, stubMessageService._messages.size)
        assertEquals(2L, stubMessageService.getMessageNode(0).handleValue) //We should have a new message with the new task (with the data)
        val ac2 = mProcessEngine.getNodeInstance(transaction, stubMessageService.getMessageNode(0), principal)

        val ac2Defines = ac2!!.getDefines(engineData)
        assertEquals(1, ac2Defines.size)


        val define = ac2Defines[0]
        assertEquals("mylabel", define.name)
        assertEquals("Hi Paul. Welcome!", define.content.contentString)

    }

    private val simpleSplitModel: ExecutableProcessModel get() {
        return ExecutableProcessModel.build {
            owner = principal
            val start = startNode {
                id = "start"
            }
            val split1 = split {
                predecessor = start.identifier
                id = "split1"
                min = 2
                max = 2
            }
            val ac1 = activity {
                predecessor = split1.identifier
                id = "ac1"
                message = XmlMessage()
                result {
                    name = "ac1result"
                    content = "ac1content".toCharArray()
                }
            }
            val ac2 = activity {
                predecessor = split1.identifier
                id = "ac2"
                message = XmlMessage()
                result {
                    name = "ac2result"
                    content = "ac2content".toCharArray()
                }
            }
            val join = join {
                predecessors(ac1, ac2)
                id = "join1"
                min = 2
                max = 2
            }
            endNode {
                id = "end"
                predecessor = join
            }
        }
    }

    companion object {

        internal val PNI_SET_HANDLE = fun(transaction: StubProcessTransaction, pni: SecureObject<ProcessNodeInstance<*>>, handle: Handle<SecureObject<ProcessNodeInstance<*>>>): SecureObject<ProcessNodeInstance<*>>? {
            if (pni.withPermission().getHandle() == handle) {
                return pni
            }
            val piBuilder = transaction.readableEngineData.instance(pni.withPermission().hProcessInstance).withPermission().builder()
            return pni.withPermission().builder(piBuilder).also { it.handle = Handles.handle(handle) }.build()
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

        private val documentBuilder: DocumentBuilder by lazy {
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isIgnoringElementContentWhitespace = false
                isCoalescing = false

            }.newDocumentBuilder()
        }

        @Throws(TransformerException::class)
        private fun toDocument(node: Node): Document {
            val result = documentBuilder.newDocument()
            Sources.writeToResult(DOMSource(node), DOMResult(result))
            return result
        }
    }


}
