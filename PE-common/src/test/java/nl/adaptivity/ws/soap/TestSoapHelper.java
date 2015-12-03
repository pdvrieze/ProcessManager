package nl.adaptivity.ws.soap;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;
import org.junit.Test;
import org.w3c.dom.Node;

import java.io.StringReader;
import java.util.LinkedHashMap;
import static org.junit.Assert.*;

/**
 * Created by pdvrieze on 03/12/15.
 */
public class TestSoapHelper {

  @Test
  public void testUnmarshalSoapBody() throws XmlException {
    String input = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                   "  <repliesParam>\n" +
                   "    <jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost:8080/ProcessEngine\" endpointName=\"soap\" serviceLocalName=\"ProcessEngine\" serviceNS=\"http://adaptivity.nl/ProcessEngine/\"/>\n" +
                   "  </repliesParam>\n" +
                   "  <taskParam>\n" +
                   "    <task summary=\"Task Bar\" remotehandle=\"18\" instancehandle=\"5\" owner=\"pdvrieze\">\n" +
                   "      <item type=\"label\" value=\"Hi . Welcome!\"></item>\n" +
                   "    </task>\n" +
                   "  </taskParam>\n" +
                   "</umh:postTask>";
    LinkedHashMap<String, Node> result = SoapHelper.unmarshalWrapper(XmlStreaming.newReader(new StringReader(input)));
    Assert.assertEquals(2, result.size());
    Assert.assertNotNull(result.get("repliesParam"));
    Assert.assertNotNull(result.get("taskParam"));
  }

}
