package nl.adaptivity.messaging;

import net.devrieze.util.InputStreamOutputStream;
import nl.adaptivity.io.Writable;
import nl.adaptivity.messaging.ISendableMessage.IHeader;
import nl.adaptivity.util.activation.SourceDataSource;
import nl.adaptivity.ws.soap.SoapHelper;
import nl.adaptivity.ws.soap.SoapMessageHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import java.io.*;
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
  private class MessageCompletionNotifier<T> extends Thread {

    /**
     * Queue containing the notifications still to be sent. This is internally
     * synchronized so doesn't need to be manually synchronized.
     */
    @NotNull private final BlockingQueue<MessageTask<T>> mPendingNotifications;

    private volatile boolean mFinished = false;


    /**
     * Create a new notifier.
     */
    public MessageCompletionNotifier() {
      super(NOTIFIERTHREADNAME);
      this.setDaemon(true); // This is just a helper thread, don't block cleanup.
      mPendingNotifications = new LinkedBlockingQueue<>(CONCURRENTCAPACITY);
    }

    /**
     * Simple message pump.
     */
    @Override
    public void run() {
      while (!mFinished) {
        try {
          final MessageTask<T> future = mPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future != null) {
            notififyCompletion(future);
          }
        } catch (@NotNull final InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    /**
     * Allow for shutting down the thread. As mFinished is volatile, this should
     * not need further synchronization.
     */
    public void shutdown() {
      mFinished = true;
      interrupt();
    }

    /**
     * Add a notification to the message queue.
     *
     * @param future The future whose completion should be communicated.
     */
    public void addNotification(final MessageTask<T> future) {
      // mPendingNotifications is threadsafe!
      mPendingNotifications.add(future);

    }

    /**
     * Helper method to notify of future completion.
     *
     * @param future The future to notify completion of.
     */
    private void notififyCompletion(@NotNull final MessageTask<T> future) {
      future.mCompletionListener.onMessageCompletion(future);
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
    private URI mDestURL;

    /** The message to send. */
    private ISendableMessage mMessage;

    /** The listener to notify of completion. */
    private CompletionListener<T> mCompletionListener;

    /** The result value. */
    @Nullable private T mResult = null;

    /** The cancellation state. */
    private boolean mCancelled = false;

    /** The response code given by the response. */
    private int mResponseCode;

    /** The exception in this future. */
    @Nullable private Exception mError = null;

    /** Set when the message sending is actually active. The processing of the future has started. */
    private boolean mStarted = false;

    /** The return type of the future. */
    @Nullable private final Class<T> mReturnType;

    /**
     * Create a new task.
     * @param destURL The url to invoke
     * @param message The message to send.
     * @param completionListener The listener to notify. This may be <code>null</code>.
     * @param returnType The return type of the message. Needed for unmarshalling.
     */
    public MessageTask(final URI destURL, final ISendableMessage message, final CompletionListener completionListener, final Class<T> returnType) {
      mDestURL = destURL;
      mMessage = message;
      mCompletionListener = completionListener;
      mReturnType = returnType;
    }

    /**
     * Simple constructor that creates a future encapsulating the exception
     *
     * @param e The exception to encapsulate.
     */
    public MessageTask(final Exception e) {
      mError = e;
      mReturnType = null;
    }

    /**
     * Create a future that just contains the value without computation/ waiting
     * possible. The result value. This is for returning synchronous values as
     * future.
     *
     * @param result The result value of the future.
     */
    @SuppressWarnings("unchecked")
    public MessageTask(@Nullable final T result) {
      if (result == null) {
        mResult = (T) NULL;
      } else {
        mResult = result;
      }
      mReturnType = null;
    }

    @Override
    public void run() {
      final boolean cancelled;
      synchronized (this) {
        mStarted = true;
        cancelled = mCancelled;
      }
      try {
        if (!cancelled) {
          final T result = sendMessage();
          synchronized (this) {
            if (result==null) {
              // Use separate value to allow for suppressing of warning.
              @SuppressWarnings("unchecked")
              final T v = (T) NULL;
              mResult = v;
            } else {
              mResult = result;
            }
          }
        }
      } catch (@NotNull final MessagingException e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        throw e;
      } catch (@NotNull final Exception e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        synchronized (this) {
          mError = e;
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
    private T sendMessage() throws IOException {
      final URL destination;

      try {
        destination = mDestURL.toURL();
      } catch (@NotNull final MalformedURLException e) {
        throw new MessagingException(e);
      }

      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection) {
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        final boolean hasPayload = mMessage.getBodySource() != null;
        connection.setDoOutput(hasPayload);
        String method = mMessage.getMethod();
        if (method == null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);

        boolean contenttypeset = false;
        for (final IHeader header : mMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        if (hasPayload && (!contenttypeset)) { // Set the content type from the source if not yet set.
          final String contentType = mMessage.getContentType();
          if ((contentType != null) && (contentType.length() > 0)) {
            httpConnection.addRequestProperty("Content-Type", contentType);
          }
        }
        try {
          httpConnection.connect();
        } catch (@NotNull final ConnectException e) {
          throw new MessagingException("Error connecting to " + destination, e);
        }
        try {
          if (hasPayload) {
            try(final OutputStream out = httpConnection.getOutputStream()) {
              Writer writer = new OutputStreamWriter(out, httpConnection.getContentEncoding());
              mMessage.getBodySource().writeTo(writer);
            }
          }
          mResponseCode = httpConnection.getResponseCode();
          if ((mResponseCode < 200) || (mResponseCode >= 400)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getErrorStream(), baos);
            final String errorMessage = "Error in sending message with " + method + " to (" + destination + ") [" + mResponseCode + "]:\n"
                + new String(baos.toByteArray());
            Logger.getLogger(DarwinMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          if (mReturnType.isAssignableFrom(SourceDataSource.class)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getInputStream(), baos);
            return mReturnType.cast(new SourceDataSource(httpConnection.getContentType(), new StreamSource(new ByteArrayInputStream(baos.toByteArray()))));
          } else {
            return JAXB.unmarshal(httpConnection.getInputStream(), mReturnType);
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
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
      if (mCancelled) {
        return true;
      }
      if (!mStarted) {
        mCancelled = true;
        return true;
      }
      // TODO support interrupt running process
      return false;
    }

    @Override
    public synchronized boolean isCancelled() {
      return mCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      return mCancelled || (mResult != null) || (mError != null);
    }

    @Nullable
    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      if (mCancelled) {
        throw new CancellationException();
      }
      if (mError != null) {
        throw new ExecutionException(mError);
      }
      if (mResult == NULL) {
        return null;
      }
      if (mResult != null)
      {
        return mResult;
      }
      wait();
      // wait for the result
      return mResult;
    }

    /**
     * {@inheritDoc} Note that there may be some inaccuracies in the waiting
     * time especially if the waiting started before the message delivery
     * started, but the timeout finished while the result was not yet in.
     */
    @Nullable
    @Override
    public synchronized T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      if (mCancelled) {
        throw new CancellationException();
      }
      if (mError != null) {
        throw new ExecutionException(mError);
      }
      if (mResult == NULL) {
        return null;
      }
      if (mResult != null) {
        return mResult;
      }
      if (timeout == 0) {
        throw new TimeoutException();
      }


      try {
        if (unit == TimeUnit.NANOSECONDS) {
          wait(unit.toMillis(timeout), (int) (unit.toNanos(timeout) % 1000000));
        } else {
          wait(unit.toMillis(timeout));
        }
      } catch (@NotNull final InterruptedException e) {
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
  private static final class MessengerHolder {

    static final DarwinMessenger _GlobalMessenger = new DarwinMessenger();
  }

  @NotNull private final ExecutorService mExecutor;

  @Nullable private ConcurrentMap<QName, ConcurrentMap<String, EndpointDescriptor>> mServices;

  @NotNull private final MessageCompletionNotifier mNotifier;

  private URI mLocalUrl;

  /**
   * Get the singleton instance. This also updates the base URL.
   *
   * @return The singleton instance.
   */
  public static void register() {
    MessagingRegistry.registerMessenger(MessengerHolder._GlobalMessenger);
  }

  /**
   * Create a new messenger. As the class is a singleton, this is only invoked
   * (indirectly) through {@link #register()}
   */
  private DarwinMessenger() {
    mExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    mNotifier = new MessageCompletionNotifier();
    mServices = new ConcurrentHashMap<>();
    mNotifier.start();

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
        mLocalUrl = URI.create(localUrl);
      } catch (@NotNull final IllegalArgumentException e) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "The given local url is not a valid uri.", e);
      }
    }

  }


  @NotNull
  @Override
  public EndpointDescriptor registerEndpoint(final QName service, final String endPoint, final URI target) {
    final EndpointDescriptorImpl endpoint = new EndpointDescriptorImpl(service, endPoint, target);
    registerEndpoint(endpoint);
    return endpoint;
  }


  @Override
  public synchronized void registerEndpoint(@NotNull final EndpointDescriptor endpoint) {
    // Note that even though it's a concurrent map we still need to synchronize to
    // prevent race conditions with multiple registrations.
    ConcurrentMap<String, EndpointDescriptor> service = mServices.get(endpoint.getServiceName());
    if (service == null) {
      service = new ConcurrentHashMap<>();
      mServices.put(endpoint.getServiceName(), service);
    }
    if (service.containsKey(endpoint.getEndpointName())) {
      service.remove(endpoint.getEndpointName());
    }
    service.put(endpoint.getEndpointName(), endpoint);
  }

  @NotNull
  @Override
  public List<EndpointDescriptor> getRegisteredEndpoints() {
    final ArrayList<EndpointDescriptor> result = new ArrayList<>();
    synchronized (mServices) {
      for (final ConcurrentMap<String, EndpointDescriptor> service: mServices.values()) {
        for(final EndpointDescriptor endpoint:service.values()) {
          result.add(endpoint);
        }
      }
    }
    return result;
  }

  @Override
  public boolean unregisterEndpoint(@NotNull final EndpointDescriptor endpoint) {
    synchronized (mServices) {
      final ConcurrentMap<String, EndpointDescriptor> service = mServices.get(endpoint.getServiceName());
      if (service==null) { return false; }
      final EndpointDescriptor result = service.remove(endpoint.getEndpointName());
      if (service.isEmpty()) {
        mServices.remove(endpoint.getServiceName());
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
  @Nullable
  @Override
  public <T> Future<T> sendMessage(@NotNull final ISendableMessage message, @Nullable final CompletionListener<T> completionListener, @NotNull final Class<T> returnType, final Class<?>[] returnContext) {
    EndpointDescriptor registeredEndpoint = getEndpoint(message.getDestination());

    if (registeredEndpoint instanceof DirectEndpoint) {
      return ((DirectEndpoint) registeredEndpoint).deliverMessage(message, completionListener, returnType);
    }

    if (registeredEndpoint instanceof Endpoint) { // Direct delivery when we don't just have a descriptor.
      if ("application/soap+xml".equals(message.getContentType())) {
        final SoapMessageHandler handler = SoapMessageHandler.newInstance(registeredEndpoint);
        final Source resultSource2;
        Writable writable = message.getBodySource();

        final MessageTask<T> resultfuture;
        if (returnType.isAssignableFrom(SourceDataSource.class)) {
          final CharArrayWriter caw = new CharArrayWriter();
          try {
            writable.writeTo(caw);
          } catch (@NotNull final IOException e) {
            throw new MessagingException(e);
          }
          resultfuture = new MessageTask<>(returnType.cast(new SourceDataSource("application/soap+xml", new StreamSource(new CharArrayReader(caw.toCharArray())))));
        } else {
          final T resultval = SoapHelper.processResponse(returnType, returnContext, writable);
          resultfuture = new MessageTask<>(resultval);
        }

        //        resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
        if (completionListener != null) {
          completionListener.onMessageCompletion(resultfuture);
        }
        return resultfuture;
      }
    }

    if (registeredEndpoint == null) {
      registeredEndpoint = message.getDestination();
    }

    final URI destURL;
    if (mLocalUrl == null) {
      destURL = registeredEndpoint.getEndpointLocation();
    } else {
      destURL = mLocalUrl.resolve(registeredEndpoint.getEndpointLocation());
    }

    final MessageTask<T> messageTask = new MessageTask<>(destURL, message, completionListener, returnType);
    mExecutor.execute(messageTask);
    return messageTask;
  }

  /**
   * Shut down the messenger. This will also unregister the messenger with the registry.
   */
  @Override
  public void shutdown() {
    MessagingRegistry.registerMessenger(null); // Unregister this messenger
    mNotifier.shutdown();
    mExecutor.shutdown();
    mServices = null;
  }

  /**
   * Method used internally (private is slower though) to notify completion of
   * tasks. This is part of the messenger as the messenger maintains a notifier
   * thread. As there is only one notifier thread, the handling of notifications
   * is expected to be fast.
   *
   * @param future The Task whose completion to notify of.
   */
  void notifyCompletionListener(final MessageTask<?> future) {
    mNotifier.addNotification(future);
  }

  /**
   * Get the endpoint registered with the given service and endpoint name.
   * @param serviceName The name of the service.
   * @param endpointName The name of the endpoint in the service.
   * @return
   */
  @Nullable
  public EndpointDescriptor getEndpoint(final QName serviceName, final String endpointName) {
    final Map<String, EndpointDescriptor> service = mServices.get(serviceName);
    if (service == null) {
      return null;
    }
    return service.get(endpointName);
  }

  /**
   * Get the endpoint registered for the given endpoint descriptor. This
   * @param endpoint The
   * @return
   */
  @Nullable
  public EndpointDescriptor getEndpoint(@NotNull final EndpointDescriptor endpoint) {
    final Map<String, EndpointDescriptor> service = mServices.get(endpoint.getServiceName());
    if (service == null) {
      return null;
    }

    return service.get(endpoint.getEndpointName());
  }

}
