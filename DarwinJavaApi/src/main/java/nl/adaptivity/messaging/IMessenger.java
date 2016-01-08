package nl.adaptivity.messaging;

import javax.xml.namespace.QName;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Future;


/**
 * Interface indicating a class that can act as messenger in the
 * {@link MessagingRegistry}. Note that only one messenger can be registered at
 * the time.
 *
 * @author Paul de Vrieze
 */
public interface IMessenger {

  /**
   * Register an endpoint.
   *
   * @param service The service to register.
   * @param endPoint The endpoint within the service.
   * @param target The url for that service.
   * @return An EndpointDescriptor that can be used to unregister the endpoint.
   */
  EndpointDescriptor registerEndpoint(QName service, String endPoint, URI target);

  /**
   * Register an endpoint. This endpoint can be interpreted by the actual
   * messenger to provide a shortcut
   *
   * @param endpoint The endpoint to register
   */
  void registerEndpoint(EndpointDescriptor endpoint);

  /**
   * Send a message using the messenger. Sending is an asynchronous process and
   * a return from this method does not imply completion of the delivery (or
   * success).
   *
   * @param message The message to be sent.
   * @param completionListener The completionListener to use when the message
   *          response is ready.
   * @param returnType The type of the return value of the sending.
   * @param returnTypeContext The jaxb context to be used when marshaling and
   *          umarshaling the return value. Basically this uses
   *          {@link javax.xml.bind.annotation.XmlSeeAlso}.
   * @return A future that can be used to retrieve the result of the sending.
   *         This result will also be passed along to the completionListener.
   */
  <T> Future<T> sendMessage(ISendableMessage message, CompletionListener<T> completionListener, Class<T> returnType, Class<?>[] returnTypeContext);

  /**
   * Get a list of all the registered enpoints.
   *
   * @return The list of registered endpoints. This may return <code>null</code>
   *         if the messenger does not support this. The default StubMessenger
   *         for example returns <code>null</code>.
   */
  List<EndpointDescriptor> getRegisteredEndpoints();

  /**
   * Unregister the given endpoint
   * @param endpoint The endpoint to unregister
   * @return <code>true</code> on success, false when the endpoint was not registered.
   */
  boolean unregisterEndpoint(EndpointDescriptor endpoint);

  /**
   * Invoked when the messenger needs to release it's resources. After this has
   * been called the messenger should not accept and is not expected to accept
   * any new messages.
   */
  void shutdown();
}
