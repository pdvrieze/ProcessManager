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

import net.devrieze.util.Iterators
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.toString
import net.devrieze.util.webServer.HttpRequest
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import nl.adaptivity.xml.schema.annotations.Attribute
import nl.adaptivity.xml.schema.annotations.Element
import nl.adaptivity.xml.schema.annotations.XmlName
import org.w3c.dom.Document
import java.io.*
import java.io.IOException
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.Principal
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.servlet.http.HttpServletRequest
import javax.xml.bind.annotation.XmlAttribute
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.Map.Entry


// TODO change this to handle regular request bodies.
/*
@Element(name = HttpMessage.ELEMENTLOCALNAME, nsUri = HttpMessage.NAMESPACE, attributes = arrayOf(
    Attribute("user")),
                                              children = arrayOf(
                                                  Child(property = "queries",
                                                                                             type = Query::class),
                                                  Child(property = "posts",
                                                                                             type = Post::class),
                                                  Child(
                                                      name = HttpMessage.BODYELEMENTLOCALNAME, property = "body",
                                                      type = AnyType::class)))
@XmlDeserializer(HttpMessage.Factory::class)
*/
class HttpMessage : XmlSerializable, SimpleXmlDeserializable {

    private val _queries: MutableMap<String, String>// by lazy { HashMap<String, String>() }

    val queries: Collection<Query>
        @XmlName("query")
        get() = QueryCollection(_queries)

    private val _post: MutableMap<String, String>// by lazy { HashMap<String, String>() }

    val posts: Collection<Post>
        @XmlName("post")
        get() = PostCollection(_post)

    @get:XmlName("body")
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

    override val elementName: QName
        get() = ELEMENTNAME


    val content: XmlReader?
        @Throws(XmlException::class)
        get() = body?.getXmlReader()

    internal var user: String?
        @XmlName("user")
        get() = userPrincipal?.name
        set(name) {
            userPrincipal = name?.let { SimplePrincipal(it) }
        }

    class Factory : XmlDeserializerFactory<HttpMessage> {

        @Throws(XmlException::class)
        override fun deserialize(reader: XmlReader): HttpMessage {
            return HttpMessage.deserialize(reader)
        }
    }

    class ByteContentDataSource(private var name: String?,
                                private var contentType: String?,
                                var byteContent: ByteArray?) : DataSource {

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
                    byteContent!!) + "\"]"
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

    abstract class PairBaseIterator<T : PairBase>(protected val mIterator: MutableIterator<Entry<String, String>>?) : MutableIterator<T> {

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

    /*
        XmlUtil.writeAttribute(out, "name", mKey);
      if (mValue!=null) { out.text(mValue); }
   */

    @Element(name = "query", nsUri = NAMESPACE, nsPrefix = "http",
                                                  attributes = arrayOf(
                                                      Attribute(value = "name",
                                                                                                     optional = false)), content = "value")
    class Query : PairBase {

        override val elementName: QName
            get() = ELEMENTNAME

        protected constructor() {}

        constructor(entry: Entry<String, String>) : super(entry) {}

        companion object {
            private val ELEMENTNAME = QName(NAMESPACE, "query", "http")

            @Throws(XmlException::class)
            fun deserialize(reader: XmlReader): Query {
                return Query().deserializeHelper(reader)
            }
        }

    }

    @Element(name = "post", nsUri = NAMESPACE, nsPrefix = "http",
                                                  attributes = [(Attribute(
                value = "name", optional = false))], content = "value")
    class Post : PairBase {

        override val elementName: QName
            get() = ELEMENTNAME

        protected constructor() {}

        constructor(entry: Entry<String, String>) : super(entry) {}

        companion object {
            private val ELEMENTNAME = QName(NAMESPACE, "post", "http")

            @Throws(XmlException::class)
            fun deserialize(`in`: XmlReader): Post {
                return Post().deserializeHelper(`in`)
            }
        }

    }

    abstract class PairBase : XmlSerializable, SimpleXmlDeserializable {

        @get:XmlName("name")
        lateinit var key: String

        lateinit var value: String

        protected constructor() {}

        protected constructor(entry: Entry<String, String>) {
            key = entry.key
            value = entry.value
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            return false
        }

        override fun deserializeChildText(elementText: CharSequence): Boolean {
            value = if (! ::value.isInitialized) elementText.toString() else value + elementText.toString()
            return true
        }

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            if ("name" == attributeLocalName) {
                key = attributeValue
                return true
            }
            return false
        }

        override fun onBeforeDeserializeChildren(reader: XmlReader) {
            /* Do nothing. */
        }

        @Throws(XmlException::class)
        override fun serialize(out: XmlWriter) {
            out.smartStartTag(elementName)
            out.writeAttribute("name", key)
            if (value != null) {
                out.text(value!!)
            }
            out.endTag(elementName)
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + if (key == null) 0 else key!!.hashCode()
            result = prime * result + if (value == null) 0 else value!!.hashCode()
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            if (javaClass != obj.javaClass) {
                return false
            }
            val other = obj as PairBase?
            if (key == null) {
                if (other!!.key != null) {
                    return false
                }
            } else if (key != other!!.key) {
                return false
            }
            if (value == null) {
                if (other.value != null) {
                    return false
                }
            } else if (value != other.value) {
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

        var post: MutableMap<String,String>? = null
        var attachments: MutableMap<String, DataSource>? = null

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
                attachments = HttpRequest.parseMultipartFormdata(request.inputStream,
                                                                 HttpRequest.mimeType(request.contentType), null)
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

    override fun deserializeChild(reader: XmlReader): Boolean {
        return false
    }

    override fun deserializeChildText(elementText: CharSequence): Boolean {
        return false
    }

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean {
        return false
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
        // do nothing
    }

    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME) {
            writeAttribute("user", userPrincipal!!.name)
            writeChildren(queries)
            writeChildren(posts)

            body?.also {
                smartStartTag(BODYELEMENTNAME) {
                    it.serialize(this)
                }

            }
        }
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

        @Throws(XmlException::class)
        private fun deserialize(reader: XmlReader): HttpMessage {
            return HttpMessage().deserializeHelper(reader)
        }

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
            val baos: ByteArrayOutputStream = if (contentLength > 0) ByteArrayOutputStream(contentLength) else ByteArrayOutputStream()

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
