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

package nl.adaptivity.process.engine

import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Constants.USER_MESSAGE_HANDLER_NS
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.util.*
import javax.xml.XMLConstants
import javax.xml.bind.JAXBException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory


/**
 * Created by pdvrieze on 24/08/15.
 */
@OptIn(XmlUtilInternal::class)
class TestXmlResultType {

    private val db: DocumentBuilder
        @Throws(ParserConfigurationException::class)
        get() {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            return dbf.newDocumentBuilder()
        }

    @Test
    @Throws(
        XPathExpressionException::class, ParserConfigurationException::class, IOException::class,
        SAXException::class
           )
    fun testXPath() {
        val expr = XPathFactory.newInstance().newXPath().compile("/result/value[@name='user']/text()")
        val testData = db.parse(InputSource(StringReader("<result><value name='user'>Paul</value></result>")))
        assertEquals("Paul", expr.evaluate(testData))
    }

    @Test
    @Throws(
        XPathExpressionException::class, ParserConfigurationException::class, IOException::class,
        SAXException::class
           )
    fun testXPathNS() {
        val xPath = XPathFactory.newInstance().newXPath()
        val prefixMap = TreeMap<String, String>()
        prefixMap["ns1"] = USER_MESSAGE_HANDLER_NS
        xPath.namespaceContext = SimpleNamespaceContext(prefixMap)
        val expr = xPath.compile("/ns1:result/ns1:value[@name='user']/text()")
        val testData = db.parse(
            InputSource(
                StringReader(
                    "<umh:result xmlns:umh='" + USER_MESSAGE_HANDLER_NS + "'><umh:value name='user'>Paul</umh:value></umh:result>"
                            )
                       )
                               )
        assertEquals("Paul", expr.evaluate(testData))
    }

    @Test
    @Throws(
        XPathExpressionException::class, ParserConfigurationException::class, IOException::class,
        SAXException::class
           )
    fun testXPathNS2() {
        val xPath = XPathFactory.newInstance().newXPath()
        val prefixMap = TreeMap<String, String>()
        prefixMap["ns1"] = USER_MESSAGE_HANDLER_NS
        xPath.namespaceContext = SimpleNamespaceContext(prefixMap)
        val expr = xPath.compile("/ns1:result/ns1:value[@name='user']/text()")
        val testData =
            db.parse(InputSource(StringReader(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<result xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                    "  <value name=\"user\">Some test value</value>\n" +
                    "</result>")))
        assertEquals("Some test value", expr.evaluate(testData))
    }

    @Test
    @Throws(JAXBException::class, XmlException::class)
    fun testXDefineHolder() {
        val testData = """<define 
            |    xmlns="http://adaptivity.nl/ProcessEngine/"
            |    xmlns:umh="http://adaptivity.nl/userMessageHandler" 
            |    path="/umh:bar/text()" />""".trimMargin()

        val testHolder = XML.decodeFromString<XmlDefineType>(testData)

        assertNotNull(SimpleNamespaceContext.from(testHolder.originalNSContext))
        assertEquals(
            USER_MESSAGE_HANDLER_NS, SimpleNamespaceContext.from(testHolder.originalNSContext)
                .getNamespaceURI("umh")
        )
    }


    @Test
    fun testXMLResultHolder() {
        val testData = "<result xmlns=\"http://adaptivity.nl/ProcessEngine/\" name=\"foo\" xmlns:umh=\"http://adaptivity.nl/userMessageHandler\" path=\"/umh:bar/text()\" />"

        val testHolder = XML.decodeFromString<XmlResultType>(testData)

        assertNotNull(SimpleNamespaceContext.from(testHolder.originalNSContext))
        assertEquals(
            USER_MESSAGE_HANDLER_NS,
            SimpleNamespaceContext.from(testHolder.originalNSContext).getNamespaceURI("umh")
        )
        assertEquals("foo" as Any, testHolder.getName())
    }

    @Test
    @Throws(Exception::class)
    fun testTaskResult() {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val document = db.newDocument()
        val expected = db.parse(
            InputSource(
                StringReader(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result xmlns=\"http://adaptivity.nl/userMessageHandler\">\n" +
                        "  <value name=\"user\">Some test value</value>\n" +
                        "</result>"
                            )
                       )
                               )

        val outer = document.createElementNS(USER_MESSAGE_HANDLER_NS, "result")
        outer.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", USER_MESSAGE_HANDLER_NS)
        val inner = document.createElementNS(USER_MESSAGE_HANDLER_NS, "value")
        inner.setAttribute("name", "user")
        inner.textContent = "Some test value"
        outer.appendChild(inner)
        val frag = document.createDocumentFragment()
        frag.appendChild(outer)

        assertXMLEqual(expected, frag)
        //    XMLAssert.assertXMLEqual(expected, document);

        val xPath = XPathFactory.newInstance().newXPath()
        val prefixMap = TreeMap<String, String>()
        prefixMap["ns1"] = USER_MESSAGE_HANDLER_NS
        xPath.namespaceContext = SimpleNamespaceContext(prefixMap)
        val expr = xPath.compile("./ns1:result/ns1:value[@name='user']/text()")
        assertEquals("Some test value", expr.evaluate(frag))
    }

}
