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

package nl.adaptivity.process.engine;

import net.devrieze.util.Streams;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.processModel.engine.*;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XMLFragmentStreamReader;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.SimpleNamespaceContext;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.custommonkey.xmlunit.*;
import org.jetbrains.annotations.NotNull;
import org.mockito.InOrder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.xpath.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.adaptivity.xml.SimpleNamespaceContext.Companion;
import static nl.adaptivity.xml.XmlStreaming.EventType.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


/**
 * Created by pdvrieze on 24/08/15.
 */
@SuppressWarnings("ConstantConditions")
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

  private static InputStream getDocument(final String name) throws FileNotFoundException {
    InputStream stream = TestProcessData.class.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name);
    if (stream==null) { stream = new FileInputStream("nl/adaptivity/process/engine/test/"+name); }
    return stream;
  }

  @BeforeMethod
  private static void init() {
    XmlStreaming.setFactory(null); // make sure to have the default factory
  }

  @NotNull
  private static ProcessModelImpl getProcessModel(final String name) throws IOException,
          XmlException {
    try (InputStream inputStream = getDocument(name)) {
      final XmlReader in = XmlStreaming.newReader(inputStream, "UTF-8");
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

  @Test
  public void testSerializeTextNode() throws XmlException {
    final CharArrayWriter caw = new CharArrayWriter();
    final XmlWriter xsw = XmlStreaming.newWriter(caw);

    final ProcessData data = new ProcessData("foo", new CompactFragment("Hello"));
    data.serialize(xsw);
    xsw.flush();
    Assert.assertEquals(caw.toString(), "<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\">Hello</pe:value>");
  }

  @Test
  public void testSerializeSingleNode() throws XmlException {
    final CharArrayWriter caw = new CharArrayWriter();
    final XmlWriter xsw = XmlStreaming.newWriter(caw);

    final ProcessData data = new ProcessData("foo", new CompactFragment("<bar/>"));
    data.serialize(xsw);
    xsw.flush();
    assertEquals(caw.toString(), "<pe:value xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\"><bar/></pe:value>");
  }

  @Test
  public void testSerializeMessage() throws Exception {
    Logger.getAnonymousLogger().setLevel(Level.ALL);
    final ProcessModelImpl pm = getProcessModel("testModel2.xml");
    ActivityImpl ac2 = (ActivityImpl) pm.getNode("ac2");
    String serialized = XmlUtil.toString(ac2.getMessage());
    XmlMessage msg2= XmlStreaming.deSerialize(new StringReader(serialized), XmlMessage.class);
    assertEquals(msg2.getMessageBody().getContentString(), ac2.getMessage().getMessageBody().getContentString());
    assertEquals(msg2, ac2.getMessage());
  }

  @Test
  public void testDeserializeProcessModel() throws Exception {
    Logger.getAnonymousLogger().setLevel(Level.ALL);
    final ProcessModelImpl pm = getProcessModel("testModel2.xml");
    ActivityImpl ac1 = null;
    ActivityImpl ac2 = null;
    StartNodeImpl start = null;
    EndNodeImpl end = null;
    for(final ExecutableProcessNode node: pm.getModelNodes()) {
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
    assertNotNull(start);
    assertNotNull(ac1);
    assertNotNull(ac2);
    assertNotNull(end);

    assertEquals(start.getSuccessors().iterator().next().getId(), "ac1");

    assertEquals(ac1.getPredecessor().getId(), "start");
    assertEquals(ac1.getSuccessors().iterator().next().getId(), "ac2");

    assertEquals(ac2.getPredecessor().getId(), "ac1");
    assertEquals(ac2.getSuccessors().iterator().next().getId(), "end");

    assertEquals(end.getPredecessor().getId(), "ac2");

    assertEquals(ac1.getResults().size(), 2);
    final XmlResultType result1 = ac1.getResults().get(0);
    assertEquals(result1.getName(), "name");
    assertEquals(result1.getPath(), "/umh:result/umh:value[@name='user']/text()");
    final SimpleNamespaceContext snc1 = (SimpleNamespaceContext) SimpleNamespaceContext.from(result1.getOriginalNSContext());
    assertEquals(snc1.size(), 1);
    assertEquals(snc1.getPrefix(0), "umh");

    final XmlResultType result2 = ac1.getResults().get(1);
    final SimpleNamespaceContext snc2 = (SimpleNamespaceContext) SimpleNamespaceContext.from(result2.getOriginalNSContext());
    assertEquals(snc1.size(), 1);
    assertEquals(snc1.getPrefix(0), "umh");

    final Document testData = getDocumentBuilder().parse(new InputSource(new StringReader("<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")));


    final CompactFragment result1_apply = result1.apply(testData).getContent();
    assertEquals(result1_apply.getContentString(), "Paul");

    final CompactFragment result2_apply = result2.apply(testData).getContent();
    XMLAssert.assertXMLEqual("<user><fullname>Paul</fullname></user>", result2_apply.getContentString());

  }

  @Test
  public void testXmlResultXpathParam() throws IOException, SAXException, XPathExpressionException {
    final SimpleNamespaceContext nsContext = new SimpleNamespaceContext(new String[]{"umh"}, new String[]{"http://adaptivity.nl/userMessageHandler"});
    final String expression = "/umh:result/umh:value[@name='user']/text()";
    final XmlResultType result = new XmlResultType("foo", expression, (char[]) null, nsContext);
    assertEquals(((SimpleNamespaceContext) Companion.from(result.getOriginalNSContext())).size(), 1);

    final Document testData = getDocumentBuilder().parse(new InputSource(new StringReader("<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>")));
    final XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(SimpleNamespaceContext.Companion.from(result.getOriginalNSContext()));
    final XPathExpression pathExpression = xPath.compile(expression);
    final NodeList apply2 = (NodeList) pathExpression.evaluate(testData, XPathConstants.NODESET);
    assertNotNull(apply2);
    assertTrue(apply2.item(0) instanceof Text);
    assertEquals(apply2.item(0).getTextContent(), "Paul");

    final Node apply3 = (Node) pathExpression.evaluate(testData, XPathConstants.NODE);
    assertNotNull(apply3);
    assertTrue(apply3 instanceof Text);
    assertEquals(apply3.getTextContent(), "Paul");

    final ProcessData apply1 = result.apply(testData);
    assertEquals(apply1.getContent().getContentString(), "Paul");
  }

  @NotNull
  private static CompactFragment createEndpoint() {
    final SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS_STR));
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
  public void testReadFragment() throws XmlException {
    String testDataInner="<b xmlns:umh='urn:foo'><umh:a xpath='/umh:value'/></b>";
    XmlReader in = XmlStreaming.newReader(new StringReader(testDataInner));
    in.next(); in.require(EventType.START_ELEMENT, "", "b");
    in.next(); in.require(EventType.START_ELEMENT, "urn:foo", "a");
    CompactFragment fragment = XmlReaderUtil.siblingsToFragment(in);
    in.require(EventType.END_ELEMENT, "", "b");
    in.next(); in.require(EventType.END_DOCUMENT, null, null);

    assertEquals(fragment.getNamespaces().size(), 1);
    assertEquals(fragment.getNamespaces().getNamespaceURI(0), "urn:foo");
    assertEquals(fragment.getNamespaces().getPrefix(0), "umh");
    assertEquals(fragment.getContentString(), "<umh:a xpath=\"/umh:value\"/>");
  }

  @Test
  public void testTransform() throws XmlException, IOException, SAXException {
    final ProcessData endpoint = new ProcessData("endpoint", createEndpoint());
    final PETransformer transformer = PETransformer.create(SimpleNamespaceContext.Companion.from(Collections.<nl.adaptivity.xml.Namespace>emptyList()), endpoint);
    final String INPUT = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
                         "  <jbi:element value=\"endpoint\"/>\n" +
                         "</umh:postTask>";
    final CompactFragment cf = new CompactFragment(new SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MODIFY_NS_STR)), INPUT.toCharArray());
    final CharArrayWriter caw = new CharArrayWriter();
    XmlWriter out = XmlStreaming.newWriter(caw, true);
    transformer.transform(XMLFragmentStreamReader.from(cf), out);
    out.close();
    {
      final String control = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost\" endpointName=\"internal\" serviceLocalName=\"foobar\" serviceNS=\"http://foo.bar\"/></umh:postTask>";
      final String test = caw.toString();
      try {
        assertXMLEqual(control, test);
      } catch (@NotNull SAXParseException| AssertionError e) {
        assertEquals(test, control);
      }
    }
  }

  @Test
  public void testRoundTripProcessModel1_ac1_result1() throws Exception {
    final ProcessModelImpl xpm = getProcessModel("testModel2.xml");
    {
      final CharArrayWriter caw = new CharArrayWriter();
      final XmlWriter xsw = XmlStreaming.newWriter(caw);

      final ExecutableProcessNode ac1;
      {
        Collection<? extends ExecutableProcessNode> modelNodes = xpm.getModelNodes();
        Iterator<? extends ExecutableProcessNode> it = modelNodes.iterator();
        it.next();
        ac1 = it.next();
      }

      assertEquals(ac1.getId(), "ac1");
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
          assertEquals(actual, expected);
        } catch (@NotNull final AssertionError f) {
          f.addSuppressed(e);
          throw f;
        }
      }
    }
  }

  @Test
  public void testRoundTripProcessModel1_ac1_result2() throws Exception {
    final ProcessModelImpl processModel = getProcessModel("testModel2.xml");
    {
      final CharArrayWriter caw = new CharArrayWriter();
      final XmlWriter xsw = XmlStreaming.newWriter(caw);

      final ExecutableProcessNode ac1 = processModel.getNode("ac1");
      assertEquals(ac1.getId(), "ac1");
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
  public void testJaxbRoundTripProcessModel1() throws Exception {

    testRoundTrip(getDocument("testModel2.xml"), ProcessModelImpl.class);

  }

  @Test
  public void testSerializeResult1() throws IOException, SAXException, XmlException {
    final ProcessModel<?, ?> pm = getProcessModel("testModel2.xml");

    final CharArrayWriter caw = new CharArrayWriter();
    final XmlWriter xsw = XmlStreaming.newWriter(caw);

    final XmlResultType result;
    {
      Collection<? extends ProcessNode<?, ?>> modelNodes = pm.getModelNodes();
      Iterator<? extends ProcessNode<?, ?>> it = modelNodes.iterator();
      it.next();
      result = (XmlResultType) it.next().getResults().iterator().next();
    }

    result.serialize(xsw);
    xsw.close();
    final String control = "<result xpath=\"/umh:result/umh:value[@name='user']/text()\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" name=\"name\" xmlns=\"http://adaptivity.nl/ProcessEngine/\"/>";
    try {
      XMLAssert.assertXMLEqual(control, caw.toString());
    } catch (@NotNull final AssertionError e) {
      assertEquals(caw.toString(), control);
    }
  }

  @Test
  public void testSerializeResult2() throws IOException, SAXException, XmlException {
    final XmlResultType result;
    {
      final ProcessModelImpl xpm = getProcessModel("testModel2.xml");
      final Iterator<? extends IXmlResultType> iterator = xpm.getNode("ac1").getResults().iterator();
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
      assertEquals(found, control);
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final InputStream in, @NotNull final Class<T> target) throws
          IOException, IllegalAccessException, InstantiationException, XmlException {
    final String expected;
    final XmlReader streamReader;
    final XMLInputFactory xif = XMLInputFactory.newFactory();
    if (in.markSupported()) {
      in.mark(Integer.MAX_VALUE);
      expected = Streams.toString(in, Charset.defaultCharset());
      in.reset();
      streamReader = XmlStreaming.newReader(in, Charset.defaultCharset().toString());
    } else {
      expected = Streams.toString(in, Charset.defaultCharset());
      streamReader = XmlStreaming.newReader(new StringReader(expected));
    }

    return testRoundTrip(expected, streamReader, target, false);
  }

  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final String xml, @NotNull final Class<T> target) throws
          IllegalAccessException, InstantiationException, XmlException, IOException, SAXException {
    return testRoundTrip(xml, target, false);
  }

  public static <T extends XmlSerializable> String testRoundTrip(@NotNull final String xml, @NotNull final Class<T> target, final boolean ignoreNs) throws
          IllegalAccessException, InstantiationException, XmlException, IOException, SAXException {
    return testRoundTrip(xml, XmlStreaming.newReader(new StringReader(xml)), target, ignoreNs);
  }

  private static <T extends XmlSerializable> String testRoundTrip(final String expected, final XmlReader actual, @NotNull final Class<T> target, final boolean ignoreNs) throws
          InstantiationException, IllegalAccessException, XmlException {
    assertNotNull(actual);
    @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = (XmlDeserializerFactory) target.getAnnotation(XmlDeserializer.class).value().newInstance();
    final T obj = factory.deserialize(actual);
    final CharArrayWriter caw = new CharArrayWriter();
    final XmlWriter xsw = XmlStreaming.newWriter(caw);
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
      assertEquals(caw.toString(), expected);
    }
    return caw.toString();
  }

  @Test
  public void testRead() throws Exception {
    String testData = "Hello<a>who<b>are</b>you</a>";
    XmlReader in = XmlStreaming.newReader(new StringReader("<wrap>"+testData+"</wrap>"));
    assertEquals(in.next(), START_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), TEXT);
    assertEquals(in.getText(), "Hello");
    assertEquals(in.next(), START_ELEMENT);
    assertEquals(in.getLocalName(), "a");
    assertEquals(in.next(), TEXT);
    assertEquals(in.getText(), "who");
    assertEquals(in.next(), START_ELEMENT);
    assertEquals(in.getLocalName(), "b");
    assertEquals(in.next(), TEXT);
    assertEquals(in.getText(), "are");
    assertEquals(in.next(), END_ELEMENT);
    assertEquals(in.getLocalName(), "b");
    assertEquals(in.next(), TEXT);
    assertEquals(in.getText(), "you");
    assertEquals(in.next(), END_ELEMENT);
    assertEquals(in.getLocalName(), "a");
    assertEquals(in.next(), END_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), END_DOCUMENT);
  }

  @Test
  public void testSiblingsToFragmentMock() throws Exception {
    String testData = "Hello<a>who<b>are</b>you</a>";
    XmlReader in = XmlStreaming.newReader(new StringReader("<wrap>"+testData+"</wrap>"));
    assertEquals(in.next(), START_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), TEXT);

    {
      XmlStreamingFactory factory = mock(XmlStreamingFactory.class);
      XmlWriter mockedWriter = mock(XmlWriter.class);
      NamespaceContext nsContext = mock(NamespaceContext.class);
      when(factory.newWriter(any(Writer.class), anyBoolean())).thenReturn(mockedWriter);
      when(mockedWriter.getNamespaceContext()).thenReturn(nsContext);
      when(nsContext.getNamespaceURI("")).thenReturn("");
      when(nsContext.getPrefix("")).thenReturn("");
      XmlStreaming.setFactory(factory);
      XmlReaderUtil.siblingsToFragment(in);

      InOrder inOrder = inOrder(mockedWriter);
      // The Hello text will not be written with a writer, but directly escaped
      // as otherwise the serializer will complain about multiple roots.
      // inOrder.verify(mockedWriter).text("Hello");
      inOrder.verify(mockedWriter).startTag("", "a", "");
      inOrder.verify(mockedWriter).text("who");
      inOrder.verify(mockedWriter).startTag("", "b", "");
      inOrder.verify(mockedWriter).text("are");
      inOrder.verify(mockedWriter).endTag("", "b", "");
      inOrder.verify(mockedWriter).text("you");
      inOrder.verify(mockedWriter).endTag("", "a", "");
      inOrder.verify(mockedWriter).close();
      inOrder.verifyNoMoreInteractions();
    }
    assertEquals(in.getEventType(), END_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), END_DOCUMENT);
  }

  @Test
  public void testSiblingsToFragment() throws Exception {
    String testData = "Hello<a>who<b>are<c>you</c>.<d>I</d></b>don't</a>know";
    XmlReader in = XmlStreaming.newReader(new StringReader("<wrap>"+testData+"</wrap>"));

    assertEquals(in.next(), START_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), TEXT);

    XmlStreaming.setFactory(null); // reset to the default one
    CompactFragment fragment = XmlReaderUtil.siblingsToFragment(in);

    assertEquals(fragment.getNamespaces().size(), 0);
    assertEquals(fragment.getContentString(), testData);
    assertEquals(in.getEventType(), END_ELEMENT);
    assertEquals(in.getLocalName(), "wrap");
    assertEquals(in.next(), END_DOCUMENT);
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
  public void testDeserializeResult2() throws Exception {
    final String xml = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"user\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
                       "  <user xmlns=\"\"\n" +
                       "    xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
                       "    <fullname>\n" +
                       "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
                       "    </fullname>\n" +
                       "  </user>\n" +
                       "</result>";

    final String expectedContent = "\n  <user xmlns=\"\"" +
                       " xmlns:jbi=\"http://adaptivity.nl/ProcessEngine/activity\">\n" +
                       "    <fullname>\n" +
                       "      <jbi:value xpath=\"/umh:result/umh:value[@name='user']/text()\"/>\n" +
                       "    </fullname>\n" +
                       "  </user>\n";

    XmlResultType rt = XmlResultType.deserialize(XmlStreaming.newReader(new StringReader(xml)));
    assertEquals(new String(rt.getContent()), expectedContent);
    Iterable<Namespace> namespaces = rt.getOriginalNSContext();
    Iterator<Namespace> it         = namespaces.iterator();
    Namespace           ns         = it.next();
    assertEquals(ns.getPrefix(), "");
    assertEquals(ns.getNamespaceURI(), "http://adaptivity.nl/ProcessEngine/");
    ns = it.next();
    assertEquals(ns.getPrefix(), "umh");
    assertEquals(ns.getNamespaceURI(), "http://adaptivity.nl/userMessageHandler");

    assertEquals(it.hasNext(), false);
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
  public void testRoundTripMessage() throws IOException, InstantiationException, SAXException,
          IllegalAccessException, XmlException {
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
