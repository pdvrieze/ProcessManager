/**
 * Created by pdvrieze on 16/08/15.
 */

package nl.adaptivity.process.userMessageHandler.server;

import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;


public class TestXmlTask {

  private XmlTask mSampleTask;

  @Before
  public void before() {
    mSampleTask = new XmlTask();
    mSampleTask.aState= TaskState.Failed;
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
}
