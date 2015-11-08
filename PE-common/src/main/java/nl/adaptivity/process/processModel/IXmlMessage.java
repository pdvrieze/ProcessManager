package nl.adaptivity.process.processModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.util.xml.CompactFragment;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;


public interface IXmlMessage {

  /**
   * Gets the value of the service property.
   *
   * @return possible object is {@link QName }
   */
  String getServiceName();

  void setServiceName(String pName);

  String getServiceNS();

  void setServiceNS(String pNamespace);

  QName getService();

  /**
   * Sets the value of the service property.
   *
   * @param value allowed object is {@link QName }
   */
  void setService(QName value);

  /**
   * Gets the value of the endpoint property.
   *
   * @return possible object is {@link String }
   */
  String getEndpoint();

  /**
   * Sets the value of the endpoint property.
   *
   * @param value allowed object is {@link String }
   */
  void setEndpoint(String value);

  EndpointDescriptor getEndpointDescriptor();

  /**
   * Gets the value of the operation property.
   *
   * @return possible object is {@link String }
   */
  String getOperation();

  @Deprecated
  void setMessageBody(Source o);

  CompactFragment getMessageBody();

  Source getBodySource();

  /**
   * Sets the value of the operation property.
   *
   * @param value allowed object is {@link String }
   */
  void setOperation(String value);

  /**
   * Gets the value of the url property.
   *
   * @return possible object is {@link String }
   */
  String getUrl();

  /**
   * Sets the value of the url property.
   *
   * @param value allowed object is {@link String }
   */
  void setUrl(String value);

  /**
   * Gets the value of the method property.
   *
   * @return possible object is {@link String }
   */
  String getMethod();

  /**
   * Sets the value of the method property.
   *
   * @param value allowed object is {@link String }
   */
  void setMethod(String value);

  String getContentType();

  void setType(String pType);

  @Override
  String toString();

}