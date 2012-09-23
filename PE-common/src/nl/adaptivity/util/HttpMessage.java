package nl.adaptivity.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import net.devrieze.util.Iterators;
import net.devrieze.util.Streams;
import net.devrieze.util.security.SimplePrincipal;
import net.devrieze.util.webServer.HttpRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


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

    public List<Node> getElements() {
      @SuppressWarnings("unchecked")
      List<Node> result = (List<Node>) ((List<?>) elements);
      return result;
    }
  }

  @XmlAccessorType(XmlAccessType.NONE)
  public static class ByteContentDataSource implements DataSource {

    private String contentType;

    private byte[] byteContent;

    private String name;

    public ByteContentDataSource(String pName, String pContentType, byte[] pByteContent) {
      name = pName;
      contentType = pContentType;
      byteContent = pByteContent;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    @XmlAttribute
    public String getContentType() {
      return contentType;
    }

    public void setByteContent(byte[] byteContent) {
      this.byteContent = byteContent;
    }

    @XmlValue
    public byte[] getByteContent() {
      return byteContent;
    }

    public DataHandler getDataHandler() {
      return new DataHandler(this);
    }

    public void setName(String name) {
      this.name = name;
    }

    @XmlAttribute
    public String getName() {
      return name;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(byteContent);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException("Byte content is not writable");

    }

    @Override
    public String toString() {
      return "ByteContentDataSource [name=" + name + ", contentType=" + contentType + ", byteContent=\"" + new String(byteContent) + "\"]";
    }

  }

  public static class QueryIterator implements Iterator<Query> {

    private final Iterator<Entry<String, String>> aIterator;

    public QueryIterator(Iterator<Entry<String, String>> pIterator) {
      aIterator = pIterator;
    }

    @Override
    public boolean hasNext() {
      return aIterator != null && aIterator.hasNext();
    }

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

    public Query(Entry<String, String> pEntry) {
      aKey = pEntry.getKey();
      aValue = pEntry.getValue();
    }

    public void setKey(String key) {
      aKey = key;
    }

    @XmlAttribute(name = "key", required = true)
    public String getKey() {
      return aKey;
    }

    public void setValue(String value) {
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
      result = prime * result + ((aKey == null) ? 0 : aKey.hashCode());
      result = prime * result + ((aValue == null) ? 0 : aValue.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Query other = (Query) obj;
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

    public QueryMapCollection(Map<String, String> pMap) {
      aMap = pMap;
    }

    @Override
    public boolean add(Query pE) {
      if (aMap == null) {
        aMap = new HashMap<String, String>();
      }
      return aMap.put(pE.getKey(), pE.getValue()) != null;
    }

    @Override
    public void clear() {
      if (aMap != null) {
        aMap.clear();
      }
    }

    @Override
    public boolean contains(Object pO) {
      if (pO instanceof Query) {
        Query q = (Query) pO;
        String match = aMap.get(q.getKey());
        return (match == null && q.getValue() == null) || (q.getValue() != null && q.getValue().equals(match));
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

    @Override
    public Iterator<Query> iterator() {
      if (aMap == null) {
        return new QueryIterator(null);
      }
      return new QueryIterator(aMap.entrySet().iterator());
    }

    @Override
    public boolean remove(Object pO) {
      if (aMap == null) {
        return false;
      }
      Query q = (Query) pO;
      String candidate = aMap.get(q.getKey());

      if ((candidate == null && q.getValue() == null) || (candidate != null && candidate.equals(q.getValue()))) {
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

  private String aCharacterEncoding;
  
  private Map<String, List<String>> aHeaders;

  private Map<String, DataSource> aAttachments;

  private Principal aUserPrincipal;

  public HttpMessage(HttpServletRequest pRequest) throws UnsupportedEncodingException, IOException {
    aHeaders = getHeaders(pRequest);
    
    aQueries = toQueries(pRequest.getQueryString());
    aUserPrincipal = pRequest.getUserPrincipal();
    
    setMethod(pRequest.getMethod());
    String pathInfo = pRequest.getPathInfo();
    setRequestPath(pathInfo==null || pathInfo.length()==0? pRequest.getServletPath(): pathInfo);
    setContextPath(pRequest.getContextPath());
    if ("POST".equals(pRequest.getMethod()) || "PUT".equals(pRequest.getMethod())) {
      aContentType = pRequest.getContentType();
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
            int j = param.indexOf('=');
            if (j >= 0) {
              String paramName = param.substring(0, j).trim();
              if (paramName.equals("charset")) {
                aCharacterEncoding = param.substring(j + 1).trim();
              }
            }

          }
        }
      }
      if (aCharacterEncoding == null) {
        aCharacterEncoding = pRequest.getCharacterEncoding();
      }
      if (aCharacterEncoding == null) {
        aCharacterEncoding = "UTF-8";
      }
      boolean isMultipart = aContentType!=null && aContentType.startsWith("multipart/");
      if ("application/x-www-form-urlencoded".equals(aContentType)) {
        aPost = toQueries(getBody(pRequest).toString(aCharacterEncoding));
      } else if (isMultipart) {
        aAttachments = HttpRequest.parseMultipartFormdata(pRequest.getInputStream(), HttpRequest.mimeType(pRequest.getContentType()), null);
      } else {
        ByteArrayOutputStream baos = getBody(pRequest);

        final Document xml;

        xml = XmlUtil.tryParseXml(new ByteArrayInputStream(baos.toByteArray()));
        if (xml == null) {
          addByteContent(baos.toByteArray(), pRequest.getContentType());
        } else {
          aBody = new Body();
          aBody.elements = new ArrayList<Object>(1);
          aBody.elements.add(xml.getDocumentElement());
        }

      }
    }
  }

  /*
   * Utility methods
   */

  @SuppressWarnings("unchecked")
  private Map<String, List<String>> getHeaders(HttpServletRequest pRequest) {
    Map<String, List<String>> result = new HashMap<String, List<String>>();
    for (String name : Iterators.<String>toIterable(pRequest.getHeaderNames())) {
      List<String> values = Iterators.<String>toList(pRequest.getHeaders(name));
      result.put(name, values);
    }
    return result;
  }

  private static Map<String, String> toQueries(String pQueryString) {
      Map<String, String> result = new HashMap<String, String>();
      
      if (pQueryString==null) {
          return result;
      }
      
      String query = pQueryString;
      if ((query.length() > 0) && (query.charAt(0) == '?')) {
        /* strip questionmark */
        query = query.substring(1);
      }

      int startPos=0;
      String key=null;
      for(int i=0; i<query.length(); ++i) {
        if (key==null) {
          if('='==query.charAt(i)) {
            key=query.substring(startPos, i);
            startPos=i+1;
          }
        } else {
          if ('&'==query.charAt(i)||';'==query.charAt(i)) {
            String value;
            try {
              value = URLDecoder.decode(query.substring(startPos,i), "UTF-8");
            } catch (UnsupportedEncodingException e) {
              throw new RuntimeException(e);
            }
            result.put(key, value);
            key=null;
            startPos=i+1;
          }
        }
      }
      if (key==null) {
        key = query.substring(startPos);
        if (key.length()>0) { result.put(key,""); }
      } else {
        try {
          String value = URLDecoder.decode(query.substring(startPos), "UTF-8");
          result.put(key, value);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }

      }

      return result;
  }


  private static ByteArrayOutputStream getBody(HttpServletRequest pRequest) throws IOException {
      ByteArrayOutputStream baos;
      {
      int contentLength = pRequest.getContentLength();
      baos = contentLength > 0 ? new ByteArrayOutputStream(contentLength) : new ByteArrayOutputStream();
      byte[] buffer = new byte[0xfffff];
      ServletInputStream is = pRequest.getInputStream();
      int i;
      while ((i=is.read(buffer))>=0) {
          baos.write(buffer, 0, i);
      }
      }
      return baos;
  }

  private void addByteContent(byte[] pByteArray, String pContentType) {
      getByteContent().add(new ByteContentDataSource(null, pContentType, pByteArray));
  }
  
  /*
   * Getters and setters
   */
  
  public String getQuery(String pName) {
    return aQueries == null ? null : aQueries.get(pName);
  }

  public String getPost(String pName) {
    if (aPost!=null) {
      String result = aPost.get(pName);
      if (result==null && aAttachments!=null) {
        DataSource source = aAttachments.get(pName);
        if (source!=null) {
          try {
            result = Streams.toString(new InputStreamReader(source.getInputStream(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return aPost == null ? null : aPost.get(pName);
  }

  public String getParam(String pName) {
    String result = getQuery(pName);
    if (result != null) {
      return result;
    }
    return getPost(pName);
  }
  
  public DataSource getAttachment(String pName) {
    if (aAttachments==null) { return null; }
    return aAttachments.get(pName);
  }
  
  public Map<String, DataSource> getAttachments() {
    if (aAttachments==null) { return Collections.emptyMap(); }
    return Collections.unmodifiableMap(aAttachments);
  }
  

  @XmlElement(name = "query", namespace = HttpMessage.NAMESPACE)
  public Collection<Query> getQueries() {
    if (aQueries == null) {
      aQueries = new HashMap<String, String>();
    }
    return new QueryMapCollection(aQueries);
  }

  @XmlElement(name = "post", namespace = HttpMessage.NAMESPACE)
  public Collection<Query> getPost() {
    if (aPost == null) {
      aPost = new HashMap<String, String>();
    }
    return new QueryMapCollection(aPost);
  }

  @XmlElement(name = "body", namespace = HttpMessage.NAMESPACE)
  public Body getBody() {
    return aBody;
  }

  public void setBody(Body pBody) {
    aBody = pBody;
  }

  public Collection<ByteContentDataSource> getByteContent() {
    if (aByteContent == null) {
      aByteContent = new ArrayList<ByteContentDataSource>();
    }
    return aByteContent;
  }

  public void setRequestPath(String pathInfo) {
    aRequestPath = pathInfo;
  }

  @XmlAttribute
  public String getRequestPath() {
    return aRequestPath;
  }
  
  public Iterable<String> getHeaders(String pName) {
    return Collections.unmodifiableList(aHeaders.get(pName));
  }
  
  public String getHeader(String pName) {
    List<String> list = aHeaders.get(pName);
    if (list==null || list.size()<1) { return null; }
    return list.get(0);
  }
  
  public Map<String, List<String>> getHeaders() {
    return Collections.unmodifiableMap(aHeaders);
  }

  public String getMethod() {
    return aMethod;
  }

  @XmlAttribute
  public void setMethod(String method) {
    aMethod = method;
  }

  public void setContextPath(String contextPath) {
    aContextPath = contextPath;
  }

  @XmlAttribute
  public String getContextPath() {
    return aContextPath;
  }

  public Source getContent() {
    final List<Node> elements = aBody.getElements();
    if (elements.size()==0) {
      return null;
    }
    if (elements.size()>1) {
      throw new IllegalStateException("");
    }
    return new DOMSource(elements.get(0)); 
  }

  @XmlAttribute(name="user")
  String getUser() {
    return aUserPrincipal.getName();
  }
  
  void setUser(String pName) {
    aUserPrincipal = new SimplePrincipal(pName);
  }
  
  public Principal getUserPrincipal() {
    return aUserPrincipal;
  }
  
  void setUserPrincipal(Principal pUserPrincipal) {
    aUserPrincipal = pUserPrincipal;
  }
}
