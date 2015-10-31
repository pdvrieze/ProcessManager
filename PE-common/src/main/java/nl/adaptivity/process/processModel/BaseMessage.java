package nl.adaptivity.process.processModel;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.adaptivity.util.xml.SimpleNamespaceContext;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class BaseMessage extends XMLContainer implements IXmlMessage{

  private QName service;
  private String endpoint;
  private QName operation;
  private String url;
  private String method;
  private String type;

  public BaseMessage() {
    super();
  }

  public BaseMessage(QName pService, String pEndpoint, QName pOperation, String pUrl, String pMethod, String pContentType, Node pMessageBody) throws
          XMLStreamException {
    super(new DOMSource(pMessageBody));
    service = pService;
    endpoint = pEndpoint;
    operation = pOperation;
    url = pUrl;
    method = pMethod;
    type = pContentType;
  }

  public BaseMessage(IXmlMessage pMessage) throws XMLStreamException {
    this(pMessage.getService(),
         pMessage.getEndpoint(),
         pMessage.getOperation(),
         pMessage.getUrl(),
         pMessage.getMethod(),
         pMessage.getContentType(),
         pMessage.getMessageBody());
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeAttributes(pOut);
    XmlUtil.writeAttribute(pOut, "serviceName", getServiceName());
    XmlUtil.writeAttribute(pOut, "serviceNS", getServiceNS());
    XmlUtil.writeAttribute(pOut, "endpoint", getEndpoint());
    XmlUtil.writeAttribute(pOut, "operation", getOperation());
    XmlUtil.writeAttribute(pOut, "url", getUrl());
    XmlUtil.writeAttribute(pOut, "method", getMethod());
    XmlUtil.writeAttribute(pOut, "type", getContentType());
  }


  @Override
  public String getServiceName() {
    return service==null ? null : service.getLocalPart();
  }

  @Override
  public void setServiceName(final String pName) {
    if (service == null) {
      service = new QName(pName);
    } else {
      service = new QName(service.getNamespaceURI(), pName);
    }
  }

  @Override
  public String getServiceNS() {
    return service==null ? null : service.getNamespaceURI();
  }

  @Override
  public void setServiceNS(final String pNamespace) {
    if (service == null) {
      service = new QName(pNamespace, "xx");
    } else {
      service = new QName(pNamespace, service.getLocalPart());
    }
  }

  @Override
  public QName getService() {
    return service;
  }

  @Override
  public void setService(final QName value) {
    this.service = value;
  }

  @Override
  public String getEndpoint() {
    return endpoint;
  }

  @Override
  public void setEndpoint(final String value) {
    this.endpoint = value;
  }

  @Override
  public QName getOperation() {
    return operation;
  }

  @Override
  public Node getMessageBody() {
    try {
      return XmlUtil.tryParseXml(new CharArrayReader(getContent()));
    } catch (IOException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public void setMessageBody(final Object o) {
    if (o instanceof Node) {
      try {
        setContent(new DOMSource((Node) o));
      } catch (XMLStreamException pE) {
        throw new RuntimeException(pE);
      }
    }
  }

  @Override
  public void setOperation(final QName value) {
    this.operation = value;
  }

  @Override
  public Source getBodySource() {
    return new DOMSource(getMessageBody());
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public void setUrl(final String value) {
    this.url = value;
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public void setMethod(final String value) {
    this.method = value;
  }

  @Override
  public String getContentType() {
    if (type == null) {
      return "application/soap+xml";
    } else {
      return type;
    }
  }

  @Override
  public void setType(final String pType) {
    type = pType;
  }

  @Override
  public String toString() {
    final TransformerFactory tf = TransformerFactory.newInstance();
    Transformer t;
    try {
      t = tf.newTransformer();
    } catch (final TransformerConfigurationException e) {
      return super.toString();
    }
    final StringWriter sw = new StringWriter();
    final StreamResult sr = new StreamResult(sw);
    try {
      t.transform(getBodySource(), sr);
    } catch (final TransformerException e) {
      return super.toString();
    }
    return sw.toString();
  }

  @Override
  public Collection<Object> getAny() {
    return Arrays.<Object>asList(getMessageBody());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaseMessage that = (BaseMessage) o;

    if (service != null ? !service.equals(that.service) : that.service != null) return false;
    if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) return false;
    if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
    if (url != null ? !url.equals(that.url) : that.url != null) return false;
    if (method != null ? !method.equals(that.method) : that.method != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;

    char[] body = getContent();
    char[] thatBody = that.getContent();
    if (body==null) return thatBody==null;
    if (thatBody==null) return false;
    return body.equals(thatBody);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (service != null ? service.hashCode() : 0);
    result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
    result = 31 * result + (operation != null ? operation.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (method != null ? method.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}