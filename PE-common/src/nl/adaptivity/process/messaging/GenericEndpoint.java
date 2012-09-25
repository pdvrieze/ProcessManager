package nl.adaptivity.process.messaging;

import javax.jws.WebMethod;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.xml.namespace.QName;

import nl.adaptivity.messaging.Endpoint;
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
public interface GenericEndpoint extends Endpoint {

  /**
   * The QName of the service
   * @return The qname.
   */
  @Override
  QName getServiceName();

  /**
   * The actual endpoint in the service provided.
   * @return
   */
  @Override
  String getEndpointName();

  /**
   * Called when the endpoint needs to clean up. This is called when
   * the {@link EndpointServlet Servlet} needs to clean up.
   */
  void destroy();

  /**
   * Called to initialize the endpoint to a given context. This can not be called init
   * as that would conflict with {@link Servlet#init(ServletConfig)}
   * @param pConfig The configuration information to use
   */
  void initEndpoint(ServletConfig pConfig);

}
