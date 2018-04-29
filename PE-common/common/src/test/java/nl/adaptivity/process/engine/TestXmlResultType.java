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

package nl.adaptivity.process.engine;

import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.DomUtil;
import nl.adaptivity.xml.*;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static nl.adaptivity.process.util.Constants.USER_MESSAGE_HANDLER_NS;
import static nl.adaptivity.xml.SimpleNamespaceContext.Companion;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


/**
 * Created by pdvrieze on 24/08/15.
 */
@SuppressWarnings("ConstantConditions")
public class TestXmlResultType {

  @Test
  public void testXPath() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    final XPathExpression expr = XPathFactory.newInstance().newXPath().compile("/result/value[@name='user']/text()");
    final Document testData = getDB().parse(new InputSource(new StringReader("<result><value name='user'>Paul</value></result>")));
    assertEquals(expr.evaluate(testData), "Paul");
  }

  @Test
  public void testXPathNS() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    final XPath xPath = XPathFactory.newInstance().newXPath();
    final Map<String, String> prefixMap = new TreeMap<>();
    prefixMap.put("ns1", Constants.USER_MESSAGE_HANDLER_NS);
    xPath.setNamespaceContext(new SimpleNamespaceContext(prefixMap));
    final XPathExpression expr = xPath.compile("/ns1:result/ns1:value[@name='user']/text()");
    final Document testData = getDB().parse(new InputSource(new StringReader("<umh:result xmlns:umh='"+Constants.USER_MESSAGE_HANDLER_NS+"'><umh:value name='user'>Paul</umh:value></umh:result>")));
    assertEquals(expr.evaluate(testData), "Paul");
  }

  @Test
  public void testXPathNS2() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    final XPath xPath = XPathFactory.newInstance().newXPath();
    final Map<String, String> prefixMap = new TreeMap<>();
    prefixMap.put("ns1", Constants.USER_MESSAGE_HANDLER_NS);
    xPath.setNamespaceContext(new SimpleNamespaceContext(prefixMap));
    final XPathExpression expr = xPath.compile("/ns1:result/ns1:value[@name='user']/text()");
    final Document testData = getDB().parse(new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?><result xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                                                                             "  <value name=\"user\">Some test value</value>\n" +
                                                                             "</result>")));
    assertEquals(expr.evaluate(testData), "Some test value");
  }

  private DocumentBuilder getDB() throws ParserConfigurationException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    return dbf.newDocumentBuilder();
  }

  @Test
  public void testApplySimple() throws ParserConfigurationException, IOException, SAXException {
    final Document testData = getDB().parse(new InputSource(new StringReader("<result><value name='user'>Paul</value></result>")));
    final XmlResultType xrt = new XmlResultType("user", "/result/value[@name='user']/text()", (char[]) null, null);

    final ProcessData actual = xrt.applyData(testData);

    final ProcessData expected = new ProcessData("user", XmlStreamingKt.CompactFragment(Collections.<Namespace>emptyList(), "Paul".toCharArray()));
    assertEquals(actual.getName(), expected.getName());
    assertEquals(actual.getContent(), expected.getContent());
//    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
  }

  @Test
  public void testApplySimpleNS() throws ParserConfigurationException, IOException, SAXException {
    final Document testData = getDB().newDocument();
    final Element result = testData.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "umh:result");
    testData.appendChild(result);
    final Element value = (Element) result.appendChild(testData.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "umh:value"));
    value.setAttribute("name", "user");
    value.appendChild(testData.createTextNode("Paul"));

    assertEquals(DomUtil.toString(testData), "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>");


    final XmlResultType xrt = new XmlResultType("user", "/*[local-name()='result']/*[@name='user']/text()", (char[]) null, null);

    final ProcessData expected = new ProcessData("user", XmlStreamingKt.CompactFragment("Paul"));
    final ProcessData actual = xrt.applyData(testData);
    assertEquals(actual.getName(), expected.getName());
    assertEquals(actual.getContent(), expected.getContent());
//    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
  }


  @Test
  public void testXDefineHolder() throws JAXBException, XmlException {
    final String testData = "<define xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" path=\"/umh:bar/text()\" />";
    final XmlReader in = XmlStreaming.newReader(new StringReader(testData));

    final XmlDefineType testHolder = XmlDefineType.Companion.deserialize(in);

    assertNotNull(SimpleNamespaceContext.Companion.from(testHolder.getOriginalNSContext()));
    assertEquals(Companion.from(testHolder.getOriginalNSContext())
                                 .getNamespaceURI("umh"), USER_MESSAGE_HANDLER_NS);

  }


  @Test
  public void testXMLResultHolder() throws XmlException {
    final String testData = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" path=\"/umh:bar/text()\" />";

    final XmlReader in = XmlStreaming.newReader(new StringReader(testData));


    final XmlResultType testHolder = XmlResultType.deserialize(in);

    assertNotNull(SimpleNamespaceContext.Companion.from(testHolder.getOriginalNSContext()));
    assertEquals(Companion.from(testHolder.getOriginalNSContext())
                                 .getNamespaceURI("umh"), USER_MESSAGE_HANDLER_NS);
    assertEquals(testHolder.getName(), "foo");
  }

  @Test
  public void testTaskResult() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document document = db.newDocument();
    Document expected = db.parse(new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?><result xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                                                                  "  <value name=\"user\">Some test value</value>\n" +
                                                                  "</result>")));

    Element outer = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "result");
    outer.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", Constants.USER_MESSAGE_HANDLER_NS);
    Element inner = document.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "value");
    inner.setAttribute("name", "user");
    inner.setTextContent("Some test value");
    outer.appendChild(inner);
    DocumentFragment frag = document.createDocumentFragment();
    frag.appendChild(outer);

    XMLUnit.setIgnoreWhitespace(true);
//    XMLAssert.assertXMLEqual(expected, document);

    final XPath xPath = XPathFactory.newInstance().newXPath();
    final Map<String, String> prefixMap = new TreeMap<>();
    prefixMap.put("ns1", Constants.USER_MESSAGE_HANDLER_NS);
    xPath.setNamespaceContext(new SimpleNamespaceContext(prefixMap));
    final XPathExpression expr = xPath.compile("./ns1:result/ns1:value[@name='user']/text()");
    assertEquals(expr.evaluate(frag), "Some test value");

  }

}
