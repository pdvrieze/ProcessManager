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

package nl.adaptivity.messaging;

import nl.adaptivity.io.Writable;
import nl.adaptivity.util.activation.Sources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3.soapEnvelope.Envelope;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class SendableSoapSource implements ISendableMessage, Writable {

  private final EndpointDescriptor mDestination;

  private final Source mMessage;

  private final Map<String, DataSource> mAttachments;

  public SendableSoapSource(final EndpointDescriptor destination, final Source message) {
    this(destination, message, Collections.<String,DataSource>emptyMap());
  }

  public SendableSoapSource(final EndpointDescriptor destination, final Source message, final Map<String, DataSource> attachments) {
    mDestination = destination;
    mMessage = message;
    mAttachments = attachments;
  }

  @Override
  public EndpointDescriptor getDestination() {
    return mDestination;
  }

  @Nullable
  @Override
  public String getMethod() {
    return null;
  }

  @NotNull
  @Override
  public Collection<? extends IHeader> getHeaders() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Writable getBodySource() {
    return mMessage==null ? null : this;
  }

  public Reader getBodyReader() {
    return Sources.toReader(mMessage);
  }

  @Override
  public String getContentType() {
    return Envelope.MIMETYPE;
  }

  @Override
  public void writeTo(final Writer destination) throws IOException {
    try {
      Sources.writeToWriter(mMessage, destination);
    } catch (TransformerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Map<String, DataSource> getAttachments() {
    return mAttachments;
  }

}
