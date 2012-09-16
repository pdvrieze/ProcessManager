package nl.adaptivity.process.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;

import net.devrieze.util.InputStreamOutputStream;
import net.devrieze.util.Tupple;

import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.messaging.AsyncMessenger.CompletionListener;
import nl.adaptivity.util.HttpMessage;


/**
 * A messenger class for sending and receiving messages.
 * This is a singleton class, and for a given classloader will always be the same instance.
 * 
 * @author pdvrieze
 * @todo Add the abiltiy to send directly to servlets on the same host.
 */
public class AsyncMessenger {

  /**
   * How big should the worker thread pool be initially.
   */
  private static final int INITIAL_WORK_THREADS = 1;

  /**
   * How many worker threads are there concurrently? Note that extra work will not block,
   * it will just be added to a waiting queue.
   */
  private static final int MAXIMUM_WORK_THREADS = 20;
  
  /**
   * How long to keep idle worker threads busy (in miliseconds).
   */
  private static final int WORKER_KEEPALIVE_MS = 60000;

  /** The name of the notification tread. */
  private static final String NOTIFIERTHREADNAME = AsyncMessenger.class.getName()+" - Completion Notifier";
  
  /** 
   * How long should the notification thread wait when polling messages. This 
   * should ensure that every 30 seconds it checks whether it's finished.
   */
  private static final long NOTIFICATIONPOLLTIMEOUTMS = 30000l; // Timeout polling for next message every 30 seconds

  /**
   * How many queued messages should be allowed. This is also the limit of pending notifications.
   */
  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  /**
   * Helper thread that performs (in a single tread) all notifications of messaging completions.
   * The notification can not be done on the sending thread (deadlocks as that thread would be waiting for itself) and the calling tread is unknown.
   * @author Paul de Vrieze
   */
  private class MessageCompletionNotifier extends Thread {
    
    private final BlockingQueue<AsyncFuture> aPendingNotifications;
    private volatile boolean aFinished = false;
    
    
    public MessageCompletionNotifier() {
      super(NOTIFIERTHREADNAME);
      this.setDaemon(true); // This is just a helper thread, don't block cleanup.
      aPendingNotifications = new LinkedBlockingQueue<AsyncFuture>(CONCURRENTCAPACITY);
    }

    /**
     * Simple message pump.
     */
    @Override
    public void run() {
      while (! aFinished) {
        try {
          AsyncFuture future = aPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future!=null) // Null when timeout. 
            notififyCompletion(future);
        } catch (InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    private void notififyCompletion(AsyncFuture pFuture) {
      ArrayList<CompletionListener> listeners;
      synchronized(aListeners) { // Make a local copy of the list of listeners
        // Notifying them all can take a long time and will block any modification
        // of the list needlessly.
        listeners = new ArrayList<CompletionListener>(aListeners);
      }
      for(CompletionListener listener: listeners) {
        listener.onMessageCompletion(pFuture);
      }
    }
    
    /**
     * Allow for shutting down the thread.
     */
    public void shutdown() {
      aFinished = true;
      interrupt();
    }

    /**
     * Add a notification to the message queue.
     * @param pFuture The future whose completion should be communicated.
     */
    public void addNotification(AsyncFuture pFuture) {
      // aPendingNotifications is threadsafe!
      aPendingNotifications.add(pFuture);
      
    }
    
    
  }
  
  /**
   * Interface for classes that can receive completion messages from the {@link AsyncMessenger}. This happens
   * in a separate thread.
   * @author Paul de Vrieze
   *
   */
  public interface CompletionListener {

    /**
     * Signify the completion of the task corresponding to the given future. Note that
     * as the notification is done as the last part of the calling there future theoretically
     * is not guaranteed to be complete.
     * @param pFuture The future that is complete.
     */
    void onMessageCompletion(AsyncFuture pFuture);

  }

  /** 
   * Special kind of future that contains additional information.
   * @author Paul de Vrieze
   *
   */
  public interface AsyncFuture extends Future<DataSource> {

    /** 
     * The handle corresponding to the message this future is the response to.
     * @return The handle.
     */
    long getHandle();

    /**
     * The HTTP response code.
     * @return The response code.
     */
    int getResponseCode();

    /**
     * Update the metadata based on the given {@link HttpURLConnection}
     * @param pHttpConnection The connection to get metadata from.
     */
    void setMetadata(HttpURLConnection pHttpConnection);
    
  }
  
  /**
   * Callable that does the actual work of communicating with the remote service.
   * @author Paul de Vrieze
   *
   */
  private class AsyncFutureCallable implements Callable<DataSource> {
    private final long aHandle;
    private final ISendableMessage aMessage;
    private int aResponseCode;
    private AsyncFuture aFuture;

    public AsyncFutureCallable(ISendableMessage pMessage, long pHandle) {
      aMessage = pMessage;
      aHandle = pHandle;
    }

    /**
     * Calls {@link #sendMessage()} to do the sending and receiving, and finally notifies of completion.
     * @return A DataSource that contains the bytes of the response body, as well as the contenttype if given.
     */
    @Override
    public DataSource call() throws Exception {
      try {
        DataSource result = sendMessage();
        return result;
      } catch (MyMessagingException e) {
        Logger.getLogger(AsyncMessenger.class.getName()).log(Level.WARNING, "Error sending message",e);
        throw e;
      } finally {
        notifyCompletionListeners(aFuture);
      }
    }

    private DataSource sendMessage() throws IOException, ProtocolException {
      URL destination;
    
      try {
        destination = new URL(aMessage.getDestination());
      } catch (MalformedURLException e) {
        destination = new URL(getOwnUrl(), aMessage.getDestination());
      }
        
      if (destination.getProtocol()==null || destination.getProtocol().length()==0 || destination.getHost()==null || destination.getHost().length()==0) {
        destination = new URL(getOwnUrl(), aMessage.getDestination());
      }
      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection){
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        boolean hasPayload = aMessage.hasBody();
        connection.setDoOutput(hasPayload);
        String method = aMessage.getMethod();
        if (method==null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);
        
        for(Tupple<String, String> header: aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getElem1(), header.getElem2());
        }
        try {
          httpConnection.connect();
        } catch (ConnectException e) {
          throw new MyMessagingException("Error connecting to "+destination, e);
        }
        try {
          if (hasPayload) {
            OutputStream out = httpConnection.getOutputStream();
          
            try {
              aMessage.writeBody(httpConnection.getOutputStream());
            } finally {
              out.close();
            }
          }
          aResponseCode = httpConnection.getResponseCode();
          if (aResponseCode<200 || aResponseCode>=400) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Future<Boolean> isos = InputStreamOutputStream.getInputStreamOutputStream(httpConnection.getErrorStream(), baos);
            try {
              if (isos.get()) {
                Logger.getLogger(AsyncMessenger.class.getName()).info("Error in sending message with "+method+" to ("+destination+") ["+aResponseCode+"]:\n"+new String(baos.toByteArray())); 
              } else {
                throw new MyMessagingException("Error in the result code: "+httpConnection.getResponseMessage());
              }
            } catch (InterruptedException e) {
              throw new MyMessagingException(e);
            } catch (ExecutionException e) {
              throw new MyMessagingException(e);
            }
          }
          ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
          byte[] buffer = new byte[0x4000];
          aFuture.setMetadata(httpConnection);
          InputStream in = httpConnection.getInputStream();
          try {
            int i=0;
            do {
              if (i>0) {
                resultBuffer.write(buffer, 0, i);
              }
              i = in.read(buffer);
            } while (i>=0);
          } finally {
            in.close();
          }
          return new HttpMessage.ByteContentDataSource(null, httpConnection.getContentType(), resultBuffer.toByteArray());
        
        } finally {
          httpConnection.disconnect();
        }
        
      } else {
        throw new UnsupportedOperationException("No support yet for non-http connections");
      }
    }

    public void setFuture(AsyncFuture pFuture) {
      aFuture = pFuture;
    }
    
  }
  
  /**
   * Class that actually implements the future. Most work is done in {@link AsyncFutureCallable} though.
   * @author Paul de Vrieze
   *
   */
  private static class AsyncFutureImpl extends FutureTask<DataSource> implements AsyncFuture {

    private AsyncFutureCallable aCallable;

    AsyncFutureImpl(AsyncMessenger pMessenger, ISendableMessage pMessage, long pHandle) {
      this(pMessenger.new AsyncFutureCallable(pMessage, pHandle));
    }
    
    private AsyncFutureImpl(AsyncFutureCallable pCallable) {
      super(pCallable);
      aCallable = pCallable;
      aCallable.setFuture(this);
    }
    
    @Override
    public long getHandle() {
      return aCallable.aHandle;
    }
    
    @Override
    public int getResponseCode() {
      return aCallable.aResponseCode;
    }

    @Override
    public void setMetadata(HttpURLConnection pHttpConnection) {
      // TODO record more metadata
    }
    
  }

  /** Let the class loader do the nasty synchronization for us, but still initialise ondemand. */
  private static class MessengerHolder {
    static final AsyncMessenger globalMessenger = new AsyncMessenger();
  }

  ExecutorService aExecutor;
  private Collection<CompletionListener> aListeners;
  /**
   * The url agains which relative urls are resolved.
   */
  private URL aBaseUrl;
  private MessageCompletionNotifier aNotifier;
  
  /**
   * Get the singleton instance. This also updates the base URL.
   * @param pBaseUrl The url to resolve relative urls to.
   * @return The singleton instance.
   */
  public static AsyncMessenger getInstance(URL pBaseUrl) {
    MessengerHolder.globalMessenger.aBaseUrl = pBaseUrl;
    return MessengerHolder.globalMessenger;
  }

  /**
   * Get the URL used to resolve relative urls against.
   * @return The base URL.
   */
  public URL getOwnUrl() {
    return aBaseUrl;
  }

  /**
   * Create the messenger.
   */
  private AsyncMessenger() {
    aExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aListeners = new ArrayList<CompletionListener>();
    aNotifier = new MessageCompletionNotifier();
    aNotifier.start();
  }

  /**
   * Add a listener for completions.
   * @param pListener
   */
  public void addCompletionListener(CompletionListener pListener) {
    synchronized (aListeners) {
      aListeners.add(pListener);
    }
  }
  
  public void notifyCompletionListeners(AsyncFuture pFuture) {
    aNotifier.addNotification(pFuture);
  }
  
  public void removeCompletionListener(CompletionListener pListener) {
    synchronized(aListeners) {
      aListeners.remove(pListener);
    }
  }

  /**
   * Shut down the messenger
   */
  public void destroy() {
    aExecutor.shutdown();
    aNotifier.shutdown();
    // Release the resources.
  }

  /**
   * Send the given message to whereever it needs to go.
   * @param pMessage The message to send
   * @param pHandle The handle of the node corresponding to the message.
   * @return A future for the asynchronous message.
   */
  public AsyncFuture sendMessage(ISendableMessage pMessage, long pHandle) {
    AsyncFutureImpl future = new AsyncFutureImpl(this, pMessage, pHandle);
    aExecutor.execute(future);
    return future;
  }

  /**
   * Update only the port part of our own url. Apparently it's impossible to determine
   * our own port from the context. Only from a request is this possible.
   * @param pLocalPort The local port to use in the aBaseUrl.
   */
  public void setOwnPort(int pLocalPort) {
    try {
      aBaseUrl = new URL(aBaseUrl.getProtocol(), aBaseUrl.getHost(), pLocalPort, aBaseUrl.getFile());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

}
