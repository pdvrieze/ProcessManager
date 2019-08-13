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
    private val destination: EndpointDescriptor,
    private val message: Source,
    private val attachments: Map<String, DataSource> = emptyMap()
                                                  ) : ISendableMessage, Writable {

    override fun getDestination(): EndpointDescriptor {
        return destination
    }

    override fun getMethod(): String? {
        return null
    }

    override fun getHeaders(): Collection<ISendableMessage.IHeader> {
        return emptyList()
    }

    override fun getBodySource(): Writable {
        return this
    }

    override fun getBodyReader(): Reader {
        return message.toReader()
    }

    override fun getContentType(): String {
        return Envelope.MIMETYPE
    }

    @Throws(IOException::class)
    override fun writeTo(destination: Writer) {
        message.writeToWriter(destination)
    }

    override fun getAttachments(): Map<String, DataSource> {
        return attachments
    }

}
