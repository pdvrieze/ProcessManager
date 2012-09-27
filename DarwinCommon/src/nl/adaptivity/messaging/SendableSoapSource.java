package nl.adaptivity.messaging;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.w3.soapEnvelope.Envelope;

import nl.adaptivity.util.activation.Sources;


public class SendableSoapSource implements ISendableMessage, DataSource {

  private final Endpoint aDestination;
  private final Source aMessage;

  public SendableSoapSource(Endpoint pDestination, Source pMessage) {
    aDestination = pDestination;
    aMessage = pMessage;
    
  }
  
  @Override
  public Endpoint getDestination() {
    return aDestination;
  }

  @Override
  public String getMethod() {
    return null;
  }

  @Override
  public Collection<? extends Header> getHeaders() {
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
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(aMessage, boas);
    } catch (TransformerException e) {
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

}
