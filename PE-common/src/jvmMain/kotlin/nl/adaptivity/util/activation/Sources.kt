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

package nl.adaptivity.util.activation


import net.devrieze.util.readString
import java.io.*
import javax.xml.bind.util.JAXBSource
import javax.xml.transform.*
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

@JvmOverloads
fun Source.writeToStream(outputStream: OutputStream, indent: Boolean = false) {
    this.writeToResult(StreamResult(outputStream), indent)
}

@JvmOverloads
fun Source.writeToWriter(writer: Writer, indent: Boolean = false) {
    this.writeToResult(StreamResult(writer), indent)
}

@JvmOverloads
fun Source.writeToResult(result: Result, indent: Boolean = false) {
    val factory = TransformerFactory.newInstance()
    val identityTransformer = factory.newTransformer()
    if (indent) {
        identityTransformer.setOutputProperty(OutputKeys.INDENT, "yes")
        identityTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    }
    identityTransformer.transform(this, result)
}

fun Source.toInputStream(): InputStream {
    if (this is StreamSource) {
        inputStream?.let { return it }
    }
    if (this is SAXSource && this !is JAXBSource) {
        inputSource.byteStream?.let { return it }
    }

    val byteArray = ByteArrayOutputStream().also { baos ->
        writeToStream(baos)
    }.toByteArray()
    return ByteArrayInputStream(byteArray)
}

fun Source.toReader(): Reader {
    if (this is StreamSource) {
        reader?.let { return it }
    }
    if (this is SAXSource && this !is JAXBSource) {
        inputSource.byteStream?.let { return InputStreamReader(it) }
    }

    val charArray = CharArrayWriter().also { caw -> writeToWriter(caw) }.toCharArray()
    return CharArrayReader(charArray)

}

@JvmOverloads
fun Source.toString(indent: Boolean = false): String {

    if (this is StreamSource) {
        reader?.let { return it.readString() }
    }
    if (this is SAXSource && this !is JAXBSource) {
        inputSource.byteStream?.let { return InputStreamReader(it).readString() }
    }

    return StringWriter().also { writer ->
        writeToWriter(writer, indent)
    }.toString()
}

object Sources {

    @Deprecated("Use extension function", ReplaceWith("source.writeToStream(outputStream)"))
    fun writeToStream(source: Source, outputStream: OutputStream) {
        source.writeToStream(outputStream)
    }

    @Deprecated("Use extension function", ReplaceWith("source.writeToStream(outputStream, indent)"))
    fun writeToStream(source: Source, outputStream: OutputStream, indent: Boolean) {
        source.writeToStream(outputStream, indent)
    }

    @Deprecated("Use extension function", ReplaceWith("source.writeToWriter(writer, indent)"))
    @JvmOverloads
    fun writeToWriter(source: Source, writer: Writer, indent: Boolean = false) {
        source.writeToWriter(writer, indent)
    }

    @Deprecated("Use extension function", ReplaceWith("source.writeToResult(result, indent)"))
    @JvmOverloads
    fun writeToResult(source: Source, result: Result, indent: Boolean = false) {
        source.writeToResult(result, indent)
    }

    @Deprecated("Use extension function", ReplaceWith("source.toInputStream()"))
    fun toInputStream(source: Source): InputStream {
        return source.toInputStream()
    }

    @Deprecated("Use extension function", ReplaceWith("source.toReader()"))
    fun toReader(source: Source): Reader {
        return source.toReader()
    }

    @Deprecated("Use extension function", ReplaceWith("source.toString(indent)"))
    @JvmOverloads
    fun toString(source: Source, indent: Boolean = false): String {
        return source.toString(indent)
    }
}
