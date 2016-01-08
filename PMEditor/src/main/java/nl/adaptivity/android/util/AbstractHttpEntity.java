package nl.adaptivity.android.util;

/**
 * Created by pdvrieze on 14/11/15.
 */
public abstract class AbstractHttpEntity implements HttpEntity {

  @Override
  public String getContentEncoding() {
    return null;
  }

  @Override
  public boolean isRepeatable() {
    return false;
  }

  @Override
  public boolean isStreaming() {
    return true;
  }

  @Override
  public long getContentLength() {
    return -1L;
  }
}
