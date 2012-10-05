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
   * @param pMessage The message to deliver
   * @param pCompletionListener The completion Listener to notify of completion.
   */
  public <T> Future<T> deliverMessage(ISendableMessage pMessage, CompletionListener pCompletionListener, Class<T> pReturnType);

}
