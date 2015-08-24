package nl.adaptivity.process.engine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.CharArrayWriter;

import static org.junit.Assert.assertEquals;


/**
 * Created by pdvrieze on 24/08/15.
 */
public class TestProcessData {

  private Document mDocument;

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

}
