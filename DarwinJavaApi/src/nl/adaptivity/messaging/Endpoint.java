package nl.adaptivity.messaging;

import javax.jws.WebMethod;


/**
 * Interface for classes that can function as service endpoints. The class
 * should have some method annotated with {@link RestMethod} or
 * {@link WebMethod} or both, as these annotations are used by
 * {@link EndpointServlet} to actually link up these services.
 *
 * @author Paul de Vrieze
 */
public interface Endpoint extends EndpointDescriptor {
  // This is a marker interface that shares the methods with descriptor
  // and can be self-describing, but can actually handle being an endpoint
}
