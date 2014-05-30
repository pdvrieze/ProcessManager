package nl.adaptivity.process.processModel;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;

import org.w3c.dom.Node;


public class BaseMessage implements IXmlMessage{

  private QName service;
  private String endpoint;
  private QName operation;
  private String url;
  private String method;
  private String type;
  private Node aBody;

  public BaseMessage() {
    super();
  }

  public BaseMessage(QName pService, String pEndpoint, QName pOperation, String pUrl, String pMethod, String pContentType, Node pMessageBody) {
    service = pService;
    endpoint = pEndpoint;
    operation = pOperation;
    url = pUrl;
    method = pMethod;
    type = pContentType;
    aBody = pMessageBody;
  }

  @Override
  public String getServiceName() {
    return service.getLocalPart();
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
    return service.getNamespaceURI();
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
  public EndpointDescriptor getEndpointDescriptor() {
    return new EndpointDescriptorImpl(service, endpoint, URI.create(url));
  }

  @Override
  public QName getOperation() {
    return operation;
  }

  @Override
  public Node getMessageBody() {
    return aBody;
  }

  @Override
  public void setMessageBody(final Object o) {
    if (o instanceof Node) {
      aBody = (Node) o;
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

}