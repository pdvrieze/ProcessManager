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
@XmlRootElement(name = "endpointDescriptor", namespace = EndpointDescriptorImpl.MY_JBI_NS)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "serviceNamespace", "serviceLocalName", "endpointName" })
public class EndpointDescriptorImpl implements EndpointDescriptor {

  public static final String MY_JBI_NS = "http://adaptivity.nl/jbi";

  private String aServiceLocalName;

  private String aServiceNamespace;

  private String aEndpointName;

  private URI aEndpointLocation;

  public EndpointDescriptorImpl() {}

  public EndpointDescriptorImpl(final QName pServiceName, final String pEndpointName, final URI pEndpointLocation) {
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((aEndpointLocation == null) ? 0 : aEndpointLocation.hashCode());
    result = prime * result + ((aEndpointName == null) ? 0 : aEndpointName.hashCode());
    result = prime * result + ((aServiceLocalName == null) ? 0 : aServiceLocalName.hashCode());
    result = prime * result + ((aServiceNamespace == null) ? 0 : aServiceNamespace.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EndpointDescriptorImpl other = (EndpointDescriptorImpl) obj;
    if (aEndpointLocation == null) {
      if (other.aEndpointLocation != null)
        return false;
    } else if (!aEndpointLocation.equals(other.aEndpointLocation))
      return false;
    if (aEndpointName == null) {
      if (other.aEndpointName != null)
        return false;
    } else if (!aEndpointName.equals(other.aEndpointName))
      return false;
    if (aServiceLocalName == null) {
      if (other.aServiceLocalName != null)
        return false;
    } else if (!aServiceLocalName.equals(other.aServiceLocalName))
      return false;
    if (aServiceNamespace == null) {
      if (other.aServiceNamespace != null)
        return false;
    } else if (!aServiceNamespace.equals(other.aServiceNamespace))
      return false;
    return true;
  }

}
