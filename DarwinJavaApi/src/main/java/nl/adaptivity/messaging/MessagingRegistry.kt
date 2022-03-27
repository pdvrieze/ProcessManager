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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
package nl.adaptivity.messaging

import java.net.URI
import java.util.*
import java.util.ArrayDeque
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import javax.xml.namespace.QName
import kotlin.concurrent.withLock

/**
 *
 *
 * This singleton class acts as the registry where a [ messenger][IMessenger] can be registered for handling messages. The usage of this central
 * registry allows for only the API project to be loaded globally into the
 * servlet container while the messenger is loaded by a specific context.
 *
 *
 * Messengers should be thread safe and provide reliable delivery (failure to
 * deliver must throw an exception).
 *
 *
 * This class will provide a temporary stub for any message sending attempts
 * made while no messenger has yet been registered. This stub will NOT however
 * attempt to deliver anything. The messages will be stored in memory and
 * forwarded on to a registered messenger when it is registered.
 *
 * @author Paul de Vrieze
 */
object MessagingRegistry {
    private var _messenger: IMessenger? = null

    /**
     * Get the messenger.
     *
     * @return The messenger to use to send messages. This will never return
     * `null`, even when no messenger has been registered (it
     * will create,register and return a stub that will queue up
     * messages).
     */
    val messenger: IMessenger
        get() = synchronized(this) {
            return _messenger ?: StubMessenger(null).also { _messenger = it }
        }

    /**
     * Register a messenger with the registry. You may not register a second
     * messenger, and this will throw. When a messenger needs to actually be
     * replaced the only valid option is to first invoke the method with
     * `null` to unregister the messenger, and then register a new one.
     *
     * @param messenger Pass `null` to unregister the current
     * messenger, otherwhise pass a messenger.
     */
    @Synchronized
    fun registerMessenger(messenger: IMessenger?) {
        if (messenger == null) {
            if (_messenger !is StubMessenger) {
                _messenger = StubMessenger(_messenger)
            }
            return
        } else {
            val oldMessenger = this._messenger
            if (oldMessenger is StubMessenger) {
                (oldMessenger as StubMessenger?)!!.flushTo(messenger)
            } else {
                check(oldMessenger == null) { "It is not allowed to register multiple messengers" }
            }
        }
        _messenger = messenger
        Logger.getAnonymousLogger().info("New messenger registered: ${messenger.javaClass.name}")
    }

    /**
     * Convenience method to send messages. This is equivalent to and invokes
     * [IMessenger.sendMessage]
     * .
     *
     * @param message The message to be sent.
     * @param completionListener The completionListener to use when the message
     * response is ready.
     * @param returnType The type of the return value of the sending.
     * @param returnTypeContext The types that need to be known for deserialization.
     * @return A future that can be used to retrieve the result of the sending.
     * This result will also be passed along to the completionListener.
     * @see IMessenger.sendMessage
     */
    fun <T> sendMessage(
        message: ISendableMessage,
        completionListener: CompletionListener<T>?,
        returnType: Class<*>,
        returnTypeContext: Array<out Class<*>>
    ): Future<T> {
        return messenger.sendMessage<T>(message, completionListener, returnType as Class<T>, returnTypeContext)
    }

    private class SimpleEndpointDescriptor(
        override val serviceName: QName?,
        override val endpointName: String?,
        override val endpointLocation: URI?
    ) : EndpointDescriptor {

        override fun isSameService(other: EndpointDescriptor?): Boolean {
            return serviceName == other!!.serviceName && endpointName == other.endpointName
        }
    }

    /**
     * A future class that makes StubMessenger work. It will basically fulfill the
     * future contract (including waiting for a specified amount of time) even
     * when a new messenger is registered.
     *
     * @author Paul de Vrieze
     * @param <T> The return value of the future.
    </T> */
    private class WrappingFuture<T>(
        private val message: ISendableMessage,
        private val completionListener: CompletionListener<T>?,
        private val returnType: Class<out T?>,
        private val returnTypeContext: Array<out Class<*>>
    ) : Future<T>, MessengerCommand, CompletionListener<T> {
        private val lock = ReentrantLock()
        private val lockCondition = lock.newCondition()
        private var origin: Future<T>? = null
        private var cancelled = false

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = lock.withLock {
            origin?.run { cancelled = cancel(mayInterruptIfRunning) } ?: run {
                cancelled = true
                completionListener?.onMessageCompletion(this)
            }
            return cancelled
        }

        override fun isCancelled(): Boolean = lock.withLock {
            return origin?.isCancelled ?: cancelled
        }

        override fun isDone(): Boolean = lock.withLock {
            return origin?.isDone ?: cancelled
        }

        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): T = lock.withLock {
            var o = origin
            while (o == null) {
                if (cancelled) {
                    throw CancellationException()
                }
                lockCondition.await()
                o = origin
            }
            return o.get()
        }

        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): T = lock.withLock {
            if (origin == null) {
                val startNanos = System.nanoTime()
                try {
                    lockCondition.await(timeout, unit)
                } catch (e: InterruptedException) {
                    if (origin != null) {
                        // Assume we are woken up because of the change not another interruption.
                        val currentNanos = System.nanoTime()
                        val nanosLeft = unit.toNanos(timeout) - (currentNanos - startNanos)
                        if (nanosLeft > 0) {
                            return origin!!.get(nanosLeft, TimeUnit.NANOSECONDS)
                        } else {
                            throw TimeoutException()
                        }
                    } else {
                        throw e
                    }
                }
            }
            if (cancelled) {
                throw CancellationException()
            } else {
                throw TimeoutException()
            }
        }

        override fun execute(messenger: IMessenger) = lock.withLock {
            if (!cancelled) {
                origin = messenger.sendMessage(message, this, returnType, returnTypeContext)
            }
            lockCondition.signalAll() // Wake up all waiters (should be only one)
        }

        override fun onMessageCompletion(future: Future<out T>) {
            completionListener?.onMessageCompletion(this)
        }
    }

    /**
     * A command that can be queued up by a stubmessenger for processing when the
     * new messenger is registered.
     *
     * @author Paul de Vrieze
     */
    private fun interface MessengerCommand {
        /**
         * Execute the command
         *
         * @param messenger The messenger to use. (this should be a real messenger,
         * not a stub).
         */
        fun execute(messenger: IMessenger)
    }

    /**
     * This messenger will only queue up commands to be executed (in order or
     * original reception) against a real messenger when it is registered. This is
     * a stopgap for timing issues, not a reliable long-term solution.
     *
     * @author Paul de Vrieze
     */
    private class StubMessenger internal constructor(oldMessenger: IMessenger?) : IMessenger {

        var commandQueue: Queue<MessengerCommand>? = ArrayDeque<MessengerCommand>().also { cq ->
            oldMessenger?.registeredEndpoints?.forEach { cq.add(RegisterEndpointCommand(it)) }
        }

        private val lock = ReentrantLock()

        private class RegisterEndpointCommand(
            private val endPoint: String?,
            private val service: QName?,
            private val target: URI?
        ) : MessengerCommand {

            constructor(endpoint: EndpointDescriptor) : this(
                endPoint = endpoint.endpointName,
                service = endpoint.serviceName,
                target = endpoint.endpointLocation
            )

            override fun execute(messenger: IMessenger) {
                messenger.registerEndpoint(service!!, endPoint!!, target!!)
            }

            val descriptor: EndpointDescriptor
                get() = SimpleEndpointDescriptor(service, endPoint, target)

            fun isEndpoint(other: EndpointDescriptor): Boolean {
                if (service == null) {
                    if (other.serviceName != null) {
                        return false
                    }
                } else {
                    if (service != other.serviceName) return false
                }
                if (endPoint == null) {
                    if (other.endpointName != null) {
                        return false
                    }
                } else {
                    if (endPoint != other.endpointName) return false
                }
                return if (target == null) {
                    other.serviceName == null
                } else {
                    service == other.serviceName
                }
            }
        }

        private inline fun <R> withCq(block: (Queue<MessengerCommand>) -> R): R = lock.withLock {
            val cq = commandQueue
            check(cq != null) { "Command queue is null, so forwarding mode is expected" }
            block(cq)
        }

        fun flushTo(messenger: IMessenger) = withCq { cq ->
            for (command in cq) {
                command.execute(messenger)
            }
            commandQueue = null // We don't need it anymore, we'll just forward.
        }

        override fun registerEndpoint(service: QName, endPoint: String, target: URI): EndpointDescriptor =
            withCq { cq ->
                cq.add(RegisterEndpointCommand(endPoint, service, target))
                return SimpleEndpointDescriptor(service, endPoint, target)
            }

        override fun registerEndpoint(endpoint: EndpointDescriptor): Unit = withCq { cq ->
            cq.add(MessengerCommand { messenger -> messenger.registerEndpoint(endpoint) })
        }

        override fun <T> sendMessage(
            message: ISendableMessage,
            completionListener: CompletionListener<T>?,
            returnType: Class<out T?>,
            returnTypeContext: Array<out Class<*>>
        ): Future<T> = withCq { cq ->
            WrappingFuture(message, completionListener, returnType, returnTypeContext).also { f ->
                cq.add(f)
            }
        }

        override fun shutdown() {
            System.err.println("Shutting down stub messenger. This should never happen. Do register an actual messenger!")
        }

        override val registeredEndpoints: List<EndpointDescriptor>
            get() = withCq { cq ->
                cq.filterIsInstance<RegisterEndpointCommand>()
                    .map(RegisterEndpointCommand::descriptor)
            }

        override fun unregisterEndpoint(endpoint: EndpointDescriptor): Boolean = withCq { cq ->
            val cqIterator = cq.iterator()
            while (cqIterator.hasNext()) {
                val command = cqIterator.next()
                if (command is RegisterEndpointCommand &&
                    command.isEndpoint(endpoint)) {
                    cqIterator.remove()
                    return true
                }
            }

            return false
        }
    }
}
