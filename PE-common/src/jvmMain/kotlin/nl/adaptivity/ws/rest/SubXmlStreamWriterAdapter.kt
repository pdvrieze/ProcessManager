/*
 * Copyright (c) 2022.
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

package nl.adaptivity.ws.rest

import nl.adaptivity.xmlutil.XmlWriter
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLStreamWriter

internal fun XmlWriter.asXmlStreamWriter(): XMLStreamWriter = SubXmlStreamWriterAdapter(this)

/**
 * This class forwards to an XmlWriter, but ignores start document/end document events as it is
 * intended to be used outside of document context.
 */
private class SubXmlStreamWriterAdapter(private val delegate: XmlWriter): XMLStreamWriter {

    private val tagStack: MutableList<Triple<String?, String, String?>> = ArrayDeque()

    override fun writeStartDocument() {}

    override fun writeStartDocument(version: String?) {}

    override fun writeStartDocument(encoding: String?, version: String?) {}

    override fun getProperty(name: String): Nothing {
        throw IllegalArgumentException("Properties not supported")
    }

    override fun flush() = delegate.flush()

    override fun close() {
        delegate.close()
    }

    override fun setNamespaceContext(context: NamespaceContext?) {
        throw UnsupportedOperationException("Setting the namespace context is not supported")
    }

    override fun getNamespaceContext(): NamespaceContext {
        return delegate.namespaceContext
    }

    override fun writeDTD(dtd: String) = delegate.docdecl(dtd)

    override fun writeProcessingInstruction(target: String) {
        delegate.processingInstruction(target)
    }

    override fun writeProcessingInstruction(target: String?, data: String?) {
        delegate.processingInstruction("$target $data")
    }

    override fun getPrefix(uri: String?): String? {
        return delegate.getPrefix(uri ?: "")
    }

    override fun setPrefix(prefix: String, uri: String) {
        delegate.setPrefix(prefix, uri)
    }

    override fun setDefaultNamespace(uri: String) {
        delegate.setPrefix("", uri)
    }

    override fun writeStartElement(localName: String) {
        tagStack.add(Triple(null, localName, null))
        delegate.startTag(null, localName, null)
    }

    override fun writeStartElement(namespaceURI: String, localName: String) {
        val prefix = getPrefix(namespaceURI)
        tagStack.add(Triple(namespaceURI, localName, prefix))
        delegate.startTag(namespaceURI, localName, prefix)
    }

    override fun writeStartElement(prefix: String, localName: String, namespaceURI: String) {
        tagStack.add(Triple(namespaceURI, localName, prefix))
        delegate.startTag(namespaceURI, localName, prefix)
    }

    override fun writeEmptyElement(namespaceURI: String, localName: String) {
        val prefix = getPrefix(namespaceURI)
        tagStack.add(Triple(namespaceURI, localName, prefix))
        delegate.startTag(namespaceURI, localName, prefix)
    }

    override fun writeEmptyElement(prefix: String, localName: String, namespaceURI: String) {
        tagStack.add(Triple(namespaceURI, localName, prefix))
        delegate.startTag(namespaceURI, localName, prefix)
    }

    override fun writeEmptyElement(localName: String) {
        tagStack.add(Triple(null, localName, null))
        delegate.startTag(null, localName, null)
    }

    override fun writeAttribute(localName: String, value: String) {
        delegate.attribute(null, localName, null, value)
    }

    override fun writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String) {
        delegate.attribute(namespaceURI, localName, prefix, value)
    }

    override fun writeAttribute(namespaceURI: String?, localName: String, value: String) {
        delegate.attribute(namespaceURI, localName, null, value)
    }

    override fun writeNamespace(prefix: String, namespaceURI: String) {
        delegate.namespaceAttr(prefix, namespaceURI)
    }

    override fun writeDefaultNamespace(namespaceURI: String) {
        delegate.namespaceAttr("", namespaceURI)
    }

    override fun writeCharacters(text: String) {
        delegate.text(text)
    }

    override fun writeCharacters(text: CharArray, start: Int, len: Int) {
        delegate.text(String(text, start, len))
    }

    override fun writeComment(data: String) {
        delegate.comment(data)
    }

    override fun writeCData(data: String) {
        delegate.cdsect(data)
    }

    override fun writeEntityRef(name: String) {
        delegate.entityRef(name)
    }

    override fun writeEndElement() {
        val (ns, localname, prefix) = tagStack.removeLast()
        delegate.endTag(ns, localname, prefix)
    }

    override fun writeEndDocument() {}
}
