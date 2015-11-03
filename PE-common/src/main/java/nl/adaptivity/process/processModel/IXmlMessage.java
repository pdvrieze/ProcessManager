package nl.adaptivity.process.processModel;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.DocumentFragment;

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
  public String getOperation();

  @Deprecated
  public DocumentFragment getMessageBody();

  @Deprecated
  public void setMessageBody(Object o);

  /**
   * Sets the value of the operation property.
   *
   * @param value allowed object is {@link String }
   */
  public void setOperation(String value);

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

  @Override
  public String toString();

}