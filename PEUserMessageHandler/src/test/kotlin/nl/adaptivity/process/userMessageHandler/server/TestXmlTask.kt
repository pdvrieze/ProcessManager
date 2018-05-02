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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultComparisonFormatter
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.dom.DOMSource


class TestXmlTask {

  private lateinit var mSampleTask: XmlTask

  @BeforeEach
  fun before() {
    mSampleTask = XmlTask()
    mSampleTask.state = NodeInstanceState.Failed
    mSampleTask.owner = SimplePrincipal("pdvrieze")
    mSampleTask.remoteHandle = Handles.handle<Any>(-1L)
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
    sampleTask2.remoteHandle = Handles.handle<Any>(1L)
    sampleTask2.instanceHandle = Handles.handle<Any>(2L)
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
      Assertions.assertEquals(Complete, result.state)
      Assertions.assertEquals(-1L, result.handleValue)
      Assertions.assertEquals(Handles.getInvalid<Any>(), result.getHandle())
      Assertions.assertEquals(Handles.getInvalid<Any>(), result.instanceHandle)
      Assertions.assertEquals(0, result.items.size)
      Assertions.assertEquals(null, result.owner)
      Assertions.assertEquals(null, result.summary)
  }

  @Test
  @Throws(XmlException::class)
  fun testDeserialize2() {
    val reader = StringReader("<task handle='1' instancehandle='3' summary='bar' state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\"><item name='one' type='label' value='two'><option>three</option><option>four</option></item></task>")
    val result = XmlStreaming.deSerialize(reader, XmlTask::class.java)
      Assertions.assertEquals(Complete, result.state)
      Assertions.assertEquals(1L, result.handleValue)
      Assertions.assertEquals(Handles.handle<Any>(1L), result.getHandle())
      Assertions.assertEquals(Handles.handle<Any>(3L), result.instanceHandle)
      Assertions.assertEquals(1, result.items.size)
      Assertions.assertEquals(null, result.owner)
      Assertions.assertEquals("bar", result.summary)
    assertNotNull(result.items)
    val item = result.getItem("one")
    assertNotNull(item)
      Assertions.assertEquals("label", item!!.type)
      Assertions.assertEquals("two", item.value)
      Assertions.assertEquals("one", item.name)
      Assertions.assertEquals(2, item.options.size)
      Assertions.assertEquals("three", item.options[0])
      Assertions.assertEquals("four", item.options[1])
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
      Assertions.assertEquals("task", root.tagName)

    val result = XmlStreaming.deSerialize(DOMSource(root), XmlTask::class.java)
      Assertions.assertEquals(Complete, result.state)
      Assertions.assertEquals(-1L, result.handleValue)
      Assertions.assertEquals(Handles.getInvalid<Any>(), result.getHandle())
      Assertions.assertEquals(Handles.getInvalid<Any>(), result.instanceHandle)
      Assertions.assertEquals(0, result.items.size)
      Assertions.assertEquals(null, result.owner)
      Assertions.assertEquals(null, result.summary)
  }
}

fun assertXMLEqual(expected: String?, actual: String?) {
    val diff = DiffBuilder.compare(expected)
        .withTest(actual)
        .ignoreWhitespace()
        .ignoreComments()
        .checkForSimilar()
        .build()

    if (diff.hasDifferences()) {
        Assertions.assertEquals(expected, actual, diff.toString(DefaultComparisonFormatter()))
    }
}
