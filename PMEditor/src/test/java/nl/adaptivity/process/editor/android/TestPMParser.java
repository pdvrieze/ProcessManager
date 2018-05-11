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

package nl.adaptivity.process.editor.android;

import net.devrieze.util.Streams;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.engine.TestProcessData;
import nl.adaptivity.process.processModel.ProcessNodeBase;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.engine.XmlProcessModel;
import nl.adaptivity.process.tasks.PostTask;
import nl.adaptivity.xml.*;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


/**
 * Created by pdvrieze on 13/11/15.
 */
public class TestPMParser {

  @BeforeMethod
  public void init() {
    XmlStreaming.setFactory(new AndroidStreamingFactory());
  }

  @Test
  public void testParseNew() throws XmlPullParserException, XmlException {
    InputStream                         inputStream = getClass().getResourceAsStream("/processmodel.xml");
    XmlReader                           parser      = new AndroidXmlReader(inputStream, "UTF-8");
      final XmlProcessModel deserializedModel = XmlProcessModel.deserialize(parser);
      DrawableProcessModel              model       = new RootDrawableProcessModel(
          deserializedModel.getRootModel());
    checkModel1(model);
  }

  @Test
  public void testParseSimple() throws XmlPullParserException, XmlException {
    InputStream inputStream = getClass().getResourceAsStream("/processmodel.xml");
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    DrawableProcessModel model = PMParser.parseProcessModel(parser, LayoutAlgorithm.<DrawableProcessNode>nullalgorithm(), LayoutAlgorithm.<DrawableProcessNode>nullalgorithm()).build();
    checkModel1(model);

  }

  @Test
  public void testNsIsue()  throws XmlPullParserException, XmlException, IOException, SAXException {
    InputStream inputStream = getClass().getResourceAsStream("/namespaceIssueModel.xml");
    String expected = Streams.readString(getClass().getResourceAsStream("/namespaceIssueModel_expected.xml"), Charset.defaultCharset());
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    CharArrayWriter out = new CharArrayWriter();
    XmlWriter writer = new AndroidXmlWriter(out);
    XmlWriterUtilCore.serialize(writer, parser);
    try {
      XMLAssert.assertXMLEqual(expected, out.toString());
    } catch (AssertionError e) {
      assertEquals(out.toString(),expected);
    }
  }

  @Test
  public void testRoundTripSoapMessage() throws Exception {
    String source = Streams.toString(getClass().getResourceAsStream("/message.xml"), Charset.forName("UTF-8"));

    XmlReader parser = new AndroidXmlReader(new StringReader(source));
    XmlMessage msg = XmlMessage.Companion.deserialize(parser);

    String out = XmlSerializableExt.toString(msg);


    try {
      XMLAssert.assertXMLEqual(source, out);
    } catch (AssertionError e) {
      assertEquals(out, source);
    }

    String bodySource = msg.getMessageBody().getContentString();
    XmlReader bodyParser = msg.getBodyStreamReader();
    Envelope<PostTask> pt = Envelope.Companion.deserialize(bodyParser, PostTask.FACTORY);
    String bodyOut = XmlSerializableExt.toString(pt);
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

    assertEquals(model.getChildElements().size(), 9, "There should be 9 effective elements in the process model (including an introduced split)");
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

    assertEquals(start.getPredecessors().toArray(), toArray());;
    assertEquals(start.getSuccessors().toArray(), toArray(ac1));;

    assertEquals(ac1.getPredecessors().toArray(), toArray(start));;
    assertEquals(ac1.getSuccessors().toArray(), toArray(split));;

    assertEquals(split.getPredecessors().toArray(), toArray(ac1));;
    assertEquals(split.getSuccessors().toArray(), toArray(ac2, ac3));;

    assertEquals(ac2.getPredecessors().toArray(), toArray(split));;
    assertEquals(ac2.getSuccessors().toArray(), toArray(j1));;

    assertEquals(ac3.getPredecessors().toArray(), toArray(split));;
    assertEquals(ac3.getSuccessors().toArray(), toArray(ac5));;

    assertEquals(ac4.getPredecessors().toArray(), toArray(j1));;
    assertEquals(ac4.getSuccessors().toArray(), toArray(end));;

    assertEquals(ac5.getPredecessors().toArray(), toArray(ac3));;
    assertEquals(ac5.getSuccessors().toArray(), toArray(j1));;

    assertEquals(end.getPredecessors().toArray(), toArray(ac4));;
    assertEquals(end.getSuccessors().toArray(), toArray());;
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
    otherTestSuite.testXmlStreamingRoundTripProcessModel1();
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
      assertNotNull(value, "Value #"+counter+" should not be null");
      ++counter;
    }
  }
}
