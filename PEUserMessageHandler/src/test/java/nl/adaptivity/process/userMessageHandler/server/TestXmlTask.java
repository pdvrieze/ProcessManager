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

/**
 * Created by pdvrieze on 16/08/15.
 */

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.Handles;
import net.devrieze.util.ReaderInputStream;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import static nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState.Complete;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


public class TestXmlTask {

  private XmlTask mSampleTask;

  @BeforeMethod
  public void before() {
    mSampleTask = new XmlTask();
    mSampleTask.mState= NodeInstanceState.Failed;
    mSampleTask.setOwnerString("pdvrieze");
    mSampleTask.setRemoteHandle(-1L);
  }

  @Test
  public void testSerialization() throws XmlException, IOException, SAXException {
    StringWriter out = new StringWriter();
    nl.adaptivity.xml.XmlUtil.serialize(mSampleTask, out);
    assertXMLEqual("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                         "<umh:task owner=\"pdvrieze\" state=\"Failed\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"/>\n", out
                         .toString());
  }

  @Test
  public void testSerialization2() throws XmlException, IOException, SAXException {
    StringWriter out = new StringWriter();
    XmlTask sampleTask2 = new XmlTask(mSampleTask);
    sampleTask2.setRemoteHandle(1L);
    sampleTask2.setInstanceHandle(2L);
    sampleTask2.setHandleValue(3L);
    sampleTask2.setSummary("testing");
    sampleTask2.setState(NodeInstanceState.FailRetry);
    nl.adaptivity.xml.XmlUtil.serialize(sampleTask2, out);
    assertXMLEqual("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                             "<umh:task handle=\"3\" instancehandle=\"2\" owner=\"pdvrieze\" remotehandle=\"1\" summary=\"testing\" state=\"FailRetry\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"/>\n", out
                         .toString());
  }

  @Test
  public void testDeserialize() throws XmlException {
    StringReader in = new StringReader("<task state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\" />");
    XmlTask result = XmlStreaming.deSerialize(in, XmlTask.class);
    assertEquals(result.getState(), Complete);
    assertEquals(result.getHandleValue(), -1L);
    assertEquals(result.getHandle(), null);
    assertEquals(result.getInstanceHandle(), -1L);
    assertEquals(result.getItems().size(), 0);
    assertEquals(result.getOwnerString(), null);
    assertEquals(result.getSummary(), null);
  }

  @Test
  public void testDeserialize2() throws XmlException {
    StringReader in = new StringReader("<task handle='1' instancehandle='3' summary='bar' state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\"><item name='one' type='label' value='two'><option>three</option><option>four</option></item></task>");
    XmlTask result = XmlStreaming.deSerialize(in, XmlTask.class);
    assertEquals(result.getState(), Complete);
    assertEquals(result.getHandleValue(), 1L);
    assertEquals(result.getHandle(), Handles.handle(1L));
    assertEquals(result.getInstanceHandle(), 3L);
    assertEquals(result.getItems().size(), 1);
    assertEquals(result.getOwnerString(), null);
    assertEquals(result.getSummary(), "bar");
    assertNotNull(result.getItems());
    XmlItem item = result.getItem("one");
    assertNotNull(item);
    assertEquals(item.getType(), "label");
    assertEquals(item.getValue(), "two");
    assertEquals(item.getName(), "one");
    assertEquals(item.getOptions().size(), 2);
    assertEquals(item.getOptions().get(0), "three");
    assertEquals(item.getOptions().get(1), "four");
  }

  @Test
  public void testDomDeserialize() throws ParserConfigurationException, IOException, SAXException {
    final String TEXT="<?xml version=\"1.0\" encoding=\"UTF-8\"?><task xmlns=\"http://adaptivity.nl/userMessageHandler\" state=\"Complete\"/>";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new ReaderInputStream(Charset.forName("UTF8"), new StringReader(TEXT)));
    Element root = doc.getDocumentElement();
    assertEquals(root.getTagName(), "task");

    XmlTask result = XmlStreaming.deSerialize(new DOMSource(root), XmlTask.class);
    assertEquals(result.getState(), Complete);
    assertEquals(result.getHandleValue(), -1L);
    assertEquals(result.getHandle(), null);
    assertEquals(result.getInstanceHandle(), -1L);
    assertEquals(result.getItems().size(), 0);
    assertEquals(result.getOwnerString(), null);
    assertEquals(result.getSummary(), null);
  }
}
