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
   * that as the notification is done as the last part of the calling there
   * future theoretically is not guaranteed to be complete.
   *
   * @param pFuture The future that is complete.
   */
  void onMessageCompletion(Future<?> pFuture);

}