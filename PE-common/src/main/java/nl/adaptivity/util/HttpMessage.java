package nl.adaptivity.util;

import net.devrieze.util.Iterators;
import net.devrieze.util.Streams;
import net.devrieze.util.security.SimplePrincipal;
import net.devrieze.util.webServer.HttpRequest;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import java.io.*;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.Map.Entry;


// TODO change this to handle regular request bodies.
@XmlRootElement(name = "httpMessage", namespace = HttpMessage.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "HttpMessage", namespace = HttpMessage.NAMESPACE)
public class HttpMessage {

  public static final String NAMESPACE = "http://adaptivity.nl/HttpMessage";


  @XmlType(name = "Body", namespace = HttpMessage.NAMESPACE)
  @XmlAccessorType(XmlAccessType.NONE)
  public static class Body {

    @XmlAnyElement(lax = false)
    List<Object> elements;

    @NotNull
    public List<Node> getElements() {
      @SuppressWarnings("unchecked")
      final List<Node> result = (List<Node>) ((List<?>) elements);
      return result;
    }
  }

  @XmlAccessorType(XmlAccessType.NONE)
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
    @XmlAttribute
    public String getContentType() {
      return contentType;
    }

    public void setByteContent(final byte[] byteContent) {
      this.byteContent = byteContent;
    }

    @XmlValue
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
    @XmlAttribute
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

  public static class QueryIterator implements Iterator<Query> {

    private final Iterator<Entry<String, String>> aIterator;

    public QueryIterator(final Iterator<Entry<String, String>> iterator) {
      aIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return (aIterator != null) && aIterator.hasNext();
    }

    @NotNull
    @Override
    public Query next() {
      if (aIterator == null) {
        throw new NoSuchElementException();
      }
      return new Query(aIterator.next());
    }

    @Override
    public void remove() {
      if (aIterator == null) {
        throw new IllegalStateException("Removing elements from empty collection");
      }
      aIterator.remove();
    }

  }

  @XmlType(name = "Query", namespace = HttpMessage.NAMESPACE)
  @XmlAccessorType(XmlAccessType.NONE)
  public static class Query {

    private String aKey;

    private String aValue;

    protected Query() {}

    public Query(@NotNull final Entry<String, String> entry) {
      aKey = entry.getKey();
      aValue = entry.getValue();
    }

    public void setKey(final String key) {
      aKey = key;
    }

    @XmlAttribute(name = "key", required = true)
    public String getKey() {
      return aKey;
    }

    public void setValue(final String value) {
      aValue = value;
    }

    @XmlValue
    public String getValue() {
      return aValue;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((aKey == null) ? 0 : aKey.hashCode());
      result = (prime * result) + ((aValue == null) ? 0 : aValue.hashCode());
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
      final Query other = (Query) obj;
      if (aKey == null) {
        if (other.aKey != null) {
          return false;
        }
      } else if (!aKey.equals(other.aKey)) {
        return false;
      }
      if (aValue == null) {
        if (other.aValue != null) {
          return false;
        }
      } else if (!aValue.equals(other.aValue)) {
        return false;
      }
      return true;
    }

  }

  public static class QueryMapCollection extends AbstractCollection<Query> {

    private Map<String, String> aMap;

    public QueryMapCollection(final Map<String, String> map) {
      aMap = map;
    }

    @Override
    public boolean add(@NotNull final Query e) {
      if (aMap == null) {
        aMap = new HashMap<>();
      }
      return aMap.put(e.getKey(), e.getValue()) != null;
    }

    @Override
    public void clear() {
      if (aMap != null) {
        aMap.clear();
      }
    }

    @Override
    public boolean contains(final Object o) {
      if (o instanceof Query) {
        final Query q = (Query) o;
        final String match = aMap.get(q.getKey());
        return ((match == null) && (q.getValue() == null)) || ((q.getValue() != null) && q.getValue().equals(match));
      } else {
        return false;
      }
    }

    @Override
    public boolean isEmpty() {
      if (aMap == null) {
        return true;
      }
      return aMap.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Query> iterator() {
      if (aMap == null) {
        return new QueryIterator(null);
      }
      return new QueryIterator(aMap.entrySet().iterator());
    }

    @Override
    public boolean remove(final Object o) {
      if (aMap == null) {
        return false;
      }
      final Query q = (Query) o;
      final String candidate = aMap.get(q.getKey());

      if (((candidate == null) && (q.getValue() == null)) || ((candidate != null) && candidate.equals(q.getValue()))) {
        aMap.remove(q.getKey());
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int size() {
      if (aMap == null) {
        return 0;
      }
      return aMap.size();
    }

  }

  private Map<String, String> aQueries;

  private Map<String, String> aPost;

  private Body aBody;

  private Collection<ByteContentDataSource> aByteContent;

  private String aRequestPath;

  private String aContextPath;

  private String aMethod;

  private String aContentType;

  @Nullable private String aCharacterEncoding;

  @NotNull private final Map<String, List<String>> aHeaders;

  private Map<String, DataSource> aAttachments;

  private Principal aUserPrincipal;

  public HttpMessage(@NotNull final HttpServletRequest request) throws IOException {
    aHeaders = getHeaders(request);

    aQueries = toQueries(request.getQueryString());
    aUserPrincipal = request.getUserPrincipal();

    setMethod(request.getMethod());
    final String pathInfo = request.getPathInfo();
    setRequestPath((pathInfo == null) || (pathInfo.length() == 0) ? request.getServletPath() : pathInfo);
    setContextPath(request.getContextPath());
    if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
      aContentType = request.getContentType();
      aCharacterEncoding = null;
      if (aContentType != null) {
        int i = aContentType.indexOf(';');
        if (i >= 0) {
          String tail = aContentType;
          aContentType = aContentType.substring(0, i).trim();
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
                aCharacterEncoding = param.substring(j + 1).trim();
              }
            }

          }
        }
      }
      if (aCharacterEncoding == null) {
        aCharacterEncoding = request.getCharacterEncoding();
      }
      if (aCharacterEncoding == null) {
        aCharacterEncoding = "UTF-8";
      }
      final boolean isMultipart = (aContentType != null) && aContentType.startsWith("multipart/");
      if ("application/x-www-form-urlencoded".equals(aContentType)) {
        aPost = toQueries(getBody(request).toString(aCharacterEncoding));
      } else if (isMultipart) {
        aAttachments = HttpRequest.parseMultipartFormdata(request.getInputStream(), HttpRequest.mimeType(request.getContentType()), null);
      } else {
        @SuppressWarnings("resource")
        final ByteArrayOutputStream baos = getBody(request);

        final Document xml;

        xml = XmlUtil.tryParseXml(new ByteArrayInputStream(baos.toByteArray()));
        if (xml == null) {
          addByteContent(baos.toByteArray(), request.getContentType());
        } else {
          aBody = new Body();
          aBody.elements = new ArrayList<>(1);
          aBody.elements.add(xml.getDocumentElement());
        }

      }
    }
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
  private static ByteArrayOutputStream getBody(@NotNull final HttpServletRequest request) throws IOException {
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
    return baos;
  }

  private void addByteContent(final byte[] byteArray, final String contentType) {
    getByteContent().add(new ByteContentDataSource(null, contentType, byteArray));
  }

  /*
   * Getters and setters
   */

  @Nullable
  public String getQuery(final String name) {
    return aQueries == null ? null : aQueries.get(name);
  }

  @Nullable
  public String getPost(final String name) {
    if (aPost != null) {
      String result = aPost.get(name);
      if ((result == null) && (aAttachments != null)) {
        final DataSource source = aAttachments.get(name);
        if (source != null) {
          try {
            result = Streams.toString(new InputStreamReader(source.getInputStream(), "UTF-8"));
          } catch (@NotNull final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          } catch (@NotNull final IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return aPost == null ? null : aPost.get(name);
  }

  @Nullable
  public String getParam(final String name) {
    final String result = getQuery(name);
    if (result != null) {
      return result;
    }
    return getPost(name);
  }

  @Nullable
  public DataSource getAttachment(final String name) {
    if (aAttachments == null) {
      return null;
    }
    return aAttachments.get(name);
  }

  public Map<String, DataSource> getAttachments() {
    if (aAttachments == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(aAttachments);
  }


  @NotNull
  @XmlElement(name = "query", namespace = HttpMessage.NAMESPACE)
  public Collection<Query> getQueries() {
    if (aQueries == null) {
      aQueries = new HashMap<>();
    }
    return new QueryMapCollection(aQueries);
  }

  @NotNull
  @XmlElement(name = "post", namespace = HttpMessage.NAMESPACE)
  public Collection<Query> getPost() {
    if (aPost == null) {
      aPost = new HashMap<>();
    }
    return new QueryMapCollection(aPost);
  }

  @XmlElement(name = "body", namespace = HttpMessage.NAMESPACE)
  public Body getBody() {
    return aBody;
  }

  public void setBody(final Body body) {
    aBody = body;
  }

  public Collection<ByteContentDataSource> getByteContent() {
    if (aByteContent == null) {
      aByteContent = new ArrayList<>();
    }
    return aByteContent;
  }

  public void setRequestPath(final String pathInfo) {
    aRequestPath = pathInfo;
  }

  @XmlAttribute
  public String getRequestPath() {
    return aRequestPath;
  }

  public Iterable<String> getHeaders(final String name) {
    return Collections.unmodifiableList(aHeaders.get(name));
  }

  @Nullable
  public String getHeader(final String name) {
    final List<String> list = aHeaders.get(name);
    if ((list == null) || (list.size() < 1)) {
      return null;
    }
    return list.get(0);
  }

  public Map<String, List<String>> getHeaders() {
    return Collections.unmodifiableMap(aHeaders);
  }

  public String getMethod() {
    return aMethod;
  }

  @XmlAttribute
  public void setMethod(final String method) {
    aMethod = method;
  }

  public void setContextPath(final String contextPath) {
    aContextPath = contextPath;
  }

  @XmlAttribute
  public String getContextPath() {
    return aContextPath;
  }

  public String getContentType() {
    return aContentType;
  }

  @Nullable
  public String getCharacterEncoding() {
    return aCharacterEncoding;
  }

  @Nullable
  public Source getContent() {
    final List<Node> elements = aBody.getElements();
    if (elements.size() == 0) {
      return null;
    }
    if (elements.size() > 1) {
      throw new IllegalStateException("");
    }
    return new DOMSource(elements.get(0));
  }

  @XmlAttribute(name = "user")
  String getUser() {
    return aUserPrincipal.getName();
  }

  void setUser(final String name) {
    aUserPrincipal = new SimplePrincipal(name);
  }

  public Principal getUserPrincipal() {
    return aUserPrincipal;
  }

  void setUserPrincipal(final Principal userPrincipal) {
    aUserPrincipal = userPrincipal;
  }
}
