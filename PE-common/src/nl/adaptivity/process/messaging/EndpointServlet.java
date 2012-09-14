package nl.adaptivity.process.messaging;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.util.HttpMessage;
import nl.adaptivity.ws.rest.RestMessageHandler;
import nl.adaptivity.ws.soap.SoapMessageHandler;


public class EndpointServlet extends HttpServlet {

  private GenericEndpoint aEndpoint;
  private SoapMessageHandler aSoapMessageHandler;
  private RestMessageHandler aRestMessageHandler;

  public EndpointServlet() {
    
  }
  
  protected EndpointServlet(GenericEndpoint pEndpoint) {
    aEndpoint = pEndpoint;
  }
  
  @Override
  protected void doDelete(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.DELETE, pReq, pResp);
  }

  @Override
  protected void doGet(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.GET, pReq, pResp);
  }

  @Override
  protected void doHead(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.HEAD, pReq, pResp);
  }

  @Override
  protected void doPost(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.POST, pReq, pResp);
  }

  @Override
  protected void doPut(HttpServletRequest pReq, HttpServletResponse pResp) throws ServletException, IOException {
    processRestSoap(HttpMethod.PUT, pReq, pResp);
  }

  private void processRestSoap(HttpMethod pMethod, HttpServletRequest pRequest, HttpServletResponse pResponse) {
    try {
      HttpMessage message = new HttpMessage(pRequest);
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
    } catch (IOException e) {
      try {
        pResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (IOException e1) {
        getLogger().log(Level.WARNING, "Failure to notify client of error", e);
      }
    }
  }
  
  /**
   * Override this to override the endpoint used by this servlet to serve connections.
   * @return A GenericEndpoint that implements the needed services.
   */
  protected GenericEndpoint getEndpointProvider() {
    return aEndpoint;
  }

  private SoapMessageHandler getSoapMessageHandler() {
    if (aSoapMessageHandler == null) {
      aSoapMessageHandler = SoapMessageHandler.newInstance(getEndpointProvider());
    }
    return aSoapMessageHandler;
  }

  private RestMessageHandler getRestMessageHandler() {
    if (aRestMessageHandler == null) {
      aRestMessageHandler = RestMessageHandler.newInstance(getEndpointProvider());
    }
    return aRestMessageHandler;
  }

  private Logger getLogger() {
    return Logger.getLogger(EndpointServlet.class.getName());
  }

  @Override
  public void init(ServletConfig pConfig) throws ServletException {
    super.init(pConfig);
    if (aEndpoint == null && getEndpointProvider()==null) {
      Class<? extends GenericEndpoint> clazz;
      String className = pConfig.getInitParameter("endpoint");
      if (className==null) { throw new ServletException("The EndpointServlet needs to be configured with an endpoint parameter."); }
      try {
        clazz = Class.forName(className).asSubclass(GenericEndpoint.class);
      } catch (ClassNotFoundException e) {
        throw new ServletException(e);
      } catch (ClassCastException e) {
        throw new ServletException("The endpoint for an EndpointServlet needs to implement "+GenericEndpoint.class.getName()+" the class given is "+className, e);
      }
      try {
        aEndpoint = clazz.newInstance();
      } catch (InstantiationException e) {
        throw new ServletException(e);
      } catch (IllegalAccessException e) {
        throw new ServletException(e);
      }
    }
  }

}
