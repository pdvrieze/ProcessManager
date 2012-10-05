package nl.adaptivity.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.devrieze.util.InputStreamOutputStream;

import nl.adaptivity.messaging.ISendableMessage.IHeader;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.util.activation.SourceDataSource;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.ws.soap.SoapHelper;
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

  private static final Object NULL = new Object();

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
      if (pResult==null) {
        aResult = (T) NULL;
      } else {
        aResult=pResult;
      }
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
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message",e);
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
        for(IHeader header: aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        if (hasPayload && (! contenttypeset)) { // Set the content type from the source if not yet set.
          String contentType = aMessage.getBodySource().getContentType();
          if (contentType!=null && contentType.length()>0) {
            httpConnection.addRequestProperty("Content-Type", contentType);
          }
        }
        try {
          httpConnection.connect();
        } catch (ConnectException e) {
          throw new MessagingException("Error connecting to "+destination, e);
        }
        try {
          if (hasPayload) {

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
            Logger.getLogger(DarwinMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          if (aReturnType.isAssignableFrom(SourceDataSource.class)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getInputStream(), baos);
            return aReturnType.cast(new SourceDataSource(httpConnection.getContentType(), new StreamSource(new ByteArrayInputStream(baos.toByteArray()))));
          } else {
            return JAXB.unmarshal(httpConnection.getInputStream(), aReturnType);
          }

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
      if (aResult==NULL) { return null; }
      if (aResult!=null) { return aResult; }
      wait();
      // wait for the result
      return aResult;
    }

    @Override
    public synchronized T get(long pTimeout, TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aCancelled) { throw new CancellationException(); }
      if (aError!=null) { throw new ExecutionException(aError); }
      if (aResult==NULL) { return null; }
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
  private ConcurrentMap<QName,ConcurrentMap<String, Endpoint>> aServices;

  private MessageCompletionNotifier aNotifier;

  private URI aLocalUrl;

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
    aServices = new ConcurrentHashMap<QName, ConcurrentMap<String,Endpoint>>();
    aNotifier.start();

    String localUrl = System.getProperty("nl.adaptivity.messaging.localurl");

    if (localUrl==null) {
      StringBuilder msg = new StringBuilder();
      msg.append("DarwinMessenger\n" +
            "------------------------------------------------\n" +
            "                    WARNING\n" +
            "------------------------------------------------\n" +
            "  Please set the nl.adaptivity.messaging.localurl property in\n" +
            "  catalina.properties (or a method appropriate for a non-tomcat\n" +
            "  container) to the base url used to contact the messenger by\n" +
            "  other components of the system. The public base url can be set as:\n" +
            "  nl.adaptivity.messaging.baseurl, this should be accessible by\n" +
            "  all clients of the system.\n" +
            "================================================");
      Logger.getAnonymousLogger().warning(msg.toString());
    } else {
      try {
        aLocalUrl = URI.create(localUrl);
      } catch (IllegalArgumentException e) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "The given local url is not a valid uri.", e);
      }
    }

  }


  void notifyCompletionListener(MessageTask<?> pFuture) {
    aNotifier.addNotification(pFuture);
  }


  @Override
  public void registerEndpoint(QName pService, String pEndPoint, URI pTarget) {
    registerEndpoint(new EndPointDescriptor(pService, pEndPoint, pTarget));
  }

  @Override
  public synchronized void registerEndpoint(Endpoint pEndpoint) {
    // Note that even though it's a concurrent map we still need to synchronize to
    // prevent race conditions with multiple registrations.
    ConcurrentMap<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service==null) {
      service = new ConcurrentHashMap<String, Endpoint>();
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

    if (registeredEndpoint!=null) { // Direct delivery TODO make this work.
      if ("application/soap+xml".equals(pMessage.getBodySource().getContentType())) {
        SoapMessageHandler handler = SoapMessageHandler.newInstance(registeredEndpoint);
        Source resultSource;
        try {
          resultSource = handler.processMessage(pMessage.getBodySource(), null); // TODO do something with attachments
        } catch (Exception e) {
          Future<T> resultfuture = new MessageTask<T>(e);
          if (pCompletionListener!=null) {
            pCompletionListener.onMessageCompletion(resultfuture);
          }
          return resultfuture;
        }

        final MessageTask<T> resultfuture;
        if (pReturnType.isAssignableFrom(SourceDataSource.class)) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try {
            InputStreamOutputStream.writeToOutputStream(Sources.toInputStream(resultSource), baos);
          } catch (IOException e) {
            throw new MyMessagingException(e);
          }
          resultfuture = new MessageTask<T>(pReturnType.cast(new SourceDataSource("application/soap+xml", new StreamSource(new ByteArrayInputStream(baos.toByteArray())))));
        } else {
          T resultval = SoapHelper.processResponse(pReturnType, resultSource);
          resultfuture = new MessageTask<T>(resultval);
        }

//        resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
        if (pCompletionListener!=null) {
          pCompletionListener.onMessageCompletion(resultfuture);
        }
        return resultfuture;
      }
    }

    if (registeredEndpoint==null) {
      registeredEndpoint = pMessage.getDestination();
    }

    final URI destURL;
    if (aLocalUrl==null) {
      destURL = registeredEndpoint.getEndpointLocation();
    } else {
      destURL = aLocalUrl.resolve(registeredEndpoint.getEndpointLocation());
    }

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

    return service.get(pEndpoint.getEndpointName());
  }

  @Override
  public void shutdown() {
    MessagingRegistry.registerMessenger(null); // Unregister this messenger
    aNotifier.shutdown();
    aExecutor.shutdown();
    aServices=null;
  }

}
