package nl.adaptivity.util.activation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.transform.Source;


public class SourceDataSource implements DataSource {

  private final String aContentType;

  private final Source aContent;

  public SourceDataSource(final String pContentType, final Source pContent) {
    aContentType = pContentType;
    aContent = pContent;
  }

  @Override
  public String getContentType() {
    return aContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return Sources.toInputStream(aContent);
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
