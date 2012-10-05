package nl.adaptivity.process.messaging;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.devrieze.util.security.PermissionDeniedException;

import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.ws.rest.RestMessageHandler;
import nl.adaptivity.ws.soap.SoapMessageHandler;


/**
 * A servlet that serves up web services provided by a {@link GenericEndpoint}
 * 
 * @author Paul de Vrieze
 */
public class EndpointServlet extends HttpServlet {

  private static final String LOGGER_NAME = EndpointServlet.class.getName();

  private static final long serialVersionUID = 5882346515807438320L;

  private GenericEndpoint aEndpoint;

  private volatile SoapMessageHandler aSoapMessageHandler;

  private volatile RestMessageHandler aRestMessageHandler;

  /**
   * Default constructor that allows this servlet to be instantiated directly in
   * tomcat. This will set the endpoint to the object itself if the object
   * implements GenericEndpoint. This allows servlets to provide services by
   * deriving from {@link EndpointServlet}
   */
  public EndpointServlet() {
    if (this instanceof GenericEndpoint) {
      aEndpoint = (GenericEndpoint) this;
    }
  }

  /**
   * A constructor for subclasses that provide an endpoint to use.
   * 
   * @param pEndpoint The endpoint to provide.
   */
  protected EndpointServlet(final GenericEndpoint pEndpoint) {
    aEndpoint = pEndpoint;
  }

  /**
   * Handle DELETE requests.
   */
  @Override
  protected void doDelete(final HttpServletRequest pReq, final HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.DELETE, pReq, pResp);
  }

  /**
   * Handle GET requests.
   */
  @Override
  protected void doGet(final HttpServletRequest pReq, final HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.GET, pReq, pResp);
  }

  /**
   * Handle HEAD requests.
   */
  @Override
  protected void doHead(final HttpServletRequest pReq, final HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.HEAD, pReq, pResp);
  }

  /**
   * Handle POST requests.
   */
  @Override
  protected void doPost(final HttpServletRequest pReq, final HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.POST, pReq, pResp);
  }

  /**
   * Handle PUT requests.
   */
  @Override
  protected void doPut(final HttpServletRequest pReq, final HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.PUT, pReq, pResp);
  }

  /**
   * Method that does the actual work of processing requests. It will, based on
   * the Content-Type header deterimine whether it's a rest or soap request, and
   * use a {@link SoapMessageHandler} or {@link RestMessageHandler} to actually
   * process the message.
   * 
   * @param pMethod The HTTP method invoked.
   * @param pRequest The request.
   * @param pResponse The response object on which responses are written.
   * @todo In case we have a soap request, respond with a proper SOAP fault, not
   *       a generic error message.
   */
  private void processRestSoap(final HttpMethod pMethod, final HttpServletRequest pRequest, final HttpServletResponse pResponse) {
    try {
      final HttpMessage message = new HttpMessage(pRequest);
      try {
        if (!SoapMessageHandler.isSoapMessage(pRequest)) {
          final RestMessageHandler restHandler = getRestMessageHandler();
          if (!restHandler.processRequest(pMethod, message, pResponse)) {
            getLogger().warning("Error processing rest request");
          }
        } else {
          final SoapMessageHandler soapHandler = getSoapMessageHandler();
          if (!soapHandler.processRequest(message, pResponse)) {
            getLogger().warning("Error processing soap request");
          }
        }
      } catch (final PermissionDeniedException e) {
        pResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This user is not allowed to perform the requested action");
        return;
      }
    } catch (final IOException e) {
      try {
        pResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        getLogger().log(Level.WARNING, "Error when processing REST/SOAP", e);
      } catch (final IOException e1) {
        getLogger().log(Level.WARNING, "Failure to notify client of error", e);
      }
    }
  }

  /**
   * Override this to override the endpoint used by this servlet to serve
   * connections. In most cases it's better to provide the endpoint to the
   * constructor instead, or as a servlet parameter.
   * 
   * @return A GenericEndpoint that implements the needed services.
   * @see {@link #init(ServletConfig)}
   */
  protected GenericEndpoint getEndpointProvider() {
    return aEndpoint;
  }

  /**
   * Get a soap handler that does the work for us. As the handler caches objects
   * instead of repeatedly using reflection it needs to be an object and is not
   * just a set of static methods.
   * 
   * @return The soap handler.
   */
  private SoapMessageHandler getSoapMessageHandler() {
    if (aSoapMessageHandler == null) {
      synchronized (this) {
        if (aSoapMessageHandler == null) {
          aSoapMessageHandler = SoapMessageHandler.newInstance(getEndpointProvider());
        }
      }
    }
    return aSoapMessageHandler;
  }

  /**
   * Get a rest handler that does the work for us. As the handler caches objects
   * instead of repeatedly using reflection it needs to be an object and is not
   * just a set of static methods.
   * 
   * @return The rest handler.
   */
  private RestMessageHandler getRestMessageHandler() {
    if (aRestMessageHandler == null) {
      synchronized (this) {
        if (aRestMessageHandler == null) {
          aRestMessageHandler = RestMessageHandler.newInstance(getEndpointProvider());
        }

      }
    }
    return aRestMessageHandler;
  }

  /**
   * Get a logger object for this servlet.
   * 
   * @return A logger to use to log messages.
   */
  private Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  /**
   * Initialize the servlet. If there is an <code>endpoint</code> parameter to
   * the servlet this will update the {@link #getEndpointProvider() endpoint}
   * used. If getEndpointProvider is overridden, that will still reflect the
   * actually used endpoint.
   */
  @Override
  public void init(final ServletConfig pConfig) throws ServletException {
    super.init(pConfig);
    final String className = pConfig.getInitParameter("endpoint");
    if ((className == null) && (getEndpointProvider() == null)) {
      throw new ServletException("The EndpointServlet needs to be configured with an endpoint parameter.");
    }
    if ((getEndpointProvider() == null) || (className != null)) {
      Class<? extends GenericEndpoint> clazz;
      try {
        clazz = Class.forName(className).asSubclass(GenericEndpoint.class);
      } catch (final ClassNotFoundException e) {
        throw new ServletException(e);
      } catch (final ClassCastException e) {
        throw new ServletException("The endpoint for an EndpointServlet needs to implement " + GenericEndpoint.class.getName()
            + " the class given is " + className, e);
      }
      try {
        aEndpoint = clazz.newInstance();
        aEndpoint.initEndpoint(pConfig);
      } catch (final InstantiationException e) {
        throw new ServletException(e);
      } catch (final IllegalAccessException e) {
        throw new ServletException(e);
      }
    } else {
      getEndpointProvider().initEndpoint(pConfig);
    }
  }

  @Override
  public void destroy() {
    if (aEndpoint != null) {
      aEndpoint.destroy();
    }
    super.destroy();
  }

}
