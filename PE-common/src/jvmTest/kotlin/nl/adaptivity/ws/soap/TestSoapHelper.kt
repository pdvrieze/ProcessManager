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

package nl.adaptivity.ws.soap

import nl.adaptivity.process.engine.*
import nl.adaptivity.xml.DebugWriter

import nl.adaptivity.ws.soap.SoapHelper
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.EventType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.w3.soapEnvelope.Envelope
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource

import java.io.CharArrayWriter
import java.io.StringReader
import java.util.LinkedHashMap

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull


/**
 * Created by pdvrieze on 03/12/15.
 */
class TestSoapHelper {

    @Test
    @Throws(XmlException::class)
    fun testUnmarshalSoapBody() {
        val input = """<umh:postTask xmlns:umh="http://adaptivity.nl/userMessageHandler" xmlns="http://adaptivity.nl/userMessageHandler">
  <repliesParam>
    <jbi:endpointDescriptor xmlns:jbi="http://adaptivity.nl/jbi" endpointLocation="http://localhost:8080/ProcessEngine" endpointName="soap" serviceLocalName="ProcessEngine" serviceNS="http://adaptivity.nl/ProcessEngine/"/>
  </repliesParam>
  <taskParam>
    <task summary="Task Bar" remotehandle="18" instancehandle="5" owner="pdvrieze">
      <item type="label" value="Hi . Welcome!"></item>
    </task>
  </taskParam>
</umh:postTask>"""
        val result = SoapHelper.unmarshalWrapper(XmlStreaming.newReader(StringReader(input)))
        Assertions.assertEquals(2, result.size)
        assertNotNull(result["repliesParam"])
        assertNotNull(result["taskParam"])
    }

    @Test
    @Throws(Exception::class)
    fun testUnmarshalSoapResponse() {
        val env = Envelope.deserialize(XmlStreaming.newReader(StringReader(SOAP_RESPONSE1)))
        val bodyContent = env.body!!.bodyContent as CompactFragment?
        assertXMLEqual(SOAP_RESPONSE1_BODY, bodyContent!!.contentString)
    }

    @Test
    @Throws(Exception::class)
    fun testRoundtripSoapResponse() {
        val env = Envelope.deserialize(XmlStreaming.newReader(StringReader(SOAP_RESPONSE1)))
        val caw = CharArrayWriter()
        val out = DebugWriter(XmlStreaming.newWriter(caw))
        env.serialize(out)
        out.close()
        assertXMLEqual(SOAP_RESPONSE1, caw.toString())
    }

    @Test
    @Throws(Exception::class)
    fun testRoundtripSoapResponse2() {
        val env = Envelope.deserialize(XmlStreaming.newReader(StringReader(SOAP_RESPONSE2)))
        val caw = CharArrayWriter()
        val out = DebugWriter(XmlStreaming.newWriter(caw))
        env.serialize(out)
        out.close()
        assertXMLEqual(SOAP_RESPONSE2, caw.toString())
    }

    @Test
    @Throws(Exception::class)
    fun testUnmarshalSoapResponse2() {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc = dbf.newDocumentBuilder().parse(InputSource(StringReader(SOAP_RESPONSE1)))
        val env = Envelope.deserialize(XmlStreaming.newReader(DOMSource(doc)))
        val bodyContent = env.body!!.bodyContent as CompactFragment?
        assertXMLEqual(SOAP_RESPONSE1_BODY, bodyContent!!.contentString)
    }

    @Test
    @Throws(Exception::class)
    fun testXmlReaderFromDom() {
        val input = "<foo xmlns=\"urn:bar\"><rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result></foo>"
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val doc = dbf.newDocumentBuilder().parse(InputSource(StringReader(input)))
        val reader = XmlStreaming.newReader(DOMSource(doc))
        reader.require(EventType.START_DOCUMENT, null, null)
        reader.next()
        reader.require(EventType.START_ELEMENT, "urn:bar", "foo")
        reader.next()
        reader.require(EventType.START_ELEMENT, "http://www.w3.org/2003/05/soap-rpc", "result")
        val parseResult = reader.siblingsToFragment()
        assertFalse(parseResult.namespaces.iterator().hasNext())
    }

    companion object {

        private val SOAP_RESPONSE1_BODY = """<getProcessNodeInstanceSoapResponse>
      <rpc:result xmlns:rpc="http://www.w3.org/2003/05/soap-rpc">result</rpc:result>
      <result>
        <pe:nodeInstance xmlns:pe="http://adaptivity.nl/ProcessEngine/" handle="18" nodeid="ac2" processinstance="5" state="Acknowledged">
          <pe:predecessor>16</pe:predecessor>
          <pe:body>
            <Envelope:Envelope xmlns="http://www.w3.org/2003/05/soap-envelope" xmlns:Envelope="http://www.w3.org/2003/05/soap-envelope" xmlns:umh="http://adaptivity.nl/userMessageHandler" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" encodingStyle="http://www.w3.org/2003/05/soap-encoding">
              <Body>
                <umh:postTask xmlns="http://adaptivity.nl/userMessageHandler">
                  <repliesParam>
                    <jbi:endpointDescriptor xmlns:jbi="http://adaptivity.nl/jbi" endpointLocation="http://localhost:8080/ProcessEngine" endpointName="soap" serviceLocalName="ProcessEngine" serviceNS="http://adaptivity.nl/ProcessEngine/"/>
                  </repliesParam>
                  <taskParam>
                    <task instancehandle="5" owner="pdvrieze" remotehandle="18" summary="Task Bar">
                      <item type="label" value="Hi . Welcome!"/>
                    </task>
                  </taskParam>
                </umh:postTask>
              </Body>
            </Envelope:Envelope>
          </pe:body>
        </pe:nodeInstance>
      </result>
    </getProcessNodeInstanceSoapResponse>
  """
        private val SOAP_RESPONSE1 = """<soap:Envelope encodingStyle="http://www.w3.org/2003/05/soap-encoding" xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>$SOAP_RESPONSE1_BODY  </soap:Body>
</soap:Envelope>
"""

        private val SOAP_RESPONSE2 = "<soap:Envelope encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\" xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                                     "  <soap:Body>" + ("<getProcessNodeInstanceSoapResponse>\n" +
                                                        //                                                                  "      <rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result>\n" +
                                                        //                                                                  "      <result>\n" +
                                                        //                                                                  "        <pe:nodeInstance xmlns:pe=\"http://adaptivity.nl/ProcessEngine/\" handle=\"18\" nodeid=\"ac2\" processinstance=\"5\" state=\"Acknowledged\">\n" +
                                                        //                                                                  "          <pe:predecessor>16</pe:predecessor>\n" +
                                                        //                                                                  "          <pe:body>\n" +
                                                        //                                                                  "            <Envelope:Envelope xmlns=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:Envelope=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">\n" +
                                                        //                                                                  "              <Body>\n" +
                                                        //                                                                  "                <umh:postTask xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                                                        //                                                                  "                  <repliesParam>\n" +
                                                        //                                                                  "                    <jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost:8080/ProcessEngine\" endpointName=\"soap\" serviceLocalName=\"ProcessEngine\" serviceNS=\"http://adaptivity.nl/ProcessEngine/\"/>\n" +
                                                        //                                                                  "                  </repliesParam>\n" +
                                                        //                                                                  "                  <taskParam>\n" +
                                                        //                                                                  "                    <task instancehandle=\"5\" owner=\"pdvrieze\" remotehandle=\"18\" summary=\"Task Bar\">\n" +
                                                        //                                                                  "                      <item type=\"label\" value=\"Hi . Welcome!\"/>\n" +
                                                        //                                                                  "                    </task>\n" +
                                                        //                                                                  "                  </taskParam>\n" +
                                                        //                                                                  "                </umh:postTask>\n" +
                                                        //                                                                  "              </Body>\n" +
                                                        //                                                                  "            </Envelope:Envelope>\n" +
                                                        //                                                                  "          </pe:body>\n" +
                                                        //                                                                  "        </pe:nodeInstance>\n" +
                                                        //                                                                  "      </result>\n" +
                                                        "    </getProcessNodeInstanceSoapResponse>\n" +
                                                        "  ") +
                                     "  </soap:Body>\n" +
                                     "</soap:Envelope>\n"
    }

}
