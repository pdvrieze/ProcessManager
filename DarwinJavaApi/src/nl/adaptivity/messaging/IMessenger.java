package nl.adaptivity.messaging;

import java.net.URI;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;


public interface IMessenger {

  /**
   * Register an endpoint. This will only work for external endpoint.
   * 
   * @param pService The service to register.
   * @param endPoint The endpoint within the service.
   * @param pTarget The url for that service.
   */
  public void registerEndpoint(QName pService, String endPoint, URI pTarget);

  /**
   * Register an endpoint. This endpoint can be interpreted by the actual
   * messenger to provide a shortcut
   * 
   * @param pEndpoint
   */
  public void registerEndpoint(Endpoint pEndpoint);

  public <T> Future<T> sendMessage(ISendableMessage pMessage, CompletionListener pCompletionListener, Class<T> pReturnType);

  public void shutdown();
}
