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

import net.devrieze.util.*
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.util.CombiningNamespaceContext
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XmlEvent.EndElementEvent
import nl.adaptivity.xmlutil.XmlEvent.StartElementEvent
import nl.adaptivity.xmlutil.XmlEvent.TextEvent
import nl.adaptivity.xmlutil.EventType
import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import javax.xml.XMLConstants
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.xpath.*

import java.util.*


class PETransformer private constructor(private val context: PETransformerContext,
                                        private val namespaceContext: NamespaceContext?,
                                        private val isRemoveWhitespace: Boolean) {


    class MyFilter(private val context: PETransformerContext,
                   private val filterNamespaceContext: NamespaceContext?,
                   delegate: XmlReader,
                   private val isRemoveWhitespace: Boolean) : XmlBufferedReader(delegate) {


        public override fun doPeek(): List<XmlEvent> {
            val results = ArrayList<XmlEvent>(1)

            doPeek(results)
            return results
        }

        private fun doPeek(results: MutableList<XmlEvent>) {
            val events = super.doPeek()

            loop@for (event in events) {
                when (event.eventType) {
                    EventType.START_ELEMENT        -> peekStartElement(results, event as StartElementEvent)
                    EventType.TEXT                 -> {
                        val text = event as TextEvent
                        if (!isIgnorableWhiteSpace(text)) {
                            results.add(event)
                            continue@loop
                        }
                        // fall through if ignorable
                        if (!isRemoveWhitespace) {
                            results.add(event)
                        }
                        doPeek(results) //peek again, as sometimes whitespace needs to be stripped to add attributes
                        // fall through if not whitespace
                    }
                    EventType.IGNORABLE_WHITESPACE -> {
                        if (!isRemoveWhitespace) {
                            results.add(event)
                        }
                        doPeek(results)
                    }
                    else                           -> results.add(event)
                }
            }
        }


        private fun stripWhiteSpaceFromPeekBuffer(results: MutableList<XmlEvent>) {
            while (results.size > 0 && ((results.last() as? TextEvent)?.let { isXmlWhitespace(it.text) } == true)) {
                results.removeAt(results.size - 1)
            }
        }

        private fun peekStartElement(results: MutableList<XmlEvent>, element: StartElementEvent) {
            if (Constants.MODIFY_NS_STR == element.namespaceUri) {
                val localname = element.localName

                val attributes = parseAttributes(element)

                when (localname) {
                    "attribute" -> {
                        stripWhiteSpaceFromPeekBuffer(results)
                        results.add(getAttribute(attributes))
                        readEndTag(element)
                        return
                    }
                    "element"   -> {
                        processElement(results, element, attributes, false)
                        readEndTag(element)
                        return
                    }
                    "value"     -> {
                        processElement(results, element, attributes, true)
                        readEndTag(element)
                        return
                    }
                    else        -> throw XmlException("Unsupported element: {${element.namespaceUri}}${element.localName}")
                }
            } else {
                var filterAttributes = false
                val newAttrs = ArrayList<XmlEvent.Attribute>()
                for (attr in element.attributes) {
                    if (attr.hasNamespaceUri() && Constants.MODIFY_NS_STR == attr.value) {
                        filterAttributes = true
                    } else {
                        newAttrs.add(attr)
                    }
                }
                val newNamespaces = ArrayList<Namespace>()
                for (ns in element.namespaceDecls) {
                    if (Constants.MODIFY_NS_STR == ns.namespaceURI) {
                        filterAttributes = true
                    } else {
                        newNamespaces.add(ns)
                    }
                }
                if (filterAttributes) {
                    results.add(StartElementEvent(element.locationInfo, element.namespaceUri, element.localName,
                                                  element.prefix,
                                                  newAttrs.toTypedArray(),
                                                  newNamespaces.toTypedArray()))
                } else {
                    results.add(element)
                }
            }
        }

        private fun readEndTag(name: StartElementEvent) {
            while (true) {
                val elems = super.doPeek()
                loop@for (elem in elems) {
                    when (elem.eventType) {
                        EventType.IGNORABLE_WHITESPACE, EventType.COMMENT -> {
                        }
                        EventType.TEXT                                    -> {
                            if (isXmlWhitespace((elem as TextEvent).text)) {
                                continue@loop
                            }
                            if (!(elem.eventType === EventType.END_ELEMENT && name.isEqualNames(
                                            elem as EndElementEvent))) {
                                throw XmlException(
                                        "Unexpected tag found ($elem) when expecting an end tag for $name")
                            }
                            return
                        }
                        else                                              -> {
                            if (!(elem.eventType === EventType.END_ELEMENT && name.isEqualNames(
                                            elem as EndElementEvent))) {
                                throw XmlException(
                                        "Unexpected tag found ($elem) when expecting an end tag for $name")
                            }
                            return
                        }
                    }
                }
            }
        }

        private fun processElement(results: MutableList<XmlEvent>,
                                   event: StartElementEvent,
                                   attributes: Map<String, CharSequence>,
                                   hasDefault: Boolean) {
            val valueName = attributes["value"]
            val xpath = attributes["xpath"]
            try {
                if (valueName == null) {
                    if (hasDefault) {
                        addAllRegular(results,
                                      applyXpath(event.namespaceContext, context.resolveDefaultValue(), xpath))
                    } else {
                        throw XmlException("This context does not allow for a missing value parameter")
                    }
                } else {
                    addAllRegular(results,
                                  applyXpath(event.namespaceContext, context.resolveElementValue(valueName), xpath))
                }
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            } catch (e: ParserConfigurationException) {
                throw RuntimeException(e)
            }

        }

        private fun addAllRegular(target: MutableList<XmlEvent>, source: Iterable<XmlEvent>) {
            for (event in source) {
                if (!event.isIgnorable) {
                    target.add(event)
                }
            }
        }

        @Throws(XPathExpressionException::class, ParserConfigurationException::class)
        private fun applyXpath(namespaceContext: NamespaceContext,
                               pendingEvents: List<XmlEvent>,
                               xpath: CharSequence?): Collection<XmlEvent> {
            val xpathstr = toString(xpath)
            if (xpathstr == null || "." == xpathstr) {
                return pendingEvents
            }
            // TODO add a function resolver
            val rawPath = XPathFactory.newInstance().newXPath()
            // Do this better
            if (filterNamespaceContext == null) {
                rawPath.namespaceContext = namespaceContext
            } else {
                rawPath.namespaceContext = CombiningNamespaceContext(namespaceContext, filterNamespaceContext)
            }
            val xpathexpr = rawPath.compile(xpathstr)
            val result = ArrayList<XmlEvent>()
            val xof = XMLOutputFactory.newFactory()
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            val db = dbf.newDocumentBuilder()
            val eventFragment = db.newDocument().createDocumentFragment()
            val domResult = DOMResult(eventFragment)

            val writer = XmlStreaming.newWriter(domResult)
            for (event in pendingEvents) {
                event.writeTo(writer)
            }
            writer.close()
            val applicationResult = xpathexpr.evaluate(eventFragment, XPathConstants.NODESET) as NodeList
            if (applicationResult.length > 0) {
                result.addAll(toEvents(ProcessData("--xpath result--", DomUtil.nodeListToFragment(applicationResult))))
            }
            return result
        }

        private fun parseAttributes(startElement: StartElementEvent): Map<String, CharSequence> {
            val result = TreeMap<String, CharSequence>()

            for (attribute in startElement.attributes) {
                result[attribute.localName] = attribute.value
            }
            return result
        }

        private fun getAttribute(attributes: Map<String, CharSequence>): XmlEvent {
            val valueName = toString(attributes["value"])
            val xpath = attributes["xpath"]
            var paramName: CharSequence? = attributes["name"]

            if (valueName != null) {
                if (paramName == null) {
                    paramName = context.resolveAttributeName(valueName)
                }
                val value = context.resolveAttributeValue(valueName, toString(xpath))
                return XmlEvent.Attribute(null, XMLConstants.NULL_NS_URI, paramName, XMLConstants.DEFAULT_NS_PREFIX,
                                          value)
            } else {
                throw MessagingFormatException("Missing parameter name")
            }
        }
    }


    interface PETransformerContext {
        fun resolveElementValue(valueName: CharSequence): List<XmlEvent>
        fun resolveDefaultValue(): List<XmlEvent>  //Just return DOM, not events (that then need to be dom-ified)
        fun resolveAttributeValue(valueName: String, xpath: String?): String
        fun resolveAttributeName(valueName: String): String

    }

    abstract class AbstractDataContext : PETransformerContext {

        protected abstract fun getData(valueName: String): ProcessData?

        override fun resolveElementValue(valueName: CharSequence): List<XmlEvent> {
            val data = getData(valueName.toString()) ?: throw IllegalArgumentException(
                    "No value with name $valueName found")
            return toEvents(data)
        }

        override fun resolveAttributeValue(valueName: String, xpath: String?): String {
            val data = getData(valueName) ?: throw IllegalArgumentException("No data value with name $valueName found")
            val dataReader = data.contentStream
            val result = StringBuilder()
            try {
                while (dataReader.hasNext()) {
                    val event = dataReader.next()
                    when (event) {
                        EventType.ATTRIBUTE, EventType.DOCDECL, EventType.START_ELEMENT                                       -> throw XmlException(
                                "Unexpected node found while resolving attribute. Only CDATA allowed: (" + event.javaClass.simpleName + ") " + event)
                        EventType.CDSECT, EventType.TEXT                                                                      -> {
                            if (!isIgnorableWhiteSpace(dataReader)) {
                                result.append(dataReader.text)
                            }
                        }
                        EventType.START_DOCUMENT, EventType.END_DOCUMENT, EventType.COMMENT, EventType.PROCESSING_INSTRUCTION -> {
                        }
                        EventType.END_ELEMENT // finished the element
                                                                                                                              -> return result.toString()
                        else                                                                                                  -> throw XmlException(
                                "Unexpected node type: $event")
                    }// ignore
                }
            } catch (e: XmlException) {
                throw XmlException("Failure to parse data (name=$valueName, value=${data.content.contentString})", e)
            }

            return result.toString()
        }

        override fun resolveAttributeName(valueName: String): String {
            val data = getData(valueName)!!
            return String(data.content.content)
        }

    }

    class ProcessDataContext : AbstractDataContext {

        private var processData: Array<ProcessData>
        private var defaultIdx: Int = 0

        constructor(vararg processData: ProcessData) {
            this.processData = Array(processData.size) { processData[it] }
            defaultIdx = if (processData.size == 1) 0 else -1
        }

        constructor(defaultIdx: Int, vararg processData: ProcessData) {
            assert(defaultIdx >= -1 && defaultIdx < processData.size)
            this.processData = Array(processData.size) { processData[it] }
            this.defaultIdx = defaultIdx
        }

        protected override fun getData(valueName: String): ProcessData? {
            for (candidate in processData) {
                if (valueName == candidate.name) {
                    return candidate
                }
            }
            return null
        }

        override fun resolveDefaultValue(): List<XmlEvent> {
            return when {
                processData.isEmpty() -> emptyList()
                else                  -> toEvents(processData[defaultIdx])
            }
        }

    }

    fun transform(content: List<*>): List<Node> {
        try {
            var document: Document? = null
            val result = ArrayList<Node>(content.size)
            for (obj in content) {
                if (obj is CharSequence) {
                    if (document == null) {
                        val dbf = DocumentBuilderFactory.newInstance()
                        dbf.isNamespaceAware = true
                        document = dbf.newDocumentBuilder().newDocument()
                    }
                    result.add(document!!.createTextNode(obj.toString()))
                } else if (obj is Node) {
                    if (document == null) {
                        document = obj.ownerDocument
                    }
                    val v = transform(obj)
                    if (v != null) {
                        result.add(v)
                    }
                } else if (obj is JAXBElement<*>) {
                    if (document == null) {
                        val dbf = DocumentBuilderFactory.newInstance()
                        dbf.isNamespaceAware = true
                        document = dbf.newDocumentBuilder().newDocument()
                    }
                    val jbe = obj as JAXBElement<*>?
                    val df = document!!.createDocumentFragment()
                    val domResult = DOMResult(df)
                    JAXB.marshal(jbe!!, domResult)
                    var n: Node? = df.firstChild
                    while (n != null) {
                        val v = transform(n)
                        if (v != null) {
                            result.add(v)
                        }
                        n = n.nextSibling
                    }
                } else if (obj != null) {
                    throw IllegalArgumentException(
                            "The node " + obj.toString() + " of type " + obj.javaClass + " is not understood")
                }
            }
            return result
        } catch (e: ParserConfigurationException) {
            throw XmlException(e)
        }

    }

    fun transform(node: Node): DocumentFragment? {
        val dbf = DocumentBuilderFactory.newInstance()
        val document: Document
        try {
            document = dbf.newDocumentBuilder().newDocument()
            val fragment = document.createDocumentFragment()
            val result = DOMResult(fragment)
            transform(DOMSource(node), result)
            return fragment
        } catch (e: ParserConfigurationException) {
            throw XmlException(e)
        }

    }

    fun transform(source: Source, result: Result) {
        transform(XmlStreaming.newReader(source), XmlStreaming.newWriter(result, true))
    }

    fun transform(`in`: XmlReader, out: XmlWriter) {
        val filteredIn = createFilter(`in`)
        while (filteredIn.hasNext()) {
            filteredIn.next() // Don't forget to move to next element as well.
            filteredIn.writeCurrent(out)
        }
    }

    fun createFilter(input: XmlReader): XmlReader {
        return MyFilter(context, namespaceContext, input, isRemoveWhitespace)
    }

    companion object {

        fun create(namespaceContext: NamespaceContext?,
                   removeWhitespace: Boolean,
                   vararg processData: ProcessData): PETransformer {
            return PETransformer(ProcessDataContext(*processData), namespaceContext, removeWhitespace)
        }

        @Deprecated("")
        fun create(removeWhitespace: Boolean, vararg processData: ProcessData): PETransformer {
            return create(null, removeWhitespace, *processData)
        }

        fun create(namespaceContext: NamespaceContext, vararg processData: ProcessData): PETransformer {
            return create(namespaceContext, true, *processData)
        }

        @Deprecated("")
        fun create(vararg processData: ProcessData): PETransformer {
            return create(null, true, *processData)
        }

        @Deprecated("")
        fun create(context: PETransformerContext): PETransformer {
            return create(context, true)
        }

        fun create(context: PETransformerContext, removeWhitespace: Boolean): PETransformer {
            return create(null, context, removeWhitespace)
        }

        @JvmOverloads
        fun create(namespaceContext: NamespaceContext?,
                   context: PETransformerContext,
                   removeWhitespace: Boolean = true): PETransformer {
            return PETransformer(context, namespaceContext, removeWhitespace)
        }

        protected fun toEvents(data: ProcessData): List<XmlEvent> {
            val result = ArrayList<XmlEvent>()

            val frag = data.contentStream
            while (frag.hasNext()) {
                frag.next()
                result.add(XmlEvent.from(frag))
            }
            return result
        }

        internal fun isIgnorableWhiteSpace(characters: TextEvent): Boolean {
            return if (characters.eventType === EventType.IGNORABLE_WHITESPACE) {
                true
            } else isXmlWhitespace(characters.text)
        }

        internal fun isIgnorableWhiteSpace(characters: XmlReader): Boolean {
            return if (characters.eventType === EventType.IGNORABLE_WHITESPACE) {
                true
            } else isXmlWhitespace(characters.text)
        }
    }
}
