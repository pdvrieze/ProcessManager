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

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.XmlDelegatingWriter


/**
 * A writer for debugging that writes all events to stdout as well
 * Created by pdvrieze on 05/12/15.
 */
class DebugWriter(delegate: XmlWriter) : XmlDelegatingWriter(delegate) {

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        println(TAG + "startTag(namespace='" + namespace + "', localName='" + localName + "', prefix='" + prefix + "')")
        super.startTag(namespace, localName, prefix)
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        println(TAG + "endTag(namespace='" + namespace + "', localName='" + localName + "', prefix='" + prefix + "')")
        super.endTag(namespace, localName, prefix)
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        println("$TAG  attribute(namespace='$namespace', name='$name', prefix='$prefix', value='$value')")
        super.attribute(namespace, name, prefix, value)
    }

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        println("$TAG  namespaceAttr(namespacePrefix='$namespacePrefix', namespaceUri='$namespaceUri')")
        super.namespaceAttr(namespacePrefix, namespaceUri)
    }

    override fun text(text: String) {
        println("$TAG--text('$text')")
        super.text(text)
    }

    override fun ignorableWhitespace(text: String) {
        println("$TAG  ignorableWhitespace()")
        super.ignorableWhitespace(text)
    }

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        println(TAG + "startDocument()")
        super.startDocument(version, encoding, standalone)
    }

    override fun comment(text: String) {
        println(TAG + "comment('" + text + "')")
        super.comment(text)
    }

    override fun processingInstruction(text: String) {
        println(TAG + "processingInstruction('" + text + "')")
        super.processingInstruction(text)
    }

    override fun close() {
        println(TAG + "close()")
        super.close()
    }

    override fun flush() {
        println(TAG + "flush()")
        super.flush()
    }

    override fun endDocument() {
        println(TAG + "endDocument()")
        super.endDocument()
    }

    companion object {

        private val TAG = "DEBUGWRITER: "
    }
}
