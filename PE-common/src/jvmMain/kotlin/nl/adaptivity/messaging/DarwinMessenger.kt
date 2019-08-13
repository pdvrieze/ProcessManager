/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.messaging

import net.devrieze.util.InputStreamOutputStream
import nl.adaptivity.util.activation.SourceDataSource
import nl.adaptivity.ws.soap.SoapHelper
import nl.adaptivity.ws.soap.SoapMessageHandler

import javax.xml.bind.JAXB
import javax.xml.namespace.QName
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import java.io.*
import java.net.*
import java.util.ArrayList
import java.util.concurrent.*
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Messenger to use in the darwin project.
 *
 * @author Paul de Vrieze
 */
class DarwinMessenger
/**
 * Create a new messenger. As the class is a singleton, this is only invoked
 * (indirectly) through [.register]
 */
private constructor() : IMessenger {

    private val executor: ExecutorService

    private var services: ConcurrentMap<QName, ConcurrentMap<String, EndpointDescriptor>>

    private val notifier: MessageCompletionNotifier<*>

    private var mLocalUrl: URI? = null

    /**
     * Helper thread that performs (in a single tread) all notifications of
     * messaging completions. The notification can not be done on the sending
     * thread (deadlocks as that thread would be waiting for itself) and the
     * calling tread is unknown.
     *
     * @author Paul de Vrieze
     */
    private inner class MessageCompletionNotifier<T> : Thread(NOTIFIERTHREADNAME) {

        /**
         * Queue containing the notifications still to be sent. This is internally
         * synchronized so doesn't need to be manually synchronized.
         */
        private val mPendingNotifications: BlockingQueue<MessageTask<T>>

        @Volatile
        private var mFinished = false

        /**
         * Create a new notifier.
         */
        init {
            this.isDaemon = true // This is just a helper thread, don't block cleanup.
            mPendingNotifications = LinkedBlockingQueue(CONCURRENTCAPACITY)
        }

        /**
         * Simple message pump.
         */
        override fun run() {
            while (!mFinished) {
                try {
                    val future = mPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS)
                    if (future != null) {
                        notififyCompletion(future)
                    }
                } catch (e: InterruptedException) {
                    // Ignore the interruption. Just continue
                }

            }

        }

        /**
         * Allow for shutting down the thread. As mFinished is volatile, this should
         * not need further synchronization.
         */
        fun shutdown() {
            mFinished = true
            interrupt()
        }

        /**
         * Add a notification to the message queue.
         *
         * @param future The future whose completion should be communicated.
         */
        fun addNotification(future: MessageTask<T>) {
            // mPendingNotifications is threadsafe!
            mPendingNotifications.add(future)

        }

        /**
         * Helper method to notify of future completion.
         *
         * @param future The future to notify completion of.
         */
        private fun notififyCompletion(future: MessageTask<T>) {
            future.completionListener?.onMessageCompletion(future)
        }


    }

    /**
     * Future that encapsulates a future that represents the sending of a message.
     * This is a message that
     *
     * @author Paul de Vrieze
     * @param T
     */
    internal inner class MessageTask<T> : RunnableFuture<T> {

        /** The uri to use for sending the message.  */
        private var destURL: URI? = null

        /** The message to send.  */
        private var message: ISendableMessage? = null

        /** The listener to notify of completion.  */
        var completionListener: CompletionListener<T>? = null

        /** The result value.  */
        private var result: T? = null

        /** The cancellation state.  */
        private var cancelled = false

        /** The response code given by the response.  */
        private var responseCode: Int = 0

        /** The exception in this future.  */
        private var error: Exception? = null

        /** Set when the message sending is actually active. The processing of the future has started.  */
        private var started = false

        /** The return type of the future.  */
        private val returnType: Class<T>?

        /**
         * Create a new task.
         * @param destURL The url to invoke
         * @param message The message to send.
         * @param completionListener The listener to notify. This may be `null`.
         * @param returnType The return type of the message. Needed for unmarshalling.
         */
        constructor(
            destURL: URI,
            message: ISendableMessage,
            completionListener: CompletionListener<T>,
            returnType: Class<T>
                   ) {
            this.destURL = destURL
            this.message = message
            this.completionListener = completionListener
            this.returnType = returnType
        }

        /**
         * Simple constructor that creates a future encapsulating the exception
         *
         * @param e The exception to encapsulate.
         */
        constructor(e: Exception) {
            error = e
            returnType = null
        }

        /**
         * Create a future that just contains the value without computation/ waiting
         * possible. The result value. This is for returning synchronous values as
         * future.
         *
         * @param result The result value of the future.
         */
        constructor(result: T?) {
            if (result == null) {
                this.result = NULL as T
            } else {
                this.result = result
            }
            returnType = null
        }

        override fun run() {
            val cancelled: Boolean
            synchronized(this) {
                started = true
                cancelled = this.cancelled
            }
            try {
                if (!cancelled) {
                    val result = sendMessage()
                    synchronized(this) {
                        if (result == null) {
                            // Use separate value to allow for suppressing of warning.
                            val v = NULL as T
                            this.result = v
                        } else {
                            this.result = result
                        }
                    }
                }
            } catch (e: MessagingException) {
                Logger.getLogger(DarwinMessenger::class.java.name).log(Level.WARNING, "Error sending message", e)
                throw e
            } catch (e: Exception) {
                Logger.getLogger(DarwinMessenger::class.java.name).log(Level.WARNING, "Error sending message", e)
                synchronized(this) {
                    error = e
                }
            } finally {
                notifyCompletionListener(this)
            }
        }

        /**
         * This method performs the actual sending of the message.
         * @return The return value of the message.
         * @throws IOException
         * @throws ProtocolException
         */
        @Throws(IOException::class)
        private fun sendMessage(): T? {
            val destination: URL

            try {
                destination = destURL!!.toURL()
            } catch (e: MalformedURLException) {
                throw MessagingException(e)
            }

            val connection = destination.openConnection()
            if (connection is HttpURLConnection) {
                val message = message!!
                val hasPayload = message.bodySource != null
                connection.setDoOutput(hasPayload)
                var method: String? = message.method
                if (method == null) {
                    method = if (hasPayload) "POST" else "GET"
                }
                connection.requestMethod = method

                var contenttypeset = false
                for (header in message.headers) {
                    connection.addRequestProperty(header.name, header.value)
                    contenttypeset = contenttypeset or ("Content-Type" == header.name)
                }
                if (hasPayload && !contenttypeset) { // Set the content type from the source if not yet set.
                    val contentType = message.contentType
                    if (contentType != null && contentType.length > 0) {
                        connection.addRequestProperty("Content-Type", contentType)
                    }
                }
                try {
                    if (hasPayload) {
                        connection.setRequestProperty("content-type", message.contentType + "; charset=UTF-8")
                        connection.outputStream.use { out ->
                            val writer = OutputStreamWriter(out, "UTF-8")
                            message.bodySource.writeTo(writer)
                            writer.close()
                        }
                    }
                    connection.connect()
                } catch (e: ConnectException) {
                    throw MessagingException("Error connecting to $destination", e)
                }

                try {
                    responseCode = connection.responseCode
                    if (responseCode < 200 || responseCode >= 400) {
                        val baos = ByteArrayOutputStream()
                        InputStreamOutputStream.writeToOutputStream(connection.errorStream, baos)
                        val errorMessage =
                            ("Error in sending message with $method to ($destination) [$responseCode]:\n${String(
                                baos.toByteArray()
                                                                                                                )}")
                        Logger.getLogger(DarwinMessenger::class.java.name).info(errorMessage)
                        throw HttpResponseException(connection.responseCode, errorMessage)
                    }
                    if (returnType!!.isAssignableFrom(SourceDataSource::class.java)) {
                        val baos = ByteArrayOutputStream()
                        InputStreamOutputStream.writeToOutputStream(connection.inputStream, baos)
                        return returnType.cast(
                            SourceDataSource(
                                connection.contentType, StreamSource(
                                    ByteArrayInputStream(baos.toByteArray())
                                                                    )
                                            )
                                              )
                    } else {
                        return JAXB.unmarshal(connection.inputStream, returnType)
                    }

                } finally {
                    connection.disconnect()
                }

            } else {
                throw UnsupportedOperationException("No support yet for non-http connections")
            }
        }

        /**
         * Cancel the performance of this task. Currently will never actually honour
         * the parameter and will never interrupt after the sending started.
         */
        @Synchronized
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            if (cancelled) {
                return true
            }
            if (!started) {
                cancelled = true
                return true
            }
            return false
        }

        @Synchronized
        override fun isCancelled(): Boolean {
            return cancelled
        }

        @Synchronized
        override fun isDone(): Boolean {
            return cancelled || result != null || error != null
        }

        @Synchronized
        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): T? {
            if (cancelled) {
                throw CancellationException()
            }
            if (error != null) {
                throw ExecutionException(error)
            }
            if (result === NULL) {
                return null
            }
            if (result != null) {
                return result
            }

            wait()
            // wait for the result
            return result
        }

        /**
         * {@inheritDoc} Note that there may be some inaccuracies in the waiting
         * time especially if the waiting started before the message delivery
         * started, but the timeout finished while the result was not yet in.
         */
        @Synchronized
        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): T? {
            if (cancelled) {
                throw CancellationException()
            }
            if (error != null) {
                throw ExecutionException(error)
            }
            if (result === NULL) {
                return null
            }
            if (result != null) {
                return result
            }
            if (timeout == 0L) {
                throw TimeoutException()
            }


            try {
                if (unit === TimeUnit.NANOSECONDS) {
                    wait(unit.toMillis(timeout), (unit.toNanos(timeout) % 1000000).toInt())
                } else {
                    wait(unit.toMillis(timeout))
                }
            } catch (e: InterruptedException) {
                return if (isDone) {
                    get(0, TimeUnit.MILLISECONDS)// Don't wait, even if somehow the state is wrong.
                } else {
                    throw e
                }
            }

            throw TimeoutException()
        }

    }

    /**
     * Let the class loader do the nasty synchronization for us, but still
     * initialise ondemand.
     */
    private object MessengerHolder {

        internal val _GlobalMessenger = DarwinMessenger()
    }

    init {
        executor = ThreadPoolExecutor(
            INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS.toLong(),
            TimeUnit.MILLISECONDS, ArrayBlockingQueue(CONCURRENTCAPACITY, true)
                                     )
        notifier = MessageCompletionNotifier<Any?>()
        services = ConcurrentHashMap()
        notifier.start()

        val localUrl = System.getProperty("nl.adaptivity.messaging.localurl")

        if (localUrl == null) {
            val msg = StringBuilder()
            msg.append(
                "DarwinMessenger\n" + "------------------------------------------------\n" + "                    WARNING\n"
                    + "------------------------------------------------\n" + "  Please set the nl.adaptivity.messaging.localurl property in\n"
                    + "  catalina.properties (or a method appropriate for a non-tomcat\n"
                    + "  container) to the base url used to contact the messenger by\n"
                    + "  other components of the system. The public base url can be set as:\n"
                    + "  nl.adaptivity.messaging.baseurl, this should be accessible by\n" + "  all clients of the system.\n"
                    + "================================================"
                      )
            Logger.getAnonymousLogger().warning(msg.toString())
        } else {
            try {
                mLocalUrl = URI.create(localUrl)
            } catch (e: IllegalArgumentException) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "The given local url is not a valid uri.", e)
            }

        }

    }


    override fun registerEndpoint(service: QName, endPoint: String, target: URI): EndpointDescriptor {
        val endpoint = EndpointDescriptorImpl(service, endPoint, target)
        registerEndpoint(endpoint)
        return endpoint
    }


    @Synchronized
    override fun registerEndpoint(endpoint: EndpointDescriptor) {
        // Note that even though it's a concurrent map we still need to synchronize to
        // prevent race conditions with multiple registrations.
        var service: ConcurrentMap<String, EndpointDescriptor>? = services!![endpoint.serviceName]
        if (service == null) {
            service = ConcurrentHashMap()
            services!![endpoint.serviceName] = service
        }
        if (service.containsKey(endpoint.endpointName)) {
            service.remove(endpoint.endpointName)
        }
        service[endpoint.endpointName] = endpoint
    }

    override fun getRegisteredEndpoints(): List<EndpointDescriptor> {
        val result = ArrayList<EndpointDescriptor>()
        synchronized(services!!) {
            for (service in services!!.values) {
                for (endpoint in service.values) {
                    result.add(endpoint)
                }
            }
        }
        return result
    }

    override fun unregisterEndpoint(endpoint: EndpointDescriptor): Boolean {
        synchronized(services!!) {
            val service = services!![endpoint.serviceName] ?: return false
            val result = service.remove(endpoint.endpointName)
            if (service.isEmpty()) {
                services!!.remove(endpoint.serviceName)
            }
            return result != null
        }
    }

    /**
     *
     *
     * {@inheritDoc} The implementation will look up the endpoint registered for
     * the destination of the message. Only when none has been registered will it
     * attempt to use the url for the message.
     *
     *
     *
     * For registered endpoints if they implement [DirectEndpoint] the
     * message will be directly delivered to the endpoind through the
     * [ deliverMessage][DirectEndpoint.deliverMessage] method. Otherwhise if the endpoint implements
     * [Endpoint] the system will use reflection to directly invoke the
     * appropriate soap methods on the endpoint.
     *
     */
    override fun <T> sendMessage(
        message: ISendableMessage,
        completionListener: CompletionListener<T>,
        returnType: Class<T>,
        returnContext: Array<Class<*>>
                                ): Future<T>? {
        var registeredEndpoint = getEndpoint(message.destination)

        if (registeredEndpoint is DirectEndpoint) {
            return registeredEndpoint.deliverMessage(message, completionListener, returnType)
        }

        if (registeredEndpoint is Endpoint) { // Direct delivery when we don't just have a descriptor.
            if ("application/soap+xml" == message.contentType) {
                val handler = SoapMessageHandler.newInstance(registeredEndpoint)
                val resultSource: Source
                try {
                    resultSource = handler.processMessage(message.bodyReader, message.attachments)

                    val resultfuture: MessageTask<T>
                    if (returnType.isAssignableFrom(SourceDataSource::class.java)) {
                        resultfuture = MessageTask(
                            returnType.cast(SourceDataSource("application/soap+xml", resultSource))
                                                  )
                    } else {
                        val resultval = SoapHelper.processResponse(returnType, returnContext, null!!, resultSource)
                        resultfuture = MessageTask(resultval)
                    }

                    //        resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
                    completionListener?.onMessageCompletion(resultfuture)
                    return resultfuture
                } catch (e: Exception) {
                    return MessageTask(e)
                }

            }
        }

        if (registeredEndpoint == null) {
            registeredEndpoint = message.destination
        }

        val destURL: URI
        if (mLocalUrl == null) {
            destURL = registeredEndpoint!!.endpointLocation!!
        } else {
            val endpointLocation = registeredEndpoint!!.endpointLocation ?: return MessageTask(
                NullPointerException("No endpoint location specified, and the service could not be found")
                                                                                              )
            destURL = mLocalUrl!!.resolve(endpointLocation)
        }

        val messageTask = MessageTask<T>(destURL, message, completionListener, returnType)
        executor.execute(messageTask)
        return messageTask
    }

    /**
     * Shut down the messenger. This will also unregister the messenger with the registry.
     */
    override fun shutdown() {
        MessagingRegistry.registerMessenger(null) // Unregister this messenger
        notifier.shutdown()
        executor.shutdown()
        services = ConcurrentHashMap()
    }

    /**
     * Method used internally (private is slower though) to notify completion of
     * tasks. This is part of the messenger as the messenger maintains a notifier
     * thread. As there is only one notifier thread, the handling of notifications
     * is expected to be fast.
     *
     * @param future The Task whose completion to notify of.
     */
    internal fun <T> notifyCompletionListener(future: MessageTask<T>) {
        (notifier as MessageCompletionNotifier<T>).addNotification(future)
    }

    /**
     * Get the endpoint registered with the given service and endpoint name.
     * @param serviceName The name of the service.
     * @param endpointName The name of the endpoint in the service.
     * @return
     */
    fun getEndpoint(serviceName: QName, endpointName: String): EndpointDescriptor? {
        val service = services!![serviceName] ?: return null
        return service[endpointName]
    }

    /**
     * Get the endpoint registered for the given endpoint descriptor. This
     * @param endpoint The
     * @return
     */
    fun getEndpoint(endpoint: EndpointDescriptor): EndpointDescriptor? {
        val service = services!![endpoint.serviceName] ?: return null

        return service[endpoint.endpointName]
    }

    companion object {

        /**
         * How big should the worker thread pool be initially.
         */
        private val INITIAL_WORK_THREADS = 1

        /**
         * How many worker threads are there concurrently? Note that extra work will
         * not block, it will just be added to a waiting queue.
         */
        private val MAXIMUM_WORK_THREADS = 20

        /**
         * How long to keep idle worker threads busy (in miliseconds).
         */
        private val WORKER_KEEPALIVE_MS = 60000

        /**
         * How many queued messages should be allowed. This is also the limit of
         * pending notifications.
         */
        private val CONCURRENTCAPACITY = 2048 // Allow 2048 pending messages

        /** The name of the notification tread.  */
        private val NOTIFIERTHREADNAME = DarwinMessenger::class.java.name + " - Completion Notifier"

        /**
         * How long should the notification thread wait when polling messages. This
         * should ensure that every 30 seconds it checks whether it's finished.
         */
        private val NOTIFICATIONPOLLTIMEOUTMS = 30000L // Timeout polling for next message every 30 seconds


        /**
         * Marker object for null results.
         */
        private val NULL = Any()

        /**
         * Get the singleton instance. This also updates the base URL.
         *
         * @return The singleton instance.
         */
        fun register() {
            MessagingRegistry.registerMessenger(MessengerHolder._GlobalMessenger)
        }
    }

}


private inline fun DarwinMessenger.wait() {
    (this as java.lang.Object).wait()
}

private inline fun DarwinMessenger.wait(timeout: Long) {
    (this as java.lang.Object).wait(timeout)
}

private inline fun DarwinMessenger.wait(timeout: Long, nanos: Int) {
    (this as java.lang.Object).wait(timeout, nanos)
}
