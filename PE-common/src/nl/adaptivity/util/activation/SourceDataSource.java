package nl.adaptivity.util.activation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;


public class SourceDataSource implements DataSource {

  private final String aContentType;
  private Source aContent;

  public SourceDataSource(String pContentType, Source pContent) {
    aContentType = pContentType;
    aContent = pContent;
  }

  @Override
  public String getContentType() {
    return aContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (aContent instanceof StreamSource) {
      return ((StreamSource) aContent).getInputStream();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      Sources.writeToStream(aContent, baos);
    } catch (TransformerException e) {
      throw new IOException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Can not write to sources");
  }

}
