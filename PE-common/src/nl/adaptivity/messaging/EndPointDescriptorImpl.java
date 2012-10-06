package nl.adaptivity.messaging;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


@XmlRootElement(name = "endpointDescriptor", namespace = EndPointDescriptorImpl.MY_JBI_NS)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "serviceNamespace", "serviceLocalName", "endpointName" })
public class EndPointDescriptorImpl implements EndpointDescriptor {

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";

  private String aServiceLocalName;

  private String aServiceNamespace;

  private String aEndpointName;

  private URI aEndpointLocation;

  public EndPointDescriptorImpl() {}

  public EndPointDescriptorImpl(final QName pServiceName, final String pEndpointName, final URI pEndpointLocation) {
    aServiceLocalName = pServiceName.getLocalPart();
    aServiceNamespace = pServiceName.getNamespaceURI();
    aEndpointName = pEndpointName;
    aEndpointLocation = pEndpointLocation;
  }

  @Override
  @XmlAttribute(name = "endpointName")
  public String getEndpointName() {
    return aEndpointName;
  }

  public void setEndpointName(final String endpointName) {
    aEndpointName = endpointName;
  }

  @XmlAttribute(name = "endpointLocation")
  public String getEndpointLocationString() {
    return aEndpointLocation.toString();
  }

  public void setEndpointLocationString(final String pLocation) {
    aEndpointLocation = URI.create(pLocation);
  }

  @Override
  public URI getEndpointLocation() {
    return aEndpointLocation;
  }

  public void setEndpointLocation(final URI pLocation) {
    aEndpointLocation = pLocation;
  }

  @XmlAttribute(name = "serviceLocalName")
  public String getServiceLocalName() {
    return aServiceLocalName;
  }

  public void setServiceLocalName(final String localName) {
    aServiceLocalName = localName;
  }

  @XmlAttribute(name = "serviceNS")
  public String getServiceNamespace() {
    return aServiceNamespace;
  }

  public void setServiceNamespace(final String serviceNamespace) {
    aServiceNamespace = serviceNamespace;
  }

  @Override
  public QName getServiceName() {
    return new QName(aServiceNamespace, aServiceLocalName);
  }

}
