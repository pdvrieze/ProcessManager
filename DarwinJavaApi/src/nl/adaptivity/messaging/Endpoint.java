package nl.adaptivity.messaging;

import java.net.URI;

import javax.jws.WebMethod;
import javax.xml.namespace.QName;


/**
 * Interface for classes that can function as service endpoints. The class
 * should have some method annotated with {@link RestMethod} or
 * {@link WebMethod} or both, as these annotations are used by
 * {@link EndpointServlet} to actually link up these services.
 * 
 * @author Paul de Vrieze
 */

public interface Endpoint {

  /**
   * The QName of the service
   * 
   * @return The qname.
   */
  QName getServiceName();

  /**
   * The actual endpoint in the service provided.
   * 
   * @return
   */
  String getEndpointName();

  /**
   * Get the URI for this endpoint. If this returns null, that means messages
   * may not be able to be delivered in all cases.
   * 
   * @return The uri through which the service can be accessed.
   */
  URI getEndpointLocation();

}
