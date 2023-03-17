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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import net.devrieze.util.InputStreamOutputStream
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.impl.dom.toFragment
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.getDefines
import nl.adaptivity.process.engine.test.ProcessEngineTestSupport
import nl.adaptivity.process.engine.test.testProcess
import nl.adaptivity.process.engine.test.testRawEngine
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.condition
import nl.adaptivity.process.processModel.engine.ExecutableCondition
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableStartNode
import nl.adaptivity.util.activation.Sources
import nl.adaptivity.xmlutil.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.CharArrayWriter
import java.util.*
import javax.xml.bind.JAXB
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import kotlin.test.fail


/**
 * Created by pdvrieze on 18/08/15.
 */
class TestProcessEngine : ProcessEngineTestSupport<ActivityInstanceContext>() {

    private fun getXml(name: String): String {
        javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name)!!.use { reader ->
            val out = ByteArrayOutputStream()
            if (!InputStreamOutputStream.getInputStreamOutputStream(reader, out).get()) {
                fail("Missing file: $name")
            }
            val byteArray = out.toByteArray()
            assertTrue(byteArray.isNotEmpty(), "Some bytes in the xml files are expected")
            return byteArray.decodeToString()
        }
    }

    @Throws(XmlException::class)
    private fun getStream(name: String): XmlReader {
        return XmlStreaming.newReader(getXml(name))
    }

    @Throws(XmlException::class)
    private fun getProcessModel(name: String): ExecutableProcessModel {
        return ExecutableProcessModel.deserialize(getStream(name))
    }

    private fun getDocument(name: String): Document {
        try {
            javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name)
                .use { reader -> return documentBuilder.parse(reader) }
        } catch (e: Exception) {
            if (e is RuntimeException) {
                throw e
            }
            throw RuntimeException(e)
        }

    }

    @Throws(XmlException::class)
    private fun serializeToXml(obj: Any): String {
        return CharArrayWriter().apply {
            val caw = this
            if (obj is XmlSerializable) {
                XmlStreaming.newWriter(this).use { writer ->
                    obj.serialize(writer)
                }
            } else {
                JAXB.marshal(obj, caw)
            }
        }.toCharArray().concatToString()
    }

    @Test
    @Throws(Exception::class)
    fun testExecuteSingleActivity() {
        val model = getProcessModel("testModel1.xml")
        testRawEngine { processEngine ->
            val transaction = processEngine.startTransaction()
            val engineData = transaction.writableEngineData
            val modelHandle = processEngine.addProcessModel(transaction, model, model.owner).handle

            val instanceHandle = processEngine.startProcess(
                transaction,
                model.owner,
                modelHandle,
                "testInstance1",
                UUID.randomUUID(),
                null
            )

            assertEquals(1, stubMessageService.messages.size)
            assertEquals(1L, stubMessageService.getMessageNode(0).handleValue)

            val expected = getXml("testModel1_task1.xml")

            val receivedChars = serializeToXml(stubMessageService.messages[0].base)

            assertXmlEquals(expected, receivedChars)

            if (true) {
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
                engineData.updateInstance(processInstance.handle) {
                    updateChild(taskNode) {
                        finishTask(engineData)
                        assertComplete()
                    }
                }
                // process the queue as the we are going around the engine.
                processEngine.processTickleQueue(transaction)
            }

            if (true) {
                val processInstance = transaction.getInstance(instanceHandle).assertIsFinished()
                val start = processInstance.child(transaction, "start")
                val ac = processInstance.child(transaction, "ac2")
                val end = processInstance.child(transaction, "end")
                processInstance.assertActive()
                processInstance.assertFinished(start, ac)
                processInstance.assertCompleted(end)

            }
        }
    }

    @Test
    fun testConditionFalse() {
        val model = ExecutableProcessModel.build {
            owner = testModelOwnerPrincipal
            val start = startNode { id = "start" }
            val ac = activity { id = "ac"; predecessor = start.identifier; condition = ExecutableCondition.FALSE }
            val end = endNode { id = "end"; predecessor = ac }
        }
        testProcess(model = model) { processEngine, transaction, model, instanceHandle ->
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
            owner = testModelOwnerPrincipal
            val start = startNode { id = "start" }
            val ac = activity { id = "ac"; predecessor = start.identifier; condition("true()") }
            val end = endNode { id = "end"; predecessor = ac }
        }
        testProcess(model) { processEngine, transaction, model, instanceHandle ->
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
        testProcess(simpleSplitModel) { processEngine, protoTransaction, model, instanceHandle ->
            val transaction = protoTransaction as StubProcessTransaction<ActivityInstanceContext>
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
                        val messageSources = stubMessageService.messages
                            .map { transaction.readableEngineData.nodeInstance(it.source).withPermission() }
                            .sortedBy { it.node.id }
                        assertEquals(listOf(ac1, ac2), messageSources)
                        stubMessageService.messages.forEach { msg ->
                            msg.source
                        }
                    }

                    engineData.updateInstance(instance.handle) {
                        updateChild(ac1) {
                            finishTask(engineData)
                            assertComplete()
                        }
                    }

                }
                // process the queue as the we are going around the engine.
                processEngine.processTickleQueue(transaction)
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

                    transaction.writableEngineData.updateInstance(instanceHandle) {
                        updateChild(ac2) {
                            startTask(transaction.writableEngineData)
                        }
                    }
                }
                // process the queue as the we are going around the engine.
                processEngine.processTickleQueue(transaction)
                run {
                    val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
                    val start = instance.child(transaction, "start")
                    val split = instance.child(transaction, "split1").assertStarted()
                    val ac1 = instance.child(transaction, "ac1").assertComplete()
                    val ac2 = instance.child(transaction, "ac2").assertStarted()
                    val join = instance.child(transaction, "join1").assertPending()
                    instance.assertFinished(ac1, start)
                    instance.assertActive(ac2, split, join)

                    engineData.updateInstance(instanceHandle) {
                        updateChild(ac2) {
                            finishTask(engineData)
                            assertComplete()
                        }
                    }
                }
                // process the queue as the we are going around the engine.
                processEngine.processTickleQueue(transaction)

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
            // process the queue as the we are going around the engine.
            processEngine.processTickleQueue(transaction)
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
        testRawEngine { processEngine ->
            val transaction = processEngine.startTransaction()
            val engineData = transaction.writableEngineData
            val modelHandle = processEngine.addProcessModel(transaction, model, model.owner).handle

            val instanceHandle = processEngine.startProcess(
                transaction,
                model.owner,
                modelHandle,
                "testInstance1",
                UUID.randomUUID(),
                null
            )

            assertEquals(1, stubMessageService.messages.size)

            assertXmlEquals(
                getXml("testModel2_task1.xml"),
                serializeToXml(stubMessageService.messages[0].base)
            )

            var ac1: ProcessNodeInstance<*, ActivityInstanceContext> =
                processEngine.getNodeInstance(transaction, stubMessageService.getMessageNode(0), model.owner)
                    ?: throw AssertionError("Message node not found")// This should be 0 as it's the first activity

            ac1.node.results.let { r ->
                assertArrayEquals(CharArray(0), r[0].content)
                assertEquals("name", r[0].getName())
                assertEquals("user", r[1].getName())
                assertEquals("/umh:result/umh:value[@name='user']/text()", r[0].getPath())
                assertEquals(null, r[1].getPath())

                assertEquals(listOf(XmlEvent.NamespaceImpl("umh", "http://adaptivity.nl/userMessageHandler")),
                             r[0].originalNSContext.sortedBy { it.prefix })
                if (r[1].originalNSContext.count() == 1) {
                    assertEquals(
                        listOf(XmlEvent.NamespaceImpl("umh", "http://adaptivity.nl/userMessageHandler")),
                        r[1].originalNSContext.toList()
                    )
                } else {
                    assertEquals(
                        listOf(
                            XmlEvent.NamespaceImpl("", "http://adaptivity.nl/ProcessEngine/"),
                            XmlEvent.NamespaceImpl("umh", "http://adaptivity.nl/userMessageHandler")
                        ),
                        r[1].originalNSContext.sortedBy { it.prefix }
                    )
                }


                val result2ExpectedContent = """|
                |      <user xmlns="" xmlns:jbi="http://adaptivity.nl/ProcessEngine/activity">
                |        <fullname>
                |          <jbi:value xpath="/umh:result/umh:value[@name='user']/text()"/>
                |        </fullname>
                |      </user>
                |    """.trimMargin("|")
                assertEquals(result2ExpectedContent, String(r[1].content!!).replace(" />", "/>"))
            }

            stubMessageService.clear() // (Process the message)
            assertEquals(0, ac1.results.size)
            ac1 = processEngine.finishTask(
                transaction,
                ac1.handle, getDocument("testModel2_response1.xml").toFragment(), model.owner
            )
            assertEquals(NodeInstanceState.Complete, ac1.state)
            ac1 = processEngine.getNodeInstance(
                transaction,
                ac1.handle, model.owner
            ) ?: throw AssertionError("Node ${ac1.handle} not found")
            assertEquals(2, ac1.results.size)
            val result1 = ac1.results[0]
            val result2 = ac1.results[1]
            assertEquals("name", result1.name)
            assertEquals("Paul", result1.content.contentString)
            assertEquals("user", result2.name)

            result2.content.contentString.let { actual ->
                val expected = "<user xmlns=''><fullname>Paul</fullname></user>"
                assertXmlEquals(expected, actual)
            }
            assertEquals(1, stubMessageService.messages.size)
            assertEquals(
                2L,
                stubMessageService.getMessageNode(0).handleValue
            ) //We should have a new message with the new task (with the data)
            val ac2 =
                processEngine.getNodeInstance(transaction, stubMessageService.getMessageNode(0), model.owner)



            val aic = engineData.processContextFactory.newActivityInstanceContext(engineData, ac2!!)
            val ac2Defines = aic.getDefines(
                processEngine.getProcessInstance(
                    transaction,
                    ac2.hProcessInstance,
                    model.owner
                )
            )
            assertEquals(1, ac2Defines.size)


            val define = ac2Defines[0]
            assertEquals("mylabel", define.name)
            assertEquals("Hi Paul. Welcome!", define.content.contentString)
        }
    }

    private val simpleSplitModel: ExecutableProcessModel
        get() {
            return ExecutableProcessModel.build {
                owner = testModelOwnerPrincipal
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
