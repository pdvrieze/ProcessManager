package nl.adaptivity.messaging;

import javax.xml.namespace.QName;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;


/**
 * <p>
 * This singleton class acts as the registry where a {@link IMessenger
 * messenger} can be registered for handling messages. The usage of this central
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


  private static class SimpleEndpointDescriptor implements EndpointDescriptor {

    private final QName aServiceName;
    private final String aEndpointName;
    private final URI aEndpointLocation;

    public SimpleEndpointDescriptor(QName serviceName, String endpointName, URI endpointLocation) {
      aServiceName = serviceName;
      aEndpointName = endpointName;
      aEndpointLocation = endpointLocation;
    }

    @Override
    public QName getServiceName() {
      return aServiceName;
    }

    @Override
    public String getEndpointName() {
      return aEndpointName;
    }

    @Override
    public URI getEndpointLocation() {
      return aEndpointLocation;
    }

  }

  /**
   * A future class that makes StubMessenger work. It will basically fulfill the
   * future contract (including waiting for a specified amount of time) even
   * when a new messenger is registered.
   *
   * @author Paul de Vrieze
   * @param <T> The return value of the future.
   */
  private static class WrappingFuture<T> implements Future<T>, MessengerCommand, CompletionListener<T> {

    private final ISendableMessage aMessage;

    private Future<T> aOrigin;

    private boolean aCancelled = false;

    private final CompletionListener aCompletionListener;

    private final Class<T> aReturnType;

    private Class<?>[] aReturnTypeContext;

    public WrappingFuture(final ISendableMessage message, final CompletionListener completionListener, final Class<T> returnType, final Class<?>[] returnTypeContext) {
      aMessage = message;
      aCompletionListener = completionListener;
      aReturnType = returnType;
      aReturnTypeContext = returnTypeContext;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
      if (aOrigin == null) {
        aCancelled = true;
        if (aCompletionListener != null) {
          aCompletionListener.onMessageCompletion(this);
        }
      } else {
        aCancelled = aOrigin.cancel(mayInterruptIfRunning);
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
    public synchronized T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aOrigin == null) {
        final long startTime = System.currentTimeMillis();
        try {
          if (unit == TimeUnit.NANOSECONDS) {
            wait(unit.toMillis(timeout), (int) (unit.toNanos(timeout) % 1000000));
          } else {
            wait(unit.toMillis(timeout));
          }
        } catch (final InterruptedException e) {
          if (aOrigin != null) {
            // Assume we are woken up because of the change not another interruption.
            final long currentTime = System.currentTimeMillis();
            final long millisLeft = unit.toMillis(timeout) - (currentTime - startTime);
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
    public synchronized void execute(final IMessenger messenger) {
      if (!aCancelled) {
        aOrigin = messenger.sendMessage(aMessage, this, aReturnType, aReturnTypeContext);
      }
      notifyAll(); // Wake up all waiters (should be only one)
    }

    @Override
    public void onMessageCompletion(final Future<? extends T> future) {
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
     * @param messenger The messenger to use. (this should be a real messenger,
     *          not a stub).
     */
    void execute(IMessenger messenger);
  }

  /**
   * This messenger will only queue up commands to be executed (in order or
   * original reception) against a real messenger when it is registered. This is
   * a stopgap for timing issues, not a reliable long-term solution.
   *
   * @author Paul de Vrieze
   */
  private static class StubMessenger implements IMessenger {

    private static final class RegisterEndpointCommand implements MessengerCommand {

      private final String mEndPoint;

      private final QName mService;

      private final URI mTarget;

      private RegisterEndpointCommand(String endPoint, QName service, URI target) {
        mEndPoint = endPoint;
        mService = service;
        mTarget = target;
      }

      public RegisterEndpointCommand(EndpointDescriptor endpoint) {
        mEndPoint = endpoint.getEndpointName();
        mService = endpoint.getServiceName();
        mTarget = endpoint.getEndpointLocation();
      }

      @Override
      public void execute(final IMessenger messenger) {
        messenger.registerEndpoint(mService, mEndPoint, mTarget);
      }
    }

    IMessenger aRealMessenger = null;

    Queue<MessengerCommand> aCommandQueue;

    StubMessenger(IMessenger oldMessenger) {
      aCommandQueue = new ArrayDeque<>();
      if (oldMessenger!=null) {
        for(EndpointDescriptor endpoint: oldMessenger.getRegisteredEndpoints()) {
          aCommandQueue.add(new RegisterEndpointCommand(endpoint));
        }
      }
    }

    public synchronized void flushTo(final IMessenger messenger) {
      aRealMessenger = messenger;
      for (final MessengerCommand command : aCommandQueue) {
        command.execute(aRealMessenger);
      }
      aCommandQueue = null; // We don't need it anymore, we'll just forward.
    }

    @Override
    public EndpointDescriptor registerEndpoint(final QName service, final String endPoint, final URI target) {
      synchronized (this) {
        if (aRealMessenger == null) {
          aCommandQueue.add(new RegisterEndpointCommand(endPoint, service, target));
          return new SimpleEndpointDescriptor(service, endPoint, target);
        }
      }
      return aRealMessenger.registerEndpoint(service, endPoint, target);
    }

    @Override
    public void registerEndpoint(final EndpointDescriptor endpoint) {
      synchronized (this) {
        if (aRealMessenger == null) {
          aCommandQueue.add(new MessengerCommand() {

            @Override
            public void execute(final IMessenger messenger) {
              messenger.registerEndpoint(endpoint);
            }

          });
          return;
        }
      }
      aRealMessenger.registerEndpoint(endpoint);
    }

    @Override
    public <T> Future<T> sendMessage(final ISendableMessage message, final CompletionListener completionListener, final Class<T> returnType, Class<?>[] returnTypeContext) {
      synchronized (this) {
        if (aRealMessenger == null) {
          final WrappingFuture<T> future = new WrappingFuture<>(message, completionListener, returnType, returnTypeContext);
          aCommandQueue.add(future);
          return future;
        }
      }
      return aRealMessenger.sendMessage(message, completionListener, returnType, returnTypeContext);
    }

    @Override
    public void shutdown() {
      System.err.println("Shutting down stub messenger. This should never happen. Do register an actual messenger!");
    }

    @Override
    public List<EndpointDescriptor> getRegisteredEndpoints() {
      synchronized (this) {
        if (aRealMessenger == null) {
          return Collections.emptyList();
        }
      }
      return aRealMessenger.getRegisteredEndpoints();
    }

    @Override
    public boolean unregisterEndpoint(final EndpointDescriptor endpoint) {
      synchronized (this) {
        if (aRealMessenger == null) {
          aCommandQueue.add(new MessengerCommand() {

            @Override
            public void execute(final IMessenger messenger) {
              messenger.unregisterEndpoint(endpoint);
            }

          });
          return true;
        }
      }
      return aRealMessenger.unregisterEndpoint(endpoint);
    }

  }

  private static IMessenger aMessenger;

  /**
   * Register a messenger with the registry. You may not register a second
   * messenger, and this will throw. When a messenger needs to actually be
   * replaced the only valid option is to first invoke the method with
   * <code>null</code> to unregister the messenger, and then register a new one.
   *
   * @param messenger Pass <code>null</code> to unregister the current
   *          messenger, otherwhise pass a messenger.
   */
  public static synchronized void registerMessenger(final IMessenger messenger) {
    if (messenger == null) {
      if (! (aMessenger instanceof StubMessenger)) {
        aMessenger = new StubMessenger(aMessenger);
      }
      return;
    } else if (aMessenger instanceof StubMessenger) {
      ((StubMessenger) aMessenger).flushTo(messenger);
      aMessenger = messenger;
    } else if (aMessenger != null) {

      throw new IllegalStateException("It is not allowed to register multiple messengers");
    }
    aMessenger = messenger;
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
      aMessenger = new StubMessenger(null);
    }
    return aMessenger;
  }

  /**
   * Convenience method to send messages. This is equivalent to and invokes
   * {@link IMessenger#sendMessage(ISendableMessage, CompletionListener, Class, Class[])}
   * .
   *
   * @param message The message to be sent.
   * @param completionListener The completionListener to use when the message
   *          response is ready.
   * @param returnType The type of the return value of the sending.
   * @param returnTypeContext
   * @return A future that can be used to retrieve the result of the sending.
   *         This result will also be passed along to the completionListener.
   * @see IMessenger#sendMessage(ISendableMessage, CompletionListener, Class, Class[])
   */
  public static <T> Future<T> sendMessage(final ISendableMessage message, final CompletionListener completionListener, final Class<T> returnType, Class<?>[] returnTypeContext) {
    return getMessenger().sendMessage(message, completionListener, returnType, returnTypeContext);
  }

}
