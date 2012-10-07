package nl.adaptivity.messaging;

import java.net.URI;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;


/**
 * Simple pojo implementation of {@link EndpointDescriptor} that supports
 * serialization through {@link JAXB}.
 *
 * @author Paul de Vrieze
 */
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
  String getEndpointLocationString() {
    return aEndpointLocation.toString();
  }

  void setEndpointLocationString(final String pLocation) {
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
  String getServiceLocalName() {
    return aServiceLocalName;
  }

  void setServiceLocalName(final String localName) {
    aServiceLocalName = localName;
  }

  @XmlAttribute(name = "serviceNS")
  String getServiceNamespace() {
    return aServiceNamespace;
  }

  void setServiceNamespace(final String serviceNamespace) {
    aServiceNamespace = serviceNamespace;
  }

  @Override
  public QName getServiceName() {
    return new QName(aServiceNamespace, aServiceLocalName);
  }

}
