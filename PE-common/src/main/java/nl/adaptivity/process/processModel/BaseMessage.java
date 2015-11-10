package nl.adaptivity.process.processModel;

import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;

import java.io.CharArrayReader;
import java.io.IOException;

public abstract class BaseMessage extends XMLContainer implements IXmlMessage{

  private QName mService;
  private String mEndpoint;
  private String mOperation;
  private String mUrl;
  private String mMethod;
  private String mType;

  public BaseMessage() {
    super();
  }

  @Deprecated
  public BaseMessage(final QName service, final String endpoint, final String operation, final String url, final String method, final String contentType, final Node messageBody) throws
          XMLStreamException {
    this(service, endpoint, operation, url, method, contentType, new DOMSource(messageBody));
  }

  public BaseMessage(final QName service, final String endpoint, final String operation, final String url, final String method, final String contentType, final Source messageBody) throws
    XMLStreamException {
    super(messageBody);
    this.mService = service;
    this.mEndpoint = endpoint;
    this.mOperation = operation;
    this.mUrl = url;
    this.mMethod = method;
    mType = contentType;
  }

  public BaseMessage(@NotNull final IXmlMessage message) throws XMLStreamException {
    this(message.getService(),
         message.getEndpoint(),
         message.getOperation(),
         message.getUrl(),
         message.getMethod(),
         message.getContentType(),
         message.getBodySource());
  }

  @Override
  protected void serializeAttributes(final XMLStreamWriter out) throws XMLStreamException {
    super.serializeAttributes(out);
    XmlUtil.writeAttribute(out, "type", getContentType());
    XmlUtil.writeAttribute(out, "serviceNS", getServiceNS());
    XmlUtil.writeAttribute(out, "serviceName", getServiceName());
    XmlUtil.writeAttribute(out, "endpoint", getEndpoint());
    XmlUtil.writeAttribute(out, "operation", getOperation());
    XmlUtil.writeAttribute(out, "url", getUrl());
    XmlUtil.writeAttribute(out, "method", getMethod());
  }


  public boolean deserializeAttribute(final String attributeNamespace, final String attributeLocalName, final String attributeValue) {
    if (XMLConstants.NULL_NS_URI.equals(attributeNamespace)) {
      switch (attributeLocalName) {
        case "endpoint": mEndpoint = attributeValue; return true;
        case "operation": mOperation = attributeValue; return true;
        case "url": mUrl =attributeValue; return true;
        case "method": mMethod = attributeValue; return true;
        case "type": mType = attributeValue; return true;
        case "serviceNS": setServiceNS(attributeValue); return true;
        case "serviceName": setServiceName(attributeValue); return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public String getServiceName() {
    return mService ==null ? null : mService.getLocalPart();
  }

  @Override
  public void setServiceName(final String name) {
    if (mService == null) {
      mService = new QName(name);
    } else {
      mService = new QName(mService.getNamespaceURI(), name);
    }
  }

  @Nullable
  @Override
  public String getServiceNS() {
    return mService ==null ? null : mService.getNamespaceURI();
  }

  @Override
  public void setServiceNS(final String namespace) {
    if (mService == null) {
      mService = new QName(namespace, "xx");
    } else {
      mService = new QName(namespace, mService.getLocalPart());
    }
  }

  @Override
  public QName getService() {
    return mService;
  }

  @Override
  public void setService(final QName value) {
    this.mService = value;
  }

  @Override
  public String getEndpoint() {
    return mEndpoint;
  }

  @Override
  public void setEndpoint(final String value) {
    this.mEndpoint = value;
  }

  @Override
  public String getOperation() {
    return mOperation;
  }

  @NotNull
  @Override
  public CompactFragment getMessageBody() {
    return new CompactFragment(getOriginalNSContext(), getContent());
  }

  public DocumentFragment getMessageBodyNode() {
    try {
      return XmlUtil.tryParseXmlFragment(new CharArrayReader(getContent()));
    } catch (@NotNull final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setMessageBody(final Source o) {
    try {
      setContent(o);
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  public Source getBodySource() {
    try {
      return new StAXSource(getBodyStreamReader());
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setOperation(final String value) {
    this.mOperation = value;
  }

  @Override
  public String getUrl() {
    return mUrl;
  }

  @Override
  public void setUrl(final String value) {
    this.mUrl = value;
  }

  @Override
  public String getMethod() {
    return mMethod;
  }

  @Override
  public void setMethod(final String value) {
    this.mMethod = value;
  }

  @Override
  public String getContentType() {
    if (mType == null) {
      return "application/soap+xml";
    } else {
      return mType;
    }
  }

  @Override
  public void setType(final String type) {
    this.mType = type;
  }

  @Override
  public String toString() {
    return XmlUtil.toString(this);
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BaseMessage that = (BaseMessage) o;

    if (mService != null ? !mService.equals(that.mService) : that.mService != null) return false;
    if (mEndpoint != null ? !mEndpoint.equals(that.mEndpoint) : that.mEndpoint != null) return false;
    if (mOperation != null ? !mOperation.equals(that.mOperation) : that.mOperation != null) return false;
    if (mUrl != null ? !mUrl.equals(that.mUrl) : that.mUrl != null) return false;
    if (mMethod != null ? !mMethod.equals(that.mMethod) : that.mMethod != null) return false;
    if (mType != null ? !mType.equals(that.mType) : that.mType != null) return false;

    final char[] body = getContent();
    final char[] thatBody = that.getContent();
    if (body==null) return thatBody==null;
    if (thatBody==null) return false;
    return body.equals(thatBody);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mService != null ? mService.hashCode() : 0);
    result = 31 * result + (mEndpoint != null ? mEndpoint.hashCode() : 0);
    result = 31 * result + (mOperation != null ? mOperation.hashCode() : 0);
    result = 31 * result + (mUrl != null ? mUrl.hashCode() : 0);
    result = 31 * result + (mMethod != null ? mMethod.hashCode() : 0);
    result = 31 * result + (mType != null ? mType.hashCode() : 0);
    return result;
  }
}