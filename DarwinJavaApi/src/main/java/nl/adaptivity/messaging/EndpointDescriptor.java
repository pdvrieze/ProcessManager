package nl.adaptivity.messaging;

import javax.xml.namespace.QName;

import java.net.URI;

/**
 * Interface describing service endpoints.
 *
 * @author Paul de Vrieze
 *
 */

public interface EndpointDescriptor {

  /**
   * The QName of the service
   * @return The qname.
   */
  QName getServiceName();

  /**
   * The actual endpoint in the service provided.
   * @return The name of the endpoint
   */
  String getEndpointName();

  /**
   * Get the URI for this endpoint. If this returns null, that means messages
   * may not be able to be delivered in all cases.
   * @return The uri through which the service can be accessed.
   */
  URI getEndpointLocation();

}
