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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import net.devrieze.util.readString
import nl.adaptivity.process.engine.impl.dom.toDocumentFragment
import nl.adaptivity.process.engine.processModel.applyData
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.engine.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xmlunit.diff.*
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.reflect.KClass


/**
 * Created by pdvrieze on 24/08/15.
 */
@OptIn(XmlUtilInternal::class)
class TestEngineProcessData {

    @Test
    @Throws(Exception::class)
    fun testDeserializeProcessModel() {
        Logger.getAnonymousLogger().level = Level.ALL
        val pm = getProcessModel("testModel2.xml")
        val ac1: XmlActivity = pm.modelNodes["ac1"] as XmlActivity

        val result1 = ac1.results[0] as IPlatformXmlResultType

        val result2 = ac1.results[1] as IPlatformXmlResultType

        val testData = CompactFragment(
            "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>"
        )


        val result1Apply = result1.applyData(testData).content
        assertEquals("Paul", result1Apply.contentString)

        val result2Apply = result2.applyData(testData).content
        assertXmlEquals("<user><fullname>Paul</fullname></user>", result2Apply.contentString)

    }

    @Test
    fun testXmlResultXpathParam() {
        val nsContext = SimpleNamespaceContext(arrayOf("umh"), arrayOf("http://adaptivity.nl/userMessageHandler"))
        val expression = "/umh:result/umh:value[@name='user']/text()"
        val result = XmlResultType("foo", expression, null as CharArray?, nsContext)

        val testData = CompactFragment(
            "<umh:result xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><umh:value name=\"user\">Paul</umh:value></umh:result>"
        )
        val xPath = XPathFactory.newInstance().newXPath()
        xPath.namespaceContext = SimpleNamespaceContext.from(result.originalNSContext)
        val pathExpression = xPath.compile(expression)
        val apply2 = pathExpression.evaluate(testData.toDocumentFragment(), XPathConstants.NODESET) as NodeList
        assertNotNull(apply2)
        assertTrue(apply2.item(0) is Text)
        assertEquals("Paul", apply2.item(0).textContent)

        val apply3 = pathExpression.evaluate(testData.toDocumentFragment(), XPathConstants.NODE) as Node
        assertNotNull(apply3)
        assertTrue(apply3 is Text)
        assertEquals("Paul", apply3.textContent)

        val apply1 = result.applyData(testData)
        assertEquals("Paul", apply1.content.contentString)
    }

    @Test
    @Throws(XmlException::class, IOException::class, SAXException::class)
    fun testTransform() {
        val endpoint = ProcessData("endpoint", createEndpoint())
        val transformer = PETransformer.create(
            SimpleNamespaceContext.from(emptyList()),
            endpoint
        )
        val input = "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\">\n" +
            "  <jbi:element value=\"endpoint\"/>\n" +
            "</umh:postTask>"
        val cf = CompactFragment(
            SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MODIFY_NS_STR)),
            input.toCharArray()
        )
        val caw = CharArrayWriter()
        val out = XmlStreaming.newWriter(caw, true)
        transformer.transform(cf.getXmlReader(), out)
        out.close()
        run {
            val control =
                "<umh:postTask xmlns:umh=\"http://adaptivity.nl/userMessageHandler\"><jbi:endpointDescriptor xmlns:jbi=\"http://adaptivity.nl/jbi\" endpointLocation=\"http://localhost\" endpointName=\"internal\" serviceLocalName=\"foobar\" serviceNS=\"http://foo.bar\"/></umh:postTask>"
            val test = caw.toString()
            try {
                assertXmlEquals(control, test)
            } catch (e: SAXParseException) {
                assertEquals(control, test)
            } catch (e: AssertionError) {
                assertEquals(control, test)
            }
        }
    }

    companion object {

        private var _documentBuilder: DocumentBuilder? = null

        private val documentBuilder: DocumentBuilder
            get() {
                if (_documentBuilder == null) {
                    val dbf = DocumentBuilderFactory.newInstance()
                    try {
                        dbf.isNamespaceAware = true
                        dbf.isIgnoringElementContentWhitespace = false
                        dbf.isCoalescing = false
                        _documentBuilder = dbf.newDocumentBuilder()
                    } catch (e: ParserConfigurationException) {
                        throw RuntimeException(e)
                    }

                }
                return _documentBuilder!!
            }

        @Throws(FileNotFoundException::class)
        private fun getDocument(name: String): InputStream {
            return TestEngineProcessData::class.java.getResourceAsStream("/nl/adaptivity/process/engine/test/$name")
                ?: FileInputStream("src/jvmTest/resources/nl/adaptivity/process/engine/test/$name")
        }

        @BeforeAll
        private fun init() {
            XmlStreaming.setFactory(null) // make sure to have the default factory
        }

        @Throws(IOException::class, XmlException::class)
        private fun getProcessModel(name: String): XmlProcessModel {
            getDocument(name).use { inputStream ->
                val input = XmlStreaming.newReader(inputStream, "UTF-8")
                return XML { autoPolymorphic = true }.decodeFromReader(XmlProcessModel.serializer(), input)
            }
        }

        private fun createEndpoint(): CompactFragment {
            val namespaces = SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS_STR))
            val content = StringBuilder()
            content.append("<jbi:endpointDescriptor")
            content.append(" endpointLocation=\"http://localhost\"")
            content.append(" endpointName=\"internal\"")
            content.append(" serviceLocalName=\"foobar\"")
            content.append(" serviceNS=\"http://foo.bar\"")
            content.append(" />")
            return CompactFragment(namespaces, content.toString().toCharArray())

        }

        @Throws(IOException::class, IllegalAccessException::class, InstantiationException::class, XmlException::class)
        fun <T : Any> testRoundTrip(
            reader: InputStream, target: KClass<out T>,
            serializer: KSerializer<T>,
            serialModule: SerializersModule,
            testObject: (T) -> Unit = {}
        ): String {
            val expected: String
            val streamReaderFactory: () -> XmlReader
            if (reader.markSupported()) {
                reader.mark(Int.MAX_VALUE)
                expected = reader.readString(Charset.defaultCharset())
                streamReaderFactory = {
                    reader.reset()
                    XmlStreaming.newReader(reader, Charset.defaultCharset().toString())
                }
            } else {
                expected = reader.readString(Charset.defaultCharset())
                streamReaderFactory = { XmlStreaming.newReader(StringReader(expected)) }
            }

            return testRoundTripCombined<T>(
                expected, streamReaderFactory, target, serializer = serializer,
                serialModule = serialModule,
                testObject = testObject
            )
        }

        @Throws(
            IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class
        )
        fun <T : Any> testRoundTrip(
            xml: String, target: KClass<out T>,
            serializer: KSerializer<T>,
            serialModule: SerializersModule,
            testObject: (T) -> Unit = {}
        ): String {
            return testRoundTripCombined(
                xml, { XmlStreaming.newReader(StringReader(xml)) }, target,
                serializer = serializer,
                serialModule = serialModule,
                testObject = testObject
            )
        }

        @Suppress("unused")
        @Throws(
            IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class
        )
        fun <T : Any> testRoundTrip(
            xml: String, target: KClass<out T>,
            serializer: KSerializer<T>,
            serialModule: SerializersModule = EmptySerializersModule,
            repairNamespaces: Boolean = false,
            omitXmlDecl: Boolean = true,
            testObject: (T) -> Unit = {}
        ): String {
            return testRoundTripCombined(
                xml, { XmlStreaming.newReader(StringReader(xml)) }, target, serializer = serializer,
                serialModule = serialModule,
                repairNamespaces = repairNamespaces,
                omitXmlDecl = omitXmlDecl
            )
        }

        @Throws(
            IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class
        )
        fun <T : Any> testRoundTrip(
            xml: String, target: KClass<out T>,
            serializer: KSerializer<T>,
            @Suppress("UNUSED_PARAMETER") ignoreNs: Boolean,
            serialModule: SerializersModule = EmptySerializersModule,
            testObject: (T) -> Unit = {}
        ): String {
            return testRoundTripCombined(
                xml, { XmlStreaming.newReader(StringReader(xml)) }, target,
                serializer = serializer,
                serialModule = serialModule,
                testObject = testObject
            )
        }

        @Suppress("unused")
        @Throws(
            IllegalAccessException::class, InstantiationException::class, XmlException::class, IOException::class,
            SAXException::class
        )
        fun <T : Any> testRoundTrip(
            xml: String, target: KClass<out T>,
            serializer: KSerializer<T>,
            @Suppress("UNUSED_PARAMETER") ignoreNs: Boolean,
            serialModule: SerializersModule,
            repairNamespaces: Boolean = false,
            omitXmlDecl: Boolean = true,
            testObject: (T) -> Unit = {}
        ): String {
            return testRoundTripCombined(
                xml, { XmlStreaming.newReader(StringReader(xml)) }, target, serializer,
                serialModule,
                repairNamespaces = repairNamespaces,
                omitXmlDecl = omitXmlDecl
            )
        }

        private inline fun <T : Any> testRoundTripCombined(
            expected: String,
            readerFactory: () -> XmlReader,
            target: KClass<out T>,
            serializer: KSerializer<T>,
            serialModule: SerializersModule,
            repairNamespaces: Boolean = false,
            omitXmlDecl: Boolean = true,
            noinline testObject: (T) -> Unit = {}
        ): String {
            val new = testRoundTripSer(
                expected,
                readerFactory(),
                serializer,
                serialModule,
                repairNamespaces,
                omitXmlDecl,
                testObject
            )

            return new
        }

        @Throws(InstantiationException::class, IllegalAccessException::class, XmlException::class)
        private fun <T : Any> testRoundTripSer(
            expected: String,
            reader: XmlReader,
            target: KSerializer<T>,
            serialModule: SerializersModule,
            repairNamespaces: Boolean = false,
            omitXmlDecl: Boolean = true,
            testObject: (T) -> Unit = {}
        ): String {
            assertNotNull(reader)
            val xml = XML(serialModule) {
                this.repairNamespaces = repairNamespaces
                this.omitXmlDecl = omitXmlDecl
                this.indent = 4
                this.autoPolymorphic = true
            }
            val obj = xml.decodeFromReader(target, reader)
            testObject(obj)

            val actual = xml.encodeToString(target, obj)

            assertXmlEquals(expected, actual)

            return actual
        }


        @Deprecated("Use arrayOf", ReplaceWith("arrayOf(value1, value2)"))
        private fun toArray(value1: Any, value2: Any) = arrayOf(value1, value2)

        @Deprecated("Use arrayOf", ReplaceWith("arrayOf(value1)"))
        private fun toArray(value1: Any) = arrayOf(value1)

        @Deprecated("Use arrayOf", ReplaceWith("arrayOf(*value)"))
        private fun toArray(vararg value: Any) = arrayOf(value)
    }

}

val NAMESPACE_DIFF_EVAL: DifferenceEvaluator = DifferenceEvaluator { comparison, outcome ->
    when {
        outcome == ComparisonResult.DIFFERENT &&
            comparison.type == ComparisonType.NAMESPACE_PREFIX -> ComparisonResult.SIMILAR

        else                                                   -> DifferenceEvaluators.Default.evaluate(
            comparison,
            outcome
        )
    }
}
