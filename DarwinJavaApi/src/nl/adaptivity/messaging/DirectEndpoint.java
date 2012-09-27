package nl.adaptivity.messaging;

import java.util.concurrent.Future;

import javax.xml.transform.Source;


public interface DirectEndpoint extends Endpoint {

  /**
   * Direct delivery of the given message.
   * @param pMessage The message to deliver
   * @param pCompletionListener The completion Listener to notify of completion.
   */
  public <T> Future<T> deliverMessage(Source pMessage, CompletionListener pCompletionListener);

}
