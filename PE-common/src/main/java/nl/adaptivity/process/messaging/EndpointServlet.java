/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.messaging;

import net.devrieze.util.security.AuthenticationNeededException;
import net.devrieze.util.security.PermissionDeniedException;
import nl.adaptivity.messaging.HttpResponseException;
import nl.adaptivity.messaging.MessagingException;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.ws.rest.RestMessageHandler;
import nl.adaptivity.ws.soap.SoapMessageHandler;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A servlet that serves up web services provided by a {@link GenericEndpoint}
 *
 * @author Paul de Vrieze
 */
public class EndpointServlet extends HttpServlet {

  private static final String LOGGER_NAME = EndpointServlet.class.getName();

  private static final long serialVersionUID = 5882346515807438320L;

  private GenericEndpoint mEndpoint;

  private volatile SoapMessageHandler mSoapMessageHandler;

  private volatile RestMessageHandler mRestMessageHandler;

  /**
   * Default constructor that allows this servlet to be instantiated directly in
   * tomcat. This will set the endpoint to the object itself if the object
   * implements GenericEndpoint. This allows servlets to provide services by
   * deriving from {@link EndpointServlet}
   */
  public EndpointServlet() {
    if (this instanceof GenericEndpoint) {
      mEndpoint = (GenericEndpoint) this;
    }
  }

  /**
   * A constructor for subclasses that provide an endpoint to use.
   *
   * @param endpoint The endpoint to provide.
   */
  protected EndpointServlet(final GenericEndpoint endpoint) {
    mEndpoint = endpoint;
  }

  /**
   * Handle DELETE requests.
   */
  @Override
  protected void doDelete(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws ServletException, IOException {
    processRestSoap(HttpMethod.DELETE, req, resp);
  }

  /**
   * Handle GET requests.
   */
  @Override
  protected void doGet(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws ServletException, IOException {
    processRestSoap(HttpMethod.GET, req, resp);
  }

  /**
   * Handle HEAD requests.
   */
  @Override
  protected void doHead(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws ServletException, IOException {
    processRestSoap(HttpMethod.HEAD, req, resp);
  }

  /**
   * Handle POST requests.
   */
  @Override
  protected void doPost(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws ServletException, IOException {
    processRestSoap(HttpMethod.POST, req, resp);
  }

  /**
   * Handle PUT requests.
   */
  @Override
  protected void doPut(@NotNull final HttpServletRequest req, @NotNull final HttpServletResponse resp) throws ServletException, IOException {
    processRestSoap(HttpMethod.PUT, req, resp);
  }

  /**
   * Method that does the actual work of processing requests. It will, based on
   * the Content-Type header deterimine whether it's a rest or soap request, and
   * use a {@link SoapMessageHandler} or {@link RestMessageHandler} to actually
   * process the message.
   *
   * @param method The HTTP method invoked.
   * @param request The request.
   * @param response The response object on which responses are written.
   * @todo In case we have a soap request, respond with a proper SOAP fault, not
   *       a generic error message.
   */
  private void processRestSoap(final HttpMethod method, @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
    try {
      request.authenticate(response); // Try to authenticate
      final HttpMessage message = new HttpMessage(request);
      try {
        try {
          if (!SoapMessageHandler.isSoapMessage(request)) {
            final RestMessageHandler restHandler = getRestMessageHandler();
            if (!restHandler.processRequest(method, message, response)) {
              getLogger().warning("Error processing rest request "+request.getRequestURI());
            }
          } else {
            final SoapMessageHandler soapHandler = getSoapMessageHandler();
            if (!soapHandler.processRequest(message, response)) {
              getLogger().warning("Error processing soap request "+request.getRequestURI());
            }
          }
        } catch (@NotNull final MessagingException e) {
          if (e.getCause() instanceof Exception) {
            getLogger().log(Level.WARNING, "MessagingException "+e.getMessage(), e);
            throw (Exception) e.getCause();
          } else {
            throw e;
          }
        }
      } catch (@NotNull final HttpResponseException e) {
        response.sendError(e.getResponseCode(), e.getMessage());
      } catch (@NotNull final AuthenticationNeededException e) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No authenticated user.");
      } catch (@NotNull final PermissionDeniedException e) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "This user is not allowed to perform the requested action.");
      } catch (@NotNull final FileNotFoundException e) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not available.");
      }
    } catch (@NotNull final Exception e) {
      try {
        getLogger().log(Level.WARNING, "Error when processing REST/SOAP ("+request.getRequestURI()+")", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (@NotNull final IOException e1) {
        e1.addSuppressed(e);
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
    return mEndpoint;
  }

  /**
   * Get a soap handler that does the work for us. As the handler caches objects
   * instead of repeatedly using reflection it needs to be an object and is not
   * just a set of static methods.
   *
   * @return The soap handler.
   */
  private SoapMessageHandler getSoapMessageHandler() {
    if (mSoapMessageHandler == null) {
      synchronized (this) {
        if (mSoapMessageHandler == null) {
          mSoapMessageHandler = SoapMessageHandler.newInstance(getEndpointProvider());
        }
      }
    }
    return mSoapMessageHandler;
  }

  /**
   * Get a rest handler that does the work for us. As the handler caches objects
   * instead of repeatedly using reflection it needs to be an object and is not
   * just a set of static methods.
   *
   * @return The rest handler.
   */
  private RestMessageHandler getRestMessageHandler() {
    if (mRestMessageHandler == null) {
      synchronized (this) {
        if (mRestMessageHandler == null) {
          mRestMessageHandler = RestMessageHandler.newInstance(getEndpointProvider());
        }

      }
    }
    return mRestMessageHandler;
  }

  /**
   * Get a logger object for this servlet.
   *
   * @return A logger to use to log messages.
   */
  private static Logger getLogger() {
    return Logger.getLogger(LOGGER_NAME);
  }

  /**
   * Initialize the servlet. If there is an <code>endpoint</code> parameter to
   * the servlet this will update the {@link #getEndpointProvider() endpoint}
   * used. If getEndpointProvider is overridden, that will still reflect the
   * actually used endpoint.
   */
  @Override
  public void init(@NotNull final ServletConfig config) throws ServletException {
    super.init(config);
    final String className = config.getInitParameter("endpoint");
    if ((className == null) && (getEndpointProvider() == null)) {
      throw new ServletException("The EndpointServlet needs to be configured with an endpoint parameter.");
    }
    if ((getEndpointProvider() == null) || (className != null)) {
      final Class<? extends GenericEndpoint> clazz;
      try {
        clazz = Class.forName(className).asSubclass(GenericEndpoint.class);
      } catch (@NotNull final ClassNotFoundException e) {
        throw new ServletException(e);
      } catch (@NotNull final ClassCastException e) {
        throw new ServletException("The endpoint for an EndpointServlet needs to implement " + GenericEndpoint.class.getName()
            + " the class given is " + className, e);
      }
      try {
        mEndpoint = clazz.newInstance();
        mEndpoint.initEndpoint(config);
      } catch (@NotNull final InstantiationException e) {
        throw new ServletException(e);
      } catch (@NotNull final IllegalAccessException e) {
        throw new ServletException(e);
      }
    } else {
      getEndpointProvider().initEndpoint(config);
    }
  }

  @Override
  public void destroy() {
    if (mEndpoint != null) {
      mEndpoint.destroy();
    }
    super.destroy();
  }

}
