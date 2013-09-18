package nl.adaptivity.messaging;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.xml.namespace.QName;


/**
 * <p>
 * This singleton class acts as the registry where a {@link IMessenger
 * messenger} can be registered for hanlding messages. The usage of this central
 * registry allows for only the API project to be loaded globally into the
 * servlet container while the messenger is loaded by a specific context.
 * <p>
 * Messengers should be thread safe and provide reliable delivery (failure to
 * deliver must throw an exception).
 * <p>
 * This class will provide a temporary stub for any message sending attempts
 * made while no messenger has yet been registered. This stub will NOT however
 * attempt to deliver anything. The messages will be stored in memory and
 * forwarded on to a registered messenger when it is registered.
 *
 * @author Paul de Vrieze
 */
public final class MessagingRegistry {

  /**
   * A future class that makes StubMessenger work. It will basically fulfill the
   * future contract (including waiting for a specified amount of time) even
   * when a new messenger is registered.
   *
   * @author Paul de Vrieze
   * @param <T> The return value of the future.
   */
  private static class WrappingFuture<T> implements Future<T>, MessengerCommand, CompletionListener {

    private final ISendableMessage aMessage;

    private Future<T> aOrigin;

    private boolean aCancelled = false;

    private final CompletionListener aCompletionListener;

    private final Class<T> aReturnType;

    public WrappingFuture(final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
      aReturnType = pReturnType;
    }

    @Override
    public synchronized boolean cancel(final boolean pMayInterruptIfRunning) {
      if (aOrigin == null) {
        aCancelled = true;
        if (aCompletionListener != null) {
          aCompletionListener.onMessageCompletion(this);
        }
      } else {
        aCancelled = aOrigin.cancel(pMayInterruptIfRunning);
      }
      return aCancelled;
    }

    @Override
    public synchronized boolean isCancelled() {
      if (aOrigin != null) {
        return aOrigin.isCancelled();
      }
      return aCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      if (aOrigin != null) {
        return aOrigin.isDone();
      }
      return false || aCancelled;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      while (aOrigin == null) {
        if (aCancelled) {
          throw new CancellationException();
        }
        wait();
      }
      return aOrigin.get();
    }

    @Override
    public synchronized T get(final long pTimeout, final TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aOrigin == null) {
        final long startTime = System.currentTimeMillis();
        try {
          if (pUnit == TimeUnit.NANOSECONDS) {
            wait(pUnit.toMillis(pTimeout), (int) (pUnit.toNanos(pTimeout) % 1000000));
          } else {
            wait(pUnit.toMillis(pTimeout));
          }
        } catch (final InterruptedException e) {
          if (aOrigin != null) {
            // Assume we are woken up because of the change not another interruption.
            final long currentTime = System.currentTimeMillis();
            final long millisLeft = pUnit.toMillis(pTimeout) - (currentTime - startTime);
            if (millisLeft > 0) {
              return aOrigin.get(millisLeft, TimeUnit.MILLISECONDS);
            } else {
              throw new TimeoutException();
            }
          } else {
            throw e;
          }
        }
      }
      if (aCancelled) {
        throw new CancellationException();
      } else {
        throw new TimeoutException();
      }
    }

    @Override
    public synchronized void execute(final IMessenger pMessenger) {
      if (!aCancelled) {
        aOrigin = pMessenger.sendMessage(aMessage, this, aReturnType);
      }
      notifyAll(); // Wake up all waiters (should be only one)
    }

    @Override
    public void onMessageCompletion(final Future<?> pFuture) {
      if (aCompletionListener != null) {
        aCompletionListener.onMessageCompletion(this);
      }
    }

  }

  /**
   * A command that can be queued up by a stubmessenger for processing when the
   * new messenger is registered.
   *
   * @author Paul de Vrieze
   */
  private static interface MessengerCommand {

    /**
     * Execute the command
     *
     * @param pMessenger The messenger to use. (this should be a real messenger,
     *          not a stub).
     */
    void execute(IMessenger pMessenger);
  }

  /**
   * This messenger will only queue up commands to be executed (in order or
   * original reception) against a real messenger when it is registered. This is
   * a stopgap for timing issues, not a reliable long-term solution.
   *
   * @author Paul de Vrieze
   */
  private static class StubMessenger implements IMessenger {

    IMessenger aRealMessenger = null;

    Queue<MessengerCommand> aCommandQueue;

    StubMessenger() {
      aCommandQueue = new ArrayDeque<>();
    }

    public synchronized void setMessenger(final IMessenger pMessenger) {
      aRealMessenger = pMessenger;
      for (final MessengerCommand command : aCommandQueue) {
        command.execute(aRealMessenger);
      }
      aCommandQueue = null; // We don't need it anymore, we'll just forward.
    }

    @Override
    public void registerEndpoint(final QName pService, final String pEndPoint, final URI pTarget) {
      synchronized (this) {
        if (aRealMessenger == null) {
          aCommandQueue.add(new MessengerCommand() {

            @Override
            public void execute(final IMessenger pMessenger) {
              pMessenger.registerEndpoint(pService, pEndPoint, pTarget);
            }

          });
          return;
        }
      }
      aRealMessenger.registerEndpoint(pService, pEndPoint, pTarget);
    }

    @Override
    public void registerEndpoint(final EndpointDescriptor pEndpoint) {
      synchronized (this) {
        if (aRealMessenger == null) {
          aCommandQueue.add(new MessengerCommand() {

            @Override
            public void execute(final IMessenger pMessenger) {
              pMessenger.registerEndpoint(pEndpoint);
            }

          });
          return;
        }
      }
      aRealMessenger.registerEndpoint(pEndpoint);
    }

    @Override
    public <T> Future<T> sendMessage(final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
      synchronized (this) {
        if (aRealMessenger == null) {
          final WrappingFuture<T> future = new WrappingFuture<>(pMessage, pCompletionListener, pReturnType);
          aCommandQueue.add(future);
          return future;
        }
      }
      return aRealMessenger.sendMessage(pMessage, pCompletionListener, pReturnType);
    }

    @Override
    public void shutdown() {
      System.err.println("Shutting down stub messenger. This should never happen. Do register an actual messenger!");
    }

  }

  private static IMessenger aMessenger;

  /**
   * Register a messenger with the registry. You may not register a second
   * messenger, and this will throw. When a messenger needs to actually be
   * replaced the only valid option is to first invoke the method with
   * <code>null</code> to unregister the messenger, and then register a new one.
   *
   * @param pMessenger Pass <code>null</code> to unregister the current
   *          messenger, otherwhise pass a messenger.
   */
  public static synchronized void registerMessenger(final IMessenger pMessenger) {
    if (pMessenger == null) {
      aMessenger = new StubMessenger();
    } else if (aMessenger instanceof StubMessenger) {
      ((StubMessenger) aMessenger).setMessenger(pMessenger);
      aMessenger = pMessenger;
    } else if (aMessenger != null) {

      throw new IllegalStateException("It is not allowed to register multiple messengers");
    }
    aMessenger = pMessenger;
    if (aMessenger != null) {
      Logger.getAnonymousLogger().info("New messenger registered: " + aMessenger.getClass().getName());
    }
  }

  /**
   * Get the messenger.
   *
   * @return The messenger to use to send messages. This will never return
   *         <code>null</code>, even when no messenger has been registered (it
   *         will create,register and return a stub that will queue up
   *         messages).
   */
  public static synchronized IMessenger getMessenger() {
    if (aMessenger == null) {
      aMessenger = new StubMessenger();
    }
    return aMessenger;
  }

  /**
   * Convenience method to send messages. This is equivalent to and invokes
   * {@link IMessenger#sendMessage(ISendableMessage, CompletionListener, Class)}
   * .
   *
   * @param pMessage The message to be sent.
   * @param pCompletionListener The completionListener to use when the message
   *          response is ready.
   * @param pReturnType The type of the return value of the sending.
   * @return A future that can be used to retrieve the result of the sending.
   *         This result will also be passed along to the completionListener.
   * @see IMessenger#sendMessage(ISendableMessage, CompletionListener, Class)
   */
  public static <T> Future<T> sendMessage(final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
    return getMessenger().sendMessage(pMessage, pCompletionListener, pReturnType);
  }

}
