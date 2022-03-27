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

import nl.adaptivity.io.Writable
import nl.adaptivity.messaging.ISendableMessage.IHeader
import java.io.Reader
import javax.activation.DataSource

/**
 * Interface signalling that a message can be sent by an [IMessenger]
 *
 * @author Paul de Vrieze
 */
interface ISendableMessage {
    /**
     * Class representing a simple HTTP header.
     *
     * @author Paul de Vrieze
     */
    interface IHeader {
        /**
         * Get the name of the header.
         *
         * @return The header name
         */
        val name: String

        /**
         * Get the value of the header.
         *
         * @return The header value
         */
        val value: String
    }

    val bodyReader: Reader

    /**
     * What should be the destination of the message.
     *
     * @return the url to open. Can be partial!
     */
    val destination: EndpointDescriptor

    /**
     * What method should be used for the message.
     *
     * @return `null` if default, otherwise the method (in uppercase)
     */
    val method: String?

    /** Get the headers needing to be set on the request.  */
    val headers: Collection<IHeader>

    /**
     * Get the source that represents the body of the message.
     *
     * @return The body of the message. Returns `null` if there is no
     * body. This is a DataSource as that will be used for the content
     * type unless overridden by a header returned by
     * [.getHeaders]
     */
    val bodySource: Writable?
    val attachments: Map<String, DataSource>
    val contentType: String
}
