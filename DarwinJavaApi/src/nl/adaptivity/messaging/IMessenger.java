package nl.adaptivity.messaging;

import java.net.URI;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;


/**
 * Interface indicating a class that can act as messenger in the
 * {@link MessagingRegistry}. Note that only one messenger can be registered at
 * the time.
 *
 * @author Paul de Vrieze
 */
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

  /**
   * Send a message using the messenger. Sending is an asynchronous process and
   * a return from this method does not imply completion of the delivery (or
   * success).
   *
   * @param pMessage The message to be sent.
   * @param pCompletionListener The completionListener to use when the message
   *          response is ready.
   * @param pReturnType The type of the return value of the sending.
   * @return A future that can be used to retrieve the result of the sending.
   *         This result will also be passed along to the completionListener.
   */
  public <T> Future<T> sendMessage(ISendableMessage pMessage, CompletionListener pCompletionListener, Class<T> pReturnType);

  /**
   * Invoked when the messenger needs to release it's resources. After this has
   * been called the messenger should not accept and is not expected to accept
   * any new messages.
   */
  public void shutdown();
}
