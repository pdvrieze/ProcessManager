package nl.adaptivity.ws.soap;

import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Assert;
import org.junit.Test;
import org.w3.soapEnvelope.Envelope;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import java.io.StringReader;
import java.util.LinkedHashMap;

/**
 * Created by pdvrieze on 03/12/15.
 */
public class TestSoapHelper {

  private static final String SOAP_RESPONSE1_BODY = "<getProcessNodeInstanceSoapResponse>\n" +
                                                    "      <rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result>\n" +
                                                    "      <result>\n" +
                                                    "        <pe:nodeInstance xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" handle=\"18\" nodeid=\"ac2\" processinstance=\"5\" state=\"Acknowledged\">\n" +
                                                    "          <pe:predecessor>16</pe:predecessor>\n" +
                                                    "          <pe:body>\n" +
                                                    "            <Envelope:Envelope xmlns=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:Envelope=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">\n" +
                                                    "              <Body>\n" +
                                                    "                <umh:postTask xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                                                    "                  <repliesParam>\n" +
                                                    "                    <jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost:8080/ProcessEngine\" endpointName=\"soap\" serviceLocalName=\"ProcessEngine\" serviceNS=\"http://adaptivity.nl/ProcessEngine/\"/>\n" +
                                                    "                  </repliesParam>\n" +
                                                    "                  <taskParam>\n" +
                                                    "                    <task instancehandle=\"5\" owner=\"pdvrieze\" remotehandle=\"18\" summary=\"Task Bar\">\n" +
                                                    "                      <item type=\"label\" value=\"Hi . Welcome!\"/>\n" +
                                                    "                    </task>\n" +
                                                    "                  </taskParam>\n" +
                                                    "                </umh:postTask>\n" +
                                                    "              </Body>\n" +
                                                    "            </Envelope:Envelope>\n" +
                                                    "          </pe:body>\n" +
                                                    "        </pe:nodeInstance>\n" +
                                                    "      </result>\n" +
                                                    "    </getProcessNodeInstanceSoapResponse>\n" +
                                                    "  ";
  private static final String SOAP_RESPONSE1 = "<soap:Envelope encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\" xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                                               "  <soap:Body>"+SOAP_RESPONSE1_BODY+
                                               "  </soap:Body>\n" +
                                               "</soap:Envelope>\n";

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

  @Test
  public void testUnmarshalSoapResponse() throws Exception {
    Envelope env = Envelope.deserialize(XmlStreaming.newReader(new StringReader(SOAP_RESPONSE1)));
    CompactFragment bodyContent = env.getBody().getBodyContent();
    XMLAssert.assertXMLEqual(SOAP_RESPONSE1_BODY, bodyContent.getContentString());
  }

  @Test
  public void testUnmarshalSoapResponse2() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(SOAP_RESPONSE1)));
    Envelope env = Envelope.deserialize(XmlStreaming.newReader(new DOMSource(doc)));
    CompactFragment bodyContent = env.getBody().getBodyContent();
    XMLAssert.assertXMLEqual(SOAP_RESPONSE1_BODY, bodyContent.getContentString());
  }

}
