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

/*
 * HttpRequest.java
 *
 * Created on 15 May 2001, 14:34
 */

package net.devrieze.util.webServer

import net.devrieze.lang.Const
import net.devrieze.util.DebugTool

import javax.activation.DataSource
import javax.activation.MimeType
import javax.activation.MimeTypeParseException

import java.io.*
import java.net.URLDecoder
import java.util.Collections
import java.util.HashMap
import java.util.TreeMap


/**
 * This class is an abstraction of an http request. It takes care of reading the
 * request.
 *
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 *
 * @constructor Creates new HttpRequest. It parses the text that is available from the in stream, and uses that to initialize itself.
 *
 * @param input The stream that get read
 * @property uri Returns the uri that is specified in the request.
 *
 */
class HttpRequest(input: BufferedReader, var uri: String) {

    val headers: Map<String, String>

    val queries: Map<String, String>

    /**
     * The HTTP method that is requested.
     */
    var method: Method? = null
        private set

    /**
     * Returns the http version requested (like HTTP/1.0).
     */
    var version = "HTTP/1.0"
        private set

    /**
     * The response that this request is going to get. This is the response code.
     */
    var response = 0

    init {
        val queries = HashMap<String, String>()

        val headers = HashMap<String, String>()

        try {
            var s: String
            do {
                s = input.readLine()
            } while (s.trim { it <= ' ' }.isEmpty())

            processFirstLine(s)

            /* now process the other parts of the request */
            s = input.readLine()
            while (!s.trim { it <= ' ' }.isEmpty()) {
                /* add headers */
                var i = s.indexOf(':')

                if (i >= 0) {
                    val t = s.substring(0, i).trim { it <= ' ' }.toLowerCase()
                    s = s.substring(i + 1).trim { it <= ' ' }
                    headers[t] = s
                }
                s = input.readLine()
                i = s.indexOf(':')
            }

            val o = headers["content-length"]

            if (o != null) {
                val cl = Integer.parseInt(o)
                val mContents = CharArray(cl)

                for (i in 0 until cl) {
                    mContents[i] = input.read().toChar()
                }

                val ct = headers["content-type"]

                if (ct?.toLowerCase() == "application/x-www-form-urlencoded") {
                    val query = String(mContents)
                    parseUrlEncodedHelper(queries, query)
                }
            } else {
                var query: String? = null
                val i = uri.indexOf('?')

                if (i >= 0) {
                    query = uri.substring(i)
                }

                if (query != null) {
                    parseUrlEncodedHelper(queries, query)
                }
            }
        } catch (e: Exception) {
            DebugTool.handle(e)
        }
        this.headers = Collections.unmodifiableMap(headers)
        this.queries = Collections.unmodifiableMap(queries)

    }

    /**
     * Get's the value of the header with the specified name. If there is no such
     * header in the request, returns NULL
     *
     * @param key The header name
     * @return the value
     */
    fun getHeader(key: String): String? = headers[key]

    /**
     * Returns the value of a certain query. This is usefull for forms, and using
     * the values returned by them.
     *
     * @param key The query that is requested. If the post method has been used,
     * only the values submitted by that method are parsed. When not
     * used, the url arguments are parsed.
     * @return The value of that key, or NULL if there is no value.
     */
    fun getQuery(key: String): String? = queries[key]


    private fun processFirstLine(pLine: String) {
        var s = pLine.trim { it <= ' ' }

        var i = s.indexOf(' ')

        if (i > 0) {
            method = Method.find(s.substring(0, i))
            s = s.substring(i + 1)

            i = s.indexOf(' ')
            if (i > 0) {
                uri = s.substring(0, i)
                s = s.substring(i + 1)
                version = s
            } else {
                uri = s
            }
        } else {
            method = Method.find(s)
        }
    }

    companion object {

        val TEXT_PLAIN = mimeType("text/plain")

        fun mimeType(rawData: String): MimeType {
            try {
                return MimeType(rawData)
            } catch (ex: MimeTypeParseException) {
                throw RuntimeException(ex)
            }

        }

        /**
         * @param input
         * @param contentType The mime type that is the content. This contains
         * relevant parameters for parsing.
         * @param encoding The character encoding of the content.
         * @return
         * @throws IOException When a read error occurs.
         */
        @Throws(IOException::class)
        fun parseMultipartFormdata(input: InputStream,
                                   contentType: MimeType,
                                   encoding: String?): Map<String, DataSource> {

            return input.parseMultipartFormDataTo(HashMap(), contentType, encoding)
        }

        fun parseUrlEncoded(pSource: CharSequence?): Map<String, String> {
            return if (pSource == null) {
                emptyMap()
            } else parseUrlEncodedHelper(TreeMap(), pSource)

        }

        private fun parseUrlEncodedHelper(result: MutableMap<String, String>,
                                          source: CharSequence?): Map<String, String> {
            if (source == null) {
                return result
            }
            var query: CharSequence = source
            if (query.length > 0 && query[0] == '?') {
                /* strip questionmark */
                query = query.subSequence(1, query.length)
            }

            var startPos = 0
            var key: CharSequence? = null
            for (i in 0 until query.length) {
                if (key == null) {
                    if ('=' == query[i]) {
                        key = query.subSequence(startPos, i)
                        startPos = i + 1
                    }
                } else {
                    if ('&' == query[i] || ';' == query[i]) {
                        val value: String
                        try {
                            value = URLDecoder.decode(query.subSequence(startPos, i).toString(), "UTF-8")
                        } catch (e: UnsupportedEncodingException) {
                            throw RuntimeException(e)
                        }

                        result[key.toString()] = value
                        key = null
                    }
                }
            }
            if (key == null) {
                key = query.subSequence(startPos, query.length)
                if (key.length > 0) {
                    result[key.toString()] = ""
                }
            } else {
                try {
                    val value = URLDecoder.decode(query.subSequence(startPos, query.length).toString(), "UTF-8")
                    result[key.toString()] = value
                } catch (e: UnsupportedEncodingException) {
                    throw RuntimeException(e)
                }

            }
            return result
        }
    }


    /** Enumeration for the various HTTP methods supported.  */
    enum class Method private constructor(private val methodString: String) {
        OPTIONS("OPTIONS"),
        GET("GET"),
        HEAD("HEAD"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE"),
        TRACE("TRACE"),
        STOP("STOP");

        override fun toString(): String {
            return methodString
        }

        companion object {

            fun find(pName: String): Method? {
                val name = pName.toUpperCase()
                for (m in Method.values()) {
                    if (name == m.methodString) {
                        return m
                    }
                }
                return null
            }
        }
    }

}


private class BytesDatasource(private val content: ByteArray,
                              private val name: String,
                              private val contentType: MimeType) : DataSource {

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException()
    }

    override fun getName(): String {
        return name
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(content)
    }

    override fun getContentType(): String {
        return contentType.toString()
    }
}


private fun toDataSource(content: ByteArrayOutputStream, name: String, contentType: MimeType): DataSource {
    return BytesDatasource(content.toByteArray(), name, contentType)
}


fun <M:MutableMap<String, DataSource>>InputStream.parseMultipartFormDataTo(receiver: M,contentType: MimeType, encoding: String?=null):M {

    val boundary = contentType.getParameter("boundary") ?: throw IllegalArgumentException(
        "Content type does not specify a boundary")

    BufferedInputStream(this).use { input ->

        var b = input.read()
        if (b == Const._CR.toInt()) {
            b = input.read()
            if (b == Const._LF.toInt()) {
                b = input.read()
            }
        }
        var curPos = 0 // We just optionally read a CRLF
        var stage = 2
        var contentDisposition: MimeType? = null
        var content: ByteArrayOutputStream? = null
        var wsBuffer: ByteArrayOutputStream? = null
        var headerLine: StringBuilder? = null
        var contentType = HttpRequest.TEXT_PLAIN
        while (b >= 0) {


            if (stage == 0 && b == Const._CR.toInt()) {
                stage = 1
            } else if (stage == 1 && b == Const._LF.toInt()) {
                stage = 2
            } else if ((stage == 2 || stage == 3) && b == '-'.toInt()) { // Reading two hyphens
                ++stage
            } else if (stage == 4 && b == boundary[curPos].toInt()) { // Reading the actual boundary
                ++curPos
                if (curPos == boundary.length) {
                    stage = 5
                    curPos = 0
                }
            } else if (stage == 5 && (b == ' '.toInt() || b == '\t'.toInt())) {
                if (wsBuffer == null) {
                    wsBuffer = ByteArrayOutputStream()
                } // Remember to be able to replay
                wsBuffer.write(b)
            } else if (stage == 5 && b == '-'.toInt() && wsBuffer == null) {
                b = input.read()
                if (b != '-'.toInt()) {
                    wsBuffer = ByteArrayOutputStream()
                    wsBuffer.write('-'.toInt())
                    continue // This will fail in the next loop iteration
                }
                // We found an end of all data.
                if (content != null) { // First time don't do this
                    val contentName = contentDisposition?.getParameter(
                        "name")
                    if (contentName == null) {
                        receiver[Integer.toString(receiver.size)] = toDataSource(content,
                                                                                             Integer.toString(
                                                                                                 receiver.size),
                                                                                             contentType)
                    } else {
                        receiver[contentName] = toDataSource(content, contentName, contentType)
                    }
                }
                break // Go out of the loop.
            } else if (stage == 5 && b == Const._CR.toInt()) {
                stage = 6
            } else if (stage == 6 && b == Const._LF.toInt()) {
                stage = 7 // We completed, next step is to finish previous block, and then read headers
                wsBuffer = null
                if (content != null) { // First time don't do this
                    val contentName = contentDisposition?.getParameter(
                        "name")
                    if (contentName == null) {
                        receiver[Integer.toString(receiver.size)] = toDataSource(content,
                                                                                             Integer.toString(
                                                                                                 receiver.size),
                                                                                             contentType)
                    } else {
                        receiver[contentName] = toDataSource(content, contentName, contentType)
                    }
                }
                contentDisposition = null
                headerLine = StringBuilder()
            } else if (stage == 7) {
                if (b == Const._CR.toInt()) {
                    b = input.read()
                    if (b != Const._LF.toInt()) {
                        content?.close()
                        throw IllegalArgumentException("Header lines should be separated by CRLF, not CR only")
                    }
                    if (headerLine == null) {
                        content?.close()
                        throw AssertionError("Headerline is null, but never should be")
                    }
                    if (headerLine.length == 0) {
                        headerLine = null
                        content = ByteArrayOutputStream()
                        stage = 0
                    } else {
                        val s = headerLine.toString()
                        headerLine = StringBuilder()
                        val colonPos = s.indexOf(':')
                        if (colonPos >= 1) {
                            val name = s.substring(0, colonPos).trim { it <= ' ' }
                            val `val` = s.substring(colonPos + 1).trim { it <= ' ' }
                            val nmLC = name.toLowerCase()
                            if ("content-disposition" == nmLC) {
                                try {
                                    if (`val`.startsWith("form-data")) {
                                        contentDisposition = MimeType("multipart/$`val`")
                                    } else {
                                        contentDisposition = MimeType(`val`)
                                    }
                                } catch (ex: MimeTypeParseException) {
                                    // Just ignore invalid content dispositions
                                }

                            } else if ("content-type" == nmLC) {
                                try {
                                    contentType = MimeType(`val`)
                                } catch (ex: MimeTypeParseException) {
                                    // Just ignore invalid content dispositions
                                }

                            }
                        }
                    }
                } else {
                    if (headerLine == null) {
                        throw AssertionError("Headerline is null, but never should be")
                    }
                    headerLine.append(b.toChar())
                }
            } else {
                if (content != null) { // Ignore any preamble (it's legal that there is stuff so ignore it
                    // Reset
                    if (stage > 0) {
                        content.write(Const._CR.toInt())
                        if (stage > 1) {
                            content.write(Const._LF.toInt())
                            if (stage > 2) {
                                content.write('-'.toInt())
                                if (stage > 3) {
                                    content.write('-'.toInt())
                                    for (i in 0 until curPos) {
                                        content.write(boundary[i].toInt())
                                    }
                                    if (stage > 4) {
                                        val ws = if (wsBuffer == null) ByteArray(0) else wsBuffer.toByteArray()
                                        for (w in ws) {
                                            content.write(ws)
                                        }
                                        if (stage > 5) {
                                            content.write(Const._CR.toInt())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    content.write(b)
                }
                curPos = 0
            }
            b = input.read()

        }


        return receiver
    }
}
