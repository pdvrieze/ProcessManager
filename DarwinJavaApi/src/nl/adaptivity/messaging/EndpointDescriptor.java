package nl.adaptivity.messaging;

import java.net.URI;

import javax.xml.namespace.QName;

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
   * @return
   */
  String getEndpointName();

  /**
   * Get the URI for this endpoint. If this returns null, that means messages
   * may not be able to be delivered in all cases.
   * @return The uri through which the service can be accessed.
   */
  URI getEndpointLocation();

}
