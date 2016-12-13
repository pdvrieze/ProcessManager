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
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.security.withPermission
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance.Builder
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableStartNode
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.util.activation.Sources
import nl.adaptivity.xml.*
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLUnit
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.bind.JAXB
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource

import java.io.*
import java.net.URI
import java.util.UUID

import java.nio.charset.Charset.defaultCharset
import org.custommonkey.xmlunit.XMLAssert.assertXMLEqual
import org.testng.Assert.*
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.pnipredecessors.predecessor


/**
 * Created by pdvrieze on 18/08/15.
 */
class TestProcessEngine {

  internal lateinit var mProcessEngine: ProcessEngine<StubProcessTransaction>
  private val mLocalEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
  private val mStubMessageService: StubMessageService<ProcessTransaction> = StubMessageService(mLocalEndpoint)
  private val mStubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
      return StubProcessTransaction(engineData)
    }
  }
  private val mPrincipal = SimplePrincipal("pdvrieze")

  private fun getXml(name: String): InputStream? {
    try {
      javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name).use { reader ->
        val out = ByteArrayOutputStream()
        if (!InputStreamOutputStream.getInputStreamOutputStream(reader, out).get()) {
          return null
        }
        val byteArray = out.toByteArray()
        assertTrue(byteArray.size > 0, "Some bytes in the xml files are expected")
        val bais = ByteArrayInputStream(byteArray)
        return bais
      }
    } catch (e: Exception) {
      if (e is RuntimeException) {
        throw e
      }
      throw RuntimeException(e)
    }

  }

  @Throws(XmlException::class)
  private fun getStream(name: String): XmlReader {
    return XmlStreaming.newReader(getXml(name)!!, "UTF-8")
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

  @BeforeMethod
  fun beforeTest() {
    mStubMessageService.clear()
    //    DelegateProcessEngineData<StubProcessTransaction> engineData =
    //            new DelegateProcessEngineData<>(mStubTransactionFactory,
    //                                            cache(new MemProcessModelMap(), 1),
    //                                            cache(new MemTransactionedHandleMap<>(), 1),
    //                                            cache(new MemTransactionedHandleMap<>(), 2));

    mProcessEngine = ProcessEngine.newTestInstance(
        mStubMessageService,
        mStubTransactionFactory,
        cacheModels<Any>(MemProcessModelMap(), 1),
        cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
        cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>(PNI_SET_HANDLE), 2), true)
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

  @Test
  @Throws(Exception::class)
  fun testExecuteSingleActivity() {
    val model = getProcessModel("testModel1.xml")
    val transaction = mProcessEngine.startTransaction()
    val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)

    val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null)

    assertEquals(mStubMessageService._messages.size, 1)
    assertEquals(mStubMessageService.getMessageNode(0).handleValue, 1L)

    val expected = getXml("testModel1_task1.xml")

    val receivedChars = serializeToXmlCharArray(mStubMessageService._messages[0].base)

    XMLUnit.setIgnoreWhitespace(true)
    try {
      assertXMLEqual(InputStreamReader(expected!!), CharArrayReader(receivedChars))
    } catch (e: AssertionError) {
      e.printStackTrace()
      try {
        assertEquals(String(receivedChars), Streams.toString(getXml("testModel1_task1.xml"), defaultCharset()))
      } catch (f: Exception) {
        f.initCause(e)
        throw f
      }

    }

    var processInstance = mProcessEngine.getProcessInstance(transaction, instanceHandle, mPrincipal)
    assertEquals(processInstance.state, State.STARTED)

    assertEquals(processInstance.active.size, 1)
    assertEquals(processInstance.finished.size, 1)
    val hfinished = processInstance.finished.iterator().next()
    val finished = mProcessEngine.getNodeInstance(transaction, hfinished, mPrincipal)
    assertTrue(finished!!.node is ExecutableStartNode)
    assertEquals(finished.node.id, "start")

    assertEquals(processInstance.completedEndnodes.size, 0)

    val taskNode = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal)
    assertEquals(taskNode!!.state, NodeInstanceState.Pending) // Our messenger does not do delivery notification

    assertEquals(mProcessEngine.finishTask(transaction, taskNode.handle, null, mPrincipal).state, NodeInstanceState.Complete)
    processInstance = mProcessEngine.getProcessInstance(transaction, instanceHandle, mPrincipal)
    assertEquals(processInstance.active.size, 0)
    assertEquals(processInstance.finished.size, 2)
    assertEquals(processInstance.completedEndnodes.size, 1)

    assertEquals(processInstance.state, State.FINISHED)
  }

  private fun <R> testProcess(model: ExecutableProcessModel, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance<StubProcessTransaction>) -> R):R {
    mProcessEngine.startTransaction().use { transaction ->

      val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)
      val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance", UUID.randomUUID(), payload)

      return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
    }
  }

  @Test
  fun testSplitJoin1() {
    testProcess(simpleSplitModel) { transaction, model, instanceHandle ->
      run {
        val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
        val start = instance.getChild(transaction, "start")?.withPermission() ?: throw AssertionError("Start node not instantiated")
        run {
          val split = instance.getChild(transaction, "split1")?.withPermission() ?: throw AssertionError("Split node not instantiated")
          val ac1 = instance.getChild(transaction, "ac1")?.withPermission() ?: throw AssertionError("Activity 1 not instantiated")
          val ac2 = instance.getChild(transaction, "ac2")?.withPermission() ?: throw AssertionError("Activity 2 not instantiated")

          assertEquals(start.state, NodeInstanceState.Complete)
          assertEquals(instance.finished.toList(), listOf(start, split))
          assertEquals(instance.active.sortedBy { transaction.readableEngineData.nodeInstance(it)?.withPermission()?.node?.id }, listOf(ac1, ac2))

          assertEquals(split.state, NodeInstanceState.Started)
          assertTrue(split.handle in instance.active)

          run {
            val messageSources = mStubMessageService._messages.map { transaction.readableEngineData.nodeInstance(it.source).withPermission() }.sortedBy { it.node.id }
            assertEquals(messageSources, listOf(ac1, ac2))
          }

          assertEquals(ac1.finishTask(transaction, null).state, NodeInstanceState.Complete)
        }
        run {
          val split = instance.getChild(transaction, "split1")?.withPermission() ?: throw AssertionError("Split node not instantiated")
          val ac1 = instance.getChild(transaction, "ac1")?.withPermission() ?: throw AssertionError("Activity 1 not instantiated")
          val ac2 = instance.getChild(transaction, "ac2")?.withPermission() ?: throw AssertionError("Activity 2 not instantiated")
          val join = (instance.getChild(transaction, "join1")?.withPermission() ?: throw AssertionError("Join node not instantiated")) as JoinInstance
          assertEquals(instance.active.map { transaction.readableEngineData.nodeInstance(it).withPermission() }.sortedBy { it.node.id }, listOf(ac2, join, split))
          assertEquals(instance.finished.map { transaction.readableEngineData.nodeInstance(it).withPermission() }.sortedBy { it.node.id }, listOf(ac1, start))
          assertEquals(join.state, NodeInstanceState.Started)
          assertEquals(ac2.state, NodeInstanceState.Acknowledged)

          assertEquals(ac2.finishTask(transaction, null).state, NodeInstanceState.Complete)
        }

        run {
          val join = (instance.getChild(transaction, "join1")?.withPermission() ?: throw AssertionError("Join node not instantiated")) as JoinInstance
          val ac2 = instance.getChild(transaction, "ac2")?.withPermission() ?: throw AssertionError("Activity 2 not instantiated")
          val end = instance.getChild(transaction, "end")?.withPermission() ?: throw AssertionError("End node not instantiated")
          assertEquals(instance.active.size, 0)
          assertEquals(join.state, NodeInstanceState.Complete)
          assertEquals(ac2.state, NodeInstanceState.Complete)
          assertEquals(end.state, NodeInstanceState.Complete)

        }

      }
      run {
        val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
        assertEquals(instance.state, State.FINISHED)
      }
    }

  }

  private val simpleSplitModel: ExecutableProcessModel get() {
    return ExecutableProcessModel.build {
      owner = mPrincipal
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
        predecessor = split1
        id = "ac1"
        message = XmlMessage()
        result {
          name = "ac1result"
          content = "ac1content".toCharArray()
        }
      }
      val ac2 = activity {
        predecessor = split1
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
      }
      endNode {
        id = "end"
        predecessor = join
      }
    }
  }

  @Test
  @Throws(Exception::class)
  fun testGetDataFromTask() {
    val model = getProcessModel("testModel2.xml")
    val transaction = mProcessEngine.startTransaction()
    val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)

    val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null)

    assertEquals(mStubMessageService._messages.size, 1)

    XMLUnit.setIgnoreWhitespace(true)
    assertXMLEqual(InputStreamReader(getXml("testModel2_task1.xml")!!), CharArrayReader(serializeToXmlCharArray(mStubMessageService
        ._messages[0].base)))
    var ac1: ProcessNodeInstance = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal) ?: throw AssertionError("Message node not found")// This should be 0 as it's the first activity


    mStubMessageService.clear() // (Process the message)
    assertEquals(ac1.results.size, 0)
    ac1 = mProcessEngine.finishTask(transaction, ac1.handle, getDocument("testModel2_response1.xml"), mPrincipal)
    assertEquals(ac1.state, NodeInstanceState.Complete)
    ac1 = mProcessEngine.getNodeInstance(transaction, ac1.handle, mPrincipal) ?: throw AssertionError("Node ${ac1.handle} not found")
    assertEquals(ac1.results.size, 2)
    val result1 = ac1.results[0]
    val result2 = ac1.results[1]
    assertEquals(result1.name, "name")
    assertEquals(result1.content.contentString, "Paul")
    assertEquals(result2.name, "user")
    assertXMLEqual("<user><fullname>Paul</fullname></user>", result2.content.contentString)

    assertEquals(mStubMessageService._messages.size, 1)
    assertEquals(mStubMessageService.getMessageNode(0).handleValue, 2L) //We should have a new message with the new task (with the data)
    val ac2 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal)

    val ac2Defines = ac2!!.getDefines(transaction)
    assertEquals(ac2Defines.size, 1)


    val define = ac2Defines[0]
    assertEquals(define.name, "mylabel")
    assertEquals(define.content.contentString, "Hi Paul. Welcome!")

  }

  companion object {

    private val PNI_SET_HANDLE = fun(pni: SecureObject<out ProcessNodeInstance>, handle: Long?): SecureObject<out ProcessNodeInstance> {
      if (pni.withPermission().getHandleValue() == handle) {
        return pni
      }
      val builder = pni.withPermission().builder()
      builder.handle = Handles.handle<SecureObject<ProcessNodeInstance>>(handle!!)
      return builder.build()
    }

    private fun <V:Any> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction> {
      return CachingHandleMap<V, StubProcessTransaction>(base, count)
    }

    private fun <V> cacheNodes(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction> {
      return CachingHandleMap(base, count, PNI_SET_HANDLE)
    }

    private fun <V> cacheModels(base: IMutableProcessModelMap<StubProcessTransaction>, count: Int): IMutableProcessModelMap<StubProcessTransaction> {
      return CachingProcessModelMap(base, count)
    }

    private val documentBuilder: DocumentBuilder by lazy {
      DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isIgnoringElementContentWhitespace = false
        isCoalescing = false

      }.newDocumentBuilder()
    }

    private fun assertXMLSimilar(expected: Document, actual: Document) {
      val diff = XMLUnit.compareXML(expected, actual)
      val detailedDiff = DetailedDiff(diff)
      if (!detailedDiff.similar()) {
        fail(detailedDiff.toString())
      }
    }

    @Throws(TransformerException::class)
    private fun toDocument(node: Node): Document {
      val result = documentBuilder.newDocument()
      Sources.writeToResult(DOMSource(node), DOMResult(result))
      return result
    }
  }


}
