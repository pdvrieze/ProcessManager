package nl.adaptivity.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import net.devrieze.util.InputStreamOutputStream;

import nl.adaptivity.messaging.ISendableMessage.Header;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.ws.soap.SoapMessageHandler;


public class DarwinMessenger implements IMessenger {

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

  /**
   * How many queued messages should be allowed. This is also the limit of pending notifications.
   */
  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  /** The name of the notification tread. */
  private static final String NOTIFIERTHREADNAME = DarwinMessenger.class.getName()+" - Completion Notifier";

  /**
   * How long should the notification thread wait when polling messages. This
   * should ensure that every 30 seconds it checks whether it's finished.
   */
  private static final long NOTIFICATIONPOLLTIMEOUTMS = 30000l; // Timeout polling for next message every 30 seconds


  /**
   * Helper thread that performs (in a single tread) all notifications of messaging completions.
   * The notification can not be done on the sending thread (deadlocks as that thread would be waiting for itself) and the calling tread is unknown.
   * @author Paul de Vrieze
   */
  private class MessageCompletionNotifier extends Thread {

    private final BlockingQueue<MessageTask<?>> aPendingNotifications;
    private volatile boolean aFinished = false;


    public MessageCompletionNotifier() {
      super(NOTIFIERTHREADNAME);
      this.setDaemon(true); // This is just a helper thread, don't block cleanup.
      aPendingNotifications = new LinkedBlockingQueue<MessageTask<?>>(CONCURRENTCAPACITY);
    }

    /**
     * Simple message pump.
     */
    @Override
    public void run() {
      while (! aFinished) {
        try {
          MessageTask<?> future = aPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future!=null) // Null when timeout.
            notififyCompletion(future);
        } catch (InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    private <T extends DataSource> void notififyCompletion(MessageTask<?> pFuture) {
      pFuture.aCompletionListener.onMessageCompletion(pFuture);
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
    public void addNotification(MessageTask<?> pFuture) {
      // aPendingNotifications is threadsafe!
      aPendingNotifications.add(pFuture);

    }


  }

  private class MessageTask<T> implements Future<T>, Runnable {

    private URI aDestURL;
    private ISendableMessage aMessage;
    private CompletionListener aCompletionListener;
    private T aResult = null;
    private boolean aCancelled = false;
    private int aResponseCode;
    private Exception aError = null;
    private boolean aStarted = false;
    private final Class<T> aReturnType;



    public MessageTask(URI pDestURL, ISendableMessage pMessage, CompletionListener pCompletionListener, Class<T> pReturnType) {
      aDestURL = pDestURL;
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
      aReturnType = pReturnType;
    }

    /**
     * Simple constructor that creates a future encapsulating the exception
     * @param pE
     */
    public MessageTask(Exception pE) {
      aError = pE;
      aReturnType=null;
    }

    /**
     * Create a future that just contains the value without computation/ waiting possible
     * @param pUnmarshal
     */
    public MessageTask(T pResult) {
      aResult=pResult;
      aReturnType = null;
    }

    @Override
    public void run() {
      boolean cancelled;
      synchronized(this) {
        aStarted = true;
        cancelled = aCancelled;
      }
      try {
        if (! cancelled ) {
          T result = sendMessage();
          synchronized(this) {
            aResult = result;
          }
        }
      } catch (MessagingException e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message",e);
        throw e;
      } catch (Exception e) {
        synchronized(this) {
          aError = e;
        }
      } finally {
        notifyCompletionListener(this);
      }
    }


    private T sendMessage() throws IOException, ProtocolException {
      URL destination;

      try {
        destination = aDestURL.toURL();
      } catch (MalformedURLException e) {
        throw new MessagingException(e);
      }

      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection){
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        boolean hasPayload = aMessage.getBodySource()!=null;
        connection.setDoOutput(hasPayload);
        String method = aMessage.getMethod();
        if (method==null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);

        boolean contenttypeset = false;
        for(Header header: aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        try {
          httpConnection.connect();
        } catch (ConnectException e) {
          throw new MessagingException("Error connecting to "+destination, e);
        }
        try {
          if (hasPayload) {
            if (! contenttypeset) { // Set the content type from the source if not yet set.
              String contentType = aMessage.getBodySource().getContentType();
              if (contentType!=null && contentType.length()>0) {
                httpConnection.addRequestProperty("Content-Type", contentType);
              }
            }

            OutputStream out = httpConnection.getOutputStream();

            try {
              InputStreamOutputStream.writeToOutputStream(aMessage.getBodySource().getInputStream(), out);
            } finally {
              out.close();
            }
          }
          aResponseCode = httpConnection.getResponseCode();
          if (aResponseCode<200 || aResponseCode>=400) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getErrorStream(), baos);
            String errorMessage = "Error in sending message with "+method+" to ("+destination+") ["+aResponseCode+"]:\n"+new String(baos.toByteArray());
            Logger.getLogger(AsyncMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          return JAXB.unmarshal(httpConnection.getInputStream(), aReturnType);

        } finally {
          httpConnection.disconnect();
        }

      } else {
        throw new UnsupportedOperationException("No support yet for non-http connections");
      }
    }


    @Override
    public synchronized boolean cancel(boolean pMayInterruptIfRunning) {
      if (aCancelled) { return true; }
      if (! aStarted) {
        aCancelled = true;
        return true;
      }
      // TODO support interrupt running process
      return false;
    }

    @Override
    public synchronized boolean isCancelled() {
      return aCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      return aCancelled || aResult!=null || aError!=null;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      if (aCancelled) { throw new CancellationException(); }
      if (aError!=null) { throw new ExecutionException(aError); }
      if (aResult!=null) { return aResult; }
      wait();
      // wait for the result
      return aResult;
    }

    @Override
    public synchronized T get(long pTimeout, TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aCancelled) { throw new CancellationException(); }
      if (aError!=null) { throw new ExecutionException(aError); }
      if (aResult!=null) { return aResult; }
      if (pTimeout==0) { throw new TimeoutException(); } 


      try {
        if (pUnit==TimeUnit.NANOSECONDS) {
          wait(pUnit.toMillis(pTimeout), (int) (pUnit.toNanos(pTimeout)%1000000));
        } else {
          wait(pUnit.toMillis(pTimeout));
        }
      } catch (InterruptedException e) {
        if (isDone()) {
          return get(0, TimeUnit.MILLISECONDS);// Don't wait, even if somehow the state is wrong.
        } else {
          throw e;
        }
      }
      throw new TimeoutException();
    }

  }

  /** Let the class loader do the nasty synchronization for us, but still initialise ondemand. */
  private static class MessengerHolder {
    static final DarwinMessenger globalMessenger = new DarwinMessenger();
  }

  ExecutorService aExecutor;
  private Map<QName,Map<String, Endpoint>> aServices;

  private MessageCompletionNotifier aNotifier;

  /**
   * Get the singleton instance. This also updates the base URL.
   * @return The singleton instance.
   */
  public static void register() {
    MessagingRegistry.registerMessenger(MessengerHolder.globalMessenger);
  }
  

  private DarwinMessenger() {
    aExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aNotifier = new MessageCompletionNotifier();
    aNotifier.start();
  }


  void notifyCompletionListener(MessageTask<?> pFuture) {
    aNotifier.addNotification(pFuture);
  }


  @Override
  public void registerEndpoint(QName pService, String pEndPoint, URI pTarget) {
    registerEndpoint(new EndPointDescriptor(pService, pEndPoint, pTarget));
  }

  @Override
  public void registerEndpoint(Endpoint pEndpoint) {
    Map<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service==null) {
      service = new TreeMap<String, Endpoint>();
      aServices.put(pEndpoint.getServiceName(), service);
    }
    if (service.containsKey(pEndpoint.getEndpointName())) {
      service.remove(pEndpoint.getEndpointName());
    }
    service.put(pEndpoint.getEndpointName(), pEndpoint);
  }

  @Override
  public <T> Future<T> sendMessage(ISendableMessage pMessage, CompletionListener pCompletionListener, Class<T> pReturnType) {
    Endpoint registeredEndpoint = getEndpoint(pMessage.getDestination());

    if (registeredEndpoint instanceof DirectEndpoint) {
      return ((DirectEndpoint) registeredEndpoint).deliverMessage(pMessage, pCompletionListener, pReturnType);
    }
    
    if (registeredEndpoint!=null) { // Direct delivery
      if ("application/soap+xml".equals(pMessage.getBodySource().getContentType())) {
        SoapMessageHandler handler = SoapMessageHandler.newInstance(registeredEndpoint);
        Source resultSource;
        try {
          resultSource = handler.processMessage(pMessage.getBodySource(), null); // TODO do something with attachments
        } catch (Exception e) {
          Future<T> resultfuture = new MessageTask<T>(e);
          pCompletionListener.onMessageCompletion(resultfuture);
          return resultfuture;
        }
        final MessageTask<T> resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
        pCompletionListener.onMessageCompletion(resultfuture);
        return resultfuture;
      }
    }

    if (registeredEndpoint==null) {
      registeredEndpoint = pMessage.getDestination();
    }

    final URI destURL = registeredEndpoint.getEndpointLocation();

    MessageTask<T> messageTask = new MessageTask<T>(destURL, pMessage, pCompletionListener, pReturnType);
    aExecutor.execute(messageTask);
    return messageTask;
  }

  public Endpoint getEndpoint(QName pServiceName, String pEndpointName) {
    Map<String, Endpoint> service = aServices.get(pServiceName);
    if (service==null) { return null; }
    return service.get(pEndpointName);
  }

  public Endpoint getEndpoint(Endpoint pEndpoint) {
    Map<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service==null) { return null; }
    return service.get(pEndpoint.getServiceName());
  }

  public void destroy() {
    aNotifier.shutdown();
    aExecutor.shutdown();
    aServices=null;
  }
  
}
