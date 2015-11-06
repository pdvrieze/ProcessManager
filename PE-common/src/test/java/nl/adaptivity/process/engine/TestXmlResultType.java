package nl.adaptivity.process.engine;

import nl.adaptivity.process.processModel.XPathHolder;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.util.xml.XmlUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class TestXmlResultType {

  @Test
  public void testXPath() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    XPathExpression expr = XPathFactory.newInstance().newXPath().compile("/result/value[@name='user']/text()");
    Document testData = getDB().parse(new InputSource(new StringReader("<result><value name='user'>Paul</value></result>")));
    assertEquals("Paul", expr.evaluate(testData));
  }

  @Test
  public void testXPathNS() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    Map<String, String> prefixMap = new TreeMap<>();
    prefixMap.put("ns1", Constants.USER_MESSAGE_HANDLER_NS);
    xPath.setNamespaceContext(new SimpleNamespaceContext(prefixMap));
    XPathExpression expr = xPath.compile("/ns1:result/ns1:value[@name='user']/text()");
    Document testData = getDB().parse(new InputSource(new StringReader("<umh:result xmlns:umh='"+Constants.USER_MESSAGE_HANDLER_NS+"'><umh:value name='user'>Paul</umh:value></umh:result>")));
    assertEquals("Paul", expr.evaluate(testData));
  }

  private DocumentBuilder getDB() throws ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    return dbf.newDocumentBuilder();
  }

  @Test
  public void testApplySimple() throws ParserConfigurationException, IOException, SAXException {
    Document testData = getDB().parse(new InputSource(new StringReader("<result><value name='user'>Paul</value></result>")));
    XmlResultType xrt = new XmlResultType("user", "/result/value[@name='user']/text()", (char[]) null, null);

    ProcessData actual = xrt.apply(testData);

    Document doc = getDB().newDocument();
    ProcessData expected = new ProcessData("user",doc.createTextNode("Paul"));
    assertEquals(expected.getName(), actual.getName());
    assertEquals(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
//    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
  }

  @Test
  public void testApplySimpleNS() throws ParserConfigurationException, IOException, SAXException {
    Document testData = getDB().newDocument();
    Element result = testData.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "umh:result");
    testData.appendChild(result);
    Element value = (Element) result.appendChild(testData.createElementNS(Constants.USER_MESSAGE_HANDLER_NS, "umh:value"));
    value.setAttribute("name", "user");
    value.appendChild(testData.createTextNode("Paul"));

    assertEquals("<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>",XmlUtil.toString(testData));


    XmlResultType xrt = new XmlResultType("user", "/*[local-name()='result']/*[@name='user']/text()", (char[]) null, null);

    ProcessData expected = new ProcessData("user",getDB().newDocument().createTextNode("Paul"));
    ProcessData actual = xrt.apply(testData);
    assertEquals(expected.getName(), actual.getName());
    assertEquals(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
//    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
  }


  @Test
  public void testXPathHolder() throws JAXBException, XMLStreamException {
    String testData = "<foo xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" path=\"/umh:bar/text()\" />";
    JAXBContext context = JAXBContext.newInstance(XPathHolder.class);
    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLStreamReader in = xif.createXMLStreamReader(new StringReader(testData));

    XPathHolder testHolder = context.createUnmarshaller().unmarshal(in, XPathHolder.class).getValue();

    assertNotNull(SimpleNamespaceContext.from(testHolder.getOriginalNSContext()));
    assertEquals(Constants.USER_MESSAGE_HANDLER_NS, SimpleNamespaceContext.from(testHolder.getOriginalNSContext())
                                                                          .getNamespaceURI("umh"));

  }


  @Test
  public void testXMLResultHolder() throws Exception {
    String testData = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" path=\"/umh:bar/text()\" />";

    XMLInputFactory xif = XMLInputFactory.newFactory();
    XMLStreamReader in = xif.createXMLStreamReader(new StringReader(testData));


    XmlResultType testHolder = XmlResultType.deserialize(in);

    assertNotNull(SimpleNamespaceContext.from(testHolder.getOriginalNSContext()));
    assertEquals(Constants.USER_MESSAGE_HANDLER_NS, SimpleNamespaceContext.from(testHolder.getOriginalNSContext())
                                                                          .getNamespaceURI("umh"));
    assertEquals(Constants.PROCESS_ENGINE_NS, SimpleNamespaceContext.from(testHolder.getOriginalNSContext())
                                                                    .getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
    assertEquals("foo", testHolder.getName());
  }

}
