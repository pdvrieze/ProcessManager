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

import org.w3.soapEnvelope.Envelope;


public class SendableSoapSource implements ISendableMessage, DataSource {

  private final EndpointDescriptor aDestination;

  private final Source aMessage;

  private final Map<String, DataSource> aAttachments;

  public SendableSoapSource(final EndpointDescriptor pDestination, final Source pMessage) {
    this(pDestination, pMessage, Collections.<String,DataSource>emptyMap());
  }

  public SendableSoapSource(final EndpointDescriptor pDestination, final Source pMessage, Map<String, DataSource> pAttachments) {
    aDestination = pDestination;
    aMessage = pMessage;
    aAttachments = pAttachments;
  }

  @Override
  public EndpointDescriptor getDestination() {
    return aDestination;
  }

  @Override
  public String getMethod() {
    return null;
  }

  @Override
  public Collection<? extends IHeader> getHeaders() {
    return Collections.emptyList();
  }

  @Override
  public DataSource getBodySource() {
    return this;
  }

  @Override
  public String getContentType() {
    return Envelope.MIMETYPE;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    final ByteArrayOutputStream boas = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(aMessage, boas);
    } catch (final TransformerException e) {
      throw new IOException(e);
    }
    return new ByteArrayInputStream(boas.toByteArray());
  }

  @Override
  public String getName() {
    return null; // No relevant name
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Map<String, DataSource> getAttachments() {
    return aAttachments;
  }

}
