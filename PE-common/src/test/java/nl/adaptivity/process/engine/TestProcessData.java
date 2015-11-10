package nl.adaptivity.process.engine;

import net.devrieze.util.Streams;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.*;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.util.xml.SimpleNamespaceContext;
import org.custommonkey.xmlunit.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;


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

  @XmlRootElement(name = "resultHolder")
  @XmlAccessorType(XmlAccessType.PROPERTY)
  private static class ResultTypeHolder {

    @XmlElement(name="result", required=true)
    private XmlResultType xmlResultType;

    public ResultTypeHolder() {}

    public ResultTypeHolder(final XmlResultType xmlResultType) {
      this.xmlResultType = xmlResultType;
    }
  }

  private static class WhiteSpaceIgnoringListener implements DifferenceListener {

    @Override
    public int differenceFound(@NotNull final Difference difference) {
      if(DifferenceConstants.TEXT_VALUE_ID==difference.getId()) {
        return 0;
      }
      return difference.getId();
    }

    @Override
    public void skippedComparison(final Node control, final Node test) {

    }
  }

  private static class NamespaceDeclIgnoringListener implements DifferenceListener {

    @Override
    public int differenceFound(@NotNull final Difference difference) {
      switch (difference.getId()) {
        case DifferenceConstants.ATTR_NAME_NOT_FOUND_ID: {
          if ((difference.getControlNodeDetail().getNode()!=null && XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(difference.getControlNodeDetail().getNode().getNamespaceURI()))||
                  (difference.getTestNodeDetail().getNode()!=null && XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(difference.getTestNodeDetail().getNode().getNamespaceURI()))){

            return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
          }
          break;
        }

      }
      return RETURN_ACCEPT_DIFFERENCE;
    }

    @Override
    public void skippedComparison(final Node control, final Node test) {

    }
  }

  private static DocumentBuilder _documentBuilder;

  private static DocumentBuilder getDocumentBuilder() {
    if (_documentBuilder==null) {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(false);
        dbf.setCoalescing(false);
        _documentBuilder = dbf.newDocumentBuilder();
      } catch (@NotNull final ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    return _documentBuilder;
  }

  private static InputStream getDocument(final String name) {
    return TestProcessData.class.getResourceAsStream("/nl/adaptivity/process/engine/test/"+name);
  }

  @NotNull
  private static ProcessModelImpl getProcessModel(final String name) throws XMLStreamException, IOException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    try (InputStream inputStream = getDocument(name)) {
      final XMLStreamReader in = xif.createXMLStreamReader(name, inputStream);
      try {
        final XmlDeserializerFactory factory = ProcessModel.class.getAnnotation(XmlDeserializer.class)
                                                                 .value()
                                                                 .newInstance();
        return (ProcessModelImpl) factory.deserialize(in);
      } catch (@NotNull InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Before
  public void setUp() throws ParserConfigurationException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
  }

  @Test
  public void testSerializeTextNode() throws XMLStreamException {
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    final CharArrayWriter caw = new CharArrayWriter();
    final XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);

    final ProcessData data = new ProcessData("foo", new CompactFragment("Hello"));
    data.serialize(xsw);
    xsw.flush();
    assertEquals("<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\">Hello</pe:value>", caw.toString());
  }

  @Test
  public void testSerializeSingleNode() throws XMLStreamException {
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    final CharArrayWriter caw = new CharArrayWriter();
    final XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);

    final ProcessData data = new ProcessData("foo", new CompactFragment("<bar/>"));
    data.serialize(xsw);
    xsw.flush();
    assertEquals("<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\"><bar/></pe:value>", caw.toString());
  }

  @Test
  public void testDeserializeProcessModel() throws IOException, SAXException, JAXBException, XMLStreamException {
    Logger.getAnonymousLogger().setLevel(Level.ALL);
    final XmlProcessModel xpm = new XmlProcessModel(getProcessModel("testModel2.xml"));
    ActivityImpl ac1 = null;
    ActivityImpl ac2 = null;
    StartNodeImpl start = null;
    EndNodeImpl end = null;
    for(final Object o: xpm.getNodes()) {
      if (o instanceof ProcessNodeImpl) {
        final ProcessNodeImpl node = (ProcessNodeImpl) o;
        if (node.getId() != null) {
          switch (node.getId()) {
            case "start":
              start = (StartNodeImpl) node;
              break;
            case "ac1":
              ac1 = (ActivityImpl) node;
              break;
            case "ac2":
              ac2 = (ActivityImpl) node;
              break;
            case "end":
              end = (EndNodeImpl) node;
              break;
          }
        }
      }
    }
    assertNotNull(start);
    assertNotNull(ac1);
    assertNotNull(ac2);
    assertNotNull(end);

    assertEquals("ac1", start.getSuccessors().iterator().next().getId());

    assertEquals("start", ac1.getPredecessor().getId());
    assertEquals("ac2", ac1.getSuccessors().iterator().next().getId());

    assertEquals("ac1", ac2.getPredecessor().getId());
    assertEquals("end", ac2.getSuccessors().iterator().next().getId());

    assertEquals("ac2", end.getPredecessor().getId());

    assertEquals(2, ac1.getResults().size());
    final XmlResultType result1 = ac1.getResults().get(0);
    assertEquals("name", result1.getName());
    assertEquals("/umh:result/umh:value[@name='user']/text()", result1.getPath());
    final SimpleNamespaceContext snc1 = (SimpleNamespaceContext) SimpleNamespaceContext.from(result1.getOriginalNSContext());
    assertEquals(1, snc1.size());
    assertEquals("umh", snc1.getPrefix(0));

    final XmlResultType result2 = ac1.getResults().get(1);
    final SimpleNamespaceContext snc2 = (SimpleNamespaceContext) SimpleNamespaceContext.from(result2.getOriginalNSContext());
    assertEquals(1, snc1.size());
    assertEquals("umh", snc1.getPrefix(0));

    final Document testData = getDocumentBuilder().parse(new InputSource(new StringReader("<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")));


    final CompactFragment result1_apply = result1.apply(testData).getContent();
    assertEquals("Paul", result1_apply.getContentString());

    final CompactFragment result2_apply = result2.apply(testData).getContent();
    XMLAssert.assertXMLEqual("<user><fullname>Paul</fullname></user>", result2_apply.getContentString());

  }

  @Test
  public void testXmlResultXpathParam() throws IOException, SAXException, XPathExpressionException {
    final SimpleNamespaceContext nsContext = new SimpleNamespaceContext(new String[]{"umh"}, new String[]{"http://adaptivity.nl/userMessageHandler"});
    final String expression = "/umh:result/umh:value[@name='user']/text()";
    final XmlResultType result = new XmlResultType("foo", expression, (char[]) null, nsContext);
    assertEquals(1, ((SimpleNamespaceContext) SimpleNamespaceContext.from(result.getOriginalNSContext())).size());

    final Document testData = getDocumentBuilder().parse(new InputSource(new StringReader("<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")));
    final XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(SimpleNamespaceContext.from(result.getOriginalNSContext()));
    final XPathExpression pathExpression = xPath.compile(expression);
    final NodeList apply2 = (NodeList) pathExpression.evaluate(testData, XPathConstants.NODESET);
    assertNotNull(apply2);
    assertTrue(apply2.item(0) instanceof Text);
    assertEquals("Paul", apply2.item(0).getTextContent());

    final Node apply3 = (Node) pathExpression.evaluate(testData, XPathConstants.NODE);
    assertNotNull(apply3);
    assertTrue(apply3 instanceof Text);
    assertEquals("Paul", apply3.getTextContent());

    final ProcessData apply1 = result.apply(testData);
    assertEquals("Paul", apply1.getContent().getContentString());
  }

  @NotNull
  private static CompactFragment createEndpoint() {
    final SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS));
    final StringBuilder content = new StringBuilder();
    content.append("<jbi:endpointDescriptor");
    content.append(" endpointLocation=\"http://localhost\"");
    content.append(" endpointName=\"internal\"");
    content.append(" serviceLocalName=\"foobar\"");
    content.append(" serviceNS=\"http://foo.bar\"");
    content.append(" />");
    return new CompactFragment(namespaces, content.toString().toCharArray());

  }

  @Test
  public void testTransform() throws XMLStreamException, IOException, SAXException {
    final ProcessData endpoint = new ProcessData("endpoint", createEndpoint());
    final PETransformer transformer = PETransformer.create(SimpleNamespaceContext.from(Collections.<Namespace>emptyList()), endpoint);
    final String INPUT = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
                         "  <jbi:element value=\"endpoint\"/>\n" +
                         "</umh:postTask>";
    final CompactFragment cf = new CompactFragment(new SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MODIFY_NS_STR)), INPUT.toCharArray());
    final CharArrayWriter caw = new CharArrayWriter();
    transformer.transform(new StAXSource(XMLFragmentStreamReader.from(cf)), new StreamResult(caw));
    {
      final String control = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost\" endpointName=\"internal\" serviceLocalName=\"foobar\" serviceNS=\"http://foo.bar\"/></umh:postTask>";
      final String test = caw.toString();
      try {
        assertXMLEqual(control, test);
      } catch (@NotNull SAXParseException| AssertionError e) {
        assertEquals(control, test);
      }
    }
  }

  @Test
  public void testRoundTripProcessModel1_ac1_result1() throws IOException, SAXException, JAXBException,
          XMLStreamException {
    final XmlProcessModel xpm = new XmlProcessModel(getProcessModel("testModel2.xml"));
    {
      final CharArrayWriter caw = new CharArrayWriter();
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      final XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);
      final ProcessNodeImpl ac1 = xpm.getNodes().get(1);
      assertEquals("ac1", ac1.getId());
      final List<? extends IXmlResultType> ac1Results = new ArrayList<>(ac1.getResults());

      final XmlResultType result = (XmlResultType) ac1Results.get(0);
      result.serialize(xsw);
      xsw.close();

      final String actual = caw.toString();
      final String expected = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>";

      XMLUnit.setIgnoreWhitespace(true);
      XMLUnit.setIgnoreAttributeOrder(true);
      final Diff diff = new Diff(expected, actual);
      try {
        assertXMLEqual(new DetailedDiff(diff), true);
      } catch (@NotNull final AssertionError e) {
        try {
          assertEquals(expected, actual);
        } catch (@NotNull final AssertionError f) {
          f.addSuppressed(e);
          throw f;
        }
      }
    }
  }

  @Test
  public void testRoundTripProcessModel1_ac1_result2() throws IOException, SAXException, JAXBException,
          XMLStreamException {
    final XmlProcessModel xpm = new XmlProcessModel(getProcessModel("testModel2.xml"));
    {
      final CharArrayWriter caw = new CharArrayWriter();
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      final XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);

      final ProcessNodeImpl ac1 = xpm.getNodes().get(1);
      assertEquals("ac1", ac1.getId());
      final List<? extends IXmlResultType> ac1Results = new ArrayList<>(ac1.getResults());
      final XmlResultType result = (XmlResultType) ac1Results.get(1);
      result.serialize(xsw);
      xsw.close();

      final String actual = caw.toString();
      final String expected = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\"><user xmlns=\"\">" +
              "<fullname>" +
              "<jbi:value  xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
              "</fullname>" +
              "</user>\n" +
              "</result>";

      XMLUnit.setIgnoreWhitespace(true);
      XMLUnit.setIgnoreAttributeOrder(true);
      final Diff diff = new Diff(expected, actual);

      assertXMLEqual(new DetailedDiff(diff), true);
    }
  }

  @Test
  public void testJaxbRoundTripProcessModel1() throws IOException, SAXException, JAXBException, XMLStreamException,
          InstantiationException, IllegalAccessException {

    testRoundTrip(getDocument("testModel2.xml"), ProcessModelImpl.class);

  }

  @Test
  public void testSerializeResult1() throws IOException, SAXException, XMLStreamException {
    final XmlProcessModel xpm = new XmlProcessModel(getProcessModel("testModel2.xml"));

    final CharArrayWriter caw = new CharArrayWriter();
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    final XMLStreamWriter xew = xof.createXMLStreamWriter(caw);
    final XmlResultType result = (XmlResultType) xpm.getNodes().get(1).getResults().iterator().next();
    result.serialize(xew);
    xew.close();
    final String control = "<result xpath=\"/umh:result/umh:value[@name='user']/text()\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xmlns=\"http://adaptivity.nl/ProcessEngine/\"/>";
    try {
      XMLAssert.assertXMLEqual(control, caw.toString());
    } catch (@NotNull final AssertionError e) {
      assertEquals(control, caw.toString());
    }
  }

  @Test
  public void testSerializeResult2() throws IOException, SAXException, XMLStreamException {
    final XmlResultType result;
    {
      final XmlProcessModel xpm = new XmlProcessModel(getProcessModel("testModel2.xml"));
      final Iterator<? extends IXmlResultType> iterator = xpm.getNodes().get(1).getResults().iterator();
      assertNotNull(iterator.next());
      result = (XmlResultType) iterator.next();
    }

    final String control = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
            "  <user xmlns=\"\"\n" +
            "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
            "    <fullname>\n" +
            "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
            "    </fullname>\n" +
            "  </user>\n" +
            "</result>";
    final String found = XmlUtil.toString(result);
    try {
      XMLUnit.setIgnoreWhitespace(true);
      XMLAssert.assertXMLEqual(control, found);
    } catch (@NotNull final AssertionError e) {
      assertEquals(control, found);
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final InputStream in, @NotNull final Class<T> target) throws IOException,
          XMLStreamException, IllegalAccessException, InstantiationException {
    final String expected;
    final XMLStreamReader streamReader;
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    if (in.markSupported()) {
      in.mark(Integer.MAX_VALUE);
      expected = Streams.toString(in, Charset.defaultCharset());
      in.reset();
      streamReader = xif.createXMLStreamReader(in);
    } else {
      expected = Streams.toString(in, Charset.defaultCharset());
      streamReader = xif.createXMLStreamReader(new StringReader(expected));
    }

    return testRoundTrip(streamReader, expected, target, false);
  }

  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final String xml, @NotNull final Class<T> target) throws
          IllegalAccessException, InstantiationException, XMLStreamException, IOException, SAXException {
    return testRoundTrip(xml, target, false);
  }

  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final String xml, @NotNull final Class<T> target, final boolean ignoreNs) throws
          IllegalAccessException, InstantiationException, XMLStreamException, IOException, SAXException {
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    return testRoundTrip(xif.createXMLStreamReader(new StringReader(xml)), xml, target, ignoreNs);
  }

  private static <T extends XmlSerializable> String testRoundTrip(final XMLStreamReader in, final String expected, @NotNull final Class<T> target, final boolean ignoreNs) throws
          InstantiationException, IllegalAccessException, XMLStreamException {
    @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = target.getAnnotation(XmlDeserializer.class).value().newInstance();
    final T obj = factory.deserialize(in);
    final XMLOutputFactory xof = XMLOutputFactory.newFactory();
    final CharArrayWriter caw = new CharArrayWriter();
    final XMLStreamWriter xsw = xof.createXMLStreamWriter(caw);
    obj.serialize(xsw);
    xsw.close();
    try {
      XMLUnit.setIgnoreWhitespace(true);
      final Diff diff = new Diff(expected, caw.toString());
      final DetailedDiff detailedDiff= new DetailedDiff(diff);
      if (ignoreNs) {
        detailedDiff.overrideDifferenceListener(new NamespaceDeclIgnoringListener());
      }
      assertXMLEqual(detailedDiff,true);
    } catch (@NotNull AssertionError | Exception e) {
      e.printStackTrace();
      assertEquals(expected, caw.toString());
    }
    return caw.toString();
  }

  @Test
  public void testRoundTripResult1() throws Exception {
    final String xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>";
    final String result = testRoundTrip(xml, XmlResultType.class);
    assertTrue(result.contains("xmlns:umh=\"http://adaptivity.nl/userMessageHandler\""));
  }

  @Test
  public void testRoundTripDefine() throws Exception {
    final String xml = "<define xmlns=\"http://adaptivity.nl/ProcessEngine/\" refnode=\"ac1\" refname=\"name\" name=\"mylabel\">Hi <jbi:value xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\".\"/>. Welcome!</define>";
    final String result = testRoundTrip(xml, XmlDefineType.class);
  }

  @Test
  public void testRoundTripResult2() throws Exception {
    final String xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
            "  <user xmlns=\"\"\n" +
            "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
            "    <fullname>\n" +
            "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
            "    </fullname>\n" +
            "  </user>\n" +
            "</result>";
    testRoundTrip(xml, XmlResultType.class);
  }

  @Test
  public void testRoundTripResult3() throws Exception {
    final String xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user2\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">" +
            "<jbi:value xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\" xpath=\"/umh:result/umh:value[@name='user']/text()\"/>" +
            "</result>";
    final String result = testRoundTrip(xml, XmlResultType.class);
    assertTrue(result.contains("xmlns:umh=\"http://adaptivity.nl/userMessageHandler\""));
  }

  @Test
  public void testRoundTripMessage() throws IOException, XMLStreamException, InstantiationException, SAXException,
          IllegalAccessException {
    final String xml = "    <pe:message xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" type=\"application/soap+xml\" serviceNS=\"http://adaptivity.nl/userMessageHandler\" serviceName=\"userMessageHandler\" endpoint=\"internal\" operation=\"postTask\" url=\"/PEUserMessageHandler/internal\">\n" +
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
            "    </pe:message>\n";
    testRoundTrip(xml, XmlMessage.class, false);
  }

  @Test
  public void testRoundTripActivity() throws Exception {
    final String xml = "  <activity xmlns=\"http://adaptivity.nl/ProcessEngine/\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"ac1\" predecessor=\"start\" id=\"ac1\">\n" +
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
            "  </activity>\n";
    testRoundTrip(xml, ActivityImpl.class, true);
  }

}
