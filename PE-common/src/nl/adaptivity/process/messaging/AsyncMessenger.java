package nl.adaptivity.process.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.devrieze.util.Urls;

import nl.adaptivity.process.engine.MyMessagingException;


/**
 * 
 * @author pdvrieze
 * @todo Add the abiltiy to send directly to servlets on the same host.
 */
public class AsyncMessenger {

  
  public interface CompletionListener {

    void onMessageCompletion(AsyncFuture pFuture);

  }

  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  public interface AsyncFuture extends Future<byte[]> {

    long getHandle();

    int getResponseCode();
    
  }
  
  private class AsyncFutureCallable implements Callable<byte[]> {
    private final long aHandle;
    private final ISendableMessage aMessage;
    private int aResponseCode;
    private AsyncFuture aFuture;

    public AsyncFutureCallable(ISendableMessage pMessage, long pHandle) {
      aMessage = pMessage;
      aHandle = pHandle;
    }

    @Override
    public byte[] call() throws Exception {
      byte[] result = sendMessage();
      notifyCompletionListeners(aFuture);
      return result;
    }

    private byte[] sendMessage() throws IOException, ProtocolException {
      URL destination = aMessage.getDestination();
      if (destination.getProtocol()==null || destination.getProtocol().length()==0 || destination.getHost()==null || destination.getHost().length()==0) {
        destination = new URL(getOwnUrl(), aMessage.getDestination().toString());
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
        
        for(Entry<String, String> header: aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getKey(), header.getValue());
        }
        httpConnection.connect();
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
            throw new MyMessagingException("Error in the result code: "+httpConnection.getResponseMessage());
          }
          ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
          byte[] buffer = new byte[0x4000];
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
          return resultBuffer.toByteArray();
        
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
  
  private static class AsyncFutureImpl extends FutureTask<byte[]> implements AsyncFuture {

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
    
  }

  // Let the class loader do the nasty synchronization for us, but still initialise ondemand.
  private static class MessengerHolder {
    static final AsyncMessenger globalMessenger = new AsyncMessenger(Urls.newURL("http://localhost/"));
  }

  ExecutorService aExecutor;
  private Collection<CompletionListener> aListeners;
  private final URL aBaseUrl;
  
  public static AsyncMessenger getInstance() {
    return MessengerHolder.globalMessenger;
  }
  
  public URL getOwnUrl() {
    return aBaseUrl;
  }

  private AsyncMessenger(URL pBaseUrl) {
    aExecutor = new ThreadPoolExecutor(1, 20, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aListeners = new ArrayList<CompletionListener>();
    aBaseUrl = pBaseUrl;
  }

  public void addCompletionListener(CompletionListener pListener) {
    aListeners.add(pListener);
  }
  
  public void notifyCompletionListeners(AsyncFuture pFuture) {
    for(CompletionListener listener: aListeners) {
      listener.onMessageCompletion(pFuture);
    }
  }
  
  public void removeCompletionListener(CompletionListener pListener) {
    aListeners.remove(pListener);
  }

  /**
   * Shut down the messenger
   */
  public void destroy() {
    aExecutor.shutdown();
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

}
