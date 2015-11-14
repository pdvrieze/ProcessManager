package nl.adaptivity.messaging;

import java.util.concurrent.Future;


/**
 * An interface marking an endpoint that can handle direct
 * (non-reflection-based) delivery of messages to it. This is an optimization
 * for the MessagingRegistry.
 *
 * @author Paul de Vrieze
 */
public interface DirectEndpoint extends Endpoint {

  /**
   * Direct delivery of the given message.
   *
   * @param message The message to deliver
   * @param completionListener The completion Listener to notify of completion.
   */
  <T> Future<T> deliverMessage(ISendableMessage message, CompletionListener completionListener, Class<T> returnType);

}
