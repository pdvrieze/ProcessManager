package nl.adaptivity.io;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;


/**
 * Created by pdvrieze on 19/11/15.
 */
public class WritableReader extends Reader {

  private Writable mContent;

  private Reader mDelegate;

  public WritableReader(final Writable content) {
    mContent = content;
  }

  private Reader getDelegate() throws IOException {
    if (mDelegate==null) {
      CharArrayWriter caw = new CharArrayWriter();
      mContent.writeTo(caw);
      mDelegate = new CharArrayReader(caw.toCharArray());
      mContent = null; // No longer needed, discard.
    }
    return mDelegate;
  }

  @Override
  public int read(final CharBuffer target) throws IOException {
    return getDelegate().read(target);
  }

  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(final char[] cbuf) throws IOException {
    return getDelegate().read(cbuf);
  }

  @Override
  public int read(final char[] cbuf, final int off, final int len) throws IOException {
    return getDelegate().read(cbuf, off, len);
  }

  @Override
  public long skip(final long n) throws IOException {
    return getDelegate().skip(n);
  }

  @Override
  public boolean ready() throws IOException {
    return getDelegate().ready();
  }

  @Override
  public boolean markSupported() {
    try {
      return getDelegate().markSupported();
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public void mark(final int readAheadLimit) throws IOException {
    getDelegate().mark(readAheadLimit);
  }

  @Override
  public void reset() throws IOException {
    getDelegate().reset();
  }

  @Override
  public void close() throws IOException {
    getDelegate().close();
  }
}
