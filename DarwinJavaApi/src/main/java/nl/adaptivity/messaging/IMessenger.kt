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
import java.util.concurrent.Future
import javax.xml.namespace.QName

/**
 * Interface indicating a class that can act as messenger in the
 * [MessagingRegistry]. Note that only one messenger can be registered at
 * the time.
 *
 * @author Paul de Vrieze
 */
interface IMessenger {
    /**
     * Register an endpoint.
     *
     * @param service The service to register.
     * @param endPoint The endpoint within the service.
     * @param target The url for that service.
     * @return An EndpointDescriptor that can be used to unregister the endpoint.
     */
    fun registerEndpoint(service: QName, endPoint: String, target: URI): EndpointDescriptor

    /**
     * Register an endpoint. This endpoint can be interpreted by the actual
     * messenger to provide a shortcut
     *
     * @param endpoint The endpoint to register
     */
    fun registerEndpoint(endpoint: EndpointDescriptor)

    /**
     * Send a message using the messenger. Sending is an asynchronous process and
     * a return from this method does not imply completion of the delivery (or
     * success).
     *
     * @param message The message to be sent.
     * @param completionListener The completionListener to use when the message
     * response is ready.
     * @param returnType The type of the return value of the sending.
     * @param returnTypeContext The jaxb context to be used when marshaling and
     * umarshaling the return value.
     * @return A future that can be used to retrieve the result of the sending.
     * This result will also be passed along to the completionListener.
     */
    fun <T> sendMessage(
        message: ISendableMessage,
        completionListener: CompletionListener<T>?,
        returnType: Class<out T?>,
        returnTypeContext: Array<out Class<*>> = emptyArray()
    ): Future<T>

    /**
     * Get a list of all the registered enpoints.
     *
     * @return The list of registered endpoints. This may return `null`
     * if the messenger does not support this. The default StubMessenger
     * for example returns `null`.
     */
    val registeredEndpoints: List<EndpointDescriptor>

    /**
     * Unregister the given endpoint
     * @param endpoint The endpoint to unregister
     * @return `true` on success, false when the endpoint was not registered.
     */
    fun unregisterEndpoint(endpoint: EndpointDescriptor): Boolean

    /**
     * Invoked when the messenger needs to release it's resources. After this has
     * been called the messenger should not accept and is not expected to accept
     * any new messages.
     */
    fun shutdown()
}
