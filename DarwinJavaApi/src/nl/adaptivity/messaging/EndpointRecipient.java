package nl.adaptivity.messaging;

import java.util.concurrent.Future;


public interface EndpointRecipient extends Endpoint {

  /**
   * Shortcut message that allows for direct delivery of messages to endpoints.
   * @param pMessage The message to deliver.
   * @param pCompletionListener The completionlistener to notify.
   */
  <T> Future<T> deliverMessage(ISendableMessage pMessage, CompletionListener pCompletionListener);

}
