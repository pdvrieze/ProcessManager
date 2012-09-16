package nl.adaptivity.process.messaging;

import javax.jws.WebMethod;
import javax.xml.namespace.QName;

import nl.adaptivity.rest.annotations.RestMethod;

/**
 * Marker interface for classes that can function as service endpoints. The
 * class should have some method annotated with {@link RestMethod} or {@link WebMethod}
 * or both, as these annotations are used by {@link EndpointServlet} to actually link
 * up these services.
 * 
 * @author Paul de Vrieze
 *
 */
public interface GenericEndpoint {

  /**
   * The QName of the service
   * @return The qname.
   */
  QName getService();

  /**
   * The actual endpoint in the service provided.
   * @return
   */
  String getEndpoint();
  
  /**
   * Called when the endpoint needs to clean up. This is called when
   * the {@link EndpointServlet Servlet} needs to clean up.
   */
  void destroy();

}
