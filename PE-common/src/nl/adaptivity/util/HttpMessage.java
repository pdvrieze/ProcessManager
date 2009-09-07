package nl.adaptivity.util;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@XmlRootElement(name = "httpMessage", namespace=HttpMessage.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="HttpMessage", namespace=HttpMessage.NAMESPACE)
public class HttpMessage {
    
    public static final String NAMESPACE = "http://adaptivity.nl/HttpMessage";
    
    
    @XmlType(name="Body", namespace=HttpMessage.NAMESPACE)
    public static class Body {
        @XmlAnyElement(lax=false)
        List<Object> elements;

    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ByteContent implements DataSource {
        
        private String contentType;
        
        private byte[] byteContent;
        
        private String name;

        public ByteContent(String pName, String pContentType, byte[] pByteContent) {
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
            if (aIterator==null) {
                throw new IllegalStateException("Removing elements from empty collection");
            }
            aIterator.remove();
        }

    }

    @XmlType(name="Query", namespace=HttpMessage.NAMESPACE)
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
        @XmlAttribute(name="key", required=true)
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
            if (aMap==null) { aMap = new HashMap<String, String>(); }
            return aMap.put(pE.getKey(), pE.getValue())!=null;
        }

        @Override
        public void clear() {
            if (aMap!=null) {
                aMap.clear();
            }
        }

        @Override
        public boolean contains(Object pO) {
            if (pO instanceof Query) {
                Query q = (Query) pO;
                String match = aMap.get(q.getKey());
                return (match==null && q.getValue()==null) || (q.getValue()!=null && q.getValue().equals(match));
            } else {
                return false;
            }
        }

        @Override
        public boolean isEmpty() {
            if (aMap==null) { return true; }
            return aMap.isEmpty();
        }

        @Override
        public Iterator<Query> iterator() {
            if (aMap==null) {
                return new QueryIterator(null);
            }
            return new QueryIterator(aMap.entrySet().iterator());
        }

        @Override
        public boolean remove(Object pO) {
            if (aMap == null) { return false; }
            Query q = (Query) pO;
            String candidate = aMap.get(q.getKey());
            
            if ((candidate==null && q.getValue()==null)|| (candidate!=null && candidate.equals(q.getValue()))) {
                aMap.remove(q.getKey());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int size() {
            if (aMap==null) { return 0; }
            return aMap.size();
        }

    }

    private Map<String, String> aQueries;
    private Map<String, String> aPost;
    private Body aBody;
    private Collection<ByteContent> aByteContent;
    private String aPathInfo;
    private String aContextPath;

    protected HttpMessage() {
        
    }
    
    public HttpMessage(HttpServletRequest pRequest) throws IOException {
        aQueries = toQueries(pRequest.getQueryString());
        setPathInfo(pRequest.getPathInfo());
        setContextPath(pRequest.getContextPath());
        if ("POST".equals(pRequest.getMethod()) || "PUT".equals(pRequest.getMethod())) {
            String contentType = pRequest.getContentType();
            if ("application/x-www-form-urlencoded".equals(contentType)) {
                String charEncoding = pRequest.getCharacterEncoding();
                if (charEncoding==null) { charEncoding= "UTF-8"; }
                aPost = toQueries(getBody(pRequest).toString(charEncoding));
            } else if ("multipart/form-data".equals(contentType)) {
                throw new UnsupportedOperationException("Not yet implemented");
            } else {
                ByteArrayOutputStream baos = getBody(pRequest);
                
                final Document xml;
                
                xml = tryParseXml(new ByteArrayInputStream(baos.toByteArray()));
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

    private ByteArrayOutputStream getBody(HttpServletRequest pRequest) throws IOException {
        ByteArrayOutputStream baos;
        {
        int contentLength = pRequest.getContentLength();
        baos = new ByteArrayOutputStream(contentLength);
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
        getByteContent().add(new ByteContent(null, pContentType, pByteArray));
    }

    private Document tryParseXml(ByteArrayInputStream pByteArrayInputStream) throws IOException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            
            Document d = db.parse(pByteArrayInputStream);
            return d;
        } catch (SAXException e) {
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, String> toQueries(String pQueryString) {
        Map<String, String> result = new HashMap<String, String>();
        
        if (pQueryString==null) {
            return result;
        }
        
        String query;
        if ((pQueryString.length() > 0) && (pQueryString.charAt(0) == '?')) {
          query = pQueryString.substring(1);
        } else {
          query = pQueryString;
        }
        /* strip questionmark */

        int i;

        while ((i = query.indexOf('&')) >= 0) {
          final String avp = query.substring(0, i);
          query = query.substring(i + 1);

          final int j = avp.indexOf('=');
          String value = "";

          if ((j + 1) < avp.length()) {
            try {
              value = URLDecoder.decode(avp.substring(j + 1), "UTF-8");
            } catch (final UnsupportedEncodingException e) {
              e.printStackTrace();
              value = avp.substring(j + 1);
            }
          }

          result.put(avp.substring(0, j), value);
        }

        final String avp = query;
        final int j = avp.indexOf('=');
        String value = "";

        if (j>=0) {
            if ((j + 1) < avp.length()) {
              try {
                value = URLDecoder.decode(avp.substring(j + 1), "UTF-8");
              } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
                value = avp.substring(j + 1);
              }
            }
            result.put(avp.substring(0, j), value);
        } else {
            result.put(avp, "");
        }

        return result;
    }

    public String getQuery(String pName) {
        return aQueries.get(pName);
    }
    
    public String getPost(String pName) {
        return aPost.get(pName);
    }
    
    public String getParam(String pName) {
        String result = getQuery(pName);
        if (result != null) {
            return result;
        }
        return getPost(pName);
    }
    
    @XmlElement(name="query", namespace=HttpMessage.NAMESPACE)
    public Collection<Query> getQueries() {
        return new QueryMapCollection(aQueries);
    }
    
    @XmlElement(name="post", namespace=HttpMessage.NAMESPACE)
    public Collection<Query> getPost() {
        return new QueryMapCollection(aPost);
    }

    @XmlElement(name="body", namespace=HttpMessage.NAMESPACE)
    public Body getBody() {
        return aBody;
    }
    
    public void setBody(Body pBody) {
        aBody = pBody;
    }
    
    public Collection<ByteContent> getByteContent() {
        if (aByteContent==null) {
            aByteContent= new ArrayList<ByteContent>();
        }
        return aByteContent; 
    }

    public void setPathInfo(String pathInfo) {
        aPathInfo = pathInfo;
    }

    @XmlAttribute
    public String getPathInfo() {
        return aPathInfo;
    }

    public void setContextPath(String contextPath) {
        aContextPath = contextPath;
    }

    @XmlAttribute
    public String getContextPath() {
        return aContextPath;
    }
}
