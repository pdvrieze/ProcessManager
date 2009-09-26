package nl.adaptivity.jbi.util;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;


@XmlRootElement(name="endPointDescriptor")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder={"serviceName", "endpointName"})
public class EndPointDescriptor {

  private QName aServiceName;
  private String aEndpointName;

  public EndPointDescriptor() {}

  public EndPointDescriptor(QName pServiceName, String pEndpointName) {
    aServiceName = pServiceName;
    aEndpointName = pEndpointName;
  }

  @XmlElement
  public String getEndpointName() {
    return aEndpointName;
  }

  public void setEndpointName(String endpointName) {
    aEndpointName = endpointName;
  }

  @XmlElement
  public QName getServiceName() {
    return aServiceName;
  }

  public void setServiceName(QName serviceName) {
    aServiceName = serviceName;
  }

}
