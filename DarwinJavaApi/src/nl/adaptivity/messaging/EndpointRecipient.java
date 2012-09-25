package nl.adaptivity.messaging;

import java.util.concurrent.Future;

import javax.activation.DataSource;


public interface EndpointRecipient extends Endpoint {

  /**
   * Shortcut message that allows for direct delivery of messages to endpoints.
   * @param pMessage The message to deliver.
   * @param pCompletionListener The completionlistener to notify.
   */
  Future<?> deliverMessage(DataSource pMessage, CompletionListener pCompletionListener);

}
