package nl.adaptivity.process.engine;

import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.XmlProcessModel;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.processModel.engine.ActivityImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.util.xml.XmlUtil;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.*;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class TestProcessData {

  private static class TestValidationEventHandler implements ValidationEventHandler {

    @Override
    public boolean handleEvent(final ValidationEvent event) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error parsing jaxb", event);
      return false;
    }
  }

  private static DocumentBuilder _documentBuilder;
  private Document mDocument;

  private static DocumentBuilder getDocumentBuilder() {
    if (_documentBuilder==null) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(false);
        dbf.setCoalescing(false);
        _documentBuilder = dbf.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    return _documentBuilder;
  }

  private static InputStream getDocument(String name) {
    return TestProcessData.class.getResourceAsStream("/nl/adaptivity/process/engine/test/"+name);
  }

  @Before
  public void setUp() throws ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    mDocument = db.newDocument();
  }

  @Test
  public void testSerializeTextNode() throws XMLStreamException {
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    CharArrayWriter caw = new CharArrayWriter();
    XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);

    ProcessData data = new ProcessData("foo", mDocument.createTextNode("Hello"));
    data.serialize(xsw);
    xsw.flush();
    assertEquals("<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\">Hello</pe:value>", caw.toString());
  }

  @Test
  public void testSerializeSingleNode() throws XMLStreamException {
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    CharArrayWriter caw = new CharArrayWriter();
    XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);

    ProcessData data = new ProcessData("foo", mDocument.createElement("bar"));
    data.serialize(xsw);
    xsw.flush();
    assertEquals("<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\"><bar/></pe:value>", caw.toString());
  }

  @Test
  public void testDeserializeProcessModel() throws IOException, SAXException, JAXBException {
    Logger.getAnonymousLogger().setLevel(Level.ALL);
    XmlProcessModel xpm;
    try (InputStream in = getDocument("testModel2.xml")) {
      Unmarshaller unmarshaller = JAXBContext.newInstance(XmlProcessModel.class).createUnmarshaller();
      unmarshaller.setEventHandler(new TestValidationEventHandler());
      xpm = JAXB.unmarshal(in, XmlProcessModel.class);
    }
    ActivityImpl ac1 = null;
    ActivityImpl ac2 = null;
    for(Object o: xpm.getNodes()) {
      if (o instanceof ProcessNodeImpl) {
        ProcessNodeImpl node = (ProcessNodeImpl) o;
        if (node.getId() != null) {
          switch (node.getId()) {
            case "ac1":
              ac1 = (ActivityImpl) node;
              break;
            case "ac2":
              ac2 = (ActivityImpl) node;
              break;
          }
        }
      }
    }
    assertNotNull(ac1);
    assertEquals(2, ac1.getResults().size());
    XmlResultType result1 = ac1.getResults().get(0);
    assertEquals("name", result1.getName());
    assertEquals("/umh:result/umh:value[@name='user']/text()", result1.getPath());
    SimpleNamespaceContext snc1 = (SimpleNamespaceContext) result1.getNamespaceContext();
    assertEquals(3, snc1.size());
    assertEquals("", snc1.getPrefix(0));
    assertEquals("soapenc", snc1.getPrefix(1));
    assertEquals("umh", snc1.getPrefix(2));

    XmlResultType result2 = ac1.getResults().get(1);
    SimpleNamespaceContext snc2 = (SimpleNamespaceContext) result2.getNamespaceContext();
    assertEquals(3, snc1.size());
    assertEquals("", snc1.getPrefix(0));
    assertEquals("soapenc", snc1.getPrefix(1));
    assertEquals("umh", snc1.getPrefix(2));

    Document testData = getDocumentBuilder().parse(new InputSource(new StringReader("<ns1:result xmlns:ns1=\"http://adaptivity.nl/userMessageHandler\"><ns1:value name=\"user\">Paul</ns1:value></ns1:result>")));


    Node result1_apply = result1.apply(testData).getNodeValue();
    assertTrue(result1_apply instanceof Text);
    assertEquals("Paul", result1_apply.getTextContent());

    Node result2_apply = result2.apply(testData).getNodeValue();
    assertTrue(result2_apply instanceof Element);
    XMLAssert.assertXMLEqual("<user><fullname>Paul</fullname></user>", XmlUtil.toString(result2_apply));

  }

  @Test
  public void testRoundTripProcessModel1() throws IOException, SAXException {
    XmlProcessModel xpm;
    try (InputStream in = getDocument("testModel2.xml")) {
      xpm = JAXB.unmarshal(in, XmlProcessModel.class);
    }
    CharArrayWriter caw = new CharArrayWriter();
    JAXB.marshal(xpm, caw);


    XMLAssert.assertXMLEqual(new InputStreamReader(getDocument("testModel2.xml")), new CharArrayReader(caw.toCharArray()));
  }

}
