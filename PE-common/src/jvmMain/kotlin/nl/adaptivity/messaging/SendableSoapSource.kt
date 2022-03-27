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

import nl.adaptivity.io.Writable
import nl.adaptivity.util.activation.toReader
import nl.adaptivity.util.activation.writeToWriter
import org.w3.soapEnvelope.Envelope
import java.io.IOException
import java.io.Reader
import java.io.Writer
import javax.activation.DataSource
import javax.xml.transform.Source


class SendableSoapSource @JvmOverloads constructor(
    override val destination: EndpointDescriptor,
    private val message: Source,
    override val attachments: Map<String, DataSource> = emptyMap()
                                                  ) : ISendableMessage, Writable {

    override val method: String?
        get() = null

    override val headers: Collection<ISendableMessage.IHeader>
        get() = emptyList()

    override val bodySource: Writable
        get() = this

    override val bodyReader: Reader
        get() = message.toReader()

    override val contentType: String
        get() = Envelope.MIMETYPE

    @Throws(IOException::class)
    override fun writeTo(destination: Writer) {
        message.writeToWriter(destination)
    }

}
