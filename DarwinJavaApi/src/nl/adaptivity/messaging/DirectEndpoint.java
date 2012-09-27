package nl.adaptivity.messaging;

import java.util.concurrent.Future;

import javax.activation.DataSource;


public interface DirectEndpoint extends Endpoint {

  /**
   * Direct delivery of the given message.
   * @param pMessage The message to deliver
   * @param pCompletionListener The completion Listener to notify of completion.
   */
  public Future<DataSource> deliverMessage(ISendableMessage pMessage, CompletionListener pCompletionListener);

}
