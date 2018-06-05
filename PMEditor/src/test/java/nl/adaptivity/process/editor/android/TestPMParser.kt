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

package nl.adaptivity.process.editor.android

import net.devrieze.util.readString
import net.devrieze.util.toString
import nl.adaptivity.diagram.Positioned
import nl.adaptivity.process.diagram.*
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.tasks.PostTask
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import org.custommonkey.xmlunit.XMLAssert
import org.custommonkey.xmlunit.XMLUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.w3.soapEnvelope.Envelope
import org.xml.sax.InputSource
import java.io.CharArrayReader
import java.io.CharArrayWriter
import java.io.StringReader
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Created by pdvrieze on 13/11/15.
 */
class TestPMParser {

    @Before
    fun init() {
        XmlStreaming.setFactory(AndroidStreamingFactory())
    }

    @Test
    fun testParseNew() {
        val inputStream = javaClass.getResourceAsStream("/processmodel.xml")
        val parser = AndroidXmlReader(inputStream, "UTF-8")
        val deserializedModel = XmlProcessModel.deserialize(parser)
        val model = RootDrawableProcessModel(
            deserializedModel.rootModel)
        checkModel1(model)
    }

    @Test
    fun testParseSimple() {
        val inputStream = javaClass.getResourceAsStream("/processmodel.xml")
        val reader = AndroidXmlReader(inputStream, "UTF-8")
        val model = PMParser.parseProcessModel(reader, LayoutAlgorithm.nullalgorithm<Positioned>(),
                                               LayoutAlgorithm.nullalgorithm<Positioned>()).build()
        checkModel1(model)

    }

    @Test
    fun testNsIsue() {
        val inputStream = javaClass.getResourceAsStream("/namespaceIssueModel.xml")
        val expected = javaClass.getResourceAsStream("/namespaceIssueModel_expected.xml").readString(Charset.defaultCharset())
        val documentBuilder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        val expectedDoc = documentBuilder.parse(InputSource(StringReader(expected)))
        val parser = AndroidXmlReader(inputStream, "UTF-8")
        val out = CharArrayWriter()
        val writer = AndroidXmlWriter(out, repairNamespaces = true)
        writer.serialize(parser)
        val outDOC = documentBuilder.parse(InputSource(CharArrayReader(out.toCharArray())))

        try {
            XMLAssert.assertXMLEqual(expectedDoc, outDOC)
        } catch (e: AssertionError) {
            assertEquals(expected, out.toString())
        }

    }

    @Test
    fun testRoundTripSoapMessage() {
        val source = toString(javaClass.getResourceAsStream("/message.xml"), Charset.forName("UTF-8"))

        val parser = AndroidXmlReader(StringReader(source))
        val msg = XmlMessage.deserialize(parser)

        val out = toString(msg)


        try {
            XMLAssert.assertXMLEqual(source, out)
        } catch (e: AssertionError) {
            assertEquals(source, out)
        }

        val bodySource = msg.messageBody.contentString
        val bodyParser = msg.bodyStreamReader
        val pt = Envelope.deserialize<PostTask>(bodyParser, PostTask.FACTORY)
        val bodyOut = toString(pt)
        XMLUnit.setIgnoreWhitespace(true)
        XMLUnit.setIgnoreComments(true)
        XMLUnit.setIgnoreAttributeOrder(true)

        try {
            XMLAssert.assertXMLEqual(bodySource, bodyOut)
        } catch (e: AssertionError) {
            assertEquals(bodyOut, bodySource)
        }

    }

    private fun checkModel1(model: DrawableProcessModel) {
        assertNotNull(model)

        assertEquals("There should be 9 effective elements in the process model (including an introduced split)", 9,
                     model.childElements.size)
        val start = model.getNode("start") as DrawableStartNode
        val ac1 = model.getNode("ac1") as DrawableActivity
        val ac2 = model.getNode("ac2") as DrawableActivity
        val ac3 = model.getNode("ac3") as DrawableActivity
        val ac4 = model.getNode("ac4") as DrawableActivity
        val ac5 = model.getNode("ac5") as DrawableActivity
        val split = model.getNode("split1") as DrawableSplit
        val j1 = model.getNode("j1") as DrawableJoin
        val end = model.getNode("end") as DrawableEndNode
        val actualNodes = model.childElements
        val expectedNodes = listOf(start, ac1, ac2,
                                   split, ac3, ac5,
                                   j1, ac4, end)
        assertEquals(actualNodes.size, expectedNodes.size)
        assertTrue(actualNodes.containsAll(expectedNodes))

        assertEqualIds(emptyArray(), start.predecessors.toTypedArray())
        assertEqualIds(arrayOf(ac1), start.successors.toTypedArray())

        assertEqualIds(arrayOf(start), ac1.predecessors.toTypedArray())
        assertEqualIds(arrayOf(split), ac1.successors.toTypedArray())

        assertEqualIds(arrayOf(ac1), split.predecessors.toTypedArray())
        assertEqualIds(arrayOf(ac2, ac3), split.successors.toTypedArray())

        assertEqualIds(arrayOf(split), ac2.predecessors.toTypedArray())
        assertEqualIds(arrayOf(j1), ac2.successors.toTypedArray())

        assertEqualIds(arrayOf(split), ac3.predecessors.toTypedArray())
        assertEqualIds(arrayOf(ac5), ac3.successors.toTypedArray())

        assertEqualIds(arrayOf(j1), ac4.predecessors.toTypedArray())
        assertEqualIds(arrayOf(end), ac4.successors.toTypedArray())

        assertEqualIds(arrayOf(ac3), ac5.predecessors.toTypedArray())
        assertEqualIds(arrayOf(j1), ac5.successors.toTypedArray())

        assertEqualIds(arrayOf(ac4), end.predecessors.toTypedArray())
        assertEqualIds(emptyArray(), end.successors.toTypedArray())
    }

    @Test
    @Throws(XmlException::class)
    fun testWriter() {
        val caw = CharArrayWriter()
        val writer = XmlStreaming.newWriter(caw)
        testWriterCommon(writer)
        assertEquals(caw.toString(), "<prefix:tag>Hello</prefix:tag>")
    }

    @Test
    @Throws(XmlException::class)
    fun testWriterRepairing() {
        val caw = CharArrayWriter()
        val writer = XmlStreaming.newWriter(caw, true)
        testWriterCommon(writer)
        assertEquals(caw.toString(), "<prefix:tag xmlns:prefix=\"urn:foo\">Hello</prefix:tag>")
    }

    @Throws(XmlException::class)
    private fun testWriterCommon(writer: XmlWriter) {
        writer.setPrefix("bar", "urn:bar")
        writer.startTag("urn:foo", "tag", "prefix")
        writer.text("Hello")
        writer.endTag("urn:foo", "tag", "prefix")
        writer.close()
    }

    @Deprecated("Use kotlin", ReplaceWith("emptyArray()"))
    private fun toArray(): Array<out Any> = emptyArray()

    @Deprecated("Use kotlin", ReplaceWith("value"))
    private fun toArray(vararg value: Any): Array<out Any> {
        return value
    }

    private fun assertEqualIds(expected: Array<Identifiable>, actual: Array<Identifiable>) {
        assertEquals(expected.map { it.id }, actual.map { it.id })
    }

}
