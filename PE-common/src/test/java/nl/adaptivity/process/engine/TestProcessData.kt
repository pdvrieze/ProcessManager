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

package nl.adaptivity.process.engine

import net.devrieze.util.toString
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.getXmlReader
import nl.adaptivity.xml.*
import nl.adaptivity.xml.SimpleNamespaceContext
import org.custommonkey.xmlunit.*
import org.custommonkey.xmlunit.XMLAssert.assertXMLEqual
import org.mockito.Matchers.any
import org.mockito.Matchers.anyBoolean
import org.mockito.Mockito.*
import org.testng.Assert
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.bind.ValidationEvent
import javax.xml.bind.ValidationEventHandler
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.stream.XMLInputFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory


/**
 * Created by pdvrieze on 24/08/15.
 */
class TestProcessData {

  private class TestValidationEventHandler : ValidationEventHandler {

    override fun handleEvent(event: ValidationEvent): Boolean {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error parsing jaxb", event)
      return false
    }
  }

  @XmlRootElement(name = "resultHolder")
  @XmlAccessorType(XmlAccessType.PROPERTY)
  private class ResultTypeHolder(private  @XmlElement(name = "result", required = true) val xmlResultType: XmlResultType? = null)

  private class WhiteSpaceIgnoringListener : DifferenceListener {

    override fun differenceFound(difference: Difference): Int {
      if (DifferenceConstants.TEXT_VALUE_ID == difference.id) {
        return 0
      }
      return difference.id
    }

    override fun skippedComparison(control: Node, test: Node) {

    }
  }

  private class NamespaceDeclIgnoringListener : DifferenceListener {

    override fun differenceFound(difference: Difference): Int {
      when (difference.id) {
        DifferenceConstants.ATTR_NAME_NOT_FOUND_ID -> {
          if (difference.controlNodeDetail.node != null && XMLConstants.XMLNS_ATTRIBUTE_NS_URI == difference.controlNodeDetail.node.namespaceURI || difference.testNodeDetail.node != null && XMLConstants.XMLNS_ATTRIBUTE_NS_URI == difference.testNodeDetail.node.namespaceURI) {

            return DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR
          }
        }
      }
      return DifferenceListener.RETURN_ACCEPT_DIFFERENCE
    }

    override fun skippedComparison(control: Node, test: Node) {

    }
  }

  @Test
  @Throws(XmlException::class)
  fun testSerializeTextNode() {
    val caw = CharArrayWriter()
    val xsw = XmlStreaming.newWriter(caw)

    val data = ProcessData("foo", CompactFragment("Hello"))
    data.serialize(xsw)
    xsw.flush()
    Assert.assertEquals(caw.toString(),
                        "<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\">Hello</pe:value>")
  }

  @Test
  @Throws(XmlException::class)
  fun testSerializeSingleNode() {
    val caw = CharArrayWriter()
    val xsw = XmlStreaming.newWriter(caw)

    val data = ProcessData("foo", CompactFragment("<bar/>"))
    data.serialize(xsw)
    xsw.flush()
    assertEquals(caw.toString(),
                 "<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\"><bar/></pe:value>")
  }

  @Test
  @Throws(Exception::class)
  fun testSerializeMessage() {
    Logger.getAnonymousLogger().level = Level.ALL
    val pm = getProcessModel("testModel2.xml")
    val ac2 = pm.getNode("ac2") as XmlActivity?
    val serialized = XmlMessage.get(ac2!!.message).toString()
    val msg2 = XmlStreaming.deSerialize(StringReader(serialized), XmlMessage::class.java)
    assertEquals(msg2.messageBody.contentString, ac2.message!!.messageBody.contentString)
    assertEquals(msg2, ac2.message)
  }

  @Test
  @Throws(Exception::class)
  fun testDeserializeProcessModel() {
    Logger.getAnonymousLogger().level = Level.ALL
    val pm = getProcessModel("testModel2.xml")
    var ac1: XmlActivity? = null
    var ac2: XmlActivity? = null
    var start: XmlStartNode? = null
    var end: XmlEndNode? = null
    for (node in pm.getModelNodes()) {
      if (node.id != null) {
        when (node.id) {
          "start" -> start = node as XmlStartNode
          "ac1"   -> ac1 = node as XmlActivity
          "ac2"   -> ac2 = node as XmlActivity
          "end"   -> end = node as XmlEndNode
        }
      }
    }
    assertNotNull(start)
    assertNotNull(ac1)
    assertNotNull(ac2)
    assertNotNull(end)

    assertEquals(start!!.successors.iterator().next().id, "ac1")

    assertEquals(ac1!!.predecessor!!.id, "start")
    assertEquals(ac1.successors.iterator().next().id, "ac2")

    assertEquals(ac2!!.predecessor!!.id, "ac1")
    assertEquals(ac2.successors.iterator().next().id, "end")

    assertEquals(end!!.predecessor!!.id, "ac2")

    assertEquals(ac1.results.size, 2)
    val result1 = ac1.results[0]
    assertEquals(result1.getName(), "name")
    assertEquals(result1.getPath(), "/umh:result/umh:value[@name='user']/text()")
    val snc1 = SimpleNamespaceContext.from(result1.originalNSContext)
    assertEquals(snc1!!.size, 1)
    assertEquals(snc1.getPrefix(0), "umh")

    val result2 = ac1.results[1]
    val snc2 = SimpleNamespaceContext.from(result2.originalNSContext)
    assertEquals(snc1.size, 1)
    assertEquals(snc1.getPrefix(0), "umh")

    val testData = documentBuilder.parse(InputSource(StringReader(
        "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")))


    val result1_apply = result1.apply(testData).content
    assertEquals(result1_apply.contentString, "Paul")

    val result2_apply = result2.apply(testData).content
    XMLAssert.assertXMLEqual("<user><fullname>Paul</fullname></user>", result2_apply.contentString)

  }

  @Test
  @Throws(IOException::class, SAXException::class, XPathExpressionException::class)
  fun testXmlResultXpathParam() {
    val nsContext = SimpleNamespaceContext(arrayOf("umh"), arrayOf("http://adaptivity.nl/userMessageHandler"))
    val expression = "/umh:result/umh:value[@name='user']/text()"
    val result = XmlResultType("foo", expression, null as CharArray?, nsContext)
    assertEquals((SimpleNamespaceContext.from(result.originalNSContext) as SimpleNamespaceContext).size, 1)

    val testData = documentBuilder.parse(InputSource(StringReader(
        "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")))
    val xPath = XPathFactory.newInstance().newXPath()
    xPath.namespaceContext = SimpleNamespaceContext.from(result.originalNSContext)
    val pathExpression = xPath.compile(expression)
    val apply2 = pathExpression.evaluate(testData, XPathConstants.NODESET) as NodeList
    assertNotNull(apply2)
    assertTrue(apply2.item(0) is Text)
    assertEquals(apply2.item(0).textContent, "Paul")

    val apply3 = pathExpression.evaluate(testData, XPathConstants.NODE) as Node
    assertNotNull(apply3)
    assertTrue(apply3 is Text)
    assertEquals(apply3.textContent, "Paul")

    val apply1 = result.apply(testData)
    assertEquals(apply1.content.contentString, "Paul")
  }

  @Test
  @Throws(XmlException::class)
  fun testReadFragment() {
    val testDataInner = "<b xmlns:umh='urn:foo'><umh:a xpath='/umh:value'/></b>"
    val reader = XmlStreaming.newReader(StringReader(testDataInner))
    reader.next()
    reader.require(EventType.START_ELEMENT, "", "b")
    reader.next()
    reader.require(EventType.START_ELEMENT, "urn:foo", "a")
    val fragment = reader.siblingsToFragment()
    reader.require(EventType.END_ELEMENT, "", "b")
    reader.next()
    reader.require(EventType.END_DOCUMENT, null, null)

    assertEquals(fragment.namespaces.size, 1)
    assertEquals(fragment.namespaces.getNamespaceURI(0), "urn:foo")
    assertEquals(fragment.namespaces.getPrefix(0), "umh")
    assertEquals(fragment.contentString, "<umh:a xpath=\"/umh:value\"/>")
  }

  @Test
  @Throws(XmlException::class, IOException::class, SAXException::class)
  fun testTransform() {
    val endpoint = ProcessData("endpoint", createEndpoint())
    val transformer = PETransformer.create(SimpleNamespaceContext.from(emptyList<nl.adaptivity.xml.Namespace>()),
                                           endpoint)
    val INPUT = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
                "  <jbi:element value=\"endpoint\"/>\n" +
                "</umh:postTask>"
    val cf = CompactFragment(SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MODIFY_NS_STR)),
                             INPUT.toCharArray())
    val caw = CharArrayWriter()
    val out = XmlStreaming.newWriter(caw, true)
    transformer.transform(cf.getXmlReader(), out)
    out.close()
    run {
      val control = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost\" endpointName=\"internal\" serviceLocalName=\"foobar\" serviceNS=\"http://foo.bar\"/></umh:postTask>"
      val test = caw.toString()
      try {
        assertXMLEqual(control, test)
      } catch (e: SAXParseException) {
        assertEquals(test, control)
      } catch (e: AssertionError) {
        assertEquals(test, control)
      }
    }
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripProcessModel1_ac1_result1() {
    val xpm = getProcessModel("testModel2.xml")
    run {
      val caw = CharArrayWriter()
      val xsw = XmlStreaming.newWriter(caw)

      val ac1 = run {
        val modelNodes = xpm.getModelNodes()
        val it = modelNodes.iterator()
        it.next()
        it.next()
      }

      assertEquals(ac1.id, "ac1")
      val ac1Results = ArrayList(ac1.results)

      val result = ac1Results[0] as XmlResultType
      result.serialize(xsw)
      xsw.close()

      val actual = caw.toString()
      val expected = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>"

      XMLUnit.setIgnoreWhitespace(true)
      XMLUnit.setIgnoreAttributeOrder(true)
      val diff = Diff(expected, actual)
      try {
        assertXMLEqual(DetailedDiff(diff), true)
      } catch (e: AssertionError) {
        try {
          assertEquals(actual, expected)
        } catch (f: AssertionError) {
          f.addSuppressed(e)
          throw f
        }

      }
    }
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripProcessModel1_ac1_result2() {
    val processModel = getProcessModel("testModel2.xml")
    run {
      val caw = CharArrayWriter()
      val xsw = XmlStreaming.newWriter(caw)

      val ac1 = processModel.getNode("ac1")
      assertEquals(ac1!!.id, "ac1")
      val ac1Results = ArrayList(ac1.results)
      val result = ac1Results[1] as XmlResultType
      result.serialize(xsw)
      xsw.close()

      val actual = caw.toString()
      val expected = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\"><user xmlns=\"\">" +
                     "<fullname>" +
                     "<jbi:value  xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
                     "</fullname>" +
                     "</user>\n" +
                     "</result>"

      XMLUnit.setIgnoreWhitespace(true)
      XMLUnit.setIgnoreAttributeOrder(true)
      val diff = Diff(expected, actual)

      assertXMLEqual(DetailedDiff(diff), true)
    }
  }

  @Test
  @Throws(Exception::class)
  fun testXmlStreamingRoundTripProcessModel1() {

    testRoundTrip(getDocument("testModel2.xml"), XmlProcessModel::class.java)

  }

  @Test
  @Throws(XmlException::class, FileNotFoundException::class)
  fun testParseProcessModel1() {
    val inputStream = getDocument("processmodel1.xml")
    val parser = XmlStreaming.newReader(inputStream, "UTF-8")
    val model = XmlProcessModel.deserialize(parser)
    checkModel1(model)
  }


  private fun checkModel1(model: XmlProcessModel) {
    assertNotNull(model)

    assertEquals(model.getModelNodes().size, 9,
                 "There should be 9 effective elements in the process model (including an introduced split)")
    val start = model.getNode("start") as XmlStartNode?
    val ac1 = model.getNode("ac1") as XmlActivity?
    val ac2 = model.getNode("ac2") as XmlActivity?
    val ac3 = model.getNode("ac3") as XmlActivity?
    val ac4 = model.getNode("ac4") as XmlActivity?
    val ac5 = model.getNode("ac5") as XmlActivity?
    val split = model.getNode("split1") as XmlSplit?
    val j1 = model.getNode("j1") as XmlJoin?
    val end = model.getNode("end") as XmlEndNode?
    val actualNodes = model.getModelNodes()
    val expectedNodes = Arrays.asList<XmlProcessNode>(start, ac1, ac2, split, ac3, ac5, j1, ac4, end)
    assertEquals(expectedNodes.size, actualNodes.size)
    assertTrue(actualNodes.containsAll(expectedNodes))

    assertEquals(start!!.predecessors.toTypedArray(), toArray())
    assertEquals(start.successors.toTypedArray(), toArray(Identifier(ac1!!.id!!)))

    assertEquals(ac1.predecessors.toTypedArray(), toArray(Identifier(start.id!!)))
    assertEquals(ac1.successors.toTypedArray(), toArray(Identifier(split!!.id!!)))

    assertEquals(split.predecessors.toTypedArray(), toArray(Identifier(ac1.id!!)))
    assertEquals(split.successors.toTypedArray(), toArray(Identifier(ac2!!.id!!), Identifier(ac3!!.id!!)))

    assertEquals(ac2.predecessors.toTypedArray(), toArray(Identifier(split.id!!)))
    assertEquals(ac2.successors.toTypedArray(), toArray(Identifier(j1!!.id!!)))

    assertEquals(ac3.predecessors.toTypedArray(), toArray(Identifier(split.id!!)))
    assertEquals(ac3.successors.toTypedArray(), toArray(Identifier(ac5!!.id!!)))

    assertEquals(ac4!!.predecessors.toTypedArray(), toArray(Identifier(j1.id!!)))
    assertEquals(ac4.successors.toTypedArray(), toArray(Identifier(end!!.id!!)))

    assertEquals(ac5.predecessors.toTypedArray(), toArray(Identifier(ac3.id!!)))
    assertEquals(ac5.successors.toTypedArray(), toArray(Identifier(j1.id!!)))

    assertEquals(end.predecessors.toTypedArray(), toArray(Identifier(ac4.id!!)))
    assertEquals(end.successors.toTypedArray(), toArray())
  }


  @Test
  @Throws(IOException::class, SAXException::class, XmlException::class)
  fun testSerializeResult1() {
    val pm = getProcessModel("testModel2.xml")

    val caw = CharArrayWriter()
    val xsw = XmlStreaming.newWriter(caw)

    val result: XmlResultType = run {
      val modelNodes = pm.getModelNodes()
      val it = modelNodes.iterator()
      it.next()
      it.next().results.iterator().next() as XmlResultType
    }

    result.serialize(xsw)
    xsw.close()
    val control = "<result xpath=\"/umh:result/umh:value[@name='user']/text()\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xmlns=\"http://adaptivity.nl/ProcessEngine/\"/>"
    try {
      XMLAssert.assertXMLEqual(control, caw.toString())
    } catch (e: AssertionError) {
      assertEquals(caw.toString(), control)
    }

  }

  @Test
  @Throws(IOException::class, SAXException::class, XmlException::class)
  fun testSerializeResult2() {
    val result: XmlResultType = run {
      val xpm = getProcessModel("testModel2.xml")
      val iterator = xpm.getNode("ac1")!!.results.iterator()
      assertNotNull(iterator.next())
      iterator.next() as XmlResultType
    }

    val control = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
                  "  <user xmlns=\"\"\n" +
                  "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
                  "    <fullname>\n" +
                  "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
                  "    </fullname>\n" +
                  "  </user>\n" +
                  "</result>"
    val found = result.toString()
    try {
      XMLUnit.setIgnoreWhitespace(true)
      XMLAssert.assertXMLEqual(control, found)
    } catch (e: AssertionError) {
      assertEquals(found, control)
    }

  }

  @Test
  @Throws(Exception::class)
  fun testRead() {
    val testData = "Hello<a>who<b>are</b>you</a>"
    val reader = XmlStreaming.newReader(StringReader("<wrap>$testData</wrap>"))
    assertEquals(reader.next(), START_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), TEXT)
    assertEquals(reader.text, "Hello")
    assertEquals(reader.next(), START_ELEMENT)
    assertEquals(reader.localName, "a")
    assertEquals(reader.next(), TEXT)
    assertEquals(reader.text, "who")
    assertEquals(reader.next(), START_ELEMENT)
    assertEquals(reader.localName, "b")
    assertEquals(reader.next(), TEXT)
    assertEquals(reader.text, "are")
    assertEquals(reader.next(), END_ELEMENT)
    assertEquals(reader.localName, "b")
    assertEquals(reader.next(), TEXT)
    assertEquals(reader.text, "you")
    assertEquals(reader.next(), END_ELEMENT)
    assertEquals(reader.localName, "a")
    assertEquals(reader.next(), END_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), END_DOCUMENT)
  }

  @Test
  @Throws(Exception::class)
  fun testSiblingsToFragmentMock() {
    val testData = "Hello<a>who<b>are</b>you</a>"
    val reader = XmlStreaming.newReader(StringReader("<wrap>$testData</wrap>"))
    assertEquals(reader.next(), START_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), TEXT)

    run {
      val factory = mock(XmlStreamingFactory::class.java)
      val mockedWriter = mock(XmlWriter::class.java)
      val nsContext = mock(NamespaceContext::class.java)
      `when`(factory.newWriter(any(Writer::class.java), anyBoolean())).thenReturn(mockedWriter)
      `when`(mockedWriter.namespaceContext).thenReturn(nsContext)
      `when`(nsContext.getNamespaceURI("")).thenReturn("")
      `when`(nsContext.getPrefix("")).thenReturn("")
      XmlStreaming.setFactory(factory)
      reader.siblingsToFragment()

      val inOrder = inOrder(mockedWriter)
      // The Hello text will not be written with a writer, but directly escaped
      // as otherwise the serializer will complain about multiple roots.
      // inOrder.verify(mockedWriter).text("Hello");
      inOrder.verify(mockedWriter).startTag("", "a", "")
      inOrder.verify(mockedWriter).text("who")
      inOrder.verify(mockedWriter).startTag("", "b", "")
      inOrder.verify(mockedWriter).text("are")
      inOrder.verify(mockedWriter).endTag("", "b", "")
      inOrder.verify(mockedWriter).text("you")
      inOrder.verify(mockedWriter).endTag("", "a", "")
      inOrder.verify(mockedWriter).close()
      inOrder.verifyNoMoreInteractions()
    }
    assertEquals(reader.eventType, END_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), END_DOCUMENT)
  }

  @Test
  @Throws(Exception::class)
  fun testSiblingsToFragment() {
    val testData = "Hello<a>who<b>are<c>you</c>.<d>I</d></b>don't</a>know"
    val reader = XmlStreaming.newReader(StringReader("<wrap>$testData</wrap>"))

    assertEquals(reader.next(), START_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), TEXT)

    XmlStreaming.setFactory(null) // reset to the default one
    val fragment = reader.siblingsToFragment()

    assertEquals(fragment.namespaces.size, 0)
    assertEquals(fragment.contentString, testData)
    assertEquals(reader.eventType, END_ELEMENT)
    assertEquals(reader.localName, "wrap")
    assertEquals(reader.next(), END_DOCUMENT)
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripResult1() {
    val xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>"
    val result = testRoundTrip(xml, XmlResultType::class.java)
    assertTrue(result.contains("xmlns:umh=\"http://adaptivity.nl/userMessageHandler\""))
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripDefine() {
    val xml = "<define xmlns=\"http://adaptivity.nl/ProcessEngine/\" refnode=\"ac1\" refname=\"name\" name=\"mylabel\">Hi <jbi:value xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\".\"/>. Welcome!</define>"
    val result = testRoundTrip(xml, XmlDefineType::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripResult2() {
    val xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
              "  <user xmlns=\"\"\n" +
              "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
              "    <fullname>\n" +
              "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
              "    </fullname>\n" +
              "  </user>\n" +
              "</result>"
    testRoundTrip(xml, XmlResultType::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testDeserializeResult2() {
    val xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
              "  <user xmlns=\"\"\n" +
              "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
              "    <fullname>\n" +
              "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
              "    </fullname>\n" +
              "  </user>\n" +
              "</result>"

    val expectedContent = "\n  <user xmlns=\"\"" +
                          " xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
                          "    <fullname>\n" +
                          "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
                          "    </fullname>\n" +
                          "  </user>\n"

    val rt = XmlResultType.deserialize(XmlStreaming.newReader(StringReader(xml)))
    assertEquals(rt.content?.toString(), expectedContent)
    val namespaces = rt.originalNSContext
    val it = namespaces.iterator()
    var ns = it.next()
    assertEquals(ns.prefix, "")
    assertEquals(ns.namespaceURI, "http://adaptivity.nl/ProcessEngine/")
    ns = it.next()
    assertEquals(ns.prefix, "umh")
    assertEquals(ns.namespaceURI, "http://adaptivity.nl/userMessageHandler")

    assertEquals(it.hasNext(), false)
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripResult3() {
    val xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user2\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">" +
              "<jbi:value xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>" +
              "</result>"
    val result = testRoundTrip(xml, XmlResultType::class.java)
    assertTrue(result.contains("xmlns:umh=\"http://adaptivity.nl/userMessageHandler\""))
  }

  @Test
  @Throws(IOException::class, InstantiationException::class, SAXException::class, IllegalAccessException::class,
          XmlException::class)
  fun testRoundTripMessage() {
    val xml = "    <pe:message xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" type=\"application/soap+xml\" serviceNS=\"http://adaptivity.nl/userMessageHandler\" serviceName=\"userMessageHandler\" endpoint=\"internal\" operation=\"postTask\" url=\"/PEUserMessageHandler/internal\">\n" +
              "      <Envelope xmlns=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">\n" +
              "        <Body>\n" +
              "          <postTask xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
              "            <repliesParam>\n" +
              "              <jbi:element value=\"endpoint\"/>\n" +
              "            </repliesParam>\n" +
              "            <taskParam>\n" +
              "              <task summary=\"Task Foo\">\n" +
              "                <jbi:attribute name=\"remotehandle\" value=\"handle\"/>\n" +
              "                <jbi:attribute name=\"instancehandle\" value=\"instancehandle\"/>\n" +
              "                <jbi:attribute name=\"owner\" value=\"owner\"/>\n" +
              "                <item name=\"lbl1\" type=\"label\" value=\"Please enter some info for task foo\"/>\n" +
              "                <item label=\"Your name\" name=\"user\" type=\"text\"/>\n" +
              "              </task>\n" +
              "            </taskParam>\n" +
              "          </postTask>\n" +
              "        </Body>\n" +
              "      </Envelope>\n" +
              "    </pe:message>\n"
    testRoundTrip(xml, XmlMessage::class.java, false)
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTripActivity() {
    val xml = "  <activity xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"ac1\" predecessor=\"start\" id=\"ac1\">\n" +
              "    <result name=\"name\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
              "    <result name=\"user\">\n" +
              "      <user xmlns=\"\"\n" +
              "            xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
              "        <fullname>\n" +
              "          <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
              "        </fullname>\n" +
              "      </user>\n" +
              "    </result>\n" +
              "    <message type=\"application/soap+xml\" serviceNS=\"http://adaptivity.nl/userMessageHandler\" serviceName=\"userMessageHandler\" endpoint=\"internal\" operation=\"postTask\" url=\"/PEUserMessageHandler/internal\">\n" +
              "      <Envelope xmlns=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">\n" +
              "        <Body>\n" +
              "          <umh:postTask xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
              "            <repliesParam>\n" +
              "              <jbi:element value=\"endpoint\"/>\n" +
              "            </repliesParam>\n" +
              "            <taskParam>\n" +
              "              <task summary=\"Task Foo\">\n" +
              "                <jbi:attribute name=\"remotehandle\" value=\"handle\"/>\n" +
              "                <jbi:attribute name=\"instancehandle\" value=\"instancehandle\"/>\n" +
              "                <jbi:attribute name=\"owner\" value=\"owner\"/>\n" +
              "                <item name=\"lbl1\" type=\"label\" value=\"Please enter some info for task foo\"/>\n" +
              "                <item label=\"Your name\" name=\"user\" type=\"text\"/>\n" +
              "              </task>\n" +
              "            </taskParam>\n" +
              "          </umh:postTask>\n" +
              "        </Body>\n" +
              "      </Envelope>\n" +
              "    </message>\n" +
              "  </activity>\n"
    testRoundTrip(xml, XmlActivity::class.java, true)
  }

  companion object {

    private var _documentBuilder: DocumentBuilder? = null

    private val documentBuilder: DocumentBuilder
      get() {
        if (_documentBuilder == null) {
          val dbf = DocumentBuilderFactory.newInstance()
          try {
            dbf.isNamespaceAware = true
            dbf.isIgnoringElementContentWhitespace = false
            dbf.isCoalescing = false
            _documentBuilder = dbf.newDocumentBuilder()
          } catch (e: ParserConfigurationException) {
            throw RuntimeException(e)
          }

        }
        return _documentBuilder!!
      }

    @Throws(FileNotFoundException::class)
    private fun getDocument(name: String): InputStream {
      var stream: InputStream? = TestProcessData::class.java.getResourceAsStream(
          "/nl/adaptivity/process/engine/test/" + name)
      if (stream == null) {
        stream = FileInputStream("nl/adaptivity/process/engine/test/" + name)
      }
      return stream
    }

    @BeforeMethod
    private fun init() {
      XmlStreaming.setFactory(null) // make sure to have the default factory
    }

    @Throws(IOException::class, XmlException::class)
    private fun getProcessModel(name: String): XmlProcessModel {
      getDocument(name).use { inputStream ->
        val input = XmlStreaming.newReader(inputStream, "UTF-8")
        try {
          val factory = ProcessModel::class.java.getAnnotation(XmlDeserializer::class.java)
              .value.java
              .newInstance()
          return factory.deserialize(input) as XmlProcessModel
        } catch (e: InstantiationException) {
          throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(e)
        }
      }
    }

    private fun createEndpoint(): CompactFragment {
      val namespaces = SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS_STR))
      val content = StringBuilder()
      content.append("<jbi:endpointDescriptor")
      content.append(" endpointLocation=\"http://localhost\"")
      content.append(" endpointName=\"internal\"")
      content.append(" serviceLocalName=\"foobar\"")
      content.append(" serviceNS=\"http://foo.bar\"")
      content.append(" />")
      return CompactFragment(namespaces, content.toString().toCharArray())

    }

    @Throws(IOException::class, IllegalAccessException::class, InstantiationException::class, XmlException::class)
    fun <T : XmlSerializable> testRoundTrip(reader: InputStream, target: Class<T>): String {
      val expected: String
      val streamReader: XmlReader
      val xif = XMLInputFactory.newFactory()
      if (reader.markSupported()) {
        reader.mark(Integer.MAX_VALUE)
        expected = toString(reader, Charset.defaultCharset())
        reader.reset()
        streamReader = XmlStreaming.newReader(reader, Charset.defaultCharset().toString())
      } else {
        expected = toString(reader, Charset.defaultCharset())
        streamReader = XmlStreaming.newReader(StringReader(expected))
      }

      return testRoundTrip(expected, streamReader, target, false)
    }

    @Throws(IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class)
    fun <T : XmlSerializable> testRoundTrip(xml: String, target: Class<T>): String {
      return testRoundTrip(xml, target, false)
    }

    @Throws(IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class)
    fun <T : XmlSerializable> testRoundTrip(xml: String, target: Class<T>, ignoreNs: Boolean): String {
      return testRoundTrip(xml, XmlStreaming.newReader(StringReader(xml)), target, ignoreNs)
    }

    @Throws(InstantiationException::class, IllegalAccessException::class, XmlException::class)
    private fun <T : XmlSerializable> testRoundTrip(expected: String,
                                                    actual: XmlReader,
                                                    target: Class<T>,
                                                    ignoreNs: Boolean): String {
      assertNotNull(actual)
      val factory = target.getAnnotation(XmlDeserializer::class.java).value.java.newInstance() as XmlDeserializerFactory<*>
      val obj = factory.deserialize(actual) as XmlSerializable
      val caw = CharArrayWriter()
      val xsw = XmlStreaming.newWriter(caw)
      obj.serialize(xsw)
      xsw.close()
      try {
        XMLUnit.setIgnoreWhitespace(true)
        val diff = Diff(expected, caw.toString())
        val detailedDiff = DetailedDiff(diff)
        if (ignoreNs) {
          detailedDiff.overrideDifferenceListener(NamespaceDeclIgnoringListener())
        }
        assertXMLEqual(detailedDiff, true)
      } catch (e: AssertionError) {
        e.printStackTrace()
        assertEquals(caw.toString(), expected)
      } catch (e: Exception) {
        e.printStackTrace()
        assertEquals(caw.toString(), expected)
      }

      return caw.toString()
    }

    private fun toArray(vararg value: Any): Array<out Any> {
      return value
    }
  }

}
