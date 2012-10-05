package nl.adaptivity.messaging;

import java.util.concurrent.Future;


/**
 * Interface for classes that can receive completion messages from the
 * {@link AsyncMessenger}. This happens in a separate thread.
 *
 * @author Paul de Vrieze
 */
public interface CompletionListener {

  /**
   * Signify the completion of the task corresponding to the given future. Note
   * that implementations sending completion messages should ensure that the
   * future is complete when this method is called. There should not be a wait
   * when invoking {@link Future#get()} on the future.
   *
   * @param pFuture The future that is complete.
   */
  void onMessageCompletion(Future<?> pFuture);

}