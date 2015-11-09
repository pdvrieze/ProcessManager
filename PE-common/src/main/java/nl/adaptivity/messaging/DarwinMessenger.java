package nl.adaptivity.messaging;

import net.devrieze.util.InputStreamOutputStream;
import nl.adaptivity.messaging.ISendableMessage.IHeader;
import nl.adaptivity.util.activation.SourceDataSource;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.ws.soap.SoapHelper;
import nl.adaptivity.ws.soap.SoapMessageHandler;

import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Messenger to use in the darwin project.
 *
 * @author Paul de Vrieze
 */
public class DarwinMessenger implements IMessenger {

  /**
   * How big should the worker thread pool be initially.
   */
  private static final int INITIAL_WORK_THREADS = 1;

  /**
   * How many worker threads are there concurrently? Note that extra work will
   * not block, it will just be added to a waiting queue.
   */
  private static final int MAXIMUM_WORK_THREADS = 20;

  /**
   * How long to keep idle worker threads busy (in miliseconds).
   */
  private static final int WORKER_KEEPALIVE_MS = 60000;

  /**
   * How many queued messages should be allowed. This is also the limit of
   * pending notifications.
   */
  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  /** The name of the notification tread. */
  private static final String NOTIFIERTHREADNAME = DarwinMessenger.class.getName() + " - Completion Notifier";

  /**
   * How long should the notification thread wait when polling messages. This
   * should ensure that every 30 seconds it checks whether it's finished.
   */
  private static final long NOTIFICATIONPOLLTIMEOUTMS = 30000l; // Timeout polling for next message every 30 seconds


  /**
   * Marker object for null results.
   */
  private static final Object NULL = new Object();

  /**
   * Helper thread that performs (in a single tread) all notifications of
   * messaging completions. The notification can not be done on the sending
   * thread (deadlocks as that thread would be waiting for itself) and the
   * calling tread is unknown.
   *
   * @author Paul de Vrieze
   */
  private class MessageCompletionNotifier extends Thread {

    /**
     * Queue containing the notifications still to be sent. This is internally
     * synchronized so doesn't need to be manually synchronized.
     */
    private final BlockingQueue<MessageTask<?>> aPendingNotifications;

    private volatile boolean aFinished = false;


    /**
     * Create a new notifier.
     */
    public MessageCompletionNotifier() {
      super(NOTIFIERTHREADNAME);
      this.setDaemon(true); // This is just a helper thread, don't block cleanup.
      aPendingNotifications = new LinkedBlockingQueue<>(CONCURRENTCAPACITY);
    }

    /**
     * Simple message pump.
     */
    @Override
    public void run() {
      while (!aFinished) {
        try {
          final MessageTask<?> future = aPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future != null) {
            notififyCompletion(future);
          }
        } catch (final InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    /**
     * Allow for shutting down the thread. As aFinished is volatile, this should
     * not need further synchronization.
     */
    public void shutdown() {
      aFinished = true;
      interrupt();
    }

    /**
     * Add a notification to the message queue.
     *
     * @param pFuture The future whose completion should be communicated.
     */
    public void addNotification(final MessageTask<?> pFuture) {
      // aPendingNotifications is threadsafe!
      aPendingNotifications.add(pFuture);

    }

    /**
     * Helper method to notify of future completion.
     *
     * @param pFuture The future to notify completion of.
     */
    private void notififyCompletion(final MessageTask<?> pFuture) {
      pFuture.aCompletionListener.onMessageCompletion(pFuture);
    }


  }

  /**
   * Future that encapsulates a future that represents the sending of a message.
   * This is a message that
   *
   * @author Paul de Vrieze
   * @param <T>
   */
  private class MessageTask<T> implements RunnableFuture<T> {

    /** The uri to use for sending the message. */
    private URI aDestURL;

    /** The message to send. */
    private ISendableMessage aMessage;

    /** The listener to notify of completion. */
    private CompletionListener aCompletionListener;

    /** The result value. */
    private T aResult = null;

    /** The cancellation state. */
    private boolean aCancelled = false;

    /** The response code given by the response. */
    private int aResponseCode;

    /** The exception in this future. */
    private Exception aError = null;

    /** Set when the message sending is actually active. The processing of the future has started. */
    private boolean aStarted = false;

    /** The return type of the future. */
    private final Class<T> aReturnType;

    /**
     * Create a new task.
     * @param pDestURL The url to invoke
     * @param pMessage The message to send.
     * @param pCompletionListener The listener to notify. This may be <code>null</code>.
     * @param pReturnType The return type of the message. Needed for unmarshalling.
     */
    public MessageTask(final URI pDestURL, final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
      aDestURL = pDestURL;
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
      aReturnType = pReturnType;
    }

    /**
     * Simple constructor that creates a future encapsulating the exception
     *
     * @param pE The exception to encapsulate.
     */
    public MessageTask(final Exception pE) {
      aError = pE;
      aReturnType = null;
    }

    /**
     * Create a future that just contains the value without computation/ waiting
     * possible. The result value. This is for returning synchronous values as
     * future.
     *
     * @param pResult The result value of the future.
     */
    @SuppressWarnings("unchecked")
    public MessageTask(final T pResult) {
      if (pResult == null) {
        aResult = (T) NULL;
      } else {
        aResult = pResult;
      }
      aReturnType = null;
    }

    @Override
    public void run() {
      boolean cancelled;
      synchronized (this) {
        aStarted = true;
        cancelled = aCancelled;
      }
      try {
        if (!cancelled) {
          final T result = sendMessage();
          synchronized (this) {
            if (result==null) {
              // Use separate value to allow for suppressing of warning.
              @SuppressWarnings("unchecked")
              final T v = (T) NULL;
              aResult = v;
            } else {
              aResult = result;
            }
          }
        }
      } catch (final MessagingException e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        throw e;
      } catch (final Exception e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        synchronized (this) {
          aError = e;
        }
      } finally {
        notifyCompletionListener(this);
      }
    }

    /**
     * This method performs the actual sending of the message.
     * @return The return value of the message.
     * @throws IOException
     * @throws ProtocolException
     */
    private T sendMessage() throws IOException, ProtocolException {
      URL destination;

      try {
        destination = aDestURL.toURL();
      } catch (final MalformedURLException e) {
        throw new MessagingException(e);
      }

      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection) {
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        final boolean hasPayload = aMessage.getBodySource() != null;
        connection.setDoOutput(hasPayload);
        String method = aMessage.getMethod();
        if (method == null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);

        boolean contenttypeset = false;
        for (final IHeader header : aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        if (hasPayload && (!contenttypeset)) { // Set the content type from the source if not yet set.
          final String contentType = aMessage.getBodySource().getContentType();
          if ((contentType != null) && (contentType.length() > 0)) {
            httpConnection.addRequestProperty("Content-Type", contentType);
          }
        }
        try {
          httpConnection.connect();
        } catch (final ConnectException e) {
          throw new MessagingException("Error connecting to " + destination, e);
        }
        try {
          if (hasPayload) {
            try(final OutputStream out = httpConnection.getOutputStream()) {
              InputStreamOutputStream.writeToOutputStream(aMessage.getBodySource().getInputStream(), out);
            }
          }
          aResponseCode = httpConnection.getResponseCode();
          if ((aResponseCode < 200) || (aResponseCode >= 400)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getErrorStream(), baos);
            final String errorMessage = "Error in sending message with " + method + " to (" + destination + ") [" + aResponseCode + "]:\n"
                + new String(baos.toByteArray());
            Logger.getLogger(DarwinMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          if (aReturnType.isAssignableFrom(SourceDataSource.class)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

    /**
     * Cancel the performance of this task. Currently will never actually honour
     * the parameter and will never interrupt after the sending started.
     */
    @Override
    public synchronized boolean cancel(final boolean pMayInterruptIfRunning) {
      if (aCancelled) {
        return true;
      }
      if (!aStarted) {
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
      return aCancelled || (aResult != null) || (aError != null);
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      if (aCancelled) {
        throw new CancellationException();
      }
      if (aError != null) {
        throw new ExecutionException(aError);
      }
      if (aResult == NULL) {
        return null;
      }
      if (aResult != null)
      {
        return aResult;
      }
      wait();
      // wait for the result
      return aResult;
    }

    /**
     * {@inheritDoc} Note that there may be some inaccuracies in the waiting
     * time especially if the waiting started before the message delivery
     * started, but the timeout finished while the result was not yet in.
     */
    @Override
    public synchronized T get(final long pTimeout, final TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aCancelled) {
        throw new CancellationException();
      }
      if (aError != null) {
        throw new ExecutionException(aError);
      }
      if (aResult == NULL) {
        return null;
      }
      if (aResult != null) {
        return aResult;
      }
      if (pTimeout == 0) {
        throw new TimeoutException();
      }


      try {
        if (pUnit == TimeUnit.NANOSECONDS) {
          wait(pUnit.toMillis(pTimeout), (int) (pUnit.toNanos(pTimeout) % 1000000));
        } else {
          wait(pUnit.toMillis(pTimeout));
        }
      } catch (final InterruptedException e) {
        if (isDone()) {
          return get(0, TimeUnit.MILLISECONDS);// Don't wait, even if somehow the state is wrong.
        } else {
          throw e;
        }
      }
      throw new TimeoutException();
    }

  }

  /**
   * Let the class loader do the nasty synchronization for us, but still
   * initialise ondemand.
   */
  private static class MessengerHolder {

    static final DarwinMessenger globalMessenger = new DarwinMessenger();
  }

  private final ExecutorService aExecutor;

  private ConcurrentMap<QName, ConcurrentMap<String, EndpointDescriptor>> aServices;

  private final MessageCompletionNotifier aNotifier;

  private URI aLocalUrl;

  /**
   * Get the singleton instance. This also updates the base URL.
   *
   * @return The singleton instance.
   */
  public static void register() {
    MessagingRegistry.registerMessenger(MessengerHolder.globalMessenger);
  }

  /**
   * Create a new messenger. As the class is a singleton, this is only invoked
   * (indirectly) through {@link #register()}
   */
  private DarwinMessenger() {
    aExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aNotifier = new MessageCompletionNotifier();
    aServices = new ConcurrentHashMap<>();
    aNotifier.start();

    final String localUrl = System.getProperty("nl.adaptivity.messaging.localurl");

    if (localUrl == null) {
      final StringBuilder msg = new StringBuilder();
      msg.append("DarwinMessenger\n" + "------------------------------------------------\n" + "                    WARNING\n"
          + "------------------------------------------------\n" + "  Please set the nl.adaptivity.messaging.localurl property in\n"
          + "  catalina.properties (or a method appropriate for a non-tomcat\n"
          + "  container) to the base url used to contact the messenger by\n"
          + "  other components of the system. The public base url can be set as:\n"
          + "  nl.adaptivity.messaging.baseurl, this should be accessible by\n" + "  all clients of the system.\n"
          + "================================================");
      Logger.getAnonymousLogger().warning(msg.toString());
    } else {
      try {
        aLocalUrl = URI.create(localUrl);
      } catch (final IllegalArgumentException e) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "The given local url is not a valid uri.", e);
      }
    }

  }


  @Override
  public EndpointDescriptor registerEndpoint(final QName pService, final String pEndPoint, final URI pTarget) {
    final EndpointDescriptorImpl endpoint = new EndpointDescriptorImpl(pService, pEndPoint, pTarget);
    registerEndpoint(endpoint);
    return endpoint;
  }


  @Override
  public synchronized void registerEndpoint(final EndpointDescriptor pEndpoint) {
    // Note that even though it's a concurrent map we still need to synchronize to
    // prevent race conditions with multiple registrations.
    ConcurrentMap<String, EndpointDescriptor> service = aServices.get(pEndpoint.getServiceName());
    if (service == null) {
      service = new ConcurrentHashMap<>();
      aServices.put(pEndpoint.getServiceName(), service);
    }
    if (service.containsKey(pEndpoint.getEndpointName())) {
      service.remove(pEndpoint.getEndpointName());
    }
    service.put(pEndpoint.getEndpointName(), pEndpoint);
  }

  @Override
  public List<EndpointDescriptor> getRegisteredEndpoints() {
    ArrayList<EndpointDescriptor> result = new ArrayList<>();
    synchronized (aServices) {
      for (ConcurrentMap<String, EndpointDescriptor> service:aServices.values()) {
        for(EndpointDescriptor endpoint:service.values()) {
          result.add(endpoint);
        }
      }
    }
    return result;
  }

  @Override
  public boolean unregisterEndpoint(EndpointDescriptor pEndpoint) {
    synchronized (aServices) {
      ConcurrentMap<String, EndpointDescriptor> service = aServices.get(pEndpoint.getServiceName());
      if (service==null) { return false; }
      EndpointDescriptor result = service.remove(pEndpoint.getEndpointName());
      if (service.isEmpty()) {
        aServices.remove(pEndpoint.getServiceName());
      }
      return result!=null;
    }
  }

  /**
   * <p>
   * {@inheritDoc} The implementation will look up the endpoint registered for
   * the destination of the message. Only when none has been registered will it
   * attempt to use the url for the message.
   * </p>
   * <p>
   * For registered endpoints if they implement {@link DirectEndpoint} the
   * message will be directly delivered to the endpoind through the
   * {@link DirectEndpoint#deliverMessage(ISendableMessage, CompletionListener, Class)
   * deliverMessage} method. Otherwhise if the endpoint implements
   * {@link Endpoint} the system will use reflection to directly invoke the
   * appropriate soap methods on the endpoint.
   * </p>
   */
  @Override
  public <T> Future<T> sendMessage(final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType, final Class<?>[] pReturnContext) {
    EndpointDescriptor registeredEndpoint = getEndpoint(pMessage.getDestination());

    if (registeredEndpoint instanceof DirectEndpoint) {
      return ((DirectEndpoint) registeredEndpoint).deliverMessage(pMessage, pCompletionListener, pReturnType);
    }

    if (registeredEndpoint instanceof Endpoint) { // Direct delivery when we don't just have a descriptor.
      if ("application/soap+xml".equals(pMessage.getBodySource().getContentType())) {
        final SoapMessageHandler handler = SoapMessageHandler.newInstance(registeredEndpoint);
        Source resultSource;
        try {
          resultSource = handler.processMessage(pMessage.getBodySource(), pMessage.getAttachments());
        } catch (final Exception e) {
          final Future<T> resultfuture = new MessageTask<>(e);
          if (pCompletionListener != null) {
            pCompletionListener.onMessageCompletion(resultfuture);
          }
          return resultfuture;
        }

        final MessageTask<T> resultfuture;
        if (pReturnType.isAssignableFrom(SourceDataSource.class)) {
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try {
            InputStreamOutputStream.writeToOutputStream(Sources.toInputStream(resultSource), baos);
          } catch (final IOException e) {
            throw new MessagingException(e);
          }
          resultfuture = new MessageTask<>(pReturnType.cast(new SourceDataSource("application/soap+xml", new StreamSource(new ByteArrayInputStream(baos.toByteArray())))));
        } else {
          final T resultval = SoapHelper.processResponse(pReturnType, pReturnContext, resultSource);
          resultfuture = new MessageTask<>(resultval);
        }

        //        resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
        if (pCompletionListener != null) {
          pCompletionListener.onMessageCompletion(resultfuture);
        }
        return resultfuture;
      }
    }

    if (registeredEndpoint == null) {
      registeredEndpoint = pMessage.getDestination();
    }

    final URI destURL;
    if (aLocalUrl == null) {
      destURL = registeredEndpoint.getEndpointLocation();
    } else {
      destURL = aLocalUrl.resolve(registeredEndpoint.getEndpointLocation());
    }

    final MessageTask<T> messageTask = new MessageTask<>(destURL, pMessage, pCompletionListener, pReturnType);
    aExecutor.execute(messageTask);
    return messageTask;
  }

  /**
   * Shut down the messenger. This will also unregister the messenger with the registry.
   */
  @Override
  public void shutdown() {
    MessagingRegistry.registerMessenger(null); // Unregister this messenger
    aNotifier.shutdown();
    aExecutor.shutdown();
    aServices = null;
  }

  /**
   * Method used internally (private is slower though) to notify completion of
   * tasks. This is part of the messenger as the messenger maintains a notifier
   * thread. As there is only one notifier thread, the handling of notifications
   * is expected to be fast.
   *
   * @param pFuture The Task whose completion to notify of.
   */
  void notifyCompletionListener(final MessageTask<?> pFuture) {
    aNotifier.addNotification(pFuture);
  }

  /**
   * Get the endpoint registered with the given service and endpoint name.
   * @param pServiceName The name of the service.
   * @param pEndpointName The name of the endpoint in the service.
   * @return
   */
  public EndpointDescriptor getEndpoint(final QName pServiceName, final String pEndpointName) {
    Map<String, EndpointDescriptor> service = aServices.get(pServiceName);
    if (service == null) {
      return null;
    }
    return service.get(pEndpointName);
  }

  /**
   * Get the endpoint registered for the given endpoint descriptor. This
   * @param pEndpoint The
   * @return
   */
  public EndpointDescriptor getEndpoint(final EndpointDescriptor pEndpoint) {
    final Map<String, EndpointDescriptor> service = aServices.get(pEndpoint.getServiceName());
    if (service == null) {
      return null;
    }

    return service.get(pEndpoint.getEndpointName());
  }

}
