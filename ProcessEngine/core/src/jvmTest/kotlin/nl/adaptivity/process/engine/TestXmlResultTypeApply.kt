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

import nl.adaptivity.process.engine.impl.dom.toFragment
import nl.adaptivity.process.engine.processModel.applyData
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.*
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException

import javax.xml.XMLConstants
import javax.xml.bind.JAXBException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

import java.io.IOException
import java.io.StringReader
import java.util.TreeMap

import nl.adaptivity.process.util.Constants.USER_MESSAGE_HANDLER_NS
import nl.adaptivity.xmlutil.SimpleNamespaceContext.Companion
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull


/**
 * Created by pdvrieze on 24/08/15.
 */
class TestXmlResultTypeApply {

    private val db: DocumentBuilder
        @Throws(ParserConfigurationException::class)
        get() {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            return dbf.newDocumentBuilder()
        }

    @Test
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun testApplySimple() {
        val testData = CompactFragment("<result><value name='user'>Paul</value></result>")
        val xrt = XmlResultType("user", "/result/value[@name='user']/text()")

        val actual = xrt.applyData(testData)

        val expected = ProcessData("user", CompactFragment(emptyList(), "Paul".toCharArray()))
        assertEquals(expected.name, actual.name)
        assertEquals(expected.content, actual.content)
        //    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
    }

    @Test
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun testApplySimpleNS() {
        val testData = db.newDocument()
        val result = testData.createElementNS(USER_MESSAGE_HANDLER_NS, "umh:result")
        testData.appendChild(result)
        val value = result.appendChild(
            testData.createElementNS(USER_MESSAGE_HANDLER_NS, "umh:value")
                                      ) as Element
        value.setAttribute("name", "user")
        value.appendChild(testData.createTextNode("Paul"))

        assertEquals(
            "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>",
            DomUtil.toString(testData)
                    )


        val xrt = XmlResultType("user", "/*[local-name()='result']/*[@name='user']/text()")

        val expected = ProcessData("user", CompactFragment("Paul"))
        val actual = xrt.applyData(testData.toFragment())
        assertEquals(expected.name, actual.name)
        assertEquals(expected.content, actual.content)
        //    assertXMLEqual(XmlUtil.toString(expected.getDocumentFragment()), XmlUtil.toString(actual.getDocumentFragment()));
    }

}
