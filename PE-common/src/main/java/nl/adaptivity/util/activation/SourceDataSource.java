package nl.adaptivity.util.activation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.transform.Source;


public class SourceDataSource implements DataSource {

  private final String aContentType;

  private final Source aContent;

  public SourceDataSource(final String contentType, final Source content) {
    aContentType = contentType;
    aContent = content;
  }

  @Override
  public String getContentType() {
    return aContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return Sources.toInputStream(aContent);
  }

  @Nullable
  @Override
  public String getName() {
    return null;
  }

  @NotNull
  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Can not write to sources");
  }

}
