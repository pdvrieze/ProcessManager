/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.messaging;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.net.URI;
import java.util.*;
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

    private final QName mServiceName;
    private final String mEndpointName;
    private final URI mEndpointLocation;

    public SimpleEndpointDescriptor(final QName serviceName, final String endpointName, final URI endpointLocation) {
      mServiceName = serviceName;
      mEndpointName = endpointName;
      mEndpointLocation = endpointLocation;
    }

    @Override
    public QName getServiceName() {
      return mServiceName;
    }

    @Override
    public String getEndpointName() {
      return mEndpointName;
    }

    @Override
    public URI getEndpointLocation() {
      return mEndpointLocation;
    }

    @Override
    public boolean isSameService(final EndpointDescriptor other) {
      return mServiceName.equals(other.getServiceName()) && mEndpointName.equals(other.getEndpointName());
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

    private final ISendableMessage mMessage;

    private Future<T> mOrigin;

    private boolean mCancelled = false;

    private final CompletionListener<T> mCompletionListener;

    private final Class<T> mReturnType;

    private final Class<?>[] mReturnTypeContext;

    public WrappingFuture(final ISendableMessage message, final CompletionListener<T> completionListener, final Class<T> returnType, final Class<?>[] returnTypeContext) {
      mMessage = message;
      mCompletionListener = completionListener;
      mReturnType = returnType;
      mReturnTypeContext = returnTypeContext;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
      if (mOrigin == null) {
        mCancelled = true;
        if (mCompletionListener != null) {
          mCompletionListener.onMessageCompletion(this);
        }
      } else {
        mCancelled = mOrigin.cancel(mayInterruptIfRunning);
      }
      return mCancelled;
    }

    @Override
    public synchronized boolean isCancelled() {
      if (mOrigin != null) {
        return mOrigin.isCancelled();
      }
      return mCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      if (mOrigin != null) {
        return mOrigin.isDone();
      }
      return mCancelled;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      while (mOrigin == null) {
        if (mCancelled) {
          throw new CancellationException();
        }
        wait();
      }
      return mOrigin.get();
    }

    @Override
    public synchronized T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      if (mOrigin == null) {
        final long startTime = System.currentTimeMillis();
        try {
          if (unit == TimeUnit.NANOSECONDS) {
            wait(TimeUnit.NANOSECONDS.toMillis(timeout), (int) (timeout % 1000000));
          } else {
            wait(unit.toMillis(timeout));
          }
        } catch (final InterruptedException e) {
          if (mOrigin != null) {
            // Assume we are woken up because of the change not another interruption.
            final long currentTime = System.currentTimeMillis();
            final long millisLeft = unit.toMillis(timeout) - (currentTime - startTime);
            if (millisLeft > 0) {
              return mOrigin.get(millisLeft, TimeUnit.MILLISECONDS);
            } else {
              throw new TimeoutException();
            }
          } else {
            throw e;
          }
        }
      }
      if (mCancelled) {
        throw new CancellationException();
      } else {
        throw new TimeoutException();
      }
    }

    @Override
    public synchronized void execute(final IMessenger messenger) {
      if (!mCancelled) {
        mOrigin = messenger.sendMessage(mMessage, this, mReturnType, mReturnTypeContext);
      }
      notifyAll(); // Wake up all waiters (should be only one)
    }

    @Override
    public void onMessageCompletion(final Future<? extends T> future) {
      if (mCompletionListener != null) {
        mCompletionListener.onMessageCompletion(this);
      }
    }

  }

  /**
   * A command that can be queued up by a stubmessenger for processing when the
   * new messenger is registered.
   *
   * @author Paul de Vrieze
   */
  private interface MessengerCommand {

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

      private RegisterEndpointCommand(final String endPoint, final QName service, final URI target) {
        mEndPoint = endPoint;
        mService = service;
        mTarget = target;
      }

      public RegisterEndpointCommand(final EndpointDescriptor endpoint) {
        mEndPoint = endpoint.getEndpointName();
        mService = endpoint.getServiceName();
        mTarget = endpoint.getEndpointLocation();
      }

      @Override
      public void execute(final IMessenger messenger) {
        messenger.registerEndpoint(mService, mEndPoint, mTarget);
      }

      public EndpointDescriptor getDescriptor() {
        return new SimpleEndpointDescriptor(mService, mEndPoint, mTarget);
      }
      
      public boolean isEndpoint(EndpointDescriptor other) {
        if (mService==null) {
          if (other.getServiceName()!=null) { return false; }
        } else {
          if (! mService.equals(other.getServiceName())) return false;
        }
        
        if (mEndPoint==null) {
          if (other.getEndpointName()!=null) { return false; }
        } else {
          if (! mEndPoint.equals(other.getEndpointName())) return false;
        }
        
        if (mTarget==null) {
          return other.getServiceName()==null;
        } else {
          return mService.equals(other.getServiceName());
        }
      }
    }

    Queue<MessengerCommand> mCommandQueue;

    StubMessenger(final IMessenger oldMessenger) {
      mCommandQueue = new ArrayDeque<>();
      if (oldMessenger!=null) {
        for(final EndpointDescriptor endpoint: oldMessenger.getRegisteredEndpoints()) {
          mCommandQueue.add(new RegisterEndpointCommand(endpoint));
        }
      }
    }

    public synchronized void flushTo(final IMessenger messenger) {
      for (final MessengerCommand command : mCommandQueue) {
        command.execute(messenger);
      }
      mCommandQueue = null; // We don't need it anymore, we'll just forward.
    }

    @Override
    public EndpointDescriptor registerEndpoint(final QName service, final String endPoint, final URI target) {
      synchronized (this) {
        mCommandQueue.add(new RegisterEndpointCommand(endPoint, service, target));
        return new SimpleEndpointDescriptor(service, endPoint, target);
      }
    }

    @Override
    public void registerEndpoint(final EndpointDescriptor endpoint) {
      synchronized (this) {
        mCommandQueue.add(new MessengerCommand() {

          @Override
          public void execute(final IMessenger messenger) {
            messenger.registerEndpoint(endpoint);
          }

        });
      }
    }

    @Override
    public <T> Future<T> sendMessage(final ISendableMessage message, final CompletionListener<T> completionListener, final Class<T> returnType, final Class<?>[] returnTypeContext) {
      synchronized (this) {
        final WrappingFuture<T> future = new WrappingFuture<>(message, completionListener, returnType, returnTypeContext);
        mCommandQueue.add(future);
        return future;
      }
    }

    @Override
    public void shutdown() {
      System.err.println("Shutting down stub messenger. This should never happen. Do register an actual messenger!");
    }

    @Override
    public List<EndpointDescriptor> getRegisteredEndpoints() {
      synchronized (this) {
        List<EndpointDescriptor> result = new ArrayList<>();
        for(MessengerCommand command:mCommandQueue) {
          if (command instanceof RegisterEndpointCommand) {
            RegisterEndpointCommand register = (RegisterEndpointCommand) command;
            result.add(register.getDescriptor());
          }
        }
        return result;
      }
    }

    @Override
    public boolean unregisterEndpoint(final EndpointDescriptor endpoint) {
      synchronized (this) {
        for(Iterator<MessengerCommand> queue= mCommandQueue.iterator(); queue.hasNext();) {
          MessengerCommand command = queue.next();
          if (command instanceof RegisterEndpointCommand && ((RegisterEndpointCommand) command).isEndpoint(endpoint)) {
            queue.remove();
            return true;
          }
        }
      }
      return false;
    }

  }

  private static IMessenger _messenger;

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
      if (! (_messenger instanceof StubMessenger)) {
        _messenger = new StubMessenger(_messenger);
      }
      return;
    } else if (_messenger instanceof StubMessenger) {
      ((StubMessenger) _messenger).flushTo(messenger);
      _messenger = messenger;
    } else if (_messenger != null) {
      throw new IllegalStateException("It is not allowed to register multiple messengers");
    }
    _messenger = messenger;
    Logger.getAnonymousLogger().info("New messenger registered: " + _messenger.getClass().getName());
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
    if (_messenger == null) {
      _messenger = new StubMessenger(null);
    }
    return _messenger;
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
   * @param returnTypeContext The types that need to be known for deserialization.
   * @return A future that can be used to retrieve the result of the sending.
   *         This result will also be passed along to the completionListener.
   * @see IMessenger#sendMessage(ISendableMessage, CompletionListener, Class, Class[])
   */
  public static <T> Future<T> sendMessage(final ISendableMessage message, final CompletionListener<T> completionListener, final Class<T> returnType, final Class<?>[] returnTypeContext) {
    return getMessenger().sendMessage(message, completionListener, returnType, returnTypeContext);
  }

}
