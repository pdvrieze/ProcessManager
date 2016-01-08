package nl.adaptivity.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by pdvrieze on 14/11/15.
 */
public interface HttpEntity {

  boolean isStreaming();

  void writeTo(OutputStream outstream) throws IOException;

  InputStream getContent() throws IOException, IllegalStateException;

  String getContentType();

  void setContentType(String contentType);

  long getContentLength();

  boolean isRepeatable();

  String getContentEncoding();
}
