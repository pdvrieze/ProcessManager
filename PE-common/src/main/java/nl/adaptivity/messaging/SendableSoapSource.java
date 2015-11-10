package nl.adaptivity.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import nl.adaptivity.util.activation.Sources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3.soapEnvelope.Envelope;


public class SendableSoapSource implements ISendableMessage, DataSource {

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
  public DataSource getBodySource() {
    return this;
  }

  @Override
  public String getContentType() {
    return Envelope.MIMETYPE;
  }

  @NotNull
  @Override
  public InputStream getInputStream() throws IOException {
    final ByteArrayOutputStream boas = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(mMessage, boas);
    } catch (@NotNull final TransformerException e) {
      throw new IOException(e);
    }
    return new ByteArrayInputStream(boas.toByteArray());
  }

  @Nullable
  @Override
  public String getName() {
    return null; // No relevant name
  }

  @NotNull
  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Map<String, DataSource> getAttachments() {
    return mAttachments;
  }

}
