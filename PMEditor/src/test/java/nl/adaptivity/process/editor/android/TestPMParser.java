package nl.adaptivity.process.editor.android;

import net.devrieze.util.Streams;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.engine.TestProcessData;
import nl.adaptivity.process.processModel.ProcessNodeBase;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.tasks.PostTask;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.*;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3.soapEnvelope.Envelope;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by pdvrieze on 13/11/15.
 */
public class TestPMParser {

  @Before
  public void init() {
    XmlStreaming.setFactory(new AndroidStreamingFactory());
  }

  @Test
  public void testParseNew() throws XmlPullParserException, XmlException {
    InputStream inputStream = getClass().getResourceAsStream("/processmodel.xml");
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    DrawableProcessModel model = DrawableProcessModel.deserialize(parser);
    checkModel1(model);
  }

  @Test
  public void testParseSimple() throws XmlPullParserException, XmlException {
    InputStream inputStream = getClass().getResourceAsStream("/processmodel.xml");
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    DrawableProcessModel model = PMParser.parseProcessModel(parser, LayoutAlgorithm.<DrawableProcessNode>nullalgorithm(), LayoutAlgorithm.<DrawableProcessNode>nullalgorithm());
    checkModel1(model);

  }

  @Test
  public void testNsIsue()  throws XmlPullParserException, XmlException, IOException, SAXException {
    InputStream inputStream = getClass().getResourceAsStream("/namespaceIssueModel.xml");
    String expected = Streams.toString(getClass().getResourceAsStream("/namespaceIssueModel_expected.xml"), Charset.defaultCharset());
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    CharArrayWriter out = new CharArrayWriter();
    XmlWriter writer = new AndroidXmlWriter(out);
    XmlUtil.serialize(parser, writer);
    try {
      XMLAssert.assertXMLEqual(expected, out.toString());
    } catch (AssertionError e) {
      assertEquals(expected, out.toString());
    }
  }

  @Test
  public void testRoundTripSoapMessage() throws Exception {
    String source = Streams.toString(getClass().getResourceAsStream("/message.xml"), Charset.forName("UTF-8"));

    XmlReader parser = new AndroidXmlReader(new StringReader(source));
    XmlMessage msg = XmlMessage.deserialize(parser);

    String out = XmlUtil.toString(msg);


    try {
      XMLAssert.assertXMLEqual(source, out);
    } catch (AssertionError e) {
      assertEquals(source, out);
    }

    String bodySource = msg.getMessageBody().getContentString();
    XmlReader bodyParser = msg.getBodyStreamReader();
    Envelope<PostTask> pt = Envelope.deserialize(bodyParser, PostTask.FACTORY);
    String bodyOut = XmlUtil.toString(pt);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    XMLUnit.setIgnoreAttributeOrder(true);

    try {
      XMLAssert.assertXMLEqual(bodySource, bodyOut);
    } catch (AssertionError e) {
      assertEquals(bodySource, bodyOut);
    }

  }

  private void checkModel1(final DrawableProcessModel model) {
    assertNotNull(model);

    assertEquals("There should be 9 effective elements in the process model (including an introduced split)", 9, model.getChildElements().size());
    DrawableStartNode start = (DrawableStartNode) model.getNode("start");
    DrawableActivity ac1 = (DrawableActivity) model.getNode("ac1");
    DrawableActivity ac2 = (DrawableActivity) model.getNode("ac2");
    DrawableActivity ac3 = (DrawableActivity) model.getNode("ac3");
    DrawableActivity ac4 = (DrawableActivity) model.getNode("ac4");
    DrawableActivity ac5 = (DrawableActivity) model.getNode("ac5");
    DrawableSplit split = (DrawableSplit) model.getNode("split1");
    DrawableJoin j1 = (DrawableJoin) model.getNode("j1");
    DrawableEndNode end = (DrawableEndNode) model.getNode("end");
    Collection<? extends Drawable> actualNodes = model.getChildElements();
    List<? extends ProcessNodeBase<DrawableProcessNode, DrawableProcessModel>> expectedNodes = Arrays.asList(start, ac1, ac2, split, ac3, ac5, j1, ac4, end);
    assertEquals(expectedNodes.size(), actualNodes.size());
    assertTrue(actualNodes.containsAll(expectedNodes));

    assertArrayEquals(toArray(), start.getPredecessors().toArray());
    assertArrayEquals(toArray(ac1), start.getSuccessors().toArray());

    assertArrayEquals(toArray(start), ac1.getPredecessors().toArray());
    assertArrayEquals(toArray(split), ac1.getSuccessors().toArray());

    assertArrayEquals(toArray(ac1), split.getPredecessors().toArray());
    assertArrayEquals(toArray(ac2, ac3), split.getSuccessors().toArray());

    assertArrayEquals(toArray(split), ac2.getPredecessors().toArray());
    assertArrayEquals(toArray(j1), ac2.getSuccessors().toArray());

    assertArrayEquals(toArray(split), ac3.getPredecessors().toArray());
    assertArrayEquals(toArray(ac5), ac3.getSuccessors().toArray());

    assertArrayEquals(toArray(j1), ac4.getPredecessors().toArray());
    assertArrayEquals(toArray(end), ac4.getSuccessors().toArray());

    assertArrayEquals(toArray(ac3), ac5.getPredecessors().toArray());
    assertArrayEquals(toArray(j1), ac5.getSuccessors().toArray());

    assertArrayEquals(toArray(ac4), end.getPredecessors().toArray());
    assertArrayEquals(toArray(), end.getSuccessors().toArray());
  }

  @Test
  public void testRoundTripResult1() throws Exception {
    TestProcessData otherTestSuite = new TestProcessData();
    otherTestSuite.testRoundTripResult1();
  }

  @Test
  public void testRoundTripResult2() throws Exception {
    TestProcessData otherTestSuite = new TestProcessData();
    otherTestSuite.testRoundTripResult2();
  }

  @Test
  public void testRoundTripActivity() throws Exception {
    TestProcessData otherTestSuite = new TestProcessData();
    otherTestSuite.testRoundTripActivity();
  }

  @Test
  public void testRoundTripMessage() throws Exception {
    TestProcessData otherTestSuite = new TestProcessData();
    otherTestSuite.testRoundTripMessage();
  }

  @Test
  public void testRoundTripProcessModel1() throws Exception {
    TestProcessData otherTestSuite = new TestProcessData();
    otherTestSuite.testJaxbRoundTripProcessModel1();
  }

  @Test
  public void testWriter() throws XmlException {
    CharArrayWriter caw = new CharArrayWriter();
    XmlWriter writer = XmlStreaming.newWriter(caw);
    testWriterCommon(writer);
    assertEquals("<prefix:tag>Hello</prefix:tag>", caw.toString());
  }

  @Test
  public void testWriterRepairing() throws XmlException {
    CharArrayWriter caw = new CharArrayWriter();
    XmlWriter writer = XmlStreaming.newWriter(caw, true);
    testWriterCommon(writer);
    assertEquals("<prefix:tag xmlns:prefix=\"urn:foo\">Hello</prefix:tag>", caw.toString());
  }

  private void testWriterCommon(final XmlWriter writer) throws XmlException {
    writer.setPrefix("bar", "urn:bar");
    writer.startTag("urn:foo", "tag", "prefix");
    writer.text("Hello");
    writer.endTag("urn:foo", "tag", "prefix");
    writer.close();
  }

  private static Object[] toArray(Object... val) {
    return val;
  }

  private static void assertNoneNull(final Object... values) {
    assertNotNull(values);
    int counter = 0;
    for(Object value: values) {
      assertNotNull("Value #"+counter+" should not be null", value);
      ++counter;
    }
  }
}
