/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util;

import net.devrieze.util.Iterators;
import net.devrieze.util.Streams;
import net.devrieze.util.StringUtil;
import net.devrieze.util.security.SimplePrincipal;
import net.devrieze.util.webServer.HttpRequest;
import nl.adaptivity.util.HttpMessage.Post;
import nl.adaptivity.util.HttpMessage.Query;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.*;
import nl.adaptivity.xml.schema.annotations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.Principal;
import java.util.*;
import java.util.Map.Entry;


// TODO change this to handle regular request bodies.
@Element(name=HttpMessage.ELEMENTLOCALNAME,
         nsUri = HttpMessage.NAMESPACE,
         attributes = {@Attribute("user")},
         children = {@Child(property="queries", type= Query.class),
                     @Child(property = "posts", type = Post.class),
                     @Child(name=HttpMessage.BODYELEMENTLOCALNAME, property = "body", type= AnyType.class)
         })
@XmlDeserializer(HttpMessage.Factory.class)
public class HttpMessage implements XmlSerializable, SimpleXmlDeserializable{

  public static class Factory implements XmlDeserializerFactory<HttpMessage> {

    @Override
    public HttpMessage deserialize(final XmlReader reader) throws XmlException {
      return HttpMessage.deserialize(reader);
    }
  }

  public static final String NAMESPACE = "http://adaptivity.nl/HttpMessage";

  public static final String ELEMENTLOCALNAME = "httpMessage";
  public static final QName ELEMENTNAME=new QName(NAMESPACE, ELEMENTLOCALNAME, "http");
  static final String BODYELEMENTLOCALNAME = "Body";
  static final QName BODYELEMENTNAME =new QName(NAMESPACE, BODYELEMENTLOCALNAME, "http");

  public static class ByteContentDataSource implements DataSource {

    private String contentType;

    private byte[] byteContent;

    private String name;

    public ByteContentDataSource(final String name, final String contentType, final byte[] byteContent) {
      this.name = name;
      this.contentType = contentType;
      this.byteContent = byteContent;
    }

    public void setContentType(final String contentType) {
      this.contentType = contentType;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    public void setByteContent(final byte[] byteContent) {
      this.byteContent = byteContent;
    }

    public byte[] getByteContent() {
      return byteContent;
    }

    @NotNull
    public DataHandler getDataHandler() {
      return new DataHandler(this);
    }

    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(byteContent);
    }

    @NotNull
    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException("Byte content is not writable");

    }

    @NotNull
    @Override
    public String toString() {
      return "ByteContentDataSource [name=" + name + ", contentType=" + contentType + ", byteContent=\"" + new String(byteContent) + "\"]";
    }

  }

  private static class QueryIterator extends PairBaseIterator<Query> {
    public QueryIterator(final Iterator<Entry<String, String>> iterator) {
      super(iterator);
    }

    protected Query newItem() {return new Query(mIterator.next());}
  }

  private static class PostIterator extends PairBaseIterator<Post> {
    public PostIterator(final Iterator<Entry<String, String>> iterator) {
      super(iterator);
    }

    protected Post newItem() {return new Post(mIterator.next());}
  }

  public static abstract class PairBaseIterator<T extends PairBase> implements Iterator<T> {

    protected final Iterator<Entry<String, String>> mIterator;

    public PairBaseIterator(final Iterator<Entry<String, String>> iterator) {
      mIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return (mIterator != null) && mIterator.hasNext();
    }

    @NotNull
    @Override
    public T next() {
      if (mIterator == null) {
        throw new NoSuchElementException();
      }
      return newItem();
    }

    @NotNull
    protected abstract T newItem();

    @Override
    public void remove() {
      if (mIterator == null) {
        throw new IllegalStateException("Removing elements from empty collection");
      }
      mIterator.remove();
    }

  }

  /*
        XmlUtil.writeAttribute(out, "name", mKey);
      if (mValue!=null) { out.text(mValue); }
   */

  @Element(name = "query",
           nsUri = NAMESPACE,
           nsPrefix = "http",
           attributes = {@Attribute(value = "name", optional = false)},
           content = "value")
  public static class Query extends PairBase {
    private static final QName ELEMENTNAME =new QName(NAMESPACE, "query", "http");

    protected Query() {
    }

    public Query(@NotNull final Entry<String, String> entry) {
      super(entry);
    }

    public static Query deserialize(XmlReader in) throws XmlException {
      return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.util.HttpMessage.Query>deserializeHelper(new Query(), in);
    }

    @Override
    public QName getElementName() {
      return ELEMENTNAME;
    }

  }

  @Element(name = "post",
           nsUri = NAMESPACE,
           nsPrefix = "http",
           attributes = {@Attribute(value = "name", optional = false)},
           content = "value")
  public static class Post extends PairBase {
    private static final QName ELEMENTNAME =new QName(NAMESPACE, "post", "http");

    protected Post() {
    }

    public Post(@NotNull final Entry<String, String> entry) {
      super(entry);
    }

    public static Query deserialize(XmlReader in) throws XmlException {
      return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.util.HttpMessage.Query>deserializeHelper(new Query(), in);
    }

    @Override
    public QName getElementName() {
      return ELEMENTNAME;
    }

  }

  public static abstract class PairBase implements XmlSerializable, SimpleXmlDeserializable {

    private String mKey;

    private String mValue;

    protected PairBase() {}

    protected PairBase(@NotNull final Entry<String, String> entry) {
      mKey = entry.getKey();
      mValue = entry.getValue();
    }

    @Override
    public boolean deserializeChild(final XmlReader in) throws XmlException {
      return false;
    }

    @Override
    public boolean deserializeChildText(final CharSequence elementText) {
      mValue = mValue==null ? elementText.toString() : mValue+elementText.toString();
      return true;
    }

    @Override
    public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
      if ("name".equals(attributeLocalName)) { mKey = StringUtil.toString(attributeValue); return true; }
      return false;
    }

    @Override
    public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) {
      /* Do nothing. */
    }

    @Override
    public void serialize(final XmlWriter out) throws XmlException {
      XmlWriterUtil.smartStartTag(out, getElementName());
      XmlWriterUtil.writeAttribute(out, "name", mKey);
      if (mValue!=null) { out.text(mValue); }
      XmlWriterUtil.endTag(out, getElementName());
    }

    public void setKey(final String key) {
      mKey = key;
    }

    @XmlName("name")
    public String getKey() {
      return mKey;
    }

    public void setValue(final String value) {
      mValue = value;
    }

    public String getValue() {
      return mValue;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((mKey == null) ? 0 : mKey.hashCode());
      result = (prime * result) + ((mValue == null) ? 0 : mValue.hashCode());
      return result;
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final PairBase other = (PairBase) obj;
      if (mKey == null) {
        if (other.mKey != null) {
          return false;
        }
      } else if (!mKey.equals(other.mKey)) {
        return false;
      }
      if (mValue == null) {
        if (other.mValue != null) {
          return false;
        }
      } else if (!mValue.equals(other.mValue)) {
        return false;
      }
      return true;
    }

  }

  public static class QueryCollection extends PairBaseCollection<Query> {

    public QueryCollection(final Map<String, String> map) {
      super(map);
    }

    @NotNull
    @Override
    public Iterator<Query> iterator() {
      if (mMap == null) { return Collections.emptyIterator(); }
      return new QueryIterator(mMap.entrySet().iterator());
    }
  }

  public static class PostCollection extends PairBaseCollection<Post> {

    public PostCollection(final Map<String, String> map) {
      super(map);
    }

    @NotNull
    @Override
    public Iterator<Post> iterator() {
      if (mMap == null) { return Collections.emptyIterator(); }
      return new PostIterator(mMap.entrySet().iterator());
    }
  }

  public static abstract class PairBaseCollection<T extends PairBase> extends AbstractCollection<T> {

    protected Map<String, String> mMap;

    public PairBaseCollection(final Map<String, String> map) {
      mMap = map;
    }

    @Override
    public boolean add(@NotNull final PairBase e) {
      if (mMap == null) {
        mMap = new HashMap<>();
      }
      return mMap.put(e.getKey(), e.getValue()) != null;
    }

    @Override
    public void clear() {
      if (mMap != null) {
        mMap.clear();
      }
    }

    @Override
    public boolean contains(final Object o) {
      if (o instanceof PairBase) {
        final PairBase q = (PairBase) o;
        final String match = mMap.get(q.getKey());
        return ((match == null) && (q.getValue() == null)) || ((q.getValue() != null) && q.getValue().equals(match));
      } else {
        return false;
      }
    }

    @Override
    public boolean isEmpty() {
      if (mMap == null) {
        return true;
      }
      return mMap.isEmpty();
    }

    @Override
    public boolean remove(final Object o) {
      if (mMap == null) {
        return false;
      }
      final Query q = (Query) o;
      final String candidate = mMap.get(q.getKey());

      if (((candidate == null) && (q.getValue() == null)) || ((candidate != null) && candidate.equals(q.getValue()))) {
        mMap.remove(q.getKey());
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int size() {
      if (mMap == null) {
        return 0;
      }
      return mMap.size();
    }

  }

  private static final Charset DEFAULT_CHARSSET = Charset.forName("UTF-8");

  private Map<String, String> mQueries;

  private Map<String, String> mPost;

  private CompactFragment mBody;

  private List<ByteContentDataSource> mByteContent;

  private String mRequestPath;

  private String mContextPath;

  private String mMethod;

  private String mContentType;

  @Nullable private Charset mCharacterEncoding;

  @NotNull private final Map<String, List<String>> mHeaders;

  private Map<String, DataSource> mAttachments;

  private Principal mUserPrincipal;

  private HttpMessage() {
    mHeaders = new HashMap<>();
  }

  public HttpMessage(@NotNull final HttpServletRequest request) throws IOException {
    mHeaders = getHeaders(request);

    mQueries = toQueries(request.getQueryString());
    mUserPrincipal = request.getUserPrincipal();

    setMethod(request.getMethod());
    final String pathInfo = request.getPathInfo();
    setRequestPath((pathInfo == null) || (pathInfo.length() == 0) ? request.getServletPath() : pathInfo);
    setContextPath(request.getContextPath());
    if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
      mContentType = request.getContentType();
      mCharacterEncoding = null;
      if (mContentType != null) {
        int i = mContentType.indexOf(';');
        if (i >= 0) {
          String tail = mContentType;
          mContentType = mContentType.substring(0, i).trim();
          while (i >= 0) {
            tail = tail.substring(i + 1).trim();
            i = tail.indexOf(';');
            final String param;
            if (i > 0) {
              param = tail.substring(0, i).trim();
            } else {
              param = tail;
            }
            final int j = param.indexOf('=');
            if (j >= 0) {
              final String paramName = param.substring(0, j).trim();
              if (paramName.equals("charset")) {
                mCharacterEncoding = Charset.forName(param.substring(j + 1).trim());
              }
            }

          }
        }
      }
      if (mCharacterEncoding == null) {
        mCharacterEncoding = getCharacterEncoding(request);
      }
      if (mCharacterEncoding == null) {
        mCharacterEncoding = DEFAULT_CHARSSET;
      }
      final boolean isMultipart = (mContentType != null) && mContentType.startsWith("multipart/");
      if ("application/x-www-form-urlencoded".equals(mContentType)) {
        mPost = toQueries(new String(getBody(request)));
      } else if (isMultipart) {
        mAttachments = HttpRequest.parseMultipartFormdata(request.getInputStream(), HttpRequest.mimeType(request.getContentType()), null);
      } else {
        @SuppressWarnings("resource")
        final byte[] bytes = getBody(request);

        final Document xml;

        // TODO don't create a DOM tree here
        xml = DomUtil.tryParseXml(new InputStreamReader(new ByteArrayInputStream(bytes), mCharacterEncoding));
        if (xml == null) {
          addByteContent(bytes, request.getContentType());
        } else {
          CharsetDecoder decoder = mCharacterEncoding.newDecoder();
          CharBuffer buffer = decoder.decode(ByteBuffer.wrap(bytes));
          char[] chars = null;
          if (buffer.hasArray()) {
            chars = buffer.array();
            if (chars.length>buffer.limit()) {
              chars=null;
            }
          }
          if (chars==null) {
            chars = new char[buffer.limit()];
            buffer.get(chars);
          }

          mBody = new CompactFragment(Collections.<Namespace>emptyList(), chars);
        }

      }
    }
  }

  protected Charset getCharacterEncoding(final @NotNull HttpServletRequest request) {
    String name = request.getCharacterEncoding();
    if (name==null) { return DEFAULT_CHARSSET; }
    return Charset.forName(request.getCharacterEncoding());
  }

  private static HttpMessage deserialize(final XmlReader in) throws XmlException {
    return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.util.HttpMessage>deserializeHelper(new HttpMessage(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(@NotNull final XmlReader reader) {
    // do nothing
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    XmlWriterUtil.writeAttribute(out, "user", mUserPrincipal.getName());
    XmlWriterUtil.writeChildren(out, getQueries());
    XmlWriterUtil.writeChildren(out, getPosts());
    if (mBody!=null) {
      XmlWriterUtil.smartStartTag(out, BODYELEMENTNAME);
      mBody.serialize(out);
      XmlWriterUtil.endTag(out, BODYELEMENTNAME);
    }

    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  /*
   * Utility methods
   */

  @NotNull
  @SuppressWarnings("unchecked")
  private static Map<String, List<String>> getHeaders(@NotNull final HttpServletRequest request) {
    final Map<String, List<String>> result = new HashMap<>();
    for (final Object oname : Iterators.<String> toIterable(request.getHeaderNames())) {
      final String name = (String) oname;
      final List<String> values = Iterators.<String> toList(request.getHeaders(name));
      result.put(name, values);
    }
    return result;
  }

  @NotNull
  private static Map<String, String> toQueries(@Nullable final String queryString) {
    final Map<String, String> result = new HashMap<>();

    if (queryString == null) {
      return result;
    }

    String query = queryString;
    if ((query.length() > 0) && (query.charAt(0) == '?')) {
      /* strip questionmark */
      query = query.substring(1);
    }

    int startPos = 0;
    String key = null;
    for (int i = 0; i < query.length(); ++i) {
      if (key == null) {
        if ('=' == query.charAt(i)) {
          key = query.substring(startPos, i);
          startPos = i + 1;
        }
      } else {
        if (('&' == query.charAt(i)) || (';' == query.charAt(i))) {
          final String value;
          try {
            value = URLDecoder.decode(query.substring(startPos, i), "UTF-8");
          } catch (@NotNull final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
          result.put(key, value);
          key = null;
          startPos = i + 1;
        }
      }
    }
    if (key == null) {
      key = query.substring(startPos);
      if (key.length() > 0) {
        result.put(key, "");
      }
    } else {
      try {
        final String value = URLDecoder.decode(query.substring(startPos), "UTF-8");
        result.put(key, value);
      } catch (@NotNull final UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

    }

    return result;
  }


  @NotNull
  private static byte[] getBody(@NotNull final HttpServletRequest request) throws IOException {
    final ByteArrayOutputStream baos;
    {
      final int contentLength = request.getContentLength();
      baos = contentLength > 0 ? new ByteArrayOutputStream(contentLength) : new ByteArrayOutputStream();
      final byte[] buffer = new byte[0xfffff];
      try (final ServletInputStream is = request.getInputStream()) {
        int i;
        while ((i = is.read(buffer)) >= 0) {
          baos.write(buffer, 0, i);
        }
      }
    }
    return baos.toByteArray();
  }

  private void addByteContent(final byte[] byteArray, final String contentType) {
    getByteContent().add(new ByteContentDataSource(null, contentType, byteArray));
  }

  /*
   * Getters and setters
   */

  @Nullable
  public String getQuery(final String name) {
    return mQueries == null ? null : mQueries.get(name);
  }

  @Nullable
  public String getPosts(final String name) {
    if (mPost != null) {
      String result = mPost.get(name);
      if ((result == null) && (mAttachments != null)) {
        final DataSource source = mAttachments.get(name);
        if (source != null) {
          try {
            result = Streams.toString(new InputStreamReader(source.getInputStream(), "UTF-8"));
            return result;
          } catch (@NotNull final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          } catch (@NotNull final IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return mPost == null ? null : mPost.get(name);
  }

  @Nullable
  public String getParam(final String name) {
    final String result = getQuery(name);
    if (result != null) {
      return result;
    }
    return getPosts(name);
  }

  @Nullable
  public DataSource getAttachment(final String name) {
    if (mAttachments == null) {
      return null;
    }
    return mAttachments.get(name);
  }

  public Map<String, DataSource> getAttachments() {
    if (mAttachments == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(mAttachments);
  }


  @NotNull
  @XmlName("query")
  public Collection<Query> getQueries() {
    if (mQueries == null) {
      mQueries = new HashMap<>();
    }
    return new QueryCollection(mQueries);
  }

  @NotNull
  @XmlName("post")
  public Collection<Post> getPosts() {
    if (mPost == null) {
      mPost = new HashMap<>();
    }
    return new PostCollection(mPost);
  }

  @XmlName("body")
  public CompactFragment getBody() {
    return mBody;
  }

  public void setBody(final CompactFragment body) {
    mBody = body;
  }

  public List<ByteContentDataSource> getByteContent() {
    if (mByteContent == null) {
      mByteContent = new ArrayList<>();
    }
    return mByteContent;
  }

  public void setRequestPath(final String pathInfo) {
    mRequestPath = pathInfo;
  }

  @XmlAttribute
  public String getRequestPath() {
    return mRequestPath;
  }

  public Iterable<String> getHeaders(final String name) {
    return Collections.unmodifiableList(mHeaders.get(name));
  }

  @Nullable
  public String getHeader(final String name) {
    final List<String> list = mHeaders.get(name);
    if ((list == null) || (list.size() < 1)) {
      return null;
    }
    return list.get(0);
  }

  public Map<String, List<String>> getHeaders() {
    return Collections.unmodifiableMap(mHeaders);
  }

  public String getMethod() {
    return mMethod;
  }

  public void setMethod(final String method) {
    mMethod = method;
  }

  public void setContextPath(final String contextPath) {
    mContextPath = contextPath;
  }

  public String getContextPath() {
    return mContextPath;
  }

  public String getContentType() {
    return mContentType;
  }

  @Nullable
  public Charset getCharacterEncoding() {
    return mCharacterEncoding;
  }

  @Nullable
  public XmlReader getContent() throws XmlException {
    return XMLFragmentStreamReader.from(mBody);
  }

  @XmlName("user")
  String getUser() {
    return mUserPrincipal.getName();
  }

  void setUser(final String name) {
    mUserPrincipal = new SimplePrincipal(name);
  }

  public Principal getUserPrincipal() {
    return mUserPrincipal;
  }

  void setUserPrincipal(final Principal userPrincipal) {
    mUserPrincipal = userPrincipal;
  }
}
