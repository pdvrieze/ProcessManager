package nl.adaptivity.jbi.util;

import java.net.URI;

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
  private URI aEndpointLocation;

  public EndPointDescriptor() {}

  public EndPointDescriptor(QName pServiceName, String pEndpointName, URI pEndpointLocation) {
    aServiceLocalName = pServiceName.getLocalPart();
    aServiceNamespace = pServiceName.getNamespaceURI();
    aEndpointName = pEndpointName;
    aEndpointLocation = pEndpointLocation;
  }

  @XmlAttribute(name="endpointName")
  public String getEndpointName() {
    return aEndpointName;
  }

  public void setEndpointName(String endpointName) {
    aEndpointName = endpointName;
  }

  @XmlAttribute(name="endpointLocation")
  public String getEndpointLocationString() {
    return aEndpointLocation.toString();
  }
  
  public void setEndpointLocationString(String pLocation) {
    aEndpointLocation = URI.create(pLocation);
  }

  public URI getEnpointLocation() {
    return aEndpointLocation;
  }
  
  public void setEndpointLocation(URI pLocation) {
    aEndpointLocation = pLocation;
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
