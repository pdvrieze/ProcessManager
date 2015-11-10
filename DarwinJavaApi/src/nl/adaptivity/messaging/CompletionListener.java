package nl.adaptivity.messaging;

import java.util.concurrent.Future;


/**
 * Interface for classes that can receive completion messages from the
 * {@link IMessenger}. This happens in a separate thread.
 *
 * @author Paul de Vrieze
 */
public interface CompletionListener<T> {

  /**
   * Signify the completion of the task corresponding to the given future. Note
   * that implementations sending completion messages should ensure that the
   * future is complete when this method is called. There should not be a wait
   * when invoking {@link Future#get()} on the future.
   *
   * @param future The future that is complete.
   */
  void onMessageCompletion(@SuppressWarnings("UnusedParameters") Future<? extends T> future);

}