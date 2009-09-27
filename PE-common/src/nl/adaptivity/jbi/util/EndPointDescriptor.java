package nl.adaptivity.jbi.util;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;


@XmlRootElement(name="endpointDescriptor", namespace=EndPointDescriptor.MY_JBI_NS)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder={"serviceNamespace", "serviceLocalName", "endpointName"})
public class EndPointDescriptor {

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";
  private String aServiceLocalName;
  private String aServiceNamespace;
  private String aEndpointName;

  public EndPointDescriptor() {}

  public EndPointDescriptor(QName pServiceName, String pEndpointName) {
    aServiceLocalName = pServiceName.getLocalPart();
    aServiceNamespace = pServiceName.getNamespaceURI();
    aEndpointName = pEndpointName;
  }

  @XmlAttribute(name="endpointName")
  public String getEndpointName() {
    return aEndpointName;
  }

  public void setEndpointName(String endpointName) {
    aEndpointName = endpointName;
  }

  @XmlAttribute(name="serviceLocalName")
  public String getServiceLocalName() {
    return aServiceLocalName;
  }

  public void setServiceLocalName(String localName) {
    aServiceLocalName = localName;
  }

  @XmlAttribute(name="serviceNS")
  public String getServiceNamespace() {
    return aServiceNamespace;
  }

  public void setServiceNamespace(String serviceNamespace) {
    aServiceNamespace = serviceNamespace;
  }

  public QName getServiceName() {
    return new QName(aServiceNamespace, aServiceLocalName);
  }

}
