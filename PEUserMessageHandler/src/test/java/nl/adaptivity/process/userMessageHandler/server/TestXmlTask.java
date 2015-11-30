/**
 * Created by pdvrieze on 16/08/15.
 */

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.ReaderInputStream;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.TaskState;
import org.junit.Before;
import org.junit.Test;
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

import static org.junit.Assert.assertEquals;


public class TestXmlTask {

  private XmlTask mSampleTask;

  @Before
  public void before() {
    mSampleTask = new XmlTask();
    mSampleTask.mState= TaskState.Failed;
    mSampleTask.setOwnerString("pdvrieze");
    mSampleTask.setRemoteHandle(-1L);
  }

  @Test
  public void testSerialization() {
    StringWriter out = new StringWriter();
    JAXB.marshal(mSampleTask, out);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                         "<umh:task handle=\"-1\" instancehandle=\"-1\" owner=\"pdvrieze\" remotehandle=\"-1\" state=\"Failed\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"/>\n", out
                         .toString());
  }

  @Test
  public void testDeserialize() {
    StringReader in = new StringReader("<task state=\"Complete\" xmlns=\"http://adaptivity.nl/userMessageHandler\" />");
    XmlTask result=JAXB.unmarshal(in, XmlTask.class);
    assertEquals(TaskState.Complete, result.getState());
    assertEquals(-1L, result.getHandle());
    assertEquals(-1L,result.getInstanceHandle());
    assertEquals(0, result.getItems().size());
    assertEquals(null,result.getOwnerString());
    assertEquals(null, result.getSummary());
  }

  @Test
  public void testDomDeserialize() throws ParserConfigurationException, IOException, SAXException {
    final String TEXT="<?xml version=\"1.0\" encoding=\"UTF-8\"?><task xmlns=\"http://adaptivity.nl/userMessageHandler\" state=\"Complete\"/>";
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new ReaderInputStream(Charset.forName("UTF8"), new StringReader(TEXT)));
    Element root = doc.getDocumentElement();
    assertEquals("task", root.getTagName());

    XmlTask result = JAXB.unmarshal(new DOMSource(root), XmlTask.class);
    assertEquals(TaskState.Complete, result.getState());
    assertEquals(-1L, result.getHandle());
    assertEquals(-1L,result.getInstanceHandle());
    assertEquals(0, result.getItems().size());
    assertEquals(null,result.getOwnerString());
    assertEquals(null, result.getSummary());
  }
}
