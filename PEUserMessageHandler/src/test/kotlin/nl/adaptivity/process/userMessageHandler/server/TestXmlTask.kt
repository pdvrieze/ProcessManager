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

package nl.adaptivity.process.userMessageHandler.server

import net.devrieze.util.Handles
import net.devrieze.util.ReaderInputStream
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.NodeInstanceState.Complete
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlStreaming
import nl.adaptivity.xml.serialize
import org.custommonkey.xmlunit.XMLAssert.assertXMLEqual
import org.testng.Assert.assertEquals
import org.testng.AssertJUnit.assertNotNull
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.dom.DOMSource


class TestXmlTask {

  private lateinit var mSampleTask: XmlTask

  @BeforeMethod
  fun before() {
    mSampleTask = XmlTask()
    mSampleTask.state = NodeInstanceState.Failed
    mSampleTask.owner = SimplePrincipal("pdvrieze")
    mSampleTask.remoteHandle = -1L
  }

  @Test
  @Throws(XmlException::class, IOException::class, SAXException::class)
  fun testSerialization() {
    val out = StringWriter()
    mSampleTask.serialize(out)
    assertXMLEqual("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<umh:task owner=\"pdvrieze\" state=\"Failed\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"/>\n",
                   out
                         .toString())
  }

  @Test
  @Throws(XmlException::class, IOException::class, SAXException::class)
  fun testSerialization2() {
    val out = StringWriter()
    val sampleTask2 = XmlTask(mSampleTask)
    sampleTask2.remoteHandle = 1L
    sampleTask2.instanceHandle = 2L
    sampleTask2.setHandleValue(3L)
    sampleTask2.summary = "testing"
    sampleTask2.state = NodeInstanceState.FailRetry
    sampleTask2.serialize(out)
    assertXMLEqual("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<umh:task handle=\"3\" instancehandle=\"2\" owner=\"pdvrieze\" remotehandle=\"1\" summary=\"testing\" state=\"FailRetry\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"/>\n",
                   out.toString())
  }

  @Test
  @Throws(XmlException::class)
  fun testDeserialize() {
    val reader = StringReader("<task state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\" />")
    val result = XmlStreaming.deSerialize(reader, XmlTask::class.java)
    assertEquals(result.state, Complete)
    assertEquals(result.handleValue, -1L)
    assertEquals(result.getHandle(), Handles.getInvalid<Any>())
    assertEquals(result.instanceHandle, -1L)
    assertEquals(result.items.size, 0)
    assertEquals(result.owner, null)
    assertEquals(result.summary, null)
  }

  @Test
  @Throws(XmlException::class)
  fun testDeserialize2() {
    val reader = StringReader("<task handle='1' instancehandle='3' summary='bar' state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\"><item name='one' type='label' value='two'><option>three</option><option>four</option></item></task>")
    val result = XmlStreaming.deSerialize(reader, XmlTask::class.java)
    assertEquals(result.state, Complete)
    assertEquals(result.handleValue, 1L)
    assertEquals(result.getHandle(), Handles.handle<Any>(1L))
    assertEquals(result.instanceHandle, 3L)
    assertEquals(result.items.size, 1)
    assertEquals(result.owner, null)
    assertEquals(result.summary, "bar")
    assertNotNull(result.items)
    val item = result.getItem("one")
    assertNotNull(item)
    assertEquals(item!!.type, "label")
    assertEquals(item.value, "two")
    assertEquals(item.name, "one")
    assertEquals(item.options.size, 2)
    assertEquals(item.options[0], "three")
    assertEquals(item.options[1], "four")
  }

  @Test
  @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
  fun testDomDeserialize() {
    val TEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><task xmlns=\"http://adaptivity.nl/userMessageHandler\" state=\"Complete\"/>"
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = true
    val db = dbf.newDocumentBuilder()
    val doc = db.parse(ReaderInputStream(Charset.forName("UTF8"), StringReader(TEXT)))
    val root = doc.documentElement
    assertEquals(root.tagName, "task")

    val result = XmlStreaming.deSerialize(DOMSource(root), XmlTask::class.java)
    assertEquals(result.state, Complete)
    assertEquals(result.handleValue, -1L)
    assertEquals(result.getHandle(), Handles.getInvalid<Any>())
    assertEquals(result.instanceHandle, -1L)
    assertEquals(result.items.size, 0)
    assertEquals(result.owner, null)
    assertEquals(result.summary, null)
  }
}
