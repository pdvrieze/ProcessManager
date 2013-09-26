package nl.adaptivity.process.processModel;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Node;

import nl.adaptivity.messaging.EndpointDescriptor;


public interface IXmlMessage {

  /**
   * Gets the value of the service property.
   *
   * @return possible object is {@link QName }
   */
  public String getServiceName();

  public void setServiceName(String pName);

  public String getServiceNS();

  public void setServiceNS(String pNamespace);

  public QName getService();

  /**
   * Sets the value of the service property.
   *
   * @param value allowed object is {@link QName }
   */
  public void setService(QName value);

  /**
   * Gets the value of the endpoint property.
   *
   * @return possible object is {@link String }
   */
  public String getEndpoint();

  /**
   * Sets the value of the endpoint property.
   *
   * @param value allowed object is {@link String }
   */
  public void setEndpoint(String value);

  public EndpointDescriptor getEndpointDescriptor();

  /**
   * Gets the value of the operation property.
   *
   * @return possible object is {@link String }
   */
  public QName getOperation();

  public Collection<Object> getAny();

  public Node getMessageBody();

  public void setMessageBody(Object o);

  /**
   * Sets the value of the operation property.
   *
   * @param value allowed object is {@link String }
   */
  public void setOperation(QName value);

  public Source getBodySource();

  /**
   * Gets the value of the url property.
   *
   * @return possible object is {@link String }
   */
  public String getUrl();

  /**
   * Sets the value of the url property.
   *
   * @param value allowed object is {@link String }
   */
  public void setUrl(String value);

  /**
   * Gets the value of the method property.
   *
   * @return possible object is {@link String }
   */
  public String getMethod();

  /**
   * Sets the value of the method property.
   *
   * @param value allowed object is {@link String }
   */
  public void setMethod(String value);

  public String getContentType();

  public void setType(String pType);

  public String toString();

}