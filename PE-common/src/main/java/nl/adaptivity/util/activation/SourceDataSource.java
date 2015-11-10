package nl.adaptivity.util.activation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.transform.Source;


public class SourceDataSource implements DataSource {

  private final String mContentType;

  private final Source mContent;

  public SourceDataSource(final String contentType, final Source content) {
    mContentType = contentType;
    mContent = content;
  }

  @Override
  public String getContentType() {
    return mContentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return Sources.toInputStream(mContent);
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
