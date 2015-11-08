package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;

import java.io.CharArrayReader;
import java.io.IOException;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class BaseMessage extends XMLContainer implements IXmlMessage{

  private QName service;
  private String endpoint;
  private String operation;
  private String url;
  private String method;
  private String type;

  public BaseMessage() {
    super();
  }

  @Deprecated
  public BaseMessage(QName pService, String pEndpoint, String pOperation, String pUrl, String pMethod, String pContentType, Node pMessageBody) throws
          XMLStreamException {
    this(pService, pEndpoint, pOperation, pUrl, pMethod, pContentType, new DOMSource(pMessageBody));
  }

  public BaseMessage(QName pService, String pEndpoint, String pOperation, String pUrl, String pMethod, String pContentType, Source pMessageBody) throws
    XMLStreamException {
    super(pMessageBody);
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
         pMessage.getBodySource());
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter pOut) throws XMLStreamException {
    super.serializeAttributes(pOut);
    XmlUtil.writeAttribute(pOut, "type", getContentType());
    XmlUtil.writeAttribute(pOut, "serviceNS", getServiceNS());
    XmlUtil.writeAttribute(pOut, "serviceName", getServiceName());
    XmlUtil.writeAttribute(pOut, "endpoint", getEndpoint());
    XmlUtil.writeAttribute(pOut, "operation", getOperation());
    XmlUtil.writeAttribute(pOut, "url", getUrl());
    XmlUtil.writeAttribute(pOut, "method", getMethod());
  }


  public boolean deserializeAttribute(final String pAttributeNamespace, final String pAttributeLocalName, final String pAttributeValue) {
    if (XMLConstants.NULL_NS_URI.equals(pAttributeNamespace)) {
      switch (pAttributeLocalName) {
        case "endpoint": endpoint = pAttributeValue; return true;
        case "operation": operation = pAttributeValue; return true;
        case "url": url=pAttributeValue; return true;
        case "method": method = pAttributeValue; return true;
        case "type": type = pAttributeValue; return true;
        case "serviceNS": setServiceNS(pAttributeValue); return true;
        case "serviceName": setServiceName(pAttributeValue); return true;
      }
    }
    return false;
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
  public String getOperation() {
    return operation;
  }

  @Override
  public CompactFragment getMessageBody() {
    return new CompactFragment(getOriginalNSContext(), getContent());
  }

  public DocumentFragment getMessageBodyNode() {
    try {
      return XmlUtil.tryParseXmlFragment(new CharArrayReader(getContent()));
    } catch (IOException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public void setMessageBody(final Source o) {
    try {
      setContent(o);
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public Source getBodySource() {
    try {
      return new StAXSource(getBodyStreamReader());
    } catch (XMLStreamException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public void setOperation(final String value) {
    this.operation = value;
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
    return XmlUtil.toString(this);
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