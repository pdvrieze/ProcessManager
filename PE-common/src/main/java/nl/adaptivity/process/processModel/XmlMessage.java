//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * <p>
 * Java class for Message complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="Message">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='lax'/>
 *       &lt;/sequence>
 *       &lt;attribute name="serviceNS" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="endpoint" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="operation" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="serviceName" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Message")
@XmlRootElement(name= XmlMessage.ELEMENTLOCALNAME)
public class XmlMessage extends BaseMessage implements IXmlMessage {

  public static final String ELEMENTLOCALNAME = "message";

  public static final QName ELEMENTNAME=new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  @XmlTransient ArrayList<Object> aAny;

  public XmlMessage() { /* default constructor */ }


  public XmlMessage(QName pService, String pEndpoint, QName pOperation, String pUrl, String pMethod, String pContentType, Node pMessageBody, Collection<Object> pAny) throws
          XMLStreamException {
    super(pService, pEndpoint, pOperation, pUrl, pMethod, pContentType, pMessageBody);
    aAny = (pAny instanceof ArrayList) ? (ArrayList<Object>) pAny : new ArrayList<>(pAny);
  }


  public static XmlMessage get(IXmlMessage pMessage) throws XMLStreamException {
    if (pMessage instanceof XmlMessage) { return (XmlMessage) pMessage; }
    return new XmlMessage(pMessage.getService(),
                          pMessage.getEndpoint(),
                          pMessage.getOperation(),
                          pMessage.getUrl(),
                          pMessage.getMethod(),
                          pMessage.getContentType(),
                          pMessage.getMessageBody(),
                          pMessage.getAny());
  }

  @Override
  protected void serializeStartElement(final XMLStreamWriter pOut) throws XMLStreamException {
    XmlUtil.writeStartElement(pOut, ELEMENTNAME);
  }

  /* (non-Javadoc)
       * @see nl.adaptivity.process.processModel.IXmlMessage#getAny()
       */
  @Override
  @XmlAnyElement(lax = true)
  public Collection<Object> getAny() {

    if (aAny == null) {
      aAny = new ArrayList<>(1);
      if (getMessageBody() != null) {
        aAny.add(getMessageBody());
        setMessageBody(null);
      }
    }
    return aAny;
  }


  @Override
  @XmlTransient
  public Node getMessageBody() {
    Node body = super.getMessageBody();
    if ((body == null) && (aAny != null)) {
      final Iterator<Object> it = aAny.iterator();
      while (it.hasNext()) {
        final Object next = it.next();
        if ((next instanceof Element) || (next instanceof Document) || (next instanceof DocumentFragment)) {
          if (body != null) {
            throw new IllegalStateException("Only one member allowed");
          }
          body = (Node) next;
        }
      }
      if (body != null) {
        setMessageBody(body);
        aAny = null;
      }
    }
    return body;
  }

  @Override
  public void setServiceName(String pName) {
    super.setServiceName(pName);
  }

  @Override
  @XmlAttribute(name = "serviceName", required = true)
  public String getServiceName() {
    return super.getServiceName();
  }

  @Override
  public void setServiceNS(String pNamespace) {
    super.setServiceNS(pNamespace);
  }

  @Override
  @XmlAttribute(name = "serviceNS")
  public String getServiceNS() {
    return super.getServiceNS();
  }

  @Override
  public void setEndpoint(String pValue) {
    super.setEndpoint(pValue);
  }

  @Override
  @XmlAttribute(name = "endpoint")
  public String getEndpoint() {
    return super.getEndpoint();
  }

  @Override
  public EndpointDescriptor getEndpointDescriptor() {
    final String url = getUrl();
    return new EndpointDescriptorImpl(getService(), getEndpoint(), url==null ? null : URI.create(url));
  }

  @Override
  public void setOperation(QName pValue) {
    super.setOperation(pValue);
  }


  @Override
  @XmlAttribute(name = "operation")
  public QName getOperation() {
    return super.getOperation();
  }

  @Override
  public void setUrl(String pValue) {
    super.setUrl(pValue);
  }

  @Override
  @XmlAttribute(name = "url")
  public String getUrl() {
    return super.getUrl();
  }

  @Override
  public void setMethod(String pValue) {
    super.setMethod(pValue);
  }

  @Override
  @XmlAttribute(name = "method")
  public String getMethod() {
    return super.getMethod();
  }

  public void setContentType(String pType) {
    super.setType(pType);
  }


  @Override
  @XmlAttribute(name = "type")
  public String getContentType() {
    return super.getContentType();
  }


}
