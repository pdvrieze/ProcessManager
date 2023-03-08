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

package nl.adaptivity.util

import io.github.pdvrieze.util.jvmOnly.HttpRequest
import io.github.pdvrieze.util.jvmOnly.parseMultipartFormDataTo
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.devrieze.util.Iterators
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.toString
import nl.adaptivity.util.HttpMessage.Companion.ELEMENTLOCALNAME
import nl.adaptivity.util.HttpMessage.Companion.NAMESPACE
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import org.w3c.dom.Document
import java.io.*
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.Principal
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.xml.bind.annotation.XmlAttribute
import kotlin.collections.Map.Entry


// TODO change this to handle regular request bodies.
@Serializable
@XmlSerialName(ELEMENTLOCALNAME, NAMESPACE, "http")
class HttpMessage {

    private val _queries: MutableMap<String, String>// by lazy { HashMap<String, String>() }

    @XmlSerialName("query", NAMESPACE, "http")
    val queries: Collection<Query>
        get() = QueryCollection(_queries)

    private val _post: MutableMap<String, String>// by lazy { HashMap<String, String>() }

    @XmlSerialName("post", NAMESPACE, "http")
    val posts: Collection<Post>
        get() = PostCollection(_post)

    @XmlSerialName("body", NAMESPACE, "http")
    var body: ICompactFragment? = null

    private val _byteContent: MutableList<ByteContentDataSource> by lazy { ArrayList<ByteContentDataSource>() }

    val byteContent: List<ByteContentDataSource>
        get() = _byteContent

    @get:XmlAttribute
    var requestPath: String? = null

    var contextPath: String? = null

    var method: String? = null

    var contentType: String? = null
        private set

    @Serializable(CharsetSerializer::class)
    var characterEncoding: Charset? = null
        private set

    private val _headers: Map<String, List<String>>

    val headers: Map<String, List<String>>
        get() = Collections.unmodifiableMap(_headers)

    private val _attachments: MutableMap<String, DataSource>

    val attachments: Map<String, DataSource>
        get() = Collections.unmodifiableMap(_attachments)

    var userPrincipal: Principal? = null
        internal set

    val content: XmlReader?
        @Throws(XmlException::class)
        get() = body?.getXmlReader()

    @XmlSerialName("user", NAMESPACE, "http")
    internal var user: String?
        get() = userPrincipal?.name
        set(name) {
            userPrincipal = name?.let { SimplePrincipal(it) }
        }

    class ByteContentDataSource(
        private var name: String?,
        private var contentType: String?,
        var byteContent: ByteArray?
                               ) : DataSource {

        val dataHandler: DataHandler
            get() = DataHandler(this)

        fun setContentType(contentType: String) {
            this.contentType = contentType
        }

        override fun getContentType(): String? {
            return contentType
        }

        fun setName(name: String) {
            this.name = name
        }

        override fun getName(): String? {
            return name
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(byteContent!!)
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
            throw UnsupportedOperationException("Byte content is not writable")

        }

        override fun toString(): String {
            return "ByteContentDataSource [name=" + name + ", contentType=" + contentType + ", byteContent=\"" + String(
                byteContent!!
                                                                                                                       ) + "\"]"
        }

    }

    private class QueryIterator(iterator: MutableIterator<Entry<String, String>>) : PairBaseIterator<Query>(iterator) {

        override fun newItem(): Query {
            return Query(mIterator!!.next())
        }
    }

    private class PostIterator(iterator: MutableIterator<Entry<String, String>>) : PairBaseIterator<Post>(iterator) {

        override fun newItem(): Post {
            return Post(mIterator!!.next())
        }
    }

    abstract class PairBaseIterator<T : PairBase>(protected val mIterator: MutableIterator<Entry<String, String>>?) :
        MutableIterator<T> {

        override fun hasNext(): Boolean {
            return mIterator != null && mIterator.hasNext()
        }

        override fun next(): T {
            if (mIterator == null) {
                throw NoSuchElementException()
            }
            return newItem()
        }

        protected abstract fun newItem(): T

        override fun remove() {
            if (mIterator == null) {
                throw IllegalStateException("Removing elements from empty collection")
            }
            mIterator.remove()
        }

    }

    class Query : PairBase {

        protected constructor() {}

        constructor(entry: Entry<String, String>) : super(entry) {}

        companion object {
            private val ELEMENTNAME = QName(NAMESPACE, "query", "http")
        }

    }

    class Post : PairBase {

        protected constructor()

        constructor(entry: Entry<String, String>) : super(entry) {}

        companion object {
            private val ELEMENTNAME = QName(NAMESPACE, "post", "http")
        }

    }

    abstract class PairBase {

        @XmlSerialName("name", NAMESPACE, "http")
        lateinit var key: String

        lateinit var value: String

        protected constructor() {}

        protected constructor(entry: Entry<String, String>) {
            key = entry.key
            value = entry.value
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + key.hashCode()
            result = prime * result + value.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            if (javaClass != other.javaClass) {
                return false
            }
            other as PairBase?
            if (key != other.key) {
                return false
            }
            if (value != other.value) {
                return false
            }
            return true
        }

    }

    class QueryCollection(map: MutableMap<String, String>) : PairBaseCollection<Query>(map) {

        override fun iterator(): MutableIterator<Query> {
            return QueryIterator(map.entries.iterator())
        }
    }

    class PostCollection(map: MutableMap<String, String>) : PairBaseCollection<Post>(map) {

        override fun iterator(): MutableIterator<Post> {
            return PostIterator(map.entries.iterator())
        }
    }

    abstract class PairBaseCollection<T : PairBase>(map: MutableMap<String, String>?) : AbstractCollection<T>() {

        protected var map = map?.toMutableMap() ?: mutableMapOf()


        override fun add(element: T): Boolean {
            return map.put(element.key, element.value) != null
        }

        override fun clear() {
            map.clear()
        }

        override operator fun contains(element: T): Boolean {
            return element.value == map[element.key]
        }

        override fun isEmpty(): Boolean {
            return map.isEmpty()
        }

        override fun remove(element: T): Boolean {
            val candidate = map[element.key]

            if (candidate != null && candidate == element.value) {
                map.remove(element.key)
                return true
            } else {
                return false
            }
        }

        override val size: Int get() = map.size

    }

    private constructor() {
        _headers = HashMap()
        _post = HashMap()
        _queries = HashMap()
        _attachments = HashMap()
    }

    @Throws(IOException::class)
    constructor(request: HttpServletRequest) {
        _headers = getHeaders(request)

        _queries = toQueries(request.queryString)

        var post: MutableMap<String, String>? = null
        var attachments: MutableMap<String, DataSource> = HashMap()

        userPrincipal = request.userPrincipal

        method = request.method
        val pathInfo = request.pathInfo
        requestPath = if (pathInfo == null || pathInfo.length == 0) request.servletPath else pathInfo
        contextPath = request.contextPath
        if ("POST" == request.method || "PUT" == request.method) {
            contentType = request.contentType
            characterEncoding = null
            contentType?.let { contentType ->
                var i = contentType.indexOf(';')
                if (i >= 0) {
                    var tail: String = contentType
                    this.contentType = contentType.substring(0, i).trim { it <= ' ' }
                    while (i >= 0) {
                        tail = tail.substring(i + 1).trim { it <= ' ' }
                        i = tail.indexOf(';')
                        val param: String
                        if (i > 0) {
                            param = tail.substring(0, i).trim { it <= ' ' }
                        } else {
                            param = tail
                        }
                        val j = param.indexOf('=')
                        if (j >= 0) {
                            val paramName = param.substring(0, j).trim { it <= ' ' }
                            if (paramName == "charset") {
                                characterEncoding = Charset.forName(param.substring(j + 1).trim { it <= ' ' })
                            }
                        }

                    }
                }
            }
            if (characterEncoding == null) {
                characterEncoding = getCharacterEncoding(request)
            }
            if (characterEncoding == null) {
                characterEncoding = DEFAULT_CHARSSET
            }
            val isMultipart = contentType != null && contentType!!.startsWith("multipart/")
            if ("application/x-www-form-urlencoded" == contentType) {
                post = toQueries(String(getBody(request)))
            } else if (isMultipart) {
                request.inputStream.parseMultipartFormDataTo(attachments, HttpRequest.mimeType(request.contentType))
            } else {
                val bytes = getBody(request)

                val xml: Document

                val isXml =
                    XmlStreaming.newReader(InputStreamReader(ByteArrayInputStream(bytes), characterEncoding!!)).isXml()

                if (!isXml) {
                    addByteContent(bytes, request.contentType)
                } else {
                    val decoder = characterEncoding!!.newDecoder()
                    val buffer = decoder.decode(ByteBuffer.wrap(bytes))
                    var chars: CharArray? = null
                    if (buffer.hasArray()) {
                        chars = buffer.array()
                        if (chars!!.size > buffer.limit()) {
                            chars = null
                        }
                    }
                    if (chars == null) {
                        chars = CharArray(buffer.limit())
                        buffer.get(chars)
                    }

                    body = CompactFragment(emptyList(), chars)
                }

            }
        }
        this._post = post ?: mutableMapOf()
        this._attachments = attachments ?: mutableMapOf()
    }

    protected fun getCharacterEncoding(request: HttpServletRequest): Charset {
        val name = request.characterEncoding ?: return DEFAULT_CHARSSET
        return Charset.forName(request.characterEncoding)
    }

    private fun addByteContent(byteArray: ByteArray, contentType: String) {
        _byteContent.add(ByteContentDataSource(null, contentType, byteArray))
    }

    /*
   * Getters and setters
   */

    fun getQuery(name: String): String? {
        return if (_queries == null) null else _queries!![name]
    }

    fun getPosts(name: String): String? {
        if (_post != null) {
            var result: String? = _post!![name]
            if (result == null && _attachments != null) {
                val source = _attachments!![name]
                if (source != null) {
                    try {
                        result = toString(InputStreamReader(source.inputStream, "UTF-8"))
                        return result
                    } catch (e: UnsupportedEncodingException) {
                        throw RuntimeException(e)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }

                }
            }
        }
        return if (_post == null) null else _post!![name]
    }

    fun getParam(name: String): String? {
        val result = getQuery(name)
        return result ?: getPosts(name)
    }

    fun getAttachment(name: String?): DataSource? {
        return _attachments[name ?: NULL_KEY]
    }

    fun getHeaders(name: String): Iterable<String> {
        return Collections.unmodifiableList(_headers[name])
    }

    fun getHeader(name: String): String? {
        val list = _headers[name]
        return if (list == null || list.size < 1) {
            null
        } else list[0]
    }

    companion object {

        const val NULL_KEY = "KJJMBZLZKNC<MNCJHASIUJZNCZM>NSJHLCALSNDM<>BNADSBLKH"

        const val NAMESPACE = "http://adaptivity.nl/HttpMessage"

        const val ELEMENTLOCALNAME = "httpMessage"
        val ELEMENTNAME = QName(NAMESPACE, ELEMENTLOCALNAME, "http")
        internal const val BODYELEMENTLOCALNAME = "Body"
        internal val BODYELEMENTNAME = QName(NAMESPACE, BODYELEMENTLOCALNAME, "http")

        private val DEFAULT_CHARSSET = Charset.forName("UTF-8")

        /*
         * Utility methods
         */

        private fun getHeaders(request: HttpServletRequest): Map<String, List<String>> {
            val result = HashMap<String, List<String>>()
            for (oname in Iterators.toIterable(request.headerNames)) {
                val name = oname as String
                val values = Iterators.toList(request.getHeaders(name))
                result[name] = values
            }
            return result
        }

        private fun toQueries(queryString: String?): MutableMap<String, String> {
            val result = HashMap<String, String>()

            if (queryString == null) {
                return result
            }

            var query: String = queryString
            if (query.length > 0 && query[0] == '?') {
                /* strip questionmark */
                query = query.substring(1)
            }

            var startPos = 0
            var key: String? = null
            for (i in 0 until query.length) {
                if (key == null) {
                    if ('=' == query[i]) {
                        key = query.substring(startPos, i)
                        startPos = i + 1
                    }
                } else {
                    if ('&' == query[i] || ';' == query[i]) {
                        val value: String
                        try {
                            value = URLDecoder.decode(query.substring(startPos, i), "UTF-8")
                        } catch (e: UnsupportedEncodingException) {
                            throw RuntimeException(e)
                        }

                        result[key] = value
                        key = null
                        startPos = i + 1
                    }
                }
            }
            if (key == null) {
                key = query.substring(startPos)
                if (key.length > 0) {
                    result[key] = ""
                }
            } else {
                try {
                    val value = URLDecoder.decode(query.substring(startPos), "UTF-8")
                    result[key] = value
                } catch (e: UnsupportedEncodingException) {
                    throw RuntimeException(e)
                }

            }

            return result
        }


        private fun getBody(request: HttpServletRequest): ByteArray {
            val contentLength = request.contentLength
            val baos: ByteArrayOutputStream =
                if (contentLength > 0) ByteArrayOutputStream(contentLength) else ByteArrayOutputStream()

            val buffer = ByteArray(0xfffff)
            request.inputStream.use { inStream ->
                var i: Int = inStream.read(buffer)
                while (i >= 0) {
                    baos.write(buffer, 0, i)
                    i = inStream.read(buffer)
                }
            }

            return baos.toByteArray()
        }
    }
}

internal object CharsetSerializer: KSerializer<Charset>  {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("java.nio.Charset", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Charset {
        return Charset.forName(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Charset) {
        encoder.encodeString(value.name())
    }
}
