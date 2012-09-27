package nl.adaptivity.messaging;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;


public final class MessagingRegistry {


  private static class WrappingFuture<T> implements Future<T>, MessengerCommand, CompletionListener {

    private ISendableMessage aMessage;
    private Future<T> aOrigin;
    private boolean aCancelled = false;
    private CompletionListener aCompletionListener;

    public WrappingFuture(ISendableMessage pMessage, CompletionListener pCompletionListener) {
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
    }

    @Override
    public synchronized boolean cancel(boolean pMayInterruptIfRunning) {
      if (aOrigin==null) {
        aCancelled = true;
        if (aCompletionListener!=null) { aCompletionListener.onMessageCompletion(this); }
      } else {
        aCancelled = aOrigin.cancel(pMayInterruptIfRunning);
      }
      return aCancelled;
    }

    @Override
    public synchronized boolean isCancelled() {
      if (aOrigin!=null) {
        return aOrigin.isCancelled();
      }
      return aCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      if (aOrigin!=null) {
        return aOrigin.isDone();
      }
      return false || aCancelled;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      while (aOrigin==null) {
        if (aCancelled) {
          throw new CancellationException();
        }
        wait();
      }
      return aOrigin.get();
    }

    @Override
    public synchronized T get(long pTimeout, TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aOrigin==null) {
        long startTime = System.currentTimeMillis();
        try {
          if (pUnit==TimeUnit.NANOSECONDS) {
            wait(pUnit.toMillis(pTimeout), (int) (pUnit.toNanos(pTimeout)%1000000));
          } else {
            wait(pUnit.toMillis(pTimeout));
          }
        } catch (InterruptedException e) {
          if (aOrigin!=null) {
            // Assume we are woken up because of the change not another interruption.
            final long currentTime = System.currentTimeMillis();
            final long millisLeft = pUnit.toMillis(pTimeout)-(currentTime - startTime);
            if (millisLeft>0) {
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
    public synchronized void execute(IMessenger pMessenger) {
      if (! aCancelled) {
        aOrigin = pMessenger.sendMessage(aMessage, this);
      }
      notifyAll(); // Wake up all waiters (should be only one)
    }

    @Override
    public void onMessageCompletion(Future<?> pFuture) {
      if (aCompletionListener!=null) {
        aCompletionListener.onMessageCompletion(this);
      }
    }

  }

  private static interface MessengerCommand {
    void execute(IMessenger pMessenger);
  }

  private static class StubMessenger implements IMessenger {
    IMessenger aMessenger=null;
    Queue<MessengerCommand> aCommandQueue;

    StubMessenger() {
      aCommandQueue = new ArrayDeque<MessengerCommand>();
    }

    public synchronized void setMessenger(IMessenger pMessenger) {
      aMessenger = pMessenger;
      for(MessengerCommand command: aCommandQueue) {
        command.execute(aMessenger);
      }
      aCommandQueue=null; // We don't need it anymore, we'll just forward.
    }

    @Override
    public void registerEndpoint(final QName pService, final String pEndPoint, final URI pTarget) {
      synchronized(this) {
        if (aMessenger==null) {
          aCommandQueue.add(new MessengerCommand(){

            @Override
            public void execute(IMessenger pMessenger) {
              pMessenger.registerEndpoint(pService, pEndPoint, pTarget);
            }

          });
          return;
        }
      }
      aMessenger.registerEndpoint(pService, pEndPoint, pTarget);
    }

    @Override
    public void registerEndpoint(final Endpoint pEndpoint) {
      synchronized(this) {
        if (aMessenger==null) {
          aCommandQueue.add(new MessengerCommand(){

            @Override
            public void execute(IMessenger pMessenger) {
              pMessenger.registerEndpoint(pEndpoint);
            }

          });
          return;
        }
      }
      aMessenger.registerEndpoint(pEndpoint);
    }

    @Override
    public <T> Future<T> sendMessage(final ISendableMessage pMessage, final CompletionListener pCompletionListener) {
      synchronized(this) {
        if (aMessenger==null) {
          final WrappingFuture<T> future = new WrappingFuture<T>(pMessage, pCompletionListener);
          aCommandQueue.add(future);
          return future;
        }
      }
      return aMessenger.sendMessage(pMessage, pCompletionListener);
    }

  }

  private static IMessenger aMessenger;

  public static synchronized void registerMessenger(IMessenger pMessenger) {
    if (pMessenger==null) { throw new NullPointerException("Null messengers are not allowed"); }
    if (aMessenger instanceof StubMessenger) {
      ((StubMessenger) aMessenger).setMessenger(pMessenger);
      aMessenger = pMessenger;
    } else if (aMessenger!=null) {
      throw new IllegalStateException("It is not allowed to register multiple messengers");
    }
    aMessenger = pMessenger;
  }

  public static synchronized IMessenger getMessenger() {
    if (aMessenger==null) {
      aMessenger = new StubMessenger();
    }
    return aMessenger;
  }

  public static <T> Future<T> sendMessage(ISendableMessage pMessage, CompletionListener pCompletionListener) {
    return getMessenger().<T>sendMessage(pMessage, pCompletionListener);
  }

}
